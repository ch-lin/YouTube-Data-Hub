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
package ch.lin.youtube.hub.backend.api.app.converter;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class OffsetDateTimeConverterTest {

    private final OffsetDateTimeConverter converter = new OffsetDateTimeConverter();

    @Test
    void convertToDatabaseColumn_nullValue_returnsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumn_validOffsetDateTime_returnsTimestamp() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2024, 8, 7, 10, 30, 0, 0, ZoneOffset.UTC);
        Timestamp timestamp = converter.convertToDatabaseColumn(offsetDateTime);
        assertNotNull(timestamp);
        assertEquals(offsetDateTime.toInstant().toEpochMilli(), timestamp.getTime());
    }

    @Test
    void convertToDatabaseColumn_withNanos_preservesNanos() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2024, 8, 7, 10, 30, 0, 123456789, ZoneOffset.UTC);
        Timestamp timestamp = converter.convertToDatabaseColumn(offsetDateTime);
        assertNotNull(timestamp);
        assertEquals(123456789, timestamp.getNanos());
    }

    @Test
    void convertToEntityAttribute_nullValue_returnsNull() {
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttribute_validTimestamp_returnsOffsetDateTimeWithUtcOffset() {
        // Using Instant to parse the timestamp string to avoid dependency on the
        // default system timezone.
        Timestamp timestamp = Timestamp.from(java.time.Instant.parse("2024-08-07T10:30:00Z"));
        OffsetDateTime offsetDateTime = converter.convertToEntityAttribute(timestamp);
        assertNotNull(offsetDateTime);
        assertEquals(2024, offsetDateTime.getYear());
        assertEquals(8, offsetDateTime.getMonthValue());
        assertEquals(7, offsetDateTime.getDayOfMonth());
        assertEquals(10, offsetDateTime.getHour());
        assertEquals(30, offsetDateTime.getMinute());
        assertEquals(0, offsetDateTime.getSecond());
        assertEquals(ZoneOffset.UTC, offsetDateTime.getOffset());
    }

    @Test
    void convertToEntityAttribute_withNanos_preservesNanos() {
        Timestamp timestamp = Timestamp.valueOf("2024-08-07 10:30:00.123456789");
        OffsetDateTime offsetDateTime = converter.convertToEntityAttribute(timestamp);
        assertNotNull(offsetDateTime);
        assertEquals(123456789, offsetDateTime.getNano());
    }

    @Test
    void convertToDatabaseColumn_nonUtcOffsetDateTime_returnsTimestampInUtc() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2024, 8, 7, 10, 30, 0, 0, ZoneOffset.ofHours(2));
        Timestamp timestamp = converter.convertToDatabaseColumn(offsetDateTime);
        assertNotNull(timestamp);
        OffsetDateTime convertedDateTime = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);

        assertEquals(offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).getHour(), convertedDateTime.getHour());
    }

}
