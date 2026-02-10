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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.lin.youtube.hub.backend.api.app.repository.YoutubeApiUsageRepository;
import ch.lin.youtube.hub.backend.api.domain.model.YoutubeApiUsage;

@ExtendWith(MockitoExtension.class)
class YoutubeApiUsageServiceImplTest {

    @Mock
    private YoutubeApiUsageRepository youtubeApiUsageRepository;

    private YoutubeApiUsageServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        service = new YoutubeApiUsageServiceImpl(youtubeApiUsageRepository);
    }

    @Test
    void getUsageHistory_ShouldReturnAll_WhenDatesAreNull() {
        YoutubeApiUsage usage = new YoutubeApiUsage();
        when(youtubeApiUsageRepository.findAllByOrderByUsageDateDesc()).thenReturn(List.of(usage));

        List<YoutubeApiUsage> result = service.getUsageHistory(null, null);

        assertThat(result).hasSize(1);
        verify(youtubeApiUsageRepository).findAllByOrderByUsageDateDesc();
    }

    @Test
    void getUsageHistory_ShouldFilterByDate_WhenDatesProvided() {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 31);
        YoutubeApiUsage usage = new YoutubeApiUsage();
        when(youtubeApiUsageRepository.findByUsageDateBetweenOrderByUsageDateDesc(start, end))
                .thenReturn(List.of(usage));

        List<YoutubeApiUsage> result = service.getUsageHistory(start, end);

        assertThat(result).hasSize(1);
        verify(youtubeApiUsageRepository).findByUsageDateBetweenOrderByUsageDateDesc(start, end);
    }

    @Test
    void getUsageHistory_ShouldUseDefaults_WhenOneDateIsNull() {
        LocalDate start = LocalDate.of(2023, 1, 1);
        LocalDate end = LocalDate.of(2023, 1, 31);

        when(youtubeApiUsageRepository.findByUsageDateBetweenOrderByUsageDateDesc(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Test end date default (should be now)
        service.getUsageHistory(start, null);
        verify(youtubeApiUsageRepository).findByUsageDateBetweenOrderByUsageDateDesc(eq(start), any(LocalDate.class));

        // Test start date default (should be 1970-01-01)
        service.getUsageHistory(null, end);
        verify(youtubeApiUsageRepository).findByUsageDateBetweenOrderByUsageDateDesc(eq(LocalDate.of(1970, 1, 1)), eq(end));
    }

    @Test
    void recordUsage_ShouldIncrementExisting_WhenRecordExists() {
        YoutubeApiUsage existing = new YoutubeApiUsage();
        existing.setRequestCount(10);
        existing.setQuotaUsed(100);

        when(youtubeApiUsageRepository.findByUsageDate(any(LocalDate.class))).thenReturn(Optional.of(existing));

        service.recordUsage(5);

        assertThat(existing.getRequestCount()).isEqualTo(11);
        assertThat(existing.getQuotaUsed()).isEqualTo(105);
        verify(youtubeApiUsageRepository).save(existing);
    }

    @Test
    @SuppressWarnings("null")
    void recordUsage_ShouldCreateNew_WhenRecordDoesNotExist() {
        when(youtubeApiUsageRepository.findByUsageDate(any(LocalDate.class))).thenReturn(Optional.empty());

        service.recordUsage(10);

        ArgumentCaptor<YoutubeApiUsage> captor = ArgumentCaptor.forClass(YoutubeApiUsage.class);
        verify(youtubeApiUsageRepository).save(captor.capture());

        YoutubeApiUsage saved = captor.getValue();
        assertThat(saved.getRequestCount()).isEqualTo(1);
        assertThat(saved.getQuotaUsed()).isEqualTo(10);
        assertThat(saved.getUsageDate()).isNotNull();
    }

    @Test
    void hasSufficientQuota_ShouldReturnTrue_WhenUsageIsLow() {
        YoutubeApiUsage usage = new YoutubeApiUsage();
        usage.setQuotaUsed(100L);
        when(youtubeApiUsageRepository.findByUsageDate(any(LocalDate.class))).thenReturn(Optional.of(usage));

        boolean result = service.hasSufficientQuota(1000L, 100L);

        assertThat(result).isTrue();
    }

    @Test
    void hasSufficientQuota_ShouldReturnFalse_WhenUsageIsHigh() {
        YoutubeApiUsage usage = new YoutubeApiUsage();
        usage.setQuotaUsed(900L);
        when(youtubeApiUsageRepository.findByUsageDate(any(LocalDate.class))).thenReturn(Optional.of(usage));

        boolean result = service.hasSufficientQuota(1000L, 100L); // 900 + 100 = 1000, not < 1000

        assertThat(result).isFalse();
    }

    @Test
    void hasSufficientQuota_ShouldReturnTrue_WhenNoUsageRecord() {
        when(youtubeApiUsageRepository.findByUsageDate(any(LocalDate.class))).thenReturn(Optional.empty());

        boolean result = service.hasSufficientQuota(1000L, 100L);

        assertThat(result).isTrue();
    }
}
