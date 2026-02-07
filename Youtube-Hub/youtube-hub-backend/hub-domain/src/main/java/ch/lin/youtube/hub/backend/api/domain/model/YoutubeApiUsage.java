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
package ch.lin.youtube.hub.backend.api.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static ch.lin.youtube.hub.backend.api.domain.model.YoutubeApiUsage.TABLE_NAME;
import static ch.lin.youtube.hub.backend.api.domain.model.YoutubeApiUsage.USAGE_DATE_COLUMN;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the daily usage statistics of the YouTube Data API. This entity
 * tracks the number of requests and estimated quota usage per day.
 */
@Table(name = TABLE_NAME, indexes = {
    @Index(name = BaseEntity.ID_INDEX, columnList = BaseEntity.ID_COLUMN),
    @Index(name = YoutubeApiUsage.USAGE_DATE_INDEX, columnList = USAGE_DATE_COLUMN)}, uniqueConstraints = {
    @UniqueConstraint(columnNames = USAGE_DATE_COLUMN)})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"usageDate"}, callSuper = false)
public class YoutubeApiUsage extends BaseEntity {

    /**
     * The name of the api usage table in the database.
     */
    public static final String TABLE_NAME = "api_usage";

    /**
     * The name of the usage date column in the database.
     */
    public static final String USAGE_DATE_COLUMN = "usage_date";

    /**
     * The name of the index for the usage date column.
     */
    public static final String USAGE_DATE_INDEX = "api_usage_date_index";

    /**
     * The name of the request count column in the database.
     */
    public static final String REQUEST_COUNT_COLUMN = "request_count";

    /**
     * The name of the quota used column in the database.
     */
    public static final String QUOTA_USED_COLUMN = "quota_used";

    /**
     * The name of the last updated column in the database.
     */
    public static final String LAST_UPDATED_COLUMN = "last_updated";

    /**
     * The ZoneId for YouTube Data API quota reset (Pacific Time). Quotas reset
     * at midnight Pacific Time.
     */
    public static final ZoneId YOUTUBE_QUOTA_ZONE = ZoneId.of("America/Los_Angeles");

    /**
     * Helper method to get the current date in the YouTube quota time zone. Use
     * this method to determine the 'usageDate' for the current request.
     *
     * @return The current LocalDate in Pacific Time.
     */
    public static LocalDate getCurrentQuotaDate() {
        return LocalDate.now(YOUTUBE_QUOTA_ZONE);
    }

    /**
     * The date for which the usage is recorded. YouTube quotas typically reset
     * at midnight Pacific Time.
     */
    @NotNull
    @Column(name = YoutubeApiUsage.USAGE_DATE_COLUMN, nullable = false, unique = true)
    private LocalDate usageDate;

    /**
     * The total number of API requests made on this date.
     */
    @Column(name = YoutubeApiUsage.REQUEST_COUNT_COLUMN)
    private long requestCount;

    /**
     * The estimated amount of quota used on this date. Different API calls
     * consume different amounts of quota (e.g., search costs 100, list costs
     * 1).
     */
    @Column(name = YoutubeApiUsage.QUOTA_USED_COLUMN)
    private long quotaUsed;

    /**
     * The timestamp of the last API request made on this date.
     */
    @Column(name = YoutubeApiUsage.LAST_UPDATED_COLUMN, columnDefinition = "TIMESTAMP")
    private OffsetDateTime lastUpdated;

    /**
     * Increments the request count and quota usage for a new request.
     *
     * @param cost The estimated quota cost of the request.
     */
    public void increment(long cost) {
        this.requestCount++;
        this.quotaUsed += cost;
        this.lastUpdated = OffsetDateTime.now();
    }
}
