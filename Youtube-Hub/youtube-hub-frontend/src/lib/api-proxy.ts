import { auth } from "@/auth";
import { NextRequest, NextResponse } from "next/server";

// Define the identifiers for your resource servers
export type ResourceServer = "youtube-hub" | "downloader-tool" | "ignore";

// Map the identifiers to their base URLs using environment variables
const resourceBaseUrls: Record<ResourceServer, string | undefined> = {
  "youtube-hub": process.env.PUBLIC_BACKEND_URL,
  "downloader-tool": process.env.PUBLIC_DOWNLOADER_URL,
  "ignore": undefined,
};

// Maps URL path patterns to the correct resource server.
// The first match in the array wins.
const pathToServerMapping: { regex: RegExp; server: ResourceServer; rewrite?: (path: string) => string }[] = [
  // youtube-hub backend paths
  // IMPORTANT: NextAuth routes should not be proxied.
  { regex: /^\/api\/auth(\/.*)?$/, server: "ignore" },
  // Special handling for Hub Configs to avoid conflict with Downloader Configs
  {
    regex: /^\/api\/hub\/configs(\/.*)?$/,
    server: "youtube-hub",
    rewrite: (path) => path.replace(/^\/api\/hub/, ""),
  },
  { regex: /^\/api\/tasks(\/.*)?$/, server: "youtube-hub" },
  { regex: /^\/api\/channels(\/.*)?$/, server: "youtube-hub" },
  { regex: /^\/api\/items(\/.*)?$/, server: "youtube-hub" },
  { regex: /^\/api\/tags(\/.*)?$/, server: "youtube-hub" },
  // downloader-tool backend paths
  { regex: /^\/api\/configs(\/.*)?$/, server: "downloader-tool" },
  { regex: /^\/api\/download(\/.*)?$/, server: "downloader-tool" },
];

/**
 * A generic API proxy handler for Next.js API routes.
 * It determines the target resource server based on the request path,
 * retrieves the session, attaches the access token, and forwards the request.
 *
 * @param req The incoming Next.js API request.
 * @returns A Response object from the downstream service or an error response.
 */
export async function handleApiProxy(req: NextRequest): Promise<Response> {
  const path = req.nextUrl.pathname;
  console.log(`[API_PROXY] Handling request: ${req.method} ${path}`);

  // Find the correct resource server based on the request path
  const mapping = pathToServerMapping.find(({ regex }) => regex.test(path));

  if (!mapping) {
    console.error(`[API_PROXY] No route configured for path: ${path}`);
    return new Response(`No API proxy route configured for path: ${path}`, {
      status: 404,
    });
  }

  // If the server is 'ignore', it means we should not proxy this request.
  if (mapping.server === "ignore") {
    // This is a NextAuth route. We must not handle it here.
    // By returning NextResponse.next(), we tell the middleware to stop
    // and let Next.js find the correct NextAuth API handler.
    return NextResponse.next();
  }

  const session = await auth();
  // If the session is invalid (e.g., token refresh failed), deny the request.
  // The `error` property is set by the `auth.ts` jwt callback on refresh failure.
  if (!session?.accessToken || session.error) {
    console.warn(`[API_PROXY] Unauthorized request to ${path}: No session found.`);
    return new Response("Unauthorized: No valid session found", { status: 401 });
  }

  const baseUrl = resourceBaseUrls[mapping.server];
  if (!baseUrl) {
    console.error(`[API_PROXY] API URL for target '${mapping.server}' is not defined.`);
    return new Response(`Configuration error for API target: ${mapping.server}`, {
      status: 500,
    });
  }

  // Reconstruct the target URL, preserving search parameters
  let targetPath: string;
  if (mapping.rewrite) {
    targetPath = mapping.rewrite(path);
  } else {
    targetPath = path.replace(/^\/api/, ""); // Remove /api prefix
  }
  const targetUrl = new URL(`${baseUrl}${targetPath}`);
  targetUrl.search = req.nextUrl.search;

  console.log(`[API_PROXY] Forwarding to: ${targetUrl.toString()}`);

  const headers = new Headers(req.headers);
  headers.set("Authorization", `Bearer ${session.accessToken}`);
  // The host header should be for the target service, not the proxy
  headers.set("host", targetUrl.host);

  // console.log(`[Bearer token] ${session.accessToken}`);

  try {
    // Forward the request with the original body, method, and new headers
    const response = await fetch(targetUrl.toString(), {
      method: req.method,
      headers,
      body: req.body,
      // Pass duplex for streaming request bodies
      ...(req.method !== "GET" && req.method !== "HEAD" && { duplex: "half" } as RequestInit),
    });
    console.log(`[API_PROXY] Response from backend: ${response.status}`);
    return response;
  } catch (error) {
    console.error(`[API_PROXY] Error fetching from backend for ${path}:`, error);
    return new Response("Proxy error: Could not connect to backend service.", {
      status: 502, // Bad Gateway
    });
  }
}
