/**
 * An array of routes that are accessible to the public.
 * These routes do not require authentication.
 * @type {string[]}
 */
export const PUBLIC_ROUTES: string[] = ["/"];

/**
 * An array of routes that are used for authentication.
 * These routes will redirect logged in users to /videos.
 * @type {string[]}
 */
export const AUTH_ROUTES: string[] = ["/login"];

/**
 * The prefix for API authentication routes.
 * Routes that start with this prefix are used for API authentication purposes.
 * @type {string}
 */
export const API_AUTH_PREFIX: string = "/api/auth";

/**
 * The default redirect path after a user logs in.
 * @type {string}
 */
export const DEFAULT_LOGIN_REDIRECT: string = "/videos";