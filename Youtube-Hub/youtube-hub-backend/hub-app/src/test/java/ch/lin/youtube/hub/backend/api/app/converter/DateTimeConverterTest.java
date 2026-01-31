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
        assertThrows(IllegalArgumentException.class,
                () -> DateTimeConverter.parse(dateTimeString));
    }

    @Test
    void parse_dateTimeStringWithoutOffset_throwsIllegalArgumentException() {
        String dateTimeString = "2020-02-23T08:00:00"; // No offset
        assertThrows(IllegalArgumentException.class,
                () -> DateTimeConverter.parse(dateTimeString));
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
