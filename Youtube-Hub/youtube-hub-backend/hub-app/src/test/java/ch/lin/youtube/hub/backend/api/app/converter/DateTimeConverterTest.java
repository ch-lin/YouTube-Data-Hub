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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DateTimeConverterTest {

    @Test
    void testConstructorIsPrivateAndThrowsException() throws NoSuchMethodException {
        Constructor<DateTimeConverter> constructor = DateTimeConverter.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "The constructor should be private.");

        constructor.setAccessible(true);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, constructor::newInstance,
                "Instantiating the utility class should throw an exception.");
        assertEquals(UnsupportedOperationException.class, thrown.getCause().getClass());
    }

    @Test
    void parse_validDateTimeString_returnsOffsetDateTime() {
        String dateTimeString = "2020-02-23T08:00:00Z";
        OffsetDateTime offsetDateTime = DateTimeConverter.parse(dateTimeString);
        assertNotNull(offsetDateTime);
        assertEquals(2020, offsetDateTime.getYear());
        assertEquals(2, offsetDateTime.getMonthValue());
        assertEquals(23, offsetDateTime.getDayOfMonth());
        assertEquals(8, offsetDateTime.getHour());
        assertEquals(0, offsetDateTime.getMinute());
        assertEquals(0, offsetDateTime.getSecond());
    }

    @Test
    void parse_validDateTimeStringWithOffset_returnsOffsetDateTime() {
        String dateTimeString = "2020-02-23T09:00:00+01:00";
        OffsetDateTime offsetDateTime = DateTimeConverter.parse(dateTimeString);
        assertNotNull(offsetDateTime);
        assertEquals(2020, offsetDateTime.getYear());
        assertEquals(2, offsetDateTime.getMonthValue());
        assertEquals(23, offsetDateTime.getDayOfMonth());
        assertEquals(9, offsetDateTime.getHour());
        assertEquals(ZoneOffset.ofHours(1), offsetDateTime.getOffset());
        assertEquals(OffsetDateTime.parse("2020-02-23T08:00:00Z").toInstant(), offsetDateTime.toInstant());
    }

    @Test
    void parse_invalidDateTimeString_throwsIllegalArgumentException() {
        String dateTimeString = "invalid-date-time-string";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DateTimeConverter.parse(dateTimeString));
        assertNotNull(exception);
    }

    @Test
    void parse_dateTimeStringWithoutOffset_throwsIllegalArgumentException() {
        String dateTimeString = "2020-02-23T08:00:00"; // No offset
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> DateTimeConverter.parse(dateTimeString));
        assertNotNull(exception);
    }

    @Test
    void format_validOffsetDateTime_returnsDateTimeString() {
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2020-02-23T08:00:00Z");
        String dateTimeString = DateTimeConverter.format(offsetDateTime);
        assertEquals("2020-02-23T08:00:00Z", dateTimeString);
    }

    @Test
    void format_nullOffsetDateTime_returnsNull() {
        String dateTimeString = DateTimeConverter.format(null);
        assertNull(dateTimeString);
    }

    @Test
    void format_nonUtcOffsetDateTime_returnsDateTimeStringWithOffset() {
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2020-02-23T09:30:00+02:00");
        String dateTimeString = DateTimeConverter.format(offsetDateTime);
        assertEquals("2020-02-23T09:30:00+02:00", dateTimeString);
    }

}
