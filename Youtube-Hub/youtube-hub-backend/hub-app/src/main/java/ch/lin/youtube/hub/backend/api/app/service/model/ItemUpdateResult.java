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
package ch.lin.youtube.hub.backend.api.app.service.model;

import java.util.List;

import ch.lin.youtube.hub.backend.api.domain.model.Item;

/**
 * Represents the result of an update operation on an {@link Item}.
 * <p>
 * This is an immutable data carrier that holds the updated item and a list of
 * any non-critical warnings that were generated during the update process, such
 * as the detection of potential duplicate files.
 *
 * @param updatedItem The {@link Item} entity after the update.
 * @param warnings A list of warning messages. May be null or empty.
 */
public record ItemUpdateResult(Item updatedItem, List<String> warnings) {

    /**
     * A factory method to create a result with no warnings.
     *
     * @param updatedItem The updated item.
     * @return An {@code ItemUpdateResult} with a null warnings list.
     */
    public static ItemUpdateResult of(Item updatedItem) {
        return new ItemUpdateResult(updatedItem, null);
    }

    /**
     * A factory method to create a result with warnings.
     *
     * @param updatedItem The updated item.
     * @param warnings A list of warning messages.
     * @return An {@code ItemUpdateResult} with the provided item and warnings.
     */
    public static ItemUpdateResult of(Item updatedItem, List<String> warnings) {
        return new ItemUpdateResult(updatedItem, warnings);
    }
}
