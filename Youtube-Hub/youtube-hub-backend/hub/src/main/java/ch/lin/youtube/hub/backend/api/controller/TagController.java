/*=============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Che-Hung Lin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *===========================================================================*/
package ch.lin.youtube.hub.backend.api.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.lin.youtube.hub.backend.api.app.service.TagService;
import ch.lin.youtube.hub.backend.api.domain.model.Tag;
import ch.lin.youtube.hub.backend.api.dto.CreateTagRequest;
import ch.lin.youtube.hub.backend.api.dto.TagResponse;
import jakarta.validation.Valid;

/**
 * REST controller for managing tags.
 * <p>
 * This controller provides API endpoints for creating, retrieving, and deleting
 * tags. It delegates business logic to the {@link TagService}.
 */
@RestController
@RequestMapping("/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    /**
     * Retrieves a list of all tags currently stored in the database.
     *
     * @return A {@link ResponseEntity} with an HTTP 200 OK status, containing a
     * list of {@link TagResponse} objects.
     * <p>
     * Example cURL request:      <pre>
     * {@code curl -X GET http://localhost:8080/tags}
     * </pre>
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TagResponse>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        List<TagResponse> response = tags.stream().map(TagResponse::new).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new tag based on the provided name.
     *
     * @param request The request body containing the name for the new tag.
     * @return A {@link ResponseEntity} with an HTTP 201 Created status,
     * containing the newly created {@link TagResponse}.
     * <p>
     * Example cURL request:      <pre>
     * {@code
     * curl -X POST http://localhost:8080/tags \
     * -H "Content-Type: application/json" \
     * -d '{
     *   "name": "java-tutorials"
     * }'
     * }
     * </pre>
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
        Tag newTag = tagService.createTag(request.getName());
        return new ResponseEntity<>(new TagResponse(newTag), HttpStatus.CREATED);
    }

    /**
     * Deletes a specific tag from the database by its name.
     *
     * @param tagName The name of the tag to be deleted.
     * @return A {@link ResponseEntity} with an HTTP 204 No Content status. An
     * error status (e.g., 404 Not Found) will be returned if the tag does not
     * exist.
     * <p>
     * Example cURL request:      <pre>
     * {@code curl -X DELETE http://localhost:8080/tags/java-tutorials}
     * </pre>
     */
    @DeleteMapping(value = "/{tagName}")
    public ResponseEntity<Void> deleteTag(@PathVariable final String tagName) {
        // This assumes a `deleteTagByName` method exists in your TagService.
        // You will need to implement this in your service and repository layers.
        tagService.deleteTagByName(tagName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Deletes all tags from the database.
     *
     * @return A {@link ResponseEntity} with an HTTP 204 No Content status upon
     * successful cleanup.
     * <p>
     * Example cURL request:      <pre>
     * {@code
     * curl -X DELETE http://localhost:8080/tags/deletion
     * }
     * </pre>
     */
    @DeleteMapping("/deletion")
    public ResponseEntity<Void> cleanUp() {
        tagService.cleanup();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
