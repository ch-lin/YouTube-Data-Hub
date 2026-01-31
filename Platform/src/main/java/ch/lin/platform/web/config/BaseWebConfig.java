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
package ch.lin.platform.web.config;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures Spring Web MVC features for the application.
 * <p>
 * This class implements {@link WebMvcConfigurer} to customize the default
 * Spring MVC behavior, specifically for content negotiation and Cross-Origin
 * Resource Sharing (CORS).
 */
@Configuration
public class BaseWebConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(BaseWebConfig.class);

    /**
     * The HTTP methods allowed for CORS requests.
     */
    private static final String[] ALLOWED_METHODS = {"GET", "POST", "PATCH", "DELETE", "PUT", "OPTIONS"};

    /**
     * The list of allowed origins for CORS requests, injected from application
     * properties.
     */
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * Configures content negotiation strategies for the application.
     * <p>
     * This setup prioritizes a URL query parameter ({@code mediaType}) for
     * determining the response content type, ignoring the standard
     * {@code Accept} header. This can be useful for clients that cannot easily
     * set the Accept header.
     *
     * @param configurer The configurer for content negotiation.
     */
    @Override
    public void configureContentNegotiation(@NonNull final ContentNegotiationConfigurer configurer) {
        logger.info("Configuring content negotiation");
        configurer.favorParameter(true)
                .parameterName("mediaType")
                .ignoreAcceptHeader(true)
                .useRegisteredExtensionsOnly(false)
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("xml", Objects.requireNonNull(MediaType.APPLICATION_XML))
                .mediaType("json", Objects.requireNonNull(MediaType.APPLICATION_JSON));
    }

    /**
     * Configures Cross-Origin Resource Sharing (CORS) for the entire
     * application.
     * <p>
     * This centralized approach is generally preferred over annotating
     * individual controllers with {@code @CrossOrigin}. It allows requests from
     * the specified frontend development server origin and defines which HTTP
     * methods and headers are permitted.
     *
     * @param registry The CORS registry to which the configuration is added.
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        logger.info("Configuring CORS");
        registry.addMapping("/**") // Apply to all endpoints
                .allowedOrigins(Objects.requireNonNull(allowedOrigins)) // Allow configured origins
                .allowedMethods(Objects.requireNonNull(ALLOWED_METHODS)) // Specify allowed methods
                .allowedHeaders("*") // Allow all headers
                .allowCredentials(true); // Allow credentials (e.g., cookies)
    }
}
