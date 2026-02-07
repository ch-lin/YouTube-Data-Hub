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

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.lin.youtube.hub.backend.api.app.service.YoutubeApiUsageService;
import ch.lin.youtube.hub.backend.api.domain.model.YoutubeApiUsage;

/**
 * REST controller for retrieving API usage statistics.
 * <p>
 * This controller provides endpoints to monitor the consumption of the YouTube
 * Data API quota. It delegates data retrieval to the {@link ApiUsageService}.
 */
@RestController
@RequestMapping("/youtube-api-usage")
public class YoutubeApiUsageController {

    private final YoutubeApiUsageService youtubeApiUsageService;

    public YoutubeApiUsageController(YoutubeApiUsageService youtubeApiUsageService) {
        this.youtubeApiUsageService = youtubeApiUsageService;
    }

    /**
     * Retrieves the history of API usage statistics.
     *
     * @param startDate Optional start date (inclusive) in YYYY-MM-DD format.
     * @param endDate Optional end date (inclusive) in YYYY-MM-DD format.
     * @return A {@link ResponseEntity} containing a list of
     * {@link YoutubeApiUsage} records, sorted by date (typically descending),
     * with an HTTP 200 OK status.
     * <p>
     * Example cURL request:
     * <pre>{@code curl -X GET "http://localhost:8080/youtube-api-usage?startDate=2023-01-01&endDate=2023-01-31"}</pre>
     */
    @GetMapping
    public ResponseEntity<List<YoutubeApiUsage>> getUsageHistory(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(youtubeApiUsageService.getUsageHistory(startDate, endDate));
    }
}
