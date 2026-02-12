"use client";

import { useActionState, useEffect, useState, Suspense } from "react";
import { useFormStatus } from "react-dom";
import { authenticate, State } from "./actions";
import { useSearchParams } from "next/navigation";

export default function LoginPage() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <LoginForm />
    </Suspense>
  );
}

function LoginForm() {
  const searchParams = useSearchParams();
  const callbackUrl = searchParams.get("callbackUrl") || "/videos";
  const [state, formAction] = useActionState<State, FormData>(authenticate, {});
  const [displayError, setDisplayError] = useState<string | undefined>();

  useEffect(() => {
    if (state?.error) {
      setDisplayError(state.error);
      const timer = setTimeout(() => {
        setDisplayError(undefined);
      }, 20000); // 60 seconds

      return () => clearTimeout(timer);
    }
  }, [state]);

  return (
    <div className="flex justify-center items-center h-screen bg-gray-100">
      <form action={formAction} className="bg-white p-8 rounded-lg shadow-md w-full max-w-sm">
        <h1 className="text-2xl font-bold mb-6 text-center">Login</h1>
        {displayError && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4" role="alert">
            <strong className="font-bold">Error: </strong>
            <span className="block sm:inline">{displayError}</span>
          </div>
        )}
        <input type="hidden" name="redirectTo" value={callbackUrl} />
        <div className="mb-4">
          <label
            className="block text-gray-700 text-sm font-bold mb-2"
            htmlFor="email"
          >
            Email
          </label>
          <input
            className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            id="email"
            name="email"
            type="email"
            placeholder="email"
            required
          />
        </div>
        <div className="mb-6">
          <label
            className="block text-gray-700 text-sm font-bold mb-2"
            htmlFor="password"
          >
            Password
          </label>
          <input
            className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 mb-3 leading-tight focus:outline-none focus:shadow-outline"
            id="password"
            name="password"
            type="password"
            required
            placeholder="password"
          />
        </div>
        <LoginButton />
      </form>
    </div>
  );
}

function LoginButton() {
  const { pending } = useFormStatus();

  return (
    <button
      className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline w-full disabled:bg-gray-400"
      type="submit"
      disabled={pending}
    >
      {pending ? "Signing In..." : "Sign In"}
    </button>
  );
}
