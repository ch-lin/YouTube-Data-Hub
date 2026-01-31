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
package ch.lin.platform.http.exception;

import java.io.IOException;
import java.io.Serial;

/**
 * Custom exception for HTTP-related errors.
 * <p>
 * This exception is thrown when an HTTP request returns a non-successful status
 * code, capturing the HTTP status code and response body to provide detailed
 * context for diagnostics. It extends {@link IOException} to signify an error
 * related to input/output operations over a network.
 */
public class HttpException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String responseBody;

    /**
     * Constructs a new HttpException with the specified detail message, status
     * code, and response body.
     *
     * @param message the detail message explaining the error.
     * @param statusCode the HTTP status code returned by the server.
     * @param responseBody the response body returned by the server.
     */
    public HttpException(String message, int statusCode, String responseBody) {
        super(message + ". Status: " + statusCode + ", Body: " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * Returns the HTTP status code associated with this exception.
     *
     * @return the HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the response body associated with this exception.
     *
     * @return the response body.
     */
    public String getResponseBody() {
        return responseBody;
    }
}
