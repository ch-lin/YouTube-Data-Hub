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

import java.time.LocalDate;
import java.util.List;

import ch.lin.youtube.hub.backend.api.domain.model.YoutubeApiUsage;

/**
 * Defines the service layer contract for managing YouTube API usage statistics.
 */
public interface YoutubeApiUsageService {

    /**
     * Retrieves the history of API usage statistics.
     *
     * @param startDate The start date of the range (inclusive). If null,
     * defaults to the beginning of time.
     * @param endDate The end date of the range (inclusive). If null, defaults
     * to the current date.
     *
     * @return A list of {@link YoutubeApiUsage} records.
     */
    List<YoutubeApiUsage> getUsageHistory(LocalDate startDate, LocalDate endDate);

    /**
     * Records a new API request usage.
     *
     * @param cost The estimated quota cost of the request (e.g., 1 for list
     * operations).
     */
    void recordUsage(long cost);

    /**
     * Checks if the current daily usage allows for further processing,
     * respecting the configured quota limit and safety threshold.
     *
     * @param dailyQuotaLimit The maximum allowed quota usage per day.
     * @param safetyThreshold The buffer amount of quota to preserve.
     * @return true if the current usage plus the threshold is less than the
     * limit; false otherwise.
     */
    boolean hasSufficientQuota(long dailyQuotaLimit, long safetyThreshold);
}
