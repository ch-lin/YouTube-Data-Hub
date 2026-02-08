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

class HubConfigTest {

    @Test
    void testNoArgsConstructor_ShouldHaveDefaultValues() {
        HubConfig config = new HubConfig();
        assertThat(config).isNotNull();
        // Verify default values set in the entity
        assertThat(config.getEnabled()).isFalse();
        assertThat(config.getAutoStartFetchScheduler()).isFalse();
        assertThat(config.getFixedRate()).isEqualTo(86400000L); // Default 24 hours
    }

    @Test
    void testAllArgsConstructor_ShouldSetAllFields() {
        HubConfig config = new HubConfig(
                "test-config",
                true,
                "api-key",
                "client-id",
                "client-secret",
                true,
                SchedulerType.CRON,
                3600000L,
                "0 0 * * * *",
                "UTC"
        );

        assertThat(config.getName()).isEqualTo("test-config");
        assertThat(config.getEnabled()).isTrue();
        assertThat(config.getYoutubeApiKey()).isEqualTo("api-key");
        assertThat(config.getClientId()).isEqualTo("client-id");
        assertThat(config.getClientSecret()).isEqualTo("client-secret");
        assertThat(config.getAutoStartFetchScheduler()).isTrue();
        assertThat(config.getSchedulerType()).isEqualTo(SchedulerType.CRON);
        assertThat(config.getFixedRate()).isEqualTo(3600000L);
        assertThat(config.getCronExpression()).isEqualTo("0 0 * * * *");
        assertThat(config.getCronTimeZone()).isEqualTo("UTC");
    }

    @Test
    void testSettersAndGetters_ShouldWorkForNewFields() {
        HubConfig config = new HubConfig();
        config.setName("new-name");
        config.setAutoStartFetchScheduler(true);
        config.setSchedulerType(SchedulerType.FIXED_RATE);
        config.setFixedRate(1000L);
        config.setCronExpression("0 0 12 * * ?");
        config.setCronTimeZone("Asia/Taipei");

        assertThat(config.getName()).isEqualTo("new-name");
        assertThat(config.getAutoStartFetchScheduler()).isTrue();
        assertThat(config.getSchedulerType()).isEqualTo(SchedulerType.FIXED_RATE);
        assertThat(config.getFixedRate()).isEqualTo(1000L);
        assertThat(config.getCronExpression()).isEqualTo("0 0 12 * * ?");
        assertThat(config.getCronTimeZone()).isEqualTo("Asia/Taipei");
    }

    @Test
    void testEqualsAndHashCode_ShouldIncludeNewFields() {
        HubConfig config1 = new HubConfig("cfg", true, "k", "i", "s", true, SchedulerType.CRON, 100L, "cron", "UTC");
        HubConfig config2 = new HubConfig("cfg", true, "k", "i", "s", true, SchedulerType.CRON, 100L, "cron", "UTC");
        HubConfig config3 = new HubConfig("cfg", true, "k", "i", "s", false, SchedulerType.CRON, 100L, "cron", "UTC"); // Different autoStart

        assertThat(config1).isEqualTo(config2);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());

        assertThat(config1).isNotEqualTo(config3);
        assertThat(config1.hashCode()).isNotEqualTo(config3.hashCode());
    }
}
