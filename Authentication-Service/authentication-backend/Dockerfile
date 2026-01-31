# The MIT License (MIT)
#
# Copyright (c) 2025 Che-Hung Lin
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

# --- Build Stage ---
# Use a Maven image with Eclipse Temurin 25 to build the application.
FROM maven:3.9-eclipse-temurin-25 AS builder

LABEL maintainer="Che-Hung Lin <edward730109@gmail.com>"

# Set the working directory for the build stage.
WORKDIR /build

# Copy the Platform project and build it first.
# This assumes the Docker build context is the parent directory of both 'Platform' and 'Authentication-Service'.
COPY Platform /build/Platform
RUN mvn -f /build/Platform/pom.xml clean install -Dmaven.test.skip=true

# Copy the Authentication-Service backend project.
COPY Authentication-Service/authentication-backend /build/authentication-backend

# Set the working directory to the Authentication-Service backend project for its build.
WORKDIR /build/authentication-backend

# Build the application, skipping tests for a faster build.
RUN mvn clean package -pl api -am -Dmaven.test.skip=true

# Extract Layers
WORKDIR /build/authentication-backend/bin
RUN java -Djarmode=layertools -jar authentication-service.jar extract --destination extracted

# --- Runtime Stage ---
# Use a secure and minimal base image with a Java 25 JRE for the final image.
FROM eclipse-temurin:25-jre-jammy AS final

LABEL maintainer="Che-Hung Lin <edward730109@gmail.com>"

# Install curl for healthcheck
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

RUN mkdir -p /app/certs && \
    chown -R appuser:appgroup /app/certs

WORKDIR /app

# Copy Layers for optimal caching
COPY --from=builder /build/authentication-backend/bin/extracted/dependencies/ ./
COPY --from=builder /build/authentication-backend/bin/extracted/spring-boot-loader/ ./
COPY --from=builder /build/authentication-backend/bin/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/authentication-backend/bin/extracted/application/ ./

USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]