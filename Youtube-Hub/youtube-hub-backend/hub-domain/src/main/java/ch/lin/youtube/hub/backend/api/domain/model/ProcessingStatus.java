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
package ch.lin.youtube.hub.backend.api.domain.model;

/**
 * Represents the processing status of an {@link Item}, defining a simple state
 * machine for its lifecycle, particularly concerning its download state.
 */
public enum ProcessingStatus {
    /**
     * The item is newly discovered and has not been processed. This is the
     * default initial state.
     */
    NEW,
    /**
     * The item has been queued for download, but the download process has not
     * yet started.
     */
    PENDING,
    /**
     * The item is actively being downloaded.
     */
    DOWNLOADING,
    /**
     * The item has been successfully downloaded via an automated process.
     */
    DOWNLOADED,
    /**
     * The item was marked as downloaded through a manual user action, without
     * an automated download process.
     */
    MANUALLY_DOWNLOADED,
    /**
     * The item has been marked as watched.
     */
    WATCHED,
    /**
     * The download process for the item has failed. This may be a final state
     * or could be retried.
     */
    FAILED,
    /**
     * The item is explicitly marked to be ignored and will be skipped by
     * processing workflows.
     */
    IGNORE,
    /**
     * The item has been marked as deleted.
     */
    DELETED
}
