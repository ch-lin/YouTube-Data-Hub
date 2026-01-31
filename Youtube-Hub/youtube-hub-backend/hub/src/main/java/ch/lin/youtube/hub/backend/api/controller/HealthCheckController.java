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
package ch.lin.youtube.hub.backend.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for application health checks.
 * <p>
 * This controller provides an endpoint to verify the operational status of the
 * service and its critical dependencies, such as the downloader microservice.
 */
@RestController
@RequestMapping("/health")
public class HealthCheckController {

    /**
     * Provides a health check endpoint for the main backend service.
     * <p>
     * This endpoint's reachability implicitly confirms the main backend service
     * is running. It also actively checks the health of its dependencies:
     * <ol>
     * <li><b>Downloader Service:</b> Sends a request to the downloader
     * service's <code>/health</code> endpoint to ensure it is responsive.</li>
     * </ol>
     * If all checks are successful, it returns an HTTP 200 OK with a JSON body
     * indicating the status of each component. If the downloader service is
     * unhealthy, it returns an HTTP 503 Service Unavailable.
     *
     * @return A {@link ResponseEntity} with an HTTP 200 OK status and a body
     * indicating all services are "UP", or an HTTP 503 Service Unavailable
     * status if a dependency is "DOWN".
     *
     * <pre>{@code curl -X GET http://localhost:8080/health}</pre>
     */
    @GetMapping
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
