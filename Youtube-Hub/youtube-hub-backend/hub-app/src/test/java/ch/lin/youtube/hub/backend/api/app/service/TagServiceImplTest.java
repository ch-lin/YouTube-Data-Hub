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
package ch.lin.youtube.hub.backend.api.app.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.youtube.hub.backend.api.app.repository.TagRepository;
import ch.lin.youtube.hub.backend.api.common.exception.TagAlreadyExistsException;
import ch.lin.youtube.hub.backend.api.common.exception.TagNotFoundException;
import ch.lin.youtube.hub.backend.api.domain.model.Tag;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagRepository tagRepository;

    private TagServiceImpl tagService;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        tagService = new TagServiceImpl(tagRepository);
    }

    @Test
    void cleanup_ShouldCleanAndReset() {
        tagService.cleanup();
        verify(tagRepository).cleanTable();
        verify(tagRepository).resetSequence();
    }

    @Test
    void getAllTags_ShouldReturnAllTags() {
        Tag tag = new Tag();
        tag.setName("test");
        when(tagRepository.findAll()).thenReturn(List.of(tag));

        List<Tag> result = tagService.getAllTags();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("test");
    }

    @SuppressWarnings("null")
    @Test
    void createTag_ShouldCreate_WhenValid() {
        String tagName = "new-tag";
        when(tagRepository.findByName(tagName)).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(i -> i.getArgument(0));

        Tag result = tagService.createTag(tagName);

        assertThat(result.getName()).isEqualTo(tagName);
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void createTag_ShouldThrow_WhenNameInvalid() {
        assertThatThrownBy(() -> tagService.createTag(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Tag name cannot be null or empty");

        assertThatThrownBy(() -> tagService.createTag("   "))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Tag name cannot be null or empty");
    }

    @Test
    void createTag_ShouldThrow_WhenTagExists() {
        String tagName = "existing";
        when(tagRepository.findByName(tagName)).thenReturn(Optional.of(new Tag()));

        assertThatThrownBy(() -> tagService.createTag(tagName))
                .isInstanceOf(TagAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void deleteTagByName_ShouldDelete_WhenFound() {
        String tagName = "existing";
        Tag tag = new Tag();
        tag.setName(tagName);
        when(tagRepository.findByName(tagName)).thenReturn(Optional.of(tag));

        tagService.deleteTagByName(tagName);

        verify(tagRepository).delete(tag);
    }

    @Test
    void deleteTagByName_ShouldThrow_WhenNameInvalid() {
        assertThatThrownBy(() -> tagService.deleteTagByName(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Tag name cannot be null or blank");

        assertThatThrownBy(() -> tagService.deleteTagByName("   "))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Tag name cannot be null or blank");
    }

    @Test
    void deleteTagByName_ShouldThrow_WhenNotFound() {
        String tagName = "missing";
        when(tagRepository.findByName(tagName)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.deleteTagByName(tagName))
                .isInstanceOf(TagNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
