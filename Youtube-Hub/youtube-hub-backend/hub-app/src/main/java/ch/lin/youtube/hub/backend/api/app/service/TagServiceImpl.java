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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.youtube.hub.backend.api.app.repository.TagRepository;
import ch.lin.youtube.hub.backend.api.common.exception.TagAlreadyExistsException;
import ch.lin.youtube.hub.backend.api.common.exception.TagNotFoundException;
import ch.lin.youtube.hub.backend.api.domain.model.Tag;

/**
 * Service implementation for managing tags.
 * <p>
 * This class provides the concrete logic for operations defined in the
 * {@link TagService} interface. It handles the creation and validation of
 * {@link Tag} entities, interacting directly with the {@link TagRepository}.
 */
@Service
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    /**
     * Constructs a new TagServiceImpl.
     *
     * @param tagRepository the repository for tag data access
     */
    public TagServiceImpl(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation deletes all tags from the database and resets the
     * primary key sequence. Note: This is a destructive operation and differs
     * from the interface's intended behavior of cleaning up only unassociated
     * tags.
     */
    @Override
    public void cleanup() {
        tagRepository.cleanTable();
        tagRepository.resetSequence();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Tag createTag(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("Tag name cannot be null or empty.");
        }

        if (tagRepository.findByName(name).isPresent()) {
            throw new TagAlreadyExistsException("Tag with name '" + name + "' already exists.");
        }

        Tag tag = new Tag();
        tag.setName(name);
        return tagRepository.save(tag);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteTagByName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("Tag name cannot be null or blank.");
        }

        Tag tag = tagRepository.findByName(name)
                .orElseThrow(() -> new TagNotFoundException("Tag with name '" + name + "' not found."));

        // The deletion will cascade to the join table (item_tags) if configured,
        // but it's good practice to disassociate or handle related entities explicitly
        // if there are constraints. Here, we assume cascading delete is not set up
        // and items are just disassociated.
        if (tag != null) {
            tagRepository.delete(tag);
        }
    }
}
