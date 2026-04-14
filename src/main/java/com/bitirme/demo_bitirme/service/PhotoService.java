package com.bitirme.demo_bitirme.service;

import com.bitirme.demo_bitirme.config.UploadConfig;
import com.bitirme.demo_bitirme.data.dto.EXIFDataDTO;
import com.bitirme.demo_bitirme.data.dto.PhotoDTO;
import com.bitirme.demo_bitirme.data.dto.UpdatePhotoRequest;
import com.bitirme.demo_bitirme.data.entity.*;
import com.bitirme.demo_bitirme.data.mapper.PhotoMapper;
import com.bitirme.demo_bitirme.data.mapper.PhotoTagMapper;
import com.bitirme.demo_bitirme.exception.ExifExtractionException;
import com.bitirme.demo_bitirme.exception.FileStorageException;
import com.bitirme.demo_bitirme.exception.PhotoNotFoundException;
import com.bitirme.demo_bitirme.exception.PhotoUploadException;
import com.bitirme.demo_bitirme.repository.ExifDataRepository;
import com.bitirme.demo_bitirme.repository.GpsDataRepository;
import com.bitirme.demo_bitirme.repository.PhotoRepository;
import com.bitirme.demo_bitirme.repository.PhotoTagRepository;
import com.bitirme.demo_bitirme.repository.TagRepository;
import com.bitirme.demo_bitirme.util.ExifExtractor;
import com.bitirme.demo_bitirme.util.ReverseGeocoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GPSInfo;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final ExifDataRepository exifDataRepository;
    private final GpsDataRepository gpsDataRepository;
    private final TagRepository tagRepository;
    private final PhotoTagRepository photoTagRepository;
    private final PhotoMapper photoMapper;
    private final PhotoTagMapper photoTagMapper;
    private final UploadConfig uploadConfig;
    private final ExifExtractor exifExtractor;
    private final ReverseGeocoder reverseGeocoder;
    private final PythonMLService pythonMLService;
    private final UserService userService;

    /**
     * Upload a photo and extract EXIF/GPS metadata
     */
    public PhotoDTO uploadPhoto(MultipartFile file, Long ownerUserId) {
        log.info("Starting photo upload: {} (owner={})", file.getOriginalFilename(), ownerUserId);

        try {
            // Validate file
            validatePhotoFile(file);

            // Create upload directory if not exists
            uploadConfig.ensureUploadDirExists();

            // Generate unique filename
            String uniqueFileName = generateUniqueFilename(file.getOriginalFilename());
            Path uploadPath = uploadConfig.getUploadPath().resolve(uniqueFileName);

            // Save file to disk
            Files.copy(file.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File saved to: {}", uploadPath);

            // Create and save Photo entity first so it gets an ID
            Photo photo = new Photo();
            photo.setFileName(uniqueFileName);
            photo.setFilePath(uploadPath.toString());
            photo.setOwner(userService.requireUser(ownerUserId));
            Photo savedPhoto = photoRepository.save(photo);
            log.debug("Photo entity saved with ID: {}", savedPhoto.getId());

            // Extract EXIF data (photo must already be saved)
            ExifData exifData = extractAndSaveExifData(savedPhoto, uploadPath.toFile());
            if (exifData != null) {
                savedPhoto.setExifData(exifData);
            }

            // Extract GPS data from EXIF (photo must already be saved)
            GpsData gpsData = extractAndSaveGpsData(savedPhoto, uploadPath.toFile());
            if (gpsData != null) {
                savedPhoto.setGpsData(gpsData);
            }

            log.info("Photo uploaded successfully with ID: {}", savedPhoto.getId());

            // Trigger async ML analysis (person vs scenery → face tagging)
            // Runs in background — upload response is not delayed.
            pythonMLService.analyzePhotoAsync(savedPhoto.getId(), uploadPath.toString(), ownerUserId);

            return toDTOForUser(savedPhoto, ownerUserId);


        } catch (PhotoUploadException | FileStorageException e) {
            log.error("Photo upload failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during photo upload: {}", e.getMessage(), e);
            throw new PhotoUploadException("Failed to upload photo", e);
        }
    }

    /**
     * Get photo by ID. The caller must either own the photo, or be a collaborator
     * on an album that contains it. Tag visibility is filtered to the caller's tags.
     */
    public PhotoDTO getPhotoById(Long id, Long userId) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found with ID: " + id));
        // NOTE: cross-user visibility (shared album) is enforced at the album endpoint level.
        // Direct access to a photo URL is only allowed for the owner.
        if (!photo.getOwner().getId().equals(userId)) {
            throw new PhotoNotFoundException("Photo not found with ID: " + id);
        }
        return toDTOForUser(photo, userId);
    }

    /**
     * Get all photos owned by the current user, paginated.
     */
    public Page<PhotoDTO> getAllPhotos(Pageable pageable, Long userId) {
        return photoRepository.findByOwnerId(userId, pageable)
                .map(p -> toDTOForUser(p, userId));
    }

    /**
     * Build a PhotoDTO that only shows the caller's own tags.
     */
    public PhotoDTO toDTOForUser(Photo photo, Long userId) {
        PhotoDTO dto = photoMapper.toPhotoDTO(photo);
        if (dto.getPhotoTags() != null && photo.getPhotoTags() != null) {
            var filtered = photo.getPhotoTags().stream()
                    .filter(pt -> pt.getAddedBy() != null
                            && pt.getAddedBy().getId().equals(userId))
                    .map(photoTagMapper::toPhotoTagDTO)
                    .toList();
            dto.setPhotoTags(filtered);
        }
        return dto;
    }

    /**
     * Add a tag to a photo.
     * Source defaults to MANUAL; pass "SYSTEM" for auto-detected tags.
     * Re-uses an existing tag row if one with the same name/type/source already exists.
     * Returns the updated photo.
     */
    /**
     * Add a tag to a photo, attributed to a specific user. Tags are per-user.
     */
    public PhotoDTO addTag(Long photoId, String tagName, String tagType, String source, Long userId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found with ID: " + photoId));

        Tag.TagType type   = Tag.TagType.valueOf(tagType.toUpperCase());
        Tag.TagSource src  = Tag.TagSource.valueOf(source != null ? source.toUpperCase() : "MANUAL");

        com.bitirme.demo_bitirme.data.entity.AppUser tagOwner = userService.requireUser(userId);

        // Find or create the Tag row for this user
        Tag tag = tagRepository
                .findByNameAndTagTypeAndSourceAndUserId(tagName, type, src, userId)
                .orElseGet(() -> {
                    Tag t = new Tag();
                    t.setName(tagName);
                    t.setTagType(type);
                    t.setSource(src);
                    t.setUser(tagOwner);
                    return tagRepository.save(t);
                });

        // Avoid duplicates on the same photo
        if (photoTagRepository.findByPhotoIdAndTagId(photoId, tag.getId()).isPresent()) {
            log.debug("Tag '{}' already on photo {} for user {}", tagName, photoId, userId);
            return toDTOForUser(photo, userId);
        }

        PhotoTag photoTag = new PhotoTag();
        photoTag.setPhoto(photo);
        photoTag.setTag(tag);
        photoTag.setAddedBy(tagOwner);
        photoTag.setConfidenceScore(java.math.BigDecimal.ONE);
        photoTagRepository.save(photoTag);

        log.info("Added {} tag '{}' ({}) to photo {} for user {}", src, tagName, tagType, photoId, userId);
        return toDTOForUser(photoRepository.findById(photoId).orElseThrow(), userId);
    }

    /** Convenience overload for the manual-tag UI flow (source = MANUAL). */
    public PhotoDTO addTag(Long photoId, String tagName, String tagType, Long userId) {
        return addTag(photoId, tagName, tagType, "MANUAL", userId);
    }

    /**
     * Remove a tag link from a photo. A user can only remove their own tags
     * (both MANUAL and SYSTEM). System tags are removable because face/place
     * detection may be wrong.
     */
    public PhotoDTO removeTag(Long photoId, Long photoTagId, Long userId) {
        PhotoTag photoTag = photoTagRepository.findById(photoTagId)
                .orElseThrow(() -> new RuntimeException("PhotoTag not found: " + photoTagId));

        if (!photoTag.getPhoto().getId().equals(photoId)) {
            throw new RuntimeException("PhotoTag does not belong to photo " + photoId);
        }
        if (photoTag.getAddedBy() == null || !photoTag.getAddedBy().getId().equals(userId)) {
            throw new RuntimeException("You can only remove your own tags");
        }

        photoTagRepository.delete(photoTag);
        log.info("Removed tag {} (source: {}) from photo {} by user {}",
                photoTagId, photoTag.getTag().getSource(), photoId, userId);
        return toDTOForUser(photoRepository.findById(photoId).orElseThrow(), userId);
    }

    /**
     * Delete a photo and its file from disk. Only the owner can delete.
     */
    public void deletePhoto(Long id, Long userId) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found with ID: " + id));

        if (!photo.getOwner().getId().equals(userId)) {
            throw new PhotoNotFoundException("Photo not found with ID: " + id);
        }

        // Delete physical file from disk
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(photo.getFilePath());
            java.nio.file.Files.deleteIfExists(filePath);
            log.info("Deleted file from disk: {}", photo.getFilePath());
        } catch (Exception e) {
            log.warn("Could not delete file from disk: {}", photo.getFilePath(), e);
        }

        photoRepository.delete(photo);
        log.info("Deleted photo with ID: {}", id);
    }

    /**
     * Update a photo's filename, EXIF data, and/or GPS data.
     * Only non-null fields in the request are applied.
     */
    public PhotoDTO updatePhoto(Long id, UpdatePhotoRequest req, Long userId) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found with ID: " + id));

        if (!photo.getOwner().getId().equals(userId)) {
            throw new PhotoNotFoundException("Photo not found with ID: " + id);
        }

        // ── Filename ──────────────────────────────────────────────────
        if (req.getFileName() != null && !req.getFileName().isBlank()) {
            photo.setFileName(req.getFileName());
        }

        // ── EXIF data ─────────────────────────────────────────────────
        if (req.getExifData() != null) {
            UpdatePhotoRequest.ExifUpdate eu = req.getExifData();
            ExifData exif = photo.getExifData();
            if (exif == null) {
                exif = new ExifData();
                exif.setPhoto(photo);
            }
            if (eu.getCameraMake()  != null) exif.setCameraMake(eu.getCameraMake());
            if (eu.getCameraModel() != null) exif.setCameraModel(eu.getCameraModel());
            if (eu.getIso()         != null) exif.setIso(eu.getIso());
            if (eu.getAperture()    != null) exif.setAperture(eu.getAperture());
            if (eu.getExposureTime()!= null) exif.setExposureTime(eu.getExposureTime());
            if (eu.getFocalLength() != null) exif.setFocalLength(eu.getFocalLength());
            if (eu.getDateTaken()   != null) exif.setDateTaken(eu.getDateTaken());
            if (eu.getOrientation() != null) exif.setOrientation(eu.getOrientation());
            exifDataRepository.save(exif);
            photo.setExifData(exif);
        }

        // ── GPS data ──────────────────────────────────────────────────
        if (req.getGpsData() != null) {
            UpdatePhotoRequest.GpsUpdate gu = req.getGpsData();
            GpsData gps = photo.getGpsData();
            if (gps == null) {
                gps = new GpsData();
                gps.setPhoto(photo);
            }
            if (gu.getLatitude()  != null) gps.setLatitude(gu.getLatitude());
            if (gu.getLongitude() != null) gps.setLongitude(gu.getLongitude());
            if (gu.getAltitude()  != null) gps.setAltitude(gu.getAltitude());

            // Re-run reverse geocoding whenever coordinates change
            if (gu.getLatitude() != null && gu.getLongitude() != null) {
                gps.setGoogleMapsLink(reverseGeocoder.generateGoogleMapsLink(gps.getLatitude(), gps.getLongitude()));
                String[] geo = reverseGeocoder.reverseGeocode(gps.getLatitude(), gps.getLongitude());
                gps.setCountry(geo[0]);
                gps.setCity(geo[1]);
            }

            gpsDataRepository.save(gps);
            photo.setGpsData(gps);
        }

        Photo saved = photoRepository.save(photo);
        log.info("Updated photo with ID: {}", id);
        return toDTOForUser(saved, userId);
    }

    /**
     * Copy a photo into the requesting user's own gallery.
     * The underlying file is shared (no disk duplication); only DB rows are created.
     * Any tags the caller already added to the source photo are copied to the new entry.
     *
     * @param sourcePhotoId the photo to copy
     * @param userId        the user who wants the copy in their gallery
     * @return the newly created PhotoDTO scoped to the caller's tags
     */
    @org.springframework.transaction.annotation.Transactional
    public PhotoDTO copyPhoto(Long sourcePhotoId, Long userId) {
        Photo source = photoRepository.findById(sourcePhotoId)
                .orElseThrow(() -> new PhotoNotFoundException("Photo not found: " + sourcePhotoId));

        AppUser owner = userService.requireUser(userId);

        // Don't create a duplicate if the user already owns this exact photo record
        if (source.getOwner().getId().equals(userId)) {
            return toDTOForUser(source, userId);
        }

        // ── Create new Photo row (same file, different owner) ──────────────
        Photo copy = new Photo();
        copy.setOwner(owner);
        copy.setFileName(source.getFileName());
        copy.setFilePath(source.getFilePath());   // shared file path — no disk I/O
        Photo saved = photoRepository.save(copy);

        // ── Copy ExifData ──────────────────────────────────────────────────
        if (source.getExifData() != null) {
            ExifData se = source.getExifData();
            ExifData exif = new ExifData();
            exif.setPhoto(saved);
            exif.setCameraMake(se.getCameraMake());
            exif.setCameraModel(se.getCameraModel());
            exif.setIso(se.getIso());
            exif.setAperture(se.getAperture());
            exif.setExposureTime(se.getExposureTime());
            exif.setFocalLength(se.getFocalLength());
            exif.setDateTaken(se.getDateTaken());
            exif.setOrientation(se.getOrientation());
            exifDataRepository.save(exif);
        }

        // ── Copy GpsData ───────────────────────────────────────────────────
        if (source.getGpsData() != null) {
            GpsData sg = source.getGpsData();
            GpsData gps = new GpsData();
            gps.setPhoto(saved);
            gps.setLatitude(sg.getLatitude());
            gps.setLongitude(sg.getLongitude());
            gps.setAltitude(sg.getAltitude());
            gps.setCountry(sg.getCountry());
            gps.setCity(sg.getCity());
            gps.setGoogleMapsLink(sg.getGoogleMapsLink());
            gpsDataRepository.save(gps);
        }

        // ── Copy caller's existing tags from the source photo ──────────────
        List<PhotoTag> callerTags =
                photoTagRepository.findByPhotoIdAndAddedByIdOrderByIdAsc(sourcePhotoId, userId);
        for (PhotoTag pt : callerTags) {
            PhotoTag newPt = new PhotoTag();
            newPt.setPhoto(saved);
            newPt.setTag(pt.getTag());        // same Tag entity (already scoped to this user)
            newPt.setAddedBy(owner);
            newPt.setConfidenceScore(pt.getConfidenceScore());
            photoTagRepository.save(newPt);
        }

        log.info("User {} copied photo {} → new photo {}", userId, sourcePhotoId, saved.getId());

        // Re-run the ML pipeline for the new owner so they get their own auto-tags
        // (face/place detection runs asynchronously; existing manually-copied tags are already set above)
        pythonMLService.analyzePhotoAsync(saved.getId(), saved.getFilePath(), userId);

        return toDTOForUser(photoRepository.findById(saved.getId()).orElseThrow(), userId);
    }

    /**
     * Validate photo file
     */
    private void validatePhotoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new PhotoUploadException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new PhotoUploadException("Only image files are allowed");
        }

        // Check supported formats
        if (!isSupportedImageFormat(contentType)) {
            throw new PhotoUploadException("Unsupported image format. Supported: JPEG, PNG, GIF, TIFF");
        }
    }

    /**
     * Check if image format is supported
     */
    private boolean isSupportedImageFormat(String contentType) {
        return contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/tiff");
    }

    /**
     * Generate unique filename using UUID
     */
    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFilename.substring(lastDot);
        }
        return UUID.randomUUID() + extension;
    }

    /**
     * Extract EXIF data from file
     */
    private ExifData extractAndSaveExifData(Photo photo, File imageFile) {
        try {
            EXIFDataDTO exifDTO = exifExtractor.extractExifData(imageFile);
            if (exifDTO == null) {
                log.debug("No EXIF data to extract");
                return null;
            }

            ExifData exifData = new ExifData();
            exifData.setPhoto(photo);
            exifData.setCameraMake(exifDTO.getCameraMake());
            exifData.setCameraModel(exifDTO.getCameraModel());
            exifData.setIso(exifDTO.getIso());
            exifData.setAperture(exifDTO.getAperture());
            exifData.setExposureTime(exifDTO.getExposureTime());
            exifData.setFocalLength(exifDTO.getFocalLength());
            exifData.setDateTaken(exifDTO.getDateTaken());
            exifData.setOrientation(exifDTO.getOrientation());

            ExifData savedExifData = exifDataRepository.save(exifData);
            log.debug("EXIF data extracted and saved for photo: {}", photo.getId());
            return savedExifData;

        } catch (ExifExtractionException e) {
            log.warn("EXIF extraction failed, continuing without it: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Unexpected error during EXIF extraction: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract GPS data from EXIF and perform reverse geocoding
     */
    private GpsData extractAndSaveGpsData(Photo photo, File imageFile) {
        try {
            EXIFDataDTO exifDTO = exifExtractor.extractExifData(imageFile);
            if (exifDTO == null) {
                log.debug("No EXIF data available for GPS extraction");
                return null;
            }

            // Extract GPS from EXIF using Apache Commons Imaging
            ImageMetadata metadata = Imaging.getMetadata(imageFile);

            if (!(metadata instanceof JpegImageMetadata jpegMetadata)) {
                log.debug("No JPEG metadata available for GPS extraction");
                return null;
            }

            TiffImageMetadata exifMetadata = jpegMetadata.getExif();
            if (exifMetadata == null) {
                log.debug("No EXIF metadata available for GPS extraction");
                return null;
            }

            GPSInfo gpsInfo = exifMetadata.getGPS();
            if (gpsInfo == null) {
                log.debug("No GPS info in EXIF data");
                return null;
            }

            double latitude = gpsInfo.getLatitudeAsDegreesNorth();
            double longitude = gpsInfo.getLongitudeAsDegreesEast();

            GpsData gpsData = new GpsData();
            gpsData.setPhoto(photo);
            gpsData.setLatitude(BigDecimal.valueOf(latitude));
            gpsData.setLongitude(BigDecimal.valueOf(longitude));

            // Set altitude if available
            TiffField altitudeField = exifMetadata.findField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE);
            if (altitudeField != null) {
                double altitude = altitudeField.getDoubleValue();
                gpsData.setAltitude(BigDecimal.valueOf(altitude));
            }

            // Generate Google Maps link
            gpsData.setGoogleMapsLink(reverseGeocoder.generateGoogleMapsLink(gpsData.getLatitude(), gpsData.getLongitude()));

            // Perform reverse geocoding
            String[] geoData = reverseGeocoder.reverseGeocode(gpsData.getLatitude(), gpsData.getLongitude());
            gpsData.setCountry(geoData[0]);
            gpsData.setCity(geoData[1]);

            GpsData savedGpsData = gpsDataRepository.save(gpsData);
            log.debug("GPS data extracted and saved for photo: {}", photo.getId());
            return savedGpsData;

        } catch (Exception e) {
            log.warn("GPS data extraction failed, continuing without it: {}", e.getMessage());
            return null;
        }
    }
}
