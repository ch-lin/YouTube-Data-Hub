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

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagTest {

    private Tag tag;
    private String tagName;

    @BeforeEach
    void setUp() {
        tagName = "Test Tag";
        tag = new Tag(tagName, new HashSet<>());
    }

    @Test
    void testGettersAndSetters() {
        Tag t = new Tag();
        Long id = 1L;
        Set<Item> items = new HashSet<>();

        t.setId(id);
        t.setName(tagName);
        t.setItems(items);

        assertEquals(id, t.getId());
        assertEquals(tagName, t.getName());
        assertEquals(items, t.getItems());
    }

    @Test
    void testNoArgsConstructor() {
        Tag t = new Tag();
        assertNotNull(t);
        assertNull(t.getId());
        assertNull(t.getName());
        assertNull(t.getItems());
    }

    @Test
    void testAllArgsConstructor() {
        Set<Item> items = new HashSet<>();
        Tag t = new Tag(tagName, items);

        assertNotNull(t);
        assertEquals(tagName, t.getName());
        assertEquals(items, t.getItems());
    }

    @Test
    void testEqualsAndHashCode() {
        Tag tag2 = new Tag(tagName, new HashSet<>()); // Same name, should be equal
        Tag tag3 = new Tag("Another Tag", new HashSet<>()); // Different name, should not be equal

        // Test for equality
        assertEquals(tag, tag2);
        assertEquals(tag.hashCode(), tag2.hashCode());

        // Test for inequality
        assertNotEquals(tag, tag3);
        assertNotEquals(tag.hashCode(), tag3.hashCode());

        // Test against null and other types
        assertNotEquals(tag, null);
        assertNotEquals(tag, new Object());
    }

    @Test
    void testAddItem() {
        Item item = new Item();
        item.setVideoId("video123");

        // The set is initialized as empty in setUp, not null.
        assertNotNull(tag.getItems());
        assertTrue(tag.getItems().isEmpty());

        // Pre-set the tag on the item to stabilize its hashCode before adding it to the
        // set.
        item.setTag(tag);

        tag.addItem(item);

        assertNotNull(tag.getItems());
        assertTrue(tag.getItems().contains(item));
        assertEquals(tag, item.getTag(), "Bidirectional relationship should be set on the item.");
    }

    @Test
    void testRemoveItem() {
        Item item = new Item();
        item.setVideoId("video123");

        // Pre-set the tag on the item to stabilize its hashCode.
        item.setTag(tag);

        // First add the item
        tag.addItem(item);
        assertTrue(tag.getItems().contains(item));
        assertEquals(tag, item.getTag());

        // Now remove it
        tag.removeItem(item);

        assertFalse(tag.getItems().contains(item));
        assertNull(item.getTag(), "Bidirectional relationship should be unset on the item.");
    }

    @Test
    void testRemoveItemFromNullSet() {
        Tag t = new Tag();
        Item item = new Item();
        // Should not throw a NullPointerException
        assertDoesNotThrow(() -> t.removeItem(item));
    }

    @Test
    void testAddItemWhenItemsSetIsNull() {
        Tag t = new Tag(); // items is null here
        assertNull(t.getItems(), "Items collection should be null initially.");

        Item item = new Item();
        item.setVideoId("video123");
        // Pre-set the tag to stabilize hashCode.
        item.setTag(t);

        t.addItem(item);

        assertNotNull(t.getItems(), "Items collection should be initialized after adding an item.");
        assertTrue(t.getItems().contains(item), "Item should be in the collection after being added.");
        assertEquals(t, item.getTag(), "Bidirectional relationship should be set.");
    }
}
