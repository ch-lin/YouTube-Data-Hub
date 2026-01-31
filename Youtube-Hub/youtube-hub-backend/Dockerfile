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
# This assumes the Docker build context is the parent directory of both 'Platform' and 'Youtube-Hub'.
COPY Platform /build/Platform
RUN mvn -f /build/Platform/pom.xml clean install -Dmaven.test.skip=true

# Copy the Youtube-Hub backend project.
COPY Youtube-Hub/youtube-hub-backend /build/youtube-hub-backend

# Set the working directory to the Youtube-Hub backend project for its build.
WORKDIR /build/youtube-hub-backend

# Build the application, skipping tests for a faster build.
RUN mvn clean package -pl hub -am -Dmaven.test.skip=true

# Extract Layers
WORKDIR /build/youtube-hub-backend/bin
RUN java -Djarmode=tools -jar youtube-hub.jar extract --layers --launcher

# --- Runtime Stage ---
# Use a secure and minimal base image with a Java 25 JRE for the final image.
FROM eclipse-temurin:25-jre-jammy AS final

LABEL maintainer="Che-Hung Lin <edward730109@gmail.com>"

# Install curl for healthcheck
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create a dedicated user and group for the application to enhance security by not running as root.
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# Set the working directory inside the container.
WORKDIR /app

# Copy Layers
COPY --from=builder /build/youtube-hub-backend/bin/youtube-hub/dependencies/ ./
COPY --from=builder /build/youtube-hub-backend/bin/youtube-hub/spring-boot-loader/ ./
COPY --from=builder /build/youtube-hub-backend/bin/youtube-hub/snapshot-dependencies/ ./
COPY --from=builder /build/youtube-hub-backend/bin/youtube-hub/application/ ./

# Switch to the non-root user.
USER appuser

EXPOSE 8080

# The command to run the application.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
