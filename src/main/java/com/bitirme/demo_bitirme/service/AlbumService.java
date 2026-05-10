package com.bitirme.demo_bitirme.service;

import com.bitirme.demo_bitirme.data.dto.AlbumDTO;
import com.bitirme.demo_bitirme.data.dto.AlbumPhotoDTO;
import com.bitirme.demo_bitirme.data.entity.*;
import com.bitirme.demo_bitirme.data.mapper.AlbumMapper;
import com.bitirme.demo_bitirme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final AlbumCollaboratorRepository collaboratorRepository;
    private final AlbumPhotoRepository albumPhotoRepository;
    private final PhotoRepository photoRepository;
    private final AppUserRepository appUserRepository;
    private final AlbumMapper albumMapper;
    private final UserService userService;
    private final PhotoService photoService;
    private final PythonMLService pythonMLService;

    // ─── Album CRUD ──────────────────────────────────────────

    public AlbumDTO createAlbum(String name, String description, String albumType, Long userId) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Album name is required");
        }
        Album.AlbumType type = Album.AlbumType.valueOf(
                (albumType == null ? "PRIVATE" : albumType).toUpperCase());

        Album album = new Album();
        album.setName(name.trim());
        album.setDescription(description);
        album.setAlbumType(type);
        album.setOwner(userService.requireUser(userId));
        Album saved = albumRepository.save(album);
        log.info("User {} created {} album '{}' (id={})", userId, type, name, saved.getId());
        return albumMapper.toDTO(saved);
    }

    public List<AlbumDTO> listVisibleAlbums(Long userId) {
        return albumRepository.findAllVisibleToUser(userId).stream()
                .map(albumMapper::toDTO)
                .toList();
    }

    public AlbumDTO getAlbum(Long albumId, Long userId) {
        Album album = requireVisible(albumId, userId);
        return albumMapper.toDTO(album);
    }

    @Transactional
    public void deleteAlbum(Long albumId, Long userId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
        if (!album.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can delete an album");
        }
        albumRepository.delete(album);
        log.info("User {} deleted album {}", userId, albumId);
    }

    public AlbumDTO renameAlbum(Long albumId, String newName, String newDescription, Long userId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
        if (!album.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can rename an album");
        }
        if (newName != null && !newName.isBlank()) album.setName(newName.trim());
        if (newDescription != null) album.setDescription(newDescription);
        return albumMapper.toDTO(albumRepository.save(album));
    }

    // ─── Collaborators ───────────────────────────────────────

    @Transactional
    public AlbumDTO addCollaborator(Long albumId, String username, Long userId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
        if (!album.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can add collaborators");
        }
        if (album.getAlbumType() != Album.AlbumType.COLLABORATIVE) {
            // Promote to collaborative automatically
            album.setAlbumType(Album.AlbumType.COLLABORATIVE);
        }
        AppUser collaborator = userService.requireByUsername(username);
        if (collaborator.getId().equals(album.getOwner().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner is implicitly a collaborator");
        }
        if (collaboratorRepository.existsByAlbumIdAndUserId(albumId, collaborator.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a collaborator");
        }
        AlbumCollaborator ac = new AlbumCollaborator();
        ac.setAlbum(album);
        ac.setUser(collaborator);
        collaboratorRepository.save(ac);
        log.info("User {} added {} as collaborator on album {}", userId, collaborator.getUsername(), albumId);

        // Trigger ML for the new collaborator on all photos already in the album
        // that they don't own (their own photos were already processed at upload time).
        List<AlbumPhoto> existingPhotos = albumPhotoRepository.findByAlbumIdOrderByAddedAtDesc(albumId);
        for (AlbumPhoto existingAp : existingPhotos) {
            Photo existingPhoto = existingAp.getPhoto();
            if (!existingPhoto.getOwner().getId().equals(collaborator.getId())) {
                log.info("Triggering ML for new collaborator {} on existing photo {} in album {}",
                        collaborator.getId(), existingPhoto.getId(), albumId);
                pythonMLService.analyzePhotoAsync(
                        existingPhoto.getId(), existingPhoto.getFilePath(), collaborator.getId());
            }
        }

        return albumMapper.toDTO(albumRepository.findById(albumId).orElseThrow());
    }

    /**
     * Leave an album (remove self from collaborators), OR owner removes a collaborator.
     */
    @Transactional
    public void removeCollaborator(Long albumId, Long collaboratorUserId, Long userId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
        boolean isOwner = album.getOwner().getId().equals(userId);
        boolean isSelfLeave = collaboratorUserId.equals(userId);
        if (!isOwner && !isSelfLeave) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the owner can remove others; you can only leave on your own");
        }
        AlbumCollaborator ac = collaboratorRepository
                .findByAlbumIdAndUserId(albumId, collaboratorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collaborator not found"));
        collaboratorRepository.delete(ac);
        log.info("User {} removed from collaborators of album {} (by {})", collaboratorUserId, albumId, userId);
    }

    // ─── Album photos ────────────────────────────────────────

    public List<AlbumPhotoDTO> listAlbumPhotos(Long albumId, Long userId) {
        requireVisible(albumId, userId);
        List<AlbumPhoto> aps = albumPhotoRepository.findByAlbumIdOrderByAddedAtDesc(albumId);
        return aps.stream().map(ap -> {
            AlbumPhotoDTO dto = albumMapper.toAlbumPhotoDTO(ap);
            // Each photo's tag list is filtered to the caller's tags only
            dto.setPhoto(photoService.toDTOForUser(ap.getPhoto(), userId));
            return dto;
        }).toList();
    }

    @Transactional
    public AlbumPhotoDTO addPhotoToAlbum(Long albumId, Long photoId, Long userId) {
        Album album = requireVisible(albumId, userId);
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
        // Only the photo's owner can add it (so you can't put someone else's photo in an album)
        if (!photo.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only add your own photos to an album");
        }
        if (albumPhotoRepository.existsByAlbumIdAndPhotoId(albumId, photoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Photo already in this album");
        }
        AlbumPhoto ap = new AlbumPhoto();
        ap.setAlbum(album);
        ap.setPhoto(photo);
        ap.setAddedBy(userService.requireUser(userId));
        AlbumPhoto saved = albumPhotoRepository.save(ap);
        log.info("User {} added photo {} to album {}", userId, photoId, albumId);

        // For collaborative albums: the adder has already had this photo processed
        // (it's their own photo). Fire ML analysis for every other album member so
        // they each get their own face/scene tags on this photo.
        if (album.getAlbumType() == Album.AlbumType.COLLABORATIVE) {
            triggerMLForOtherMembers(album, photo, userId);
        }

        AlbumPhotoDTO dto = albumMapper.toAlbumPhotoDTO(saved);
        dto.setPhoto(photoService.toDTOForUser(saved.getPhoto(), userId));
        return dto;
    }

    @Transactional
    public void removePhotoFromAlbum(Long albumId, Long albumPhotoId, Long userId) {
        AlbumPhoto ap = albumPhotoRepository.findById(albumPhotoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album photo not found"));
        if (!ap.getAlbum().getId().equals(albumId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mismatched album id");
        }
        boolean isAdder = ap.getAddedBy().getId().equals(userId);
        boolean isAlbumOwner = ap.getAlbum().getOwner().getId().equals(userId);
        if (!isAdder && !isAlbumOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the person who added this photo or the album owner can remove it");
        }
        albumPhotoRepository.delete(ap);
        log.info("User {} removed photo {} from album {}", userId, ap.getPhoto().getId(), albumId);
    }

    // ─── Helpers ─────────────────────────────────────────────

    /**
     * For a collaborative album, fires async ML analysis for every member
     * except the user who just added the photo ({@code adderUserId}).
     * The adder already has the photo processed (it's their own upload).
     * Each member gets their own independent FACE/PLACE/CAMERA tags.
     */
    private void triggerMLForOtherMembers(Album album, Photo photo, Long adderUserId) {
        Set<Long> memberIds = new HashSet<>();
        memberIds.add(album.getOwner().getId());
        collaboratorRepository.findByAlbumId(album.getId())
                .forEach(c -> memberIds.add(c.getUser().getId()));
        memberIds.remove(adderUserId);

        for (Long memberId : memberIds) {
            log.info("Triggering ML for member {} on photo {} (album {})",
                    memberId, photo.getId(), album.getId());
            pythonMLService.analyzePhotoAsync(photo.getId(), photo.getFilePath(), memberId);
        }
    }

    private Album requireVisible(Long albumId, Long userId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
        boolean owner = album.getOwner().getId().equals(userId);
        boolean collaborator = collaboratorRepository.existsByAlbumIdAndUserId(albumId, userId);
        if (!owner && !collaborator) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found");
        }
        return album;
    }
}
