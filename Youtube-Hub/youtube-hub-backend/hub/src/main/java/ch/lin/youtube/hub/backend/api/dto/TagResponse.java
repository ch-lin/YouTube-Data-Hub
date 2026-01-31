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
package ch.lin.youtube.hub.backend.api.dto;

import ch.lin.youtube.hub.backend.api.domain.model.Tag;
import lombok.Getter;

/**
 * Represents the data transfer object for a tag in API responses.
 * <p>
 * This DTO provides a client-friendly representation of a {@link Tag} entity.
 * It is returned by endpoints in the
 * {@link ch.lin.youtube.hub.backend.api.controller.TagController}, such as
 * {@link ch.lin.youtube.hub.backend.api.controller.TagController#getAllTags()}
 * and
 * {@link ch.lin.youtube.hub.backend.api.controller.TagController#createTag(CreateTagRequest)}.
 * <p>
 * Example JSON structure:
 * <pre>
 * {@code
 * {
 *   "id": 1,
 *   "name": "java-tutorials"
 * }
 * }
 * </pre>
 */
@Getter
public class TagResponse {

    /**
     * The unique identifier of the tag.
     */
    private final Long id;

    /**
     * The name of the tag.
     */
    private final String name;

    /**
     * Constructs a new TagResponse from a Tag domain entity.
     *
     * @param tag The {@link Tag} entity to map to a response DTO.
     */
    public TagResponse(Tag tag) {
        this.id = tag.getId();
        this.name = tag.getName();
    }

}
