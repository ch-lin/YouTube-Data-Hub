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
package ch.lin.platform.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a client request is malformed or contains invalid
 * parameters.
 * <p>
 * This is a checked exception that extends {@link IllegalArgumentException} to
 * semantically indicate a problem with arguments provided by a client. The
 * {@link ResponseStatus} annotation suggests that if this exception is
 * unhandled by a more specific handler, Spring MVC should respond with an HTTP
 * 400 (Bad Request) status. It is typically caught by a global exception
 * handler to produce a standardized error response.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new InvalidRequestException with the specified detail
     * message.
     *
     * @param message the detail message.
     */
    public InvalidRequestException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidRequestException with the specified detail
     * message and cause.
     *
     * @param message the detail message.
     * @param cause the cause (which is saved for later retrieval by the
     * {@link #getCause()} method).
     */
    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
