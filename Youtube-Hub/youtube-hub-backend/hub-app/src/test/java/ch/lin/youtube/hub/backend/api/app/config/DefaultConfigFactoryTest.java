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
package ch.lin.youtube.hub.backend.api.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import ch.lin.youtube.hub.backend.api.domain.model.HubConfig;

class DefaultConfigFactoryTest {

    @Test
    void create_ShouldReturnHubConfig_FromProperties() {
        HubDefaultProperties properties = new HubDefaultProperties();
        properties.setName("test-config");
        properties.setEnabled(false);
        properties.setYoutubeApiKey("api-key");
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");

        DefaultConfigFactory factory = new DefaultConfigFactory();
        HubConfig config = factory.create(properties);

        assertThat(config.getName()).isEqualTo("test-config");
        assertThat(config.getEnabled()).isFalse();
        assertThat(config.getYoutubeApiKey()).isEqualTo("api-key");
        assertThat(config.getClientId()).isEqualTo("client-id");
        assertThat(config.getClientSecret()).isEqualTo("client-secret");
    }
}
