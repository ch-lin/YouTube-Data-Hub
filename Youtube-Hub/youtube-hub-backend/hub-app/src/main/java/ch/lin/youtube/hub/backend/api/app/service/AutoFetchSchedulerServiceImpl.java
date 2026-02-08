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
import java.time.ZoneId;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;
import ch.lin.youtube.hub.backend.api.domain.model.SchedulerType;
import jakarta.annotation.PostConstruct;

/**
 * Implementation of {@link AutoFetchSchedulerService} that manages a scheduled
 * task to periodically fetch YouTube data.
 * <p>
 * The service supports both fixed-rate and Cron-based scheduling, configurable
 * via {@link HubConfig}.
 */
@Service
public class AutoFetchSchedulerServiceImpl implements AutoFetchSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(AutoFetchSchedulerServiceImpl.class);

    private final ConfigsService configsService;
    private final YoutubeHubService youtubeHubService;
    private final TaskScheduler taskScheduler;

    private ScheduledFuture<?> scheduledTask;

    /**
     * Constructs a new {@code AutoFetchSchedulerServiceImpl} with the required
     * dependencies.
     *
     * @param configsService The service for retrieving application
     * configurations.
     * @param youtubeHubService The service for executing the YouTube data fetch
     * job.
     * @param taskScheduler The Spring TaskScheduler used to schedule the fetch
     * job.
     */
    public AutoFetchSchedulerServiceImpl(ConfigsService configsService, YoutubeHubService youtubeHubService,
            TaskScheduler taskScheduler) {
        this.configsService = configsService;
        this.youtubeHubService = youtubeHubService;
        this.taskScheduler = taskScheduler;
    }

    /**
     * Initializes the service after construction.
     * <p>
     * On application startup, this method checks the active configuration. If
     * {@code autoStartFetchScheduler} is enabled, it starts the scheduler.
     */
    @PostConstruct
    public void init() {
        HubConfig config = configsService.getResolvedConfig(null);
        if (Boolean.TRUE.equals(config.getAutoStartFetchScheduler())) {
            logger.info("Auto-start fetch scheduler is enabled. Starting...");
            start();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation retrieves the active configuration using
     * {@link ConfigsService}. It supports two scheduling types:
     * <ul>
     * <li>{@link SchedulerType#CRON}: Schedules the task based on a Cron
     * expression and time zone.</li>
     * <li>{@link SchedulerType#FIXED_RATE}: Schedules the task to run at a
     * fixed interval.</li>
     * </ul>
     * If the scheduler is already running, this method does nothing.
     */
    @Override
    @SuppressWarnings("null")
    public synchronized void start() {
        if (isSchedulerRunning()) {
            logger.warn("Auto-fetch scheduler is already running.");
            return;
        }

        HubConfig config = configsService.getResolvedConfig(null);
        SchedulerType type = config.getSchedulerType();
        if (type == null) {
            type = SchedulerType.FIXED_RATE; // Default
        }

        if (type == SchedulerType.CRON) {
            String expression = config.getCronExpression();
            String timeZone = config.getCronTimeZone();

            if (StringUtils.hasText(expression)) {
                try {
                    CronTrigger trigger;
                    if (StringUtils.hasText(timeZone)) {
                        trigger = new CronTrigger(expression, ZoneId.of(timeZone));
                    } else {
                        trigger = new CronTrigger(expression);
                    }
                    scheduledTask = taskScheduler.schedule(this::executeFetchJob, trigger);
                    logger.info("Auto-fetch scheduler started with CRON expression: {} (TimeZone: {})", expression,
                            timeZone);
                } catch (IllegalArgumentException e) {
                    logger.error("Failed to start scheduler: Invalid CRON expression '{}'", expression, e);
                }
            } else {
                logger.warn("Cannot start auto-fetch scheduler: CRON expression is empty.");
            }
        } else {
            // FIXED_RATE
            Long rate = config.getFixedRate();
            if (rate != null && rate > 0) {
                // Using scheduleAtFixedRate to align with the configuration name "fixedRate".
                scheduledTask = taskScheduler.scheduleAtFixedRate(this::executeFetchJob, Duration.ofMillis(rate));
                logger.info("Auto-fetch scheduler started with fixed rate of {} ms.", rate);
            } else {
                logger.warn("Cannot start auto-fetch scheduler: Fixed rate is invalid ({}).", rate);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Cancels the currently running scheduled task. If the task is currently
     * executing, it will not be interrupted (may finish its current run).
     */
    @Override
    public synchronized void stop() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false); // false: allow current execution to finish
            logger.info("Auto-fetch scheduler stopped.");
        } else {
            logger.warn("Auto-fetch scheduler is not running.");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Checks if the scheduled task exists and is active (not cancelled and not
     * done).
     */
    @Override
    public boolean isSchedulerRunning() {
        return scheduledTask != null && !scheduledTask.isCancelled() && !scheduledTask.isDone();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method is the actual task executed by the scheduler. It retrieves
     * the latest configuration (API key, etc.) and delegates the processing to
     * {@link YoutubeHubService#processJob}.
     * <p>
     * Any exceptions thrown during execution are caught and logged to prevent
     * the scheduler from crashing.
     */
    @Override
    public void executeFetchJob() {
        logger.info("Executing scheduled auto-fetch job...");
        try {
            // Retrieve the latest config to get the API key and name
            HubConfig config = configsService.getResolvedConfig(null);

            // Execute the job with default parameters for automation
            youtubeHubService.processJob(
                    config.getYoutubeApiKey(),
                    config.getName(),
                    100L, // Default delay
                    null, // publishedAfter (null means incremental update based on last processed time)
                    false, // forcePublishedAfter
                    null // channelIds (null means all channels)
            );
        } catch (Exception e) {
            logger.error("Error occurred during scheduled auto-fetch job execution.", e);
        }
    }
}
