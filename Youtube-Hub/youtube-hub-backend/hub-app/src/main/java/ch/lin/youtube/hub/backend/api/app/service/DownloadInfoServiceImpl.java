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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ch.lin.youtube.hub.backend.api.app.repository.DownloadInfoRepository;

/**
 * Implementation of the {@link DownloadInfoService} interface.
 * <p>
 * This service provides the business logic for managing download information,
 * including deletion of records based on specific criteria.
 */
@Service
public class DownloadInfoServiceImpl implements DownloadInfoService {

    private static final Logger logger = LoggerFactory.getLogger(DownloadInfoServiceImpl.class);

    private final DownloadInfoRepository downloadInfoRepository;

    public DownloadInfoServiceImpl(DownloadInfoRepository downloadInfoRepository) {
        this.downloadInfoRepository = downloadInfoRepository;
    }

    /**
     * Deletes download information based on the provided criteria.
     * <p>
     * <ul>
     * <li>If {@code id} is provided, the record with that ID is deleted.</li>
     * <li>If {@code taskId} is provided, the record associated with that task
     * ID is deleted.</li>
     * <li>If neither is provided, all download information records are deleted,
     * and the table sequence is reset.</li>
     * </ul>
     *
     * @param id the unique identifier of the download info record (optional)
     * @param taskId the unique identifier of the download task (optional)
     */
    @Override
    public void delete(Long id, String taskId) {
        if (id != null) {
            logger.info("Deleting download info with ID: {}", id);
            downloadInfoRepository.deleteById(id);
        }
        if (taskId != null) {
            logger.info("Deleting download info with Task ID: {}", taskId);
            downloadInfoRepository.deleteByDownloadTaskId(taskId);
        }
        if (id == null && taskId == null) {
            logger.info("Cleaning download info table and resetting sequence");
            downloadInfoRepository.cleanTable();
            downloadInfoRepository.resetSequence();
        }
    }
}
