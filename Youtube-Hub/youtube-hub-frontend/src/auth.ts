import NextAuth from "next-auth";
import Credentials from "next-auth/providers/credentials";
import { JWT } from "next-auth/jwt";

interface BackendToken {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
}

// The backend's refresh response is the same as the login response.
interface BackendRefreshResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    refreshToken?: string;
    accessTokenExpires?: number;
    error?: "RefreshAccessTokenError";
  }
}

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    error?: "RefreshAccessTokenError";
  }
  interface User {
    backendToken?: BackendToken;
  }
}

/**
 * Takes a token, and returns a new token with updated
 * `accessToken` and `accessTokenExpires`. If an error occurs,
 * returns the old token and an error property
 */
async function refreshAccessToken(token: JWT): Promise<JWT> {
  try {
    const refreshUrl = `${process.env.AUTH_SERVICE_API_URL}/api/v1/auth/refresh`;
    console.log("[AUTH_DEBUG] Attempting to refresh token at URL:", refreshUrl);

    if (!token.refreshToken) {
      throw new Error("Missing refresh token");
    }

    const response = await fetch(refreshUrl, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token.refreshToken}`,
      },
    });

    if (!response.ok) {      
      const errorBody = await response.text().catch(() => "Could not read error body.");
      console.error(
        "[AUTH_DEBUG] Failed to refresh token. Status:",
        response.status,
        "StatusText:",
        response.statusText,
        "Response:",
        errorBody
      );
      throw new Error(`Failed to refresh token. Status: ${response.status}`);
    }
    const refreshedTokens: BackendRefreshResponse = await response.json();
    console.log("[AUTH_DEBUG] Token refreshed successfully.");

    return {
      ...token,
      accessToken: refreshedTokens.access_token,
      accessTokenExpires: Date.now() + refreshedTokens.expires_in * 1000,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken, // Fallback to old refresh token
    };
  } catch (error) {
    console.error("[AUTH_DEBUG] Exception during token refresh:", error);
    // When refresh fails, we want to invalidate the session.
    // Returning the token with an error property will propagate it to the session.
    // Clearing the tokens will effectively log the user out.
    return {
      ...token,
      error: "RefreshAccessTokenError",
      accessToken: undefined, // Clear expired token
      refreshToken: undefined, // Clear expired token
      accessTokenExpires: 0, // Set expiry to past
    };
  }
}

export const { handlers, signIn, signOut, auth } = NextAuth({
  trustHost: true,
  session: { strategy: "jwt" },
  events: {
    signOut: (message) => {
      // This event is called when the user signs out.
      if ("token" in message && message.token) {
        const token = message.token; // Create a new constant with the narrowed type
        Object.keys(token).forEach((key) => {
          delete token[key as keyof JWT];
        });
      }
    },
  },
  providers: [
    Credentials({
      name: "Credentials",
      credentials: {
        email: { label: "Email", type: "email" },
        password: { label: "Password", type: "password" },
      },
      async authorize(credentials) {
        if (!credentials?.email || !credentials.password) return null;

        const authUrl = `${process.env.AUTH_SERVICE_API_URL}/api/v1/auth/authenticate`;
        console.log("[AUTH_DEBUG] Attempting to authorize at URL:", authUrl);

        try {
          const response = await fetch(
            authUrl,
            {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({
                email: credentials.email,
                password: credentials.password,
              }),
            }
          );

          if (response.ok) {
            const backendToken: BackendToken = await response.json();
            // The user object here is passed to the `jwt` callback
            return {
              id: "1", // A placeholder ID
              email: credentials.email as string,
              name: "User", // Placeholder name
              backendToken: backendToken,
            };
          }
          return null;
        } catch (error) {
          console.error("[AUTH_DEBUG] Fetch failed during authorize call. Details:", {
            message: error instanceof Error ? error.message : "Unknown error",
            cause: error instanceof Error ? error.cause : "No cause available",
            fullError: error,
          });
          return null;
        }
      },
    }),
  ],
  callbacks: {
    async jwt({ token, user }) {
      // Initial sign-in
      if (user && "backendToken" in user) {
        const backendToken = user.backendToken as BackendToken;
        token.accessToken = backendToken.access_token;
        token.refreshToken = backendToken.refresh_token;
        token.accessTokenExpires = Date.now() + backendToken.expires_in * 1000;
        console.log("[AUTH_DEBUG] Initial JWT created.");
        return token;
      }

      // Return previous token if the access token has not expired yet
      if (token.accessTokenExpires && Date.now() < token.accessTokenExpires) {
        console.log("[AUTH_DEBUG] Access token is still valid.");
        return token;
      }

      // Access token has expired, try to update it
      console.log("[AUTH_DEBUG] Access token has expired, attempting to refresh...");
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      // The session callback receives the token from the jwt callback.
      session.error = token.error;
      if (token.accessToken) {
        session.accessToken = token.accessToken as string;
      }
      return session;
    },
    async redirect({ url, baseUrl }) {
      console.log("REDIRECT CALLBACK", { url, baseUrl });

      // If the redirect URL is relative, make it absolute.
      if (url.startsWith("/")) {
        console.log("REDIRECT CALLBACK: Relative URL detected, returning absolute URL:", `${baseUrl}${url}`);
        return `${baseUrl}${url}`;
      }

      // If the redirect URL is absolute, ensure it's on the same origin.
      return new URL(url).origin === baseUrl ? url : baseUrl;
    },
  },
  pages: {
    signIn: "/login",
  },
});
