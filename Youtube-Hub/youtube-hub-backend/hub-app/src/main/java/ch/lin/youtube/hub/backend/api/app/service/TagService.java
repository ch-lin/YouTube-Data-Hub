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

import ch.lin.youtube.hub.backend.api.domain.model.Tag;

/**
 * Service for managing tags.
 */
public interface TagService {

    /**
     * Removes tags that are not associated with any videos.
     */
    void cleanup();

    /**
     * Retrieves all existing tags.
     *
     * @return a list of all tags
     */
    List<Tag> getAllTags();

    /**
     * Creates a new tag with the given name.
     *
     * @param name the name of the tag to create
     * @return the newly created tag
     * @throws
     * ch.lin.youtube.hub.backend.common.exception.InvalidRequestException if
     * the tag name is null or blank.
     * @throws
     * ch.lin.youtube.hub.backend.api.common.exception.TagAlreadyExistsException
     * if a tag with the same name already exists.
     */
    Tag createTag(String name);

    /**
     * Deletes a tag by its name.
     *
     * @param name the name of the tag to delete
     * @throws
     * ch.lin.youtube.hub.backend.common.exception.InvalidRequestException if
     * the tag name is null or blank.
     * @throws
     * ch.lin.youtube.hub.backend.api.common.exception.TagNotFoundException if
     * no tag with the given name is found.
     */
    void deleteTagByName(String name);

}
