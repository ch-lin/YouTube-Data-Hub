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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ch.lin.platform.api.ApiResponse;
import ch.lin.youtube.hub.backend.api.app.service.ItemService;
import ch.lin.youtube.hub.backend.api.app.service.model.ItemUpdateResult;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;
import ch.lin.youtube.hub.backend.api.dto.ItemResponse;
import ch.lin.youtube.hub.backend.api.dto.UpdateItemRequest;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    @Mock
    private ItemService itemService;

    private ItemController itemController;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        itemController = new ItemController(itemService);
    }

    @Test
    @SuppressWarnings("null")
    void getAllItems_ShouldReturnItems() {
        Item item = new Item();
        item.setVideoId("vid1");
        Page<Item> page = new PageImpl<>(List.of(item));
        Pageable pageable = PageRequest.of(0, 50);
        when(itemService.getItems(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<Page<ItemResponse>> response = itemController.getAllItems(null, null, null, null, null, null, null, pageable);
        Page<ItemResponse> body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.getContent()).hasSize(1);
        assertThat(body.getContent().get(0).getVideoId()).isEqualTo("vid1");
    }

    @Test
    void getAllItems_ShouldLimitPageSize_WhenNoChannelIdsAndSizeTooLarge() {
        Pageable requestedPageable = PageRequest.of(0, 500);
        when(itemService.getItems(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        itemController.getAllItems(null, null, null, null, null, null, null, requestedPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemService).getItems(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void getAllItems_ShouldNotLimitPageSize_WhenChannelIdsProvided() {
        List<String> channelIds = List.of("ch1");
        Pageable requestedPageable = PageRequest.of(0, 500);
        when(itemService.getItems(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        itemController.getAllItems(null, null, null, null, null, null, channelIds, requestedPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemService).getItems(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(500);
    }

    @Test
    void getAllItems_ShouldLimitPageSize_WhenChannelIdsIsEmpty() {
        Pageable requestedPageable = PageRequest.of(0, 500);
        when(itemService.getItems(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        itemController.getAllItems(null, null, null, null, null, null, Collections.emptyList(), requestedPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemService).getItems(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void getAllItems_ShouldHandleNullChannelIds() {
        Pageable requestedPageable = PageRequest.of(0, 50);
        when(itemService.getItems(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        itemController.getAllItems(null, null, null, null, null, null, null, requestedPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(itemService).getItems(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), pageableCaptor.capture());

        // In absence of channelIds, there shouldn't be any filtering based on channel id
        // so page size should still be applied even with no channel ids
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void updateItemFileInfo_ShouldUpdateAndReturnResponse() {
        String videoId = "vid1";
        UpdateItemRequest request = new UpdateItemRequest();
        request.setDownloadTaskId("task1");
        request.setFileSize(1000L);
        request.setFilePath("/path/to/file");
        request.setStatus(ProcessingStatus.DOWNLOADED);

        Item updatedItem = new Item();
        updatedItem.setVideoId(videoId);
        updatedItem.setStatus(ProcessingStatus.DOWNLOADED);

        ItemUpdateResult result = new ItemUpdateResult(updatedItem, List.of("warning"));
        when(itemService.updateItemFileInfo(videoId, "task1", 1000L, "/path/to/file", ProcessingStatus.DOWNLOADED))
                .thenReturn(result);

        ResponseEntity<ApiResponse<ItemResponse>> response = itemController.updateItemFileInfo(videoId, request);
        ApiResponse<ItemResponse> body = response.getBody();
        Objects.requireNonNull(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.getData().getVideoId()).isEqualTo(videoId);
        assertThat(body.getWarnings()).contains("warning");
    }
}
