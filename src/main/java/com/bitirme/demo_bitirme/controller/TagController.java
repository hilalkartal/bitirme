package com.bitirme.demo_bitirme.controller;

import com.bitirme.demo_bitirme.data.dto.TagDTO;
import com.bitirme.demo_bitirme.data.entity.Tag;
import com.bitirme.demo_bitirme.data.mapper.TagMapper;
import com.bitirme.demo_bitirme.repository.TagRepository;
import com.bitirme.demo_bitirme.service.PythonMLService;
import com.bitirme.demo_bitirme.util.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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
     * Rename a tag globally (affects all photos using this tag).
     * PUT /tags/{tagId}
     * Body: { "name": "new name" }
     */
    @PutMapping("/{tagId}")
    public ResponseEntity<ApiResponse<TagDTO>> renameTag(
            @PathVariable Long tagId,
            @RequestBody Map<String, String> body) {

        String newName = body.get("name");
        if (newName == null || newName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag name cannot be blank");
        }
        newName = newName.trim();

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + tagId));

        // Check for name conflict: another tag with same (name, type, source) already exists
        String finalNewName = newName;
        boolean conflict = tagRepository
                .findByNameAndTagTypeAndSource(newName, tag.getTagType(), tag.getSource())
                .filter(existing -> !existing.getId().equals(tagId))
                .isPresent();
        if (conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A tag named '" + finalNewName + "' already exists for this type");
        }

        String oldName = tag.getName();
        tag.setName(newName);
        Tag saved = tagRepository.save(tag);
        log.info("Renamed tag {} from '{}' to '{}' (type={}, source={})",
                tagId, oldName, newName, saved.getTagType(), saved.getSource());

        // If this is a FACE tag, keep Python's persons table in sync so that
        // future uploads post the correct user-chosen name (e.g. "Selin")
        // instead of the stale auto-generated name (e.g. "Person 2").
        if (saved.getTagType() == Tag.TagType.FACE) {
            pythonMLService.renamePersonAsync(oldName, newName);
        }

        return ResponseEntity.ok(ApiResponse.success("Tag renamed", tagMapper.toTagDTO(saved)));
    }

    /**
     * Search tags by partial name across all tag types.
     * GET /tags/search?q=vol
     * Returns results grouped (sorted) by tagType then name.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TagDTO>>> search(
            @RequestParam(name = "q", defaultValue = "") String q) {

        if (q.isBlank() || q.length() < 2) {
            return ResponseEntity.ok(ApiResponse.success("No query", List.of()));
        }

        log.debug("Tag search: '{}'", q);
        List<TagDTO> results = tagRepository
                .findByNameContainingIgnoreCaseOrderByTagTypeAscNameAsc(q)
                .stream()
                .map(tagMapper::toTagDTO)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Tags found", results));
    }
}
