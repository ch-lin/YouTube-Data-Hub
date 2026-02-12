"use server";

import { signIn } from "../../auth";
import { AuthError } from "next-auth";

export type State = {
  error?: string;
};

export async function authenticate(prevState: State | undefined, formData: FormData): Promise<State> {
  console.log("Attempting to authenticate with credentials:", Object.fromEntries(formData));
  try {
    await signIn("credentials", formData);
    console.log("Authentication successful, redirecting...");
    return {};
  } catch (error) {
    // The `signIn` function throws a `NEXT_REDIRECT` error when the login is successful.
    // We need to catch this error and re-throw it to allow Next.js to handle the redirect.
    if ((error as Error).message.includes("NEXT_REDIRECT")) {
      throw error;
    }
    console.error("Authentication failed:", error);
    if (error instanceof AuthError) {
      console.log("AuthError type:", error.type);
      if (error.type === "CredentialsSignin") {
        return { error: "Invalid email or password." };
      } else if (error.type === "CallbackRouteError") {
        // This can happen if the user is redirected from the provider, but there's an error during the callback.
        // We can treat it as a generic error for now.
        return { error: "An error occurred during login." };
      } else {
        return { error: `An AuthError occurred: ${error.type}` };
      }
    }
    return { error: "An unknown error occurred." };
  }
}
