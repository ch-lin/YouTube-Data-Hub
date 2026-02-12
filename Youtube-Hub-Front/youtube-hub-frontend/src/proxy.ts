import { NextResponse } from "next/server";
import { auth } from "@/auth";
import { handleApiProxy } from "./lib/api-proxy";
import {
  API_AUTH_PREFIX,
  AUTH_ROUTES,
  DEFAULT_LOGIN_REDIRECT,
  PUBLIC_ROUTES,
} from "@/routes";

export default auth(async (req) => {
  const { nextUrl } = req;
  // A user is only considered logged in if there is a session AND no error from token refresh.
  // The `error` property is set in the `auth.ts` jwt callback.
  const isLoggedIn = !!req.auth && !req.auth.error;

  const isApiAuthRoute = nextUrl.pathname.startsWith(API_AUTH_PREFIX);
  const isPublicRoute = PUBLIC_ROUTES.includes(nextUrl.pathname);
  const isAuthRoute = AUTH_ROUTES.includes(nextUrl.pathname);
  const isApiProxyRoute = nextUrl.pathname.startsWith("/api/");

  // 1. Handle API routes: NextAuth routes pass, others are proxied.
  if (isApiProxyRoute) {
    if (isApiAuthRoute) {
      return NextResponse.next(); // Let NextAuth.js handle its own routes
    }
    return handleApiProxy(req);
  }

  // 2. Redirect logged-in users from authentication routes to the default redirect page.
  if (isAuthRoute) {
    if (isLoggedIn) {
      return NextResponse.redirect(new URL(DEFAULT_LOGIN_REDIRECT, nextUrl));
    }
    return NextResponse.next();
  }

  // 3. Protect all other routes.
  if (!isLoggedIn && !isPublicRoute) {
    return NextResponse.redirect(new URL("/login", nextUrl));
  }

  return NextResponse.next();
});

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
