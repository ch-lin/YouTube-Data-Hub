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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.lin.youtube.hub.backend.api.app.service.DownloadInfoService;

/**
 * REST controller for managing download information.
 * <p>
 * This controller provides API endpoints for managing download metadata. It
 * delegates the core business logic to the {@link DownloadInfoService}.
 */
@RestController
@RequestMapping("/download-info")
public class DownloadInfoController {

    private final DownloadInfoService downloadInfoService;

    public DownloadInfoController(DownloadInfoService downloadInfoService) {
        this.downloadInfoService = downloadInfoService;
    }

    /**
     * Deletes download information.
     * <p>
     * This endpoint allows deleting specific download records by ID or Task ID.
     * If no parameters are provided, it triggers a cleanup of all download
     * information.
     *
     * @param id the unique identifier of the download info record (optional)
     * @param taskId the unique identifier of the download task (optional)
     */
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam(required = false) Long id, @RequestParam(required = false) String taskId) {
        downloadInfoService.delete(id, taskId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
