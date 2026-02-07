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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.lin.youtube.hub.backend.api.app.repository.YoutubeApiUsageRepository;
import ch.lin.youtube.hub.backend.api.domain.model.YoutubeApiUsage;

/**
 * Service implementation for managing YouTube API usage statistics.
 */
@Service
public class YoutubeApiUsageServiceImpl implements YoutubeApiUsageService {

    private final YoutubeApiUsageRepository youtubeApiUsageRepository;

    public YoutubeApiUsageServiceImpl(YoutubeApiUsageRepository youtubeApiUsageRepository) {
        this.youtubeApiUsageRepository = youtubeApiUsageRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<YoutubeApiUsage> getUsageHistory(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            // Returns usage history sorted by date descending (newest first)
            return youtubeApiUsageRepository.findAllByOrderByUsageDateDesc();
        }
        LocalDate start = startDate != null ? startDate : LocalDate.of(1970, 1, 1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return youtubeApiUsageRepository.findByUsageDateBetweenOrderByUsageDateDesc(start, end);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public synchronized void recordUsage(long cost) {
        LocalDate today = YoutubeApiUsage.getCurrentQuotaDate();
        YoutubeApiUsage usage = youtubeApiUsageRepository.findByUsageDate(today)
                .orElseGet(() -> {
                    YoutubeApiUsage newUsage = new YoutubeApiUsage();
                    newUsage.setUsageDate(today);
                    return newUsage;
                });
        usage.increment(cost);
        youtubeApiUsageRepository.save(usage);
    }

}
