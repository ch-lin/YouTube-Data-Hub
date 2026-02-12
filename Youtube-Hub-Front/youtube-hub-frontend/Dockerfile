# Dockerfile for Next.js Frontend

# 1. Base Stage: Setup Node.js and pnpm
FROM node:22-alpine AS base

# Set working directory
WORKDIR /app

# Install pnpm
RUN npm install -g pnpm

# 2. Builder Stage: Build the Next.js application
FROM base AS builder

# Copy package files and install dependencies with pnpm
COPY package.json pnpm-lock.yaml* ./

# Install dependencies
RUN pnpm install

# Make build-time environment variables available
ARG NEXT_PUBLIC_BROWSER_API_URL
ENV NEXT_PUBLIC_BROWSER_API_URL=$NEXT_PUBLIC_BROWSER_API_URL

# Copy the rest of the application source code
COPY . .

# Build the Next.js application for production
# This will generate the .next/standalone directory
RUN pnpm run build

# 3. Runner Stage: Create the final, lightweight production image
FROM base AS runner

WORKDIR /app

# Set the NODE_ENV to 'production' for performance and security
ENV NODE_ENV production

# Copy the standalone output from the builder stage
COPY --from=builder /app/.next/standalone ./

# Copy the public assets and static files
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/static ./.next/static

# Expose the port the app will run on
EXPOSE 3000

# The command to start the production server
CMD ["node", "server.js"]