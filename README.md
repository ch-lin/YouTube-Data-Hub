# YouTube Data Hub

![Java](https://img.shields.io/badge/Java-25%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![Next.js](https://img.shields.io/badge/Next.js-16-black)
![Architecture](https://img.shields.io/badge/Architecture-Microservices-blueviolet)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![MCP](https://img.shields.io/badge/MCP-Experimental-yellow)

**YouTube Data Hub** is a self-hosted data asset management platform and intelligence hub designed for YouTube content tracking.

It was born out of a simple yet frustrating pain point: **The limitations of YouTube's native notification system.** When creators upload multiple videos in a short burst, native notifications often miss some updates, forcing users to manually check channel pages.

This project solves that problem through automated tracking and provides a unified platform for asset management, downloading, and AI interaction.

## 🏗️ Architecture & Design Philosophy

This project adopts a **Microservices Architecture**, decoupling different responsibilities into separate containers orchestrated via Docker Compose.

### 1. Core Services

* **YouTube-Hub (Core & UI)**
    * **Role**: The "Brain" and Facade of the system.
    * **Function**: Provides a Next.js frontend for users to register channels they want to track. It ensures no video is missed by polling the YouTube Data API.
    * **Design Patterns**:
        * **Proxy Pattern**: Acts as the intermediary between the user and underlying infrastructure. The user does not need to know how the backend services communicate.
        * **Fire-and-Forget (EIP)**: When a user initiates a download via the UI, the Hub asynchronously forwards the request to the Downloader and returns immediately. This ensures a non-blocking, fluid user experience.
    * **Future Plan**: The UI is currently tightly coupled with the backend. A refactor is planned to decouple the Next.js frontend into a standalone service, communicating with the Core via REST APIs.

* **Downloader Service**
    * **Role**: The "Worker" responsible for heavy lifting.
    * **Function**: A wrapper service around the powerful `yt-dlp` tool.
    * **Problem Solved**: Abstracts away the complexity of `yt-dlp` command-line arguments. Users can configure formats and quality via a GUI, and the service translates them into the correct CLI commands.

* **Authentication Service**
    * **Role**: The Security Gatekeeper.
    * **Function**: Handles JWT Token issuance and validation to protect the API. It secures access for both human users and future Client Apps (e.g., mobile apps). Designed with extensibility for future OAuth2 flows.

### 2. Infrastructure Layer (Platform)

* **Platform**
    * Contains shared **Java Class Libraries**, ensuring consistency of Data Transfer Objects (DTOs) and utility classes across the Authentication Service and Downloader.

* **Setup-Scripts**
    * Includes automation scripts (`Build.sh`, `Clean.sh`) for initializing the Docker container environment and CI/CD workflows.

### 3. AI Integration (Ongoing)

* **MCP Server (Model Context Protocol)**
    * **Status**: 🧪 *Experimental / In Development*
    * **Function**: Implements the MCP standard, allowing AI Agents (like Cursor, Claude Desktop) to communicate directly with the database using natural language.
    * **Note**: The current MCP implementation does not yet enforce API Key authentication. It is recommended for use only in trusted internal networks.

## 🧩 Integration Patterns & Design Decisions

This project adopts specific integration patterns and architectural choices to ensure scalability, reliability, and a smooth user experience.

### 1. Asynchronous Processing (Fire-and-Forget)
* **Challenge**: Video downloading and processing are I/O intensive operations. Handling them synchronously would block the UI thread and degrade the user experience.
* **Solution**: Implemented the **Fire-and-Forget** pattern for the Download Service.
* **Trade-off**: While this significantly improves UX, it introduces complexity in error tracking.
* **Mitigation**: A comprehensive state tracking mechanism (PENDING -> DOWNLOADING -> COMPLETED/FAILED) is implemented in the database to ensure eventual consistency and provide real-time status updates via the UI.

### 2. Callback Pattern (Status Reporting)
* **Challenge**: Polling the Downloader service for status updates is inefficient and creates unnecessary network load.
* **Solution**: The Downloader service actively calls back the Hub's REST API to report status changes (Downloading -> Completed/Failed).
* **Benefit**: Ensures real-time consistency without the overhead of polling.

### 3. Proxy Pattern (Centralized Access)
* **Challenge**: Direct communication between the frontend and multiple backend services (Downloader, Auth) increases coupling and security surface area.
* **Solution**: The **YouTube-Hub** acts as an intermediary proxy. The user/UI only interacts with the Hub, which orchestrates requests to the Downloader or Database.
* **Benefit**: Encapsulates system complexity and provides a single point of entry for security and logic.
* **Future Plan**: The Downloader is architected as a standalone service, with plans to eventually expose it for direct use by other clients (e.g., CLI tools, Mobile Apps).

### 4. Flat Mono-repo Structure
* **Decision**: The project utilizes a **Flat Mono-repo** structure where services (`Authentication`, `Downloader`) and the core application (`YouTube-Hub`) coexist as siblings.
* **Rationale**: This approach simplifies dependency management and ensures atomic commits across full-stack features, which is ideal for tight-knit microservices development. It also allows AI coding assistants to maintain full context of the project.

### 5. Model Context Protocol (MCP) Integration
* **Architecture**: Instead of building a traditional rigid chatbot, the system implements the **Model Context Protocol (MCP)**.
* **Benefit**: This decouples the backend logic from specific AI models. The system remains "Model Agnostic"—switching the AI client (e.g., from Cursor to Claude Desktop) requires zero code changes in the backend.

## 📂 Project Structure

The project follows a flat mono-repo structure where common services and the main application act as independent modules:

```text
youtube-data-hub/
├── Authentication-Service/    # Independent JWT Auth Service
├── Downloader/                # Standalone yt-dlp Wrapper Service
├── Platform/                  # Shared Java Class Libraries (DTOs, Utils)
├── Setup-Scripts/             # Build Scripts
├── Youtube-Hub/               # The Youtube-Hub Application
```

## 🚀 Quick Start

This project includes automated scripts to handle secret generation and service orchestration.

1. Create Shared Network
The microservices communicate via a dedicated bridge network. Create it first to avoid connection errors:

```
docker network create \
  --driver bridge \
  --subnet 172.16.10.0/24 \
  --gateway 172.16.10.1 \
  shared-services-network

```

2. Create Base Configuration
Prepare the environment configuration files from the provided templates.

2.1 Create Service .env files: Copy .env.example to .env in each service directory:

* **Authentication-Service/.env**
* **Downloader/.env**
* **Youtube-Hub/.env**

2.2 Configure Setup Scripts: Navigate to the scripts directory and create your config files:

```
cd Setup-Scripts

# 1. API Key
cp youtube-api-key.conf.example youtube-api-key.conf
# -> Edit and paste your Google Cloud API Key

# 2. Admin User
cp user-info.conf.example user-info.conf
# -> Edit to set your initial Admin credentials

# 3. Local Paths
cp local.conf.example local.conf
```

2.3. Initialize Environment & Secrets

Run the initialization script to automatically generate RSA keys, sync OAuth2 Client IDs, and inject your configurations into the service .env files.

```
# Inside Setup-Scripts/
chmod +x Init-secrets.sh
./Init-secrets.sh
```

What this script does:

Generates RSA Key Pairs for JWT signing.

Generates unique Client IDs & Secrets for inter-service communication.

Injects the Admin credentials and API Key into the respective .env files.

Generates a Postman Client configuration for API testing.

2.4. Build and Run

Use the `BuildAll.sh` script to bring up the services. This script handles the dependency order (Auth -> Downloader -> Hub).

```
chmod +x BuildAll.sh

# Option A: Start Everything (Recommended)
./BuildAll.sh

# Option B: Start Individual Services
# ./BuildAll.sh auth
# ./BuildAll.sh downloader
# ./BuildAll.sh youtube
```

Once started, access the services at:
* **Web UI**: http://localhost:3000

## 🔭 Observability & Reliability

To ensure system stability in a containerized environment:

* **Health Checks**: Each microservice exposes standard health endpoints (`/actuator/health` or `/health`) compatible with Docker Compose and Kubernetes liveness probes.
* **Centralized Logging**: Logs are structured in JSON format, ready for ingestion by aggregation tools (e.g., ELK Stack, Azure Monitor) to facilitate cross-service debugging.

## 🔮 Future Roadmap: Data Intelligence

Moving beyond asset management, the platform is evolving into a data intelligence hub:

1.  **Viral Detection**: A scheduled background job will ingest public signals (View/Like velocity) to detect potential viral videos before they peak.
2.  **Python Analytics Worker**: Plans to integrate a dedicated Python service for performing trend analysis and calculating a "Viral Score," which can then be queried via the MCP interface.

## ✅ TODO List

- [ ] Add More MCP Endpoints
- [O] Add YouTube Data API Request Counter
- [O] Add YouTube Data API Fetch Scheduler
- [O] Save "Force Published After" UI Status
- [O] Implement Quota Check before YouTube Data API Requests
- [O] Add Paging Feature in YouTube Hub's REST API to Avoid Out of Memory
- [ ] Implement YouTube Statistics Processing Task
- [O] Decouple Frontend into Independent Project and Isolate Database Network
- [O] YouTube-Hub `YoutubeHubService` Refactor
- [ ] Downloader `ExecutorService` Refactor
- [ ] Infrastructure: Deploy to GCP using Terraform (Long-term Plan)

## ⚠️ Disclaimer

1.  **Educational Purpose**: This project (`YouTube-Data-Hub`) is intended solely for educational and technical research purposes. It is designed to demonstrate microservices architecture, MCP integration, and automation workflows.
2.  **User Responsibility**: The `Downloader` service is a wrapper around `yt-dlp`. Users assume full legal responsibility and liability for any content downloaded or used through this tool. The developers of this project are not responsible for any misuse of the software.
3.  **Intellectual Property & TOS**: Users must strictly adhere to YouTube's [Terms of Service](https://www.youtube.com/t/terms) and applicable copyright laws in their jurisdiction. This project does not endorse, support, or encourage copyright infringement. Download functionality should only be used for content you own or for content that falls under "Fair Use" or similar exemptions.
4.  **No Warranty**: This software is provided "as is", without warranty of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose, and non-infringement.

## License

[MIT](LICENSE.MIT) © 2025 Che-Hung Lin
