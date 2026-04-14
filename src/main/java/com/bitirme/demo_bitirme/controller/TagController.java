package com.bitirme.demo_bitirme.controller;

import com.bitirme.demo_bitirme.config.CurrentUserId;
import com.bitirme.demo_bitirme.data.dto.TagDTO;
import com.bitirme.demo_bitirme.data.entity.Tag;
import com.bitirme.demo_bitirme.data.mapper.TagMapper;
import com.bitirme.demo_bitirme.repository.TagRepository;
import com.bitirme.demo_bitirme.service.PythonMLService;
import com.bitirme.demo_bitirme.util.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final PythonMLService pythonMLService;

    /**
     * Rename a tag. Users can only rename their own tags.
     * For FACE tags, this rename is per-user only — it does not affect other users'
     * labels for the same person (face cluster).
     */
    @PutMapping("/{tagId}")
    public ResponseEntity<ApiResponse<TagDTO>> renameTag(
            @PathVariable Long tagId,
            @RequestBody Map<String, String> body,
            @CurrentUserId Long userId) {

        String newName = body.get("name");
        if (newName == null || newName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag name cannot be blank");
        }
        newName = newName.trim();

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + tagId));

        if (tag.getUser() == null || !tag.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only rename your own tags");
        }

        // Conflict check is scoped to this user
        String finalNewName = newName;
        boolean conflict = tagRepository
                .findByNameAndTagTypeAndSourceAndUserId(newName, tag.getTagType(), tag.getSource(), userId)
                .filter(existing -> !existing.getId().equals(tagId))
                .isPresent();
        if (conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a tag named '" + finalNewName + "' of this type");
        }

        String oldName = tag.getName();
        tag.setName(newName);
        Tag saved = tagRepository.save(tag);
        log.info("User {} renamed tag {} from '{}' to '{}' (type={}, source={})",
                userId, tagId, oldName, newName, saved.getTagType(), saved.getSource());

        // For FACE tags, notify Python so its per-user label stays in sync
        if (saved.getTagType() == Tag.TagType.FACE) {
            pythonMLService.renamePersonForUserAsync(oldName, newName, userId);
        }

        return ResponseEntity.ok(ApiResponse.success("Tag renamed", tagMapper.toTagDTO(saved)));
    }

    /**
     * Search the caller's tags by partial name.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TagDTO>>> search(
            @RequestParam(name = "q", defaultValue = "") String q,
            @CurrentUserId Long userId) {

        if (q.isBlank() || q.length() < 2) {
            return ResponseEntity.ok(ApiResponse.success("No query", List.of()));
        }

        List<TagDTO> results = tagRepository
                .findByUserIdAndNameContainingIgnoreCaseOrderByTagTypeAscNameAsc(userId, q)
                .stream()
                .map(tagMapper::toTagDTO)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Tags found", results));
    }
}
