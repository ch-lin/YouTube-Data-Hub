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

import java.util.List;

/**
 * Represents the response for the item verification operation.
 * <p>
 * This DTO is returned by the
 * {@link ch.lin.youtube.hub.backend.api.controller.YoutubeHubController#verifyItemsExistence(VerifyItemsRequest)}
 * endpoint. It categorizes the provided URLs into two lists: those that are
 * completely new to the system, and those that exist but have not yet been
 * downloaded.
 * <p>
 * Example JSON response:
 * <pre>
 * {@code
 * {
 *   "newUrls": [
 *     "https://www.youtube.com/watch?v=newVideoId"
 *   ],
 *   "undownloadedUrls": [
 *     "https://www.youtube.com/watch?v=existingButNotDownloadedId"
 *   ]
 * }
 * }
 * </pre>
 *
 * @param newUrls A list of URLs that do not correspond to any existing item in
 * the database.
 * @param undownloadedUrls A list of URLs that correspond to existing items in
 * the database but have not yet been downloaded.
 */
public record VerifyItemsResponse(List<String> newUrls, List<String> undownloadedUrls) {

}
