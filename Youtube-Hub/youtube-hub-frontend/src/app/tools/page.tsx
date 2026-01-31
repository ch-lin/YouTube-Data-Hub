"use client";

import { useState, useEffect } from "react";

export default function ToolsPage() {
  const [urlsToVerify, setUrlsToVerify] = useState("");
  const [isMarkingDone, setIsMarkingDone] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const [verificationResult, setVerificationResult] = useState<{
    newUrls: string | null;
    undownloaded: string | null;
    error: string | null;
  } | null>(null);

  useEffect(() => {
    const handleFocus = async () => {
      try {
        // Modern browsers require the document to be focused to read clipboard
        if (!document.hasFocus()) {
          return;
        }
        // Ask for permission and read clipboard content
        const text = await navigator.clipboard.readText();
        const youtubeUrlRegex =
          /^(https?:\/\/)?(www\.)?(youtube\.com|youtu\.be)\/(watch\?v=|embed\/|v\/|shorts\/|.+\?v=)?([^"&?\/\s]{11})/;

        if (youtubeUrlRegex.test(text)) {
          let processedUrl = text.trim();
          
          try {
            const urlObj = new URL(processedUrl);
            // Check if it's a YouTube domain and has a 'list' parameter
            if (urlObj.hostname.includes('youtube.com') || urlObj.hostname.includes('youtu.be')) {
              if (urlObj.searchParams.has('list')) {
                urlObj.searchParams.delete('list');
              }
              if (urlObj.searchParams.has('start_radio')) {
                urlObj.searchParams.delete('start_radio');
              }
              processedUrl = urlObj.toString();
            }
          } catch (e) {
            console.warn("Could not parse URL for list parameter removal, using original URL:", e);
            // If URL parsing fails, fall back to the original trimmed URL
          }

          // Avoid adding duplicates
          if (!urlsToVerify.includes(processedUrl)) {
            setUrlsToVerify((prevUrls) =>
              prevUrls ? `${prevUrls}\n${processedUrl}` : processedUrl
            );
          }
        }
      } catch (err) {
        // This can happen if the user denies permission, the clipboard is empty,
        // or the API is not supported. We can fail silently.
        console.log("Could not read from clipboard:", err);
      }
    };

    window.addEventListener("focus", handleFocus);

    return () => {
      window.removeEventListener("focus", handleFocus);
    };
  }, [urlsToVerify]); // Re-run if urlsToVerify changes to avoid stale closure

  const handleMarkAllDone = async () => {
    if (
      !window.confirm(
        "Are you sure you want to mark all available videos as done?"
      )
    ) {
      return;
    }
    setIsMarkingDone(true);
    try {
      const apiUrl = "/api";
      const response = await fetch(`${apiUrl}/tasks/mark-all-manually-downloaded`, {
        method: "PATCH",
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: `HTTP error! Status: ${response.status}`}));
        throw new Error(errorData.message || "Failed to mark all as done.");
      }
      const result = await response.json();
      alert(`Successfully marked ${result.data.updatedItems} videos as done.`);
    } catch (err) {
      alert(`Error: ${err instanceof Error ? err.message : "Unknown error"}`);
    } finally {
      setIsMarkingDone(false);
    }
  };

  const handleDeleteAllData = async () => {
    if (
      window.confirm(
        "Are you sure you want to delete ALL video data? This action cannot be undone."
      )
    ) {
      setIsDeleting(true);
      try {
        const apiUrl = "/api";
        const response = await fetch(`${apiUrl}/tasks/deletion`, {
          method: "DELETE",
        });

        if (!response.ok) {
          const errorData = await response.json().catch(() => ({ message: `HTTP error! Status: ${response.status}`}));
          throw new Error(errorData.message || "Failed to delete all data.");
        }

        alert("Successfully deleted all video data.");
      } catch (err) {
        alert(`Error: ${err instanceof Error ? err.message : "Unknown error"}`);
      } finally {
        setIsDeleting(false);
      }
    }
  };

  const handleVerifyUrls = async () => {
    setVerificationResult(null); // Clear previous results
    const urls = urlsToVerify.split('\n').map(url => url.trim()).filter(url => url);
    if (urls.length === 0) {
      alert("Please enter some URLs to verify.");
      return;
    }

    setIsVerifying(true);
    try {
      const apiUrl = "/api";
      const response = await fetch(`${apiUrl}/tasks/verification`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ urls }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: `HTTP error! Status: ${response.status}`}));
        throw new Error(errorData.message || "Failed to verify URLs.");
      }

      const result = await response.json();
      const newUrls = result.newUrls || [];
      const undownloadedUrls = result.undownloadedUrls || [];

      if (newUrls.length > 0 || undownloadedUrls.length > 0) {
        setVerificationResult({
          newUrls: newUrls.length > 0 ? newUrls.join('\n') : "All provided URLs are already in the system.",
          undownloaded: undownloadedUrls.length > 0 ? undownloadedUrls.join('\n') : "All provided URLs are downloaded.",
          error: null,
        });
      } else {
        setVerificationResult({
          newUrls: "All provided URLs are already in the system.",
          undownloaded: "All provided URLs are downloaded.",
          error: null,
        });
      }
    } catch (err) {
      const errorMessage = `Error: ${err instanceof Error ? err.message : "Unknown error"}`;
      setVerificationResult({ newUrls: null, undownloaded: null, error: errorMessage });
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <div className="font-sans flex justify-center min-h-screen p-4 sm:p-8">
      <main className="w-full max-w-4xl">
        <h1 className="text-3xl font-bold mb-8 text-center">
          Application Tools
        </h1>

        <div className="space-y-8">
          {/* Tool 1: Mark all Processed */}
          <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md border dark:border-gray-700">
            <h2 className="text-xl font-semibold mb-2">Mark All Videos as Done</h2>
            <p className="text-gray-600 dark:text-gray-400 mb-4">
              This will mark all currently &quot;Available&quot; videos as processed. This is useful for clearing out a large backlog of videos you don&apos;t intend to download.
            </p>
            <button
              onClick={handleMarkAllDone}
              disabled={isMarkingDone}
              className="px-4 py-2 bg-yellow-500 text-white font-semibold rounded-md hover:bg-yellow-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500 disabled:bg-gray-400 disabled:cursor-wait"
            >
              Mark All as Processed
            </button>
          </div>

          {/* Tool 2: Verify Video URLs */}
          <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md border dark:border-gray-700">
            <h2 className="text-xl font-semibold mb-2">Verify Video URLs</h2>
            <p className="text-gray-600 dark:text-gray-400 mb-4">
              Paste a list of YouTube video URLs (one per line) to check their status or perform other checks.
            </p>
            <textarea
              value={urlsToVerify}
              onChange={(e) => setUrlsToVerify(e.target.value)}
              disabled={isVerifying}
              placeholder="https://www.youtube.com/watch?v=...\nhttps://www.youtube.com/watch?v=..."
              className="w-full h-40 p-2 border rounded-md bg-gray-50 dark:bg-gray-700 dark:border-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 mb-4 disabled:bg-gray-200"
            />
            <button
              onClick={handleVerifyUrls}
              disabled={isVerifying}
              className="px-4 py-2 bg-blue-600 text-white font-semibold rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-wait"
            >
              Verify URLs
            </button>
            {verificationResult !== null && (
              <div className="mt-4">
                {verificationResult.error ? (
                  <textarea
                    readOnly
                    value={verificationResult.error}
                    className="w-full h-32 p-2 border rounded-md bg-red-50 text-red-700 dark:bg-red-900/20 dark:border-red-500/30 dark:text-red-400"
                  />
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        New URLs
                      </label>
                      <textarea
                        readOnly
                        value={verificationResult.newUrls ?? ""}
                        className="w-full h-32 p-2 border rounded-md bg-gray-100 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-300"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                        Undownloaded URLs
                      </label>
                      <textarea
                        readOnly
                        value={verificationResult.undownloaded ?? ""}
                        className="w-full h-32 p-2 border rounded-md bg-gray-100 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-300"
                      />
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Tool 3: Delete all data */}
          <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow-md border border-red-500/50 dark:border-red-500/30">
            <h2 className="text-xl font-semibold mb-2 text-red-600 dark:text-red-400">Cleanup Database</h2>
            <p className="text-gray-600 dark:text-gray-400 mb-4">
              <span className="font-bold">Warning:</span> This will permanently delete all video items from the database. This action cannot be undone.
            </p>
            <button
              onClick={handleDeleteAllData}
              disabled={isDeleting}
              className="px-4 py-2 bg-red-600 text-white font-semibold rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:bg-gray-400 disabled:cursor-wait"
            >
              Delete All Video Data
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
