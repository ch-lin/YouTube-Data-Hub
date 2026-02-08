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

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;
import ch.lin.youtube.hub.backend.api.domain.model.SchedulerType;

@ExtendWith(MockitoExtension.class)
class AutoFetchSchedulerServiceImplTest {

    @Mock
    private ConfigsService configsService;
    @Mock
    private YoutubeHubService youtubeHubService;
    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private AutoFetchSchedulerServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        service = new AutoFetchSchedulerServiceImpl(configsService, youtubeHubService, taskScheduler);
    }

    @Test
    @SuppressWarnings("null")
    void init_ShouldStart_WhenAutoStartEnabled() {
        HubConfig config = new HubConfig();
        config.setAutoStartFetchScheduler(true);
        config.setSchedulerType(SchedulerType.FIXED_RATE);
        config.setFixedRate(1000L);
        when(configsService.getResolvedConfig(null)).thenReturn(config);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class))).thenAnswer(i -> scheduledFuture);

        service.init();

        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofMillis(1000L)));
    }

    @Test
    @SuppressWarnings("null")
    void init_ShouldNotStart_WhenAutoStartDisabled() {
        HubConfig config = new HubConfig();
        config.setAutoStartFetchScheduler(false);
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        service.init();

        verify(taskScheduler, never()).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @SuppressWarnings("null")
    void start_ShouldDoNothing_WhenAlreadyRunning() {
        // Simulate running state
        HubConfig config = new HubConfig();
        config.setFixedRate(1000L);
        when(configsService.getResolvedConfig(null)).thenReturn(config);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class))).thenAnswer(i -> scheduledFuture);

        service.start(); // First start

        // Setup for second start call
        when(scheduledFuture.isCancelled()).thenReturn(false);
        when(scheduledFuture.isDone()).thenReturn(false);

        service.start(); // Second start

        verify(taskScheduler, times(1)).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
    }

    @Test
    @SuppressWarnings("null")
    void start_ShouldUseFixedRate_WhenTypeIsFixedRate() {
        HubConfig config = new HubConfig();
        config.setSchedulerType(SchedulerType.FIXED_RATE);
        config.setFixedRate(5000L);
        when(configsService.getResolvedConfig(null)).thenReturn(config);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class))).thenAnswer(i -> scheduledFuture);

        service.start();

        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofMillis(5000L)));
    }

    @Test
    @SuppressWarnings("null")
    void start_ShouldUseFixedRate_WhenTypeIsNull() {
        HubConfig config = new HubConfig();
        config.setSchedulerType(null); // Default to FIXED_RATE
        config.setFixedRate(5000L);
        when(configsService.getResolvedConfig(null)).thenReturn(config);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class))).thenAnswer(i -> scheduledFuture);

        service.start();

        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofMillis(5000L)));
    }

    @Test
    @SuppressWarnings("null")
    void start_ShouldNotStart_WhenFixedRateInvalid() {
        HubConfig config = new HubConfig();
        config.setSchedulerType(SchedulerType.FIXED_RATE);
        config.setFixedRate(-100L);
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        service.start();

        verify(taskScheduler, never()).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
    }

    @Test
    @SuppressWarnings("null")
    void start_ShouldNotStart_WhenFixedRateIsNull() {
        HubConfig config = new HubConfig();
        config.setSchedulerType(SchedulerType.FIXED_RATE);
        config.setFixedRate(null);
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        service.start();

        verify(taskScheduler, never()).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
    }

    @Test
    @SuppressWarnings("null")
    void start_ShouldUseCron_WhenTypeIsCron() {
        HubConfig config = new HubConfig();
        config.setSchedulerType(SchedulerType.CRON);
        config.setCronExpression("0 0 * * * *");
        when(configsService.getResolvedConfig(null)).thenReturn(config);
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class))).thenAnswer(i -> scheduledFuture);

        service.start();

        ArgumentCaptor<CronTrigger> captor = ArgumentCaptor.forClass(CronTrigger.class);
        verify(taskScheduler).schedule(any(Runnable.class), captor.capture());
        assertThat(captor.getValue().getExpression()).isEqualTo("0 0 * * * *");
    }

    @Test
    @SuppressWarnings("null")
    void start_ShouldUseCron_WithTimeZone() {
        HubConfig config = new HubConfig();
        config.setSchedulerType(SchedulerType.CRON);
        config.setCronExpression("0 0 * * * *");
        config.setCronTimeZone("UTC");
        when(configsService.getResolvedConfig(null)).thenReturn(config);
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class))).thenAnswer(i -> scheduledFuture);

        service.start();

        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @SuppressWarnings("null")
    void start_ShouldNotStart_WhenCronExpressionInvalid() {
        HubConfig config = new HubConfig();
        config.setSchedulerType(SchedulerType.CRON);
        config.setCronExpression("invalid-cron");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        service.start();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @SuppressWarnings("null")
    void start_ShouldNotStart_WhenCronExpressionIsEmpty() {
        HubConfig config = new HubConfig();
        config.setSchedulerType(SchedulerType.CRON);
        config.setCronExpression("");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        service.start();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @SuppressWarnings("null")
    void stop_ShouldCancelTask_WhenRunning() {
        // Start first
        HubConfig config = new HubConfig();
        config.setFixedRate(1000L);
        when(configsService.getResolvedConfig(null)).thenReturn(config);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class))).thenAnswer(i -> scheduledFuture);
        service.start();

        when(scheduledFuture.isDone()).thenReturn(false);

        service.stop();

        verify(scheduledFuture).cancel(false);
    }

    @Test
    void executeFetchJob_ShouldCallYoutubeHubService() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("api-key");
        config.setName("config-name");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        service.executeFetchJob();

        verify(youtubeHubService).processJob(
                eq("api-key"),
                eq("config-name"),
                eq(100L),
                eq(null),
                eq(false),
                eq(null)
        );
    }

    @Test
    void stop_ShouldDoNothing_WhenTaskIsNull() {
        service.stop();
        // No exception thrown, coverage hit for "Auto-fetch scheduler is not running."
    }

    @Test
    @SuppressWarnings("null")
    void stop_ShouldDoNothing_WhenTaskIsDone() {
        HubConfig config = new HubConfig();
        config.setFixedRate(1000L);
        when(configsService.getResolvedConfig(null)).thenReturn(config);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class))).thenAnswer(i -> scheduledFuture);
        service.start();

        when(scheduledFuture.isDone()).thenReturn(true);
        service.stop();

        verify(scheduledFuture, never()).cancel(false);
    }

    @Test
    void isSchedulerRunning_ShouldReturnFalse_WhenTaskIsNull() {
        assertThat(service.isSchedulerRunning()).isFalse();
    }

    @Test
    @SuppressWarnings("null")
    void isSchedulerRunning_ShouldReturnFalse_WhenTaskIsCancelled() {
        HubConfig config = new HubConfig();
        config.setFixedRate(1000L);
        when(configsService.getResolvedConfig(null)).thenReturn(config);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class))).thenAnswer(i -> scheduledFuture);
        service.start();

        when(scheduledFuture.isCancelled()).thenReturn(true);
        assertThat(service.isSchedulerRunning()).isFalse();
    }

    @Test
    @SuppressWarnings("null")
    void isSchedulerRunning_ShouldReturnFalse_WhenTaskIsDone() {
        HubConfig config = new HubConfig();
        config.setFixedRate(1000L);
        when(configsService.getResolvedConfig(null)).thenReturn(config);
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), any(Duration.class))).thenAnswer(i -> scheduledFuture);
        service.start();

        when(scheduledFuture.isCancelled()).thenReturn(false);
        when(scheduledFuture.isDone()).thenReturn(true);
        assertThat(service.isSchedulerRunning()).isFalse();
    }

    @Test
    void executeFetchJob_ShouldHandleException() {
        HubConfig config = new HubConfig();
        config.setYoutubeApiKey("api-key");
        when(configsService.getResolvedConfig(null)).thenReturn(config);

        doThrow(new RuntimeException("Job failed")).when(youtubeHubService).processJob(any(), any(), any(), any(), eq(false), any());

        service.executeFetchJob();

        verify(youtubeHubService).processJob(any(), any(), any(), any(), eq(false), any());
    }
}
