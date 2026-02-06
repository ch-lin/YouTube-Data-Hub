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
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ch.lin.youtube.hub.backend.api.app.service.TagService;
import ch.lin.youtube.hub.backend.api.domain.model.Tag;
import ch.lin.youtube.hub.backend.api.dto.CreateTagRequest;
import ch.lin.youtube.hub.backend.api.dto.TagResponse;

@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock
    private TagService tagService;

    private TagController tagController;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        tagController = new TagController(tagService);
    }

    @Test
    void getAllTags_ShouldReturnTags() {
        Tag tag = new Tag();
        tag.setName("test");
        when(tagService.getAllTags()).thenReturn(List.of(tag));

        ResponseEntity<List<TagResponse>> response = tagController.getAllTags();
        List<TagResponse> body = Objects.requireNonNull(response.getBody());
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body).hasSize(1);
        assertThat(body.get(0).getName()).isEqualTo("test");
    }

    @Test
    void createTag_ShouldReturnCreatedTag() {
        CreateTagRequest request = new CreateTagRequest();
        request.setName("new-tag");

        Tag createdTag = new Tag();
        createdTag.setName("new-tag");
        when(tagService.createTag("new-tag")).thenReturn(createdTag);

        ResponseEntity<TagResponse> response = tagController.createTag(request);
        TagResponse body = Objects.requireNonNull(response.getBody());
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(body).isNotNull();
        assertThat(body.getName()).isEqualTo("new-tag");
    }

    @Test
    void deleteTag_ShouldCallService() {
        ResponseEntity<Void> response = tagController.deleteTag("tag-to-delete");

        verify(tagService).deleteTagByName("tag-to-delete");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void cleanUp_ShouldCallService() {
        ResponseEntity<Void> response = tagController.cleanUp();

        verify(tagService).cleanup();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
