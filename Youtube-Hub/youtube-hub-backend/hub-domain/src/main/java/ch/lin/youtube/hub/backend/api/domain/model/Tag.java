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

import static ch.lin.youtube.hub.backend.api.domain.model.Tag.NAME_COLUMN;
import static ch.lin.youtube.hub.backend.api.domain.model.Tag.NAME_INDEX;
import static ch.lin.youtube.hub.backend.api.domain.model.Tag.TABLE_NAME;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a tag used to categorize video items, stored as a JPA entity.
 */
@Table(name = TABLE_NAME, indexes = {
    @Index(name = BaseEntity.ID_INDEX, columnList = BaseEntity.ID_COLUMN),
    @Index(name = NAME_INDEX, columnList = NAME_COLUMN)}, uniqueConstraints = {
    @UniqueConstraint(columnNames = NAME_COLUMN)})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"name"}, callSuper = false)
public class Tag extends BaseEntity {

    /**
     * The name of the tag table in the database.
     */
    public static final String TABLE_NAME = "tag";

    /**
     * The name of the name column in the database.
     */
    public static final String NAME_COLUMN = "name";

    /**
     * The name of the index for the name column.
     */
    public static final String NAME_INDEX = "tag_name_index";

    /**
     * The unique name of the tag (e.g., "Tutorial", "Music Video"). This serves
     * as the business key.
     */
    @NotNull
    @Column(name = Tag.NAME_COLUMN, unique = true, nullable = false)
    private String name;

    /**
     * The set of {@link Item} entities associated with this tag.
     * <p>
     * This defines a one-to-many relationship managed by the {@code Item}
     * entity. Persisting or merging a {@code Tag} will cascade the operation to
     * its associated {@code Item}s. Deleting a tag does not remove the
     * associated items.
     */
    @OneToMany(mappedBy = "tag", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Item> items;

    /**
     * Adds an item to this tag.
     * <p>
     * This is a convenience helper method to ensure the bidirectional
     * relationship between {@code Tag} and {@code Item} is correctly maintained
     * on both sides. It adds the item to the tag's collection and sets this tag
     * on the item.
     *
     * @param item The item to add.
     */
    public void addItem(Item item) {
        if (this.items == null) {
            this.items = new HashSet<>();
        }
        this.items.add(item);
        item.setTag(this);
    }

    /**
     * Removes an item from this tag.
     * <p>
     * This is a convenience helper method to ensure the bidirectional
     * relationship between {@code Tag} and {@code Item} is correctly managed on
     * both sides. It removes the item from the tag's collection and unsets the
     * tag reference on the item.
     *
     * @param item The item to remove.
     */
    public void removeItem(Item item) {
        if (this.items != null) {
            this.items.remove(item);
            item.setTag(null);
        }
    }
}
