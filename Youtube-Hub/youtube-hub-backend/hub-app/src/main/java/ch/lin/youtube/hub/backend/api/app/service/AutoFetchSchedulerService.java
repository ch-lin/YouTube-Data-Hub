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

/**
 * Defines the contract for a service that automatically fetches YouTube data.
 * <p>
 * Implementations are responsible for managing a scheduled task that
 * periodically triggers the fetch job based on the application's configuration.
 */
public interface AutoFetchSchedulerService {

    /**
     * Starts the fetch scheduler.
     * <p>
     * If the scheduler is already running, this method will do nothing. The
     * scheduling strategy (Fixed Rate or Cron) is determined by the active
     * {@link ch.lin.youtube.hub.backend.api.domain.model.HubConfig}.
     */
    void start();

    /**
     * Stops the fetch scheduler.
     */
    void stop();

    /**
     * Checks if the scheduler is currently running.
     *
     * @return true if the scheduler is active, false otherwise.
     */
    boolean isSchedulerRunning();

    /**
     * Executes the fetch job immediately.
     * <p>
     * This method is typically invoked by the scheduler but can be called
     * manually. It delegates the actual processing to
     * {@link YoutubeHubService#processJob}.
     */
    void executeFetchJob();
}
