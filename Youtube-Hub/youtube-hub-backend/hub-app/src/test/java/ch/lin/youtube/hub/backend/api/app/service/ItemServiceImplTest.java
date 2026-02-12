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

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import ch.lin.platform.exception.InvalidRequestException;
import ch.lin.youtube.hub.backend.api.app.repository.ItemRepository;
import ch.lin.youtube.hub.backend.api.app.repository.TagRepository;
import ch.lin.youtube.hub.backend.api.app.service.model.ItemUpdateResult;
import ch.lin.youtube.hub.backend.api.common.exception.ItemNotFoundException;
import ch.lin.youtube.hub.backend.api.domain.model.DownloadInfo;
import ch.lin.youtube.hub.backend.api.domain.model.Item;
import ch.lin.youtube.hub.backend.api.domain.model.ProcessingStatus;
import ch.lin.youtube.hub.backend.api.domain.model.Tag;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private TagRepository tagRepository;

    private ItemServiceImpl itemService;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        itemService = new ItemServiceImpl(itemRepository, tagRepository);
    }

    @Test
    void cleanup_ShouldCleanTable() {
        itemService.cleanup();
        verify(itemRepository).cleanTable();
    }

    @Test
    @SuppressWarnings({"unchecked", "null"})
    void getItems_ShouldCallRepositoryWithSpecification() {
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        Page<Item> result = itemService.getItems(true, false, "none", false, false, false, null, Pageable.unpaged());

        assertThat(result).isEmpty();
        verify(itemRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "null"})
    void getItems_WithDifferentFilters_ShouldCallRepository() {
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        itemService.getItems(false, true, "not_none", true, true, true, List.of("ch1"), Pageable.unpaged());

        verify(itemRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void updateItemFileInfo_ShouldThrow_WhenVideoIdInvalid() {
        assertThatThrownBy(() -> itemService.updateItemFileInfo(null, "task1", 100L, "path", ProcessingStatus.DOWNLOADED))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Video ID cannot be null or empty");

        assertThatThrownBy(() -> itemService.updateItemFileInfo("", "task1", 100L, "path", ProcessingStatus.DOWNLOADED))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Video ID cannot be null or empty");
    }

    @Test
    void updateItemFileInfo_ShouldThrow_WhenFileSizeNegative() {
        assertThatThrownBy(() -> itemService.updateItemFileInfo("vid1", "task1", -1L, "path", ProcessingStatus.DOWNLOADED))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("File size cannot be negative");
    }

    @Test
    void updateItemFileInfo_ShouldThrow_WhenItemNotFound() {
        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.updateItemFileInfo("vid1", "task1", 100L, "path", ProcessingStatus.DOWNLOADED))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    void updateItemFileInfo_ShouldUpdateStatusOnly_WhenNoFileInfo() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setStatus(ProcessingStatus.NEW);
        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        ItemUpdateResult result = itemService.updateItemFileInfo("vid1", null, null, null, ProcessingStatus.DOWNLOADED);

        assertThat(result.updatedItem().getStatus()).isEqualTo(ProcessingStatus.DOWNLOADED);
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void updateItemFileInfo_ShouldUpdateExistingDownloadInfo_WhenTaskIdProvided() {
        Item item = new Item();
        item.setVideoId("vid1");
        DownloadInfo info = new DownloadInfo();
        info.setDownloadTaskId("task1");
        item.setDownloadInfos(new HashSet<>(List.of(info)));

        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        ItemUpdateResult result = itemService.updateItemFileInfo("vid1", "task1", 1024L, "/path/to/file.mp4", null);

        assertThat(info.getFileSize()).isEqualTo(1024L);
        assertThat(info.getFilePath()).isEqualTo("/path/to/file.mp4");
        assertThat(result.updatedItem()).isEqualTo(item);
    }

    @Test
    void updateItemFileInfo_ShouldThrow_WhenTaskIdProvidedButNotFound() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setDownloadInfos(new HashSet<>());

        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.updateItemFileInfo("vid1", "task1", 100L, "path", null))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("DownloadInfo with taskId task1 not found");
    }

    @Test
    void updateItemFileInfo_ShouldCreateNewDownloadInfo_WhenTaskIdNotProvidedAndNoManualInfo() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setDownloadInfos(new HashSet<>());

        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        itemService.updateItemFileInfo("vid1", null, 2048L, "path", null);

        assertThat(item.getDownloadInfos()).hasSize(1);
        DownloadInfo info = item.getDownloadInfos().iterator().next();
        assertThat(info.getDownloadTaskId()).isNull();
        assertThat(info.getFileSize()).isEqualTo(2048L);
    }

    @Test
    void updateItemFileInfo_ShouldAssociateTag_WhenTagsFoundInPath() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setDownloadInfos(new HashSet<>());
        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        Tag tagShort = new Tag();
        tagShort.setName("Short");
        Tag tagLong = new Tag();
        tagLong.setName("LongerTag");

        when(tagRepository.findTagsWithinFilePath("path/LongerTag/file.mp4")).thenReturn(List.of(tagShort, tagLong));
        when(itemRepository.findByTagAndVideoIdNotAndDownloadInfosFileSize(any(), anyString(), anyLong())).thenReturn(Collections.emptyList());

        ItemUpdateResult result = itemService.updateItemFileInfo("vid1", null, 100L, "path/LongerTag/file.mp4", null);

        assertThat(item.getTag()).isEqualTo(tagLong);
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void updateItemFileInfo_ShouldWarn_WhenNoTagsFoundInPath() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setDownloadInfos(new HashSet<>());
        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        when(tagRepository.findTagsWithinFilePath("path/file.mp4")).thenReturn(Collections.emptyList());

        ItemUpdateResult result = itemService.updateItemFileInfo("vid1", null, 100L, "path/file.mp4", null);

        assertThat(item.getTag()).isNull();
        assertThat(result.warnings()).contains("No matching tag found for the given file path.");
    }

    @Test
    void updateItemFileInfo_ShouldWarn_WhenDuplicateFound() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setDownloadInfos(new HashSet<>());
        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        Tag tag = new Tag();
        tag.setName("Tag");
        when(tagRepository.findTagsWithinFilePath("path/Tag/file.mp4")).thenReturn(List.of(tag));

        Item duplicate = new Item();
        duplicate.setVideoId("vid2");
        duplicate.setTitle("Dup Title");
        when(itemRepository.findByTagAndVideoIdNotAndDownloadInfosFileSize(tag, "vid1", 100L)).thenReturn(List.of(duplicate));

        ItemUpdateResult result = itemService.updateItemFileInfo("vid1", null, 100L, "path/Tag/file.mp4", null);

        assertThat(item.getTag()).isEqualTo(tag);
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0)).contains("Potential duplicate");
        assertThat(result.warnings().get(0)).contains("Dup Title");
    }

    @Test
    void updateItemFileInfo_ShouldUpdateFileSizeOnly_WhenFilePathIsNull() {
        Item item = new Item();
        item.setVideoId("vid1");
        DownloadInfo info = new DownloadInfo();
        info.setDownloadTaskId("task1");
        info.setFilePath("old/path");
        item.setDownloadInfos(new HashSet<>(List.of(info)));

        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        itemService.updateItemFileInfo("vid1", "task1", 999L, null, null);

        assertThat(info.getFileSize()).isEqualTo(999L);
        assertThat(info.getFilePath()).isEqualTo("old/path");
    }

    @Test
    void updateItemFileInfo_ShouldUpdateFilePathOnly_WhenFileSizeIsNull() {
        Item item = new Item();
        item.setVideoId("vid1");
        DownloadInfo info = new DownloadInfo();
        info.setDownloadTaskId("task1");
        info.setFileSize(500L);
        item.setDownloadInfos(new HashSet<>(List.of(info)));

        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));
        when(tagRepository.findTagsWithinFilePath("new/path")).thenReturn(Collections.emptyList());

        itemService.updateItemFileInfo("vid1", "task1", null, "new/path", null);

        assertThat(info.getFilePath()).isEqualTo("new/path");
        assertThat(info.getFileSize()).isEqualTo(500L);
    }

    @Test
    void updateItemFileInfo_ShouldTreatBlankTaskIdAsManual() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setDownloadInfos(new HashSet<>());

        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        itemService.updateItemFileInfo("vid1", "", 100L, null, null);

        assertThat(item.getDownloadInfos()).hasSize(1);
        DownloadInfo info = item.getDownloadInfos().iterator().next();
        assertThat(info.getDownloadTaskId()).isNull();
    }

    @Test
    void updateItemFileInfo_ShouldUpdateExistingManualDownloadInfo() {
        Item item = new Item();
        item.setVideoId("vid1");
        DownloadInfo manualInfo = new DownloadInfo();
        manualInfo.setDownloadTaskId(null);
        manualInfo.setFileSize(100L);
        item.setDownloadInfos(new HashSet<>(List.of(manualInfo)));

        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        itemService.updateItemFileInfo("vid1", null, 200L, null, null);

        assertThat(item.getDownloadInfos()).hasSize(1);
        assertThat(manualInfo.getFileSize()).isEqualTo(200L);
    }

    @Test
    void updateItemFileInfo_ShouldCreateNewManualInfo_WhenOnlyTaskInfosExist() {
        Item item = new Item();
        item.setVideoId("vid1");
        DownloadInfo taskInfo = new DownloadInfo();
        taskInfo.setDownloadTaskId("task1");
        item.setDownloadInfos(new HashSet<>(List.of(taskInfo)));

        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        itemService.updateItemFileInfo("vid1", null, 300L, null, null);

        assertThat(item.getDownloadInfos()).hasSize(2);
        boolean hasManual = item.getDownloadInfos().stream().anyMatch(di -> di.getDownloadTaskId() == null && di.getFileSize() == 300L);
        assertThat(hasManual).isTrue();
    }

    @Test
    void updateItemFileInfo_ShouldSkipTagLogic_WhenFilePathIsBlank() {
        Item item = new Item();
        item.setVideoId("vid1");
        DownloadInfo info = new DownloadInfo();
        info.setDownloadTaskId("task1");
        item.setDownloadInfos(new HashSet<>(List.of(info)));

        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        itemService.updateItemFileInfo("vid1", "task1", 100L, "   ", null);

        assertThat(info.getFilePath()).isEqualTo("   ");
        verify(tagRepository, never()).findTagsWithinFilePath(anyString());
    }

    @Test
    void updateItemFileInfo_ShouldSkipDuplicateCheck_WhenFileSizeIsZero() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setDownloadInfos(new HashSet<>());
        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        Tag tag = new Tag();
        tag.setName("Tag");
        when(tagRepository.findTagsWithinFilePath("path/Tag/file.mp4")).thenReturn(List.of(tag));

        itemService.updateItemFileInfo("vid1", null, 0L, "path/Tag/file.mp4", null);

        assertThat(item.getTag()).isEqualTo(tag);
        verify(itemRepository, never()).findByTagAndVideoIdNotAndDownloadInfosFileSize(any(), anyString(), anyLong());
    }

    @Test
    void updateItemFileInfo_ShouldFormatMultipleDuplicates() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setDownloadInfos(new HashSet<>());
        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        Tag tag = new Tag();
        tag.setName("Tag");
        when(tagRepository.findTagsWithinFilePath("path/Tag/file.mp4")).thenReturn(List.of(tag));

        Item dup1 = new Item();
        dup1.setVideoId("v2");
        dup1.setTitle("Title2");
        Item dup2 = new Item();
        dup2.setVideoId("v3");
        dup2.setTitle("Title3");

        when(itemRepository.findByTagAndVideoIdNotAndDownloadInfosFileSize(tag, "vid1", 100L)).thenReturn(List.of(dup1, dup2));

        ItemUpdateResult result = itemService.updateItemFileInfo("vid1", null, 100L, "path/Tag/file.mp4", null);

        assertThat(result.warnings()).hasSize(1);
        String warning = result.warnings().get(0);
        assertThat(warning).contains("2 other item(s)");
        assertThat(warning).contains("'Title2' (ID: v2)");
        assertThat(warning).contains("'Title3' (ID: v3)");
        assertThat(warning).contains(", ");
    }

    @Test
    void updateItemFileInfo_ShouldAssociateTag_WhenFileSizeIsNull() {
        Item item = new Item();
        item.setVideoId("vid1");
        item.setDownloadInfos(new HashSet<>());
        when(itemRepository.findByVideoIdWithAssociations("vid1")).thenReturn(Optional.of(item));

        Tag tag = new Tag();
        tag.setName("Tag");
        when(tagRepository.findTagsWithinFilePath("path/Tag/file.mp4")).thenReturn(List.of(tag));

        itemService.updateItemFileInfo("vid1", null, null, "path/Tag/file.mp4", null);

        assertThat(item.getTag()).isEqualTo(tag);
        verify(itemRepository, never()).findByTagAndVideoIdNotAndDownloadInfosFileSize(any(), anyString(), anyLong());
    }

    @SuppressWarnings({"unchecked", "null"})
    @Test
    void getItems_ShouldHandleNullNotDownloaded() {
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
        itemService.getItems(null, false, null, false, false, false, null, Pageable.unpaged());
        verify(itemRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @SuppressWarnings({"unchecked", "null"})
    @Test
    void getItems_ShouldHandleEdgeCaseFilters() {
        when(itemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        // Case 1: Nulls for Boolean filters, Blank liveBroadcastContent, Empty channelIds
        // filterNoFileSize = null, filterNoTag = null, filterDeleted = null
        itemService.getItems(false, null, "   ", false, null, null, Collections.emptyList(), Pageable.unpaged());

        // Case 2: Invalid liveBroadcastContent
        itemService.getItems(false, false, "invalid_value", false, false, false, null, Pageable.unpaged());

        // Case 3: liveBroadcastContent = "not_none", pastOnly = null
        itemService.getItems(false, false, "not_none", null, false, false, null, Pageable.unpaged());

        verify(itemRepository, times(3)).findAll(any(Specification.class), any(Pageable.class));
    }

    @SuppressWarnings({"unchecked", "null"})
    @Test
    void getItems_ShouldExecuteSpecificationPredicates() {
        // Mocks for Criteria API to ensure branch coverage of Specification lambdas
        Root<Item> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);
        Path<Object> path = mock(Path.class);
        Join<Object, Object> join = mock(Join.class);
        Fetch<Object, Object> fetch = mock(Fetch.class);

        // Setup common mock interactions
        when(cb.conjunction()).thenReturn(predicate);
        when(cb.and(any(), any())).thenReturn(predicate);
        lenient().when(cb.or(any(), any())).thenReturn(predicate);
        when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(predicate);
        when(cb.notEqual(any(Expression.class), any(Object.class))).thenReturn(predicate);
        when(cb.isNotNull(any())).thenReturn(predicate);
        when(cb.lessThan(any(Expression.class), any(OffsetDateTime.class))).thenReturn(predicate);
        when(cb.greaterThanOrEqualTo(any(Expression.class), any(OffsetDateTime.class))).thenReturn(predicate);
        when(cb.isNull(any())).thenReturn(predicate);

        when(root.get(anyString())).thenReturn(path);
        when(root.join(anyString())).thenReturn(join);
        when(root.fetch(anyString(), any(JoinType.class))).thenReturn(fetch);
        when(fetch.fetch(anyString(), any(JoinType.class))).thenReturn(fetch);

        when(path.get(anyString())).thenReturn(path);
        when(path.in(any(Object[].class))).thenReturn(predicate);
        when(path.in(any(java.util.Collection.class))).thenReturn(predicate);
        Expression<OffsetDateTime> offsetDateTimeExpression = mock(Expression.class);
        when(path.as(OffsetDateTime.class)).thenReturn(offsetDateTimeExpression);

        when(join.get(anyString())).thenReturn(path);

        ArgumentCaptor<Specification<Item>> captor = ArgumentCaptor.forClass(Specification.class);
        when(itemRepository.findAll(captor.capture(), any(Pageable.class))).thenReturn(Page.empty());

        // 1. Execute with notDownloaded = true
        itemService.getItems(true, false, null, false, false, false, null, Pageable.unpaged());
        Specification<Item> spec1 = captor.getValue();

        // Execute predicate logic for different query types
        spec1.toPredicate(root, null, cb); // Query is null
        doReturn(Long.class).when(query).getResultType();
        spec1.toPredicate(root, query, cb); // Count query
        doReturn(long.class).when(query).getResultType();
        spec1.toPredicate(root, query, cb); // Primitive count query
        doReturn(Item.class).when(query).getResultType();
        spec1.toPredicate(root, query, cb); // Fetch query

        // 2. Execute with other filters to hit remaining branches
        itemService.getItems(false, true, "none", false, true, true, List.of("ch1"), Pageable.unpaged());
        Specification<Item> spec2 = captor.getValue();
        spec2.toPredicate(root, query, cb);
        spec2.toPredicate(root, null, cb); // Cover query == null inside filterNoFileSize

        // 3. Execute with liveBroadcastContent = "not_none" and pastOnly = true/false
        itemService.getItems(false, false, "not_none", true, false, false, null, Pageable.unpaged());
        captor.getValue().toPredicate(root, query, cb);
        itemService.getItems(false, false, "not_none", false, false, false, null, Pageable.unpaged());
        captor.getValue().toPredicate(root, query, cb);
    }
}
