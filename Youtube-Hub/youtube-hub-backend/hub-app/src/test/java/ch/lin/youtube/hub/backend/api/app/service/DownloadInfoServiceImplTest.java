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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.lin.youtube.hub.backend.api.app.repository.DownloadInfoRepository;

@ExtendWith(MockitoExtension.class)
class DownloadInfoServiceImplTest {

    @Mock
    private DownloadInfoRepository downloadInfoRepository;

    private DownloadInfoServiceImpl downloadInfoService;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        downloadInfoService = new DownloadInfoServiceImpl(downloadInfoRepository);
    }

    @Test
    void delete_ShouldDeleteById_WhenIdIsProvided() {
        Long id = 1L;
        downloadInfoService.delete(id, null);

        verify(downloadInfoRepository).deleteById(id);
        verifyNoMoreInteractions(downloadInfoRepository);
    }

    @Test
    void delete_ShouldDeleteByTaskId_WhenTaskIdIsProvided() {
        String taskId = "task-123";
        downloadInfoService.delete(null, taskId);

        verify(downloadInfoRepository).deleteByDownloadTaskId(taskId);
        verifyNoMoreInteractions(downloadInfoRepository);
    }

    @Test
    void delete_ShouldDeleteByBoth_WhenBothAreProvided() {
        Long id = 1L;
        String taskId = "task-123";
        downloadInfoService.delete(id, taskId);

        verify(downloadInfoRepository).deleteById(id);
        verify(downloadInfoRepository).deleteByDownloadTaskId(taskId);
        verifyNoMoreInteractions(downloadInfoRepository);
    }

    @Test
    void delete_ShouldCleanTable_WhenNeitherIsProvided() {
        downloadInfoService.delete(null, null);

        verify(downloadInfoRepository).cleanTable();
        verify(downloadInfoRepository).resetSequence();
        verifyNoMoreInteractions(downloadInfoRepository);
    }
}
