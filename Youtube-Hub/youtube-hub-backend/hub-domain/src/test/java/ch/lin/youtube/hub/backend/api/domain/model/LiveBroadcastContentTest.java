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

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class LiveBroadcastContentTest {

    @Test
    void testEnumValuesExist() {
        // Test that all expected enum constants exist
        assertThat(LiveBroadcastContent.LIVE).isNotNull();
        assertThat(LiveBroadcastContent.NONE).isNotNull();
        assertThat(LiveBroadcastContent.UPCOMING).isNotNull();
    }

    @Test
    void testValueOf_withValidStrings() {
        // Test that valueOf correctly returns the enum constant for a given string
        assertThat(LiveBroadcastContent.valueOf("LIVE")).isEqualTo(LiveBroadcastContent.LIVE);
        assertThat(LiveBroadcastContent.valueOf("NONE")).isEqualTo(LiveBroadcastContent.NONE);
        assertThat(LiveBroadcastContent.valueOf("UPCOMING")).isEqualTo(LiveBroadcastContent.UPCOMING);
    }

    @Test
    void testValues_returnsAllConstants() {
        // Test that the values() method returns all enum constants in the declared
        // order
        LiveBroadcastContent[] expectedValues = {LiveBroadcastContent.LIVE, LiveBroadcastContent.NONE,
            LiveBroadcastContent.UPCOMING};
        LiveBroadcastContent[] actualValues = LiveBroadcastContent.values();

        assertThat(actualValues).containsExactly(expectedValues);
        assertThat(actualValues).hasSize(3);
    }
}
