"use client";

import { useCallback, useEffect, useState } from "react";

interface Channel {
  channelId: string;
  title: string;
  handle: string;
}

interface AddChannelsResponse {
  addedChannels: Channel[];
  failedUrls: FailedUrl[];
}

interface FailedUrl {
  url: string;
  reason: string;
}

export default function ChannelsPage() {
  const [channels, setChannels] = useState<Channel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deletingChannels, setDeletingChannels] = useState<Set<string>>(new Set());
  const [newChannelUrl, setNewChannelUrl] = useState("");
  const [isAdding, setIsAdding] = useState(false);
  const [addChannelNotice, setAddChannelNotice] = useState<string | null>(null);

  const fetchChannels = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      // Use the API proxy route
      const apiUrl = "/api";
      const response = await fetch(`${apiUrl}/channels`);

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      const data: Channel[] = await response.json();
      const sortedData = data.sort((a, b) => a.title.localeCompare(b.title));
      setChannels(sortedData);
    } catch (e) {
      if (e instanceof Error) {
        setError(e.message);
      } else {
        setError("An unknown error occurred while fetching data.");
      }
      console.error("Failed to fetch channels:", e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchChannels();
  }, [fetchChannels]);

  const handleRefresh = () => {
    fetchChannels();
  };

  const handleAddChannel = async () => {
    if (!newChannelUrl.trim()) {
      alert("Please enter a channel URL.");
      return;
    }

    setIsAdding(true);
    try {
      // Use the API proxy route
      const apiUrl = "/api";
      const response = await fetch(`${apiUrl}/channels`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          urls: [newChannelUrl],
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || `Failed to add channel. Status: ${response.status}`);
      }

      const result: AddChannelsResponse = await response.json();

      if (result.failedUrls && result.failedUrls.length > 0) {
        const failureMessage = result.failedUrls
          .map(f => `Failed: ${f.url} (${f.reason})`)
          .join('\n');
        setAddChannelNotice(failureMessage);
        setTimeout(() => {
          setAddChannelNotice(null);
        }, 3000);
      }

      setNewChannelUrl(""); // Clear input on success
      if (result.addedChannels && result.addedChannels.length > 0) {
        fetchChannels(); // Refresh the list only if channels were added
      }
    } catch (err) {
      console.error("Failed to add channel:", err);
      alert(`Failed to add channel: ${err instanceof Error ? err.message : "Unknown error"}`);
    } finally {
      setIsAdding(false);
    }
  };
  const handleDelete = async (channelId: string, channelTitle: string) => {
    if (
      !window.confirm(`Are you sure you want to delete "${channelTitle}"?`)
    ) {
      return;
    }

    setDeletingChannels((prev) => new Set(prev).add(channelId));

    try {
      // Use the API proxy route
      const apiUrl = "/api";
      const response = await fetch(`${apiUrl}/channels/${channelId}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        throw new Error(`Failed to delete channel. Status: ${response.status}`);
      }

      // On success, remove the channel from the local state to update the UI
      setChannels((prevChannels) =>
        prevChannels.filter((c) => c.channelId !== channelId)
      );
    } catch (err) {
      console.error("Failed to delete channel:", err);
      alert(`Failed to delete channel ${channelTitle}. Please try again.`);
      // Re-enable the button on error
      setDeletingChannels((prev) => {
        const newSet = new Set(prev);
        newSet.delete(channelId);
        return newSet;
      });
    }
  };

  return (
    <div className="font-sans flex justify-center min-h-screen p-4 sm:p-8">
      <main className="w-full max-w-7xl">
        <div className="flex justify-between items-center mb-4">
          {!loading && !error && (
            <>
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  placeholder="Add Channel URL (e.g., with @handle)"
                  className="bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2 focus:outline-none focus:ring-2 focus:ring-blue-500 w-80"
                  value={newChannelUrl}
                  onChange={(e) => setNewChannelUrl(e.target.value)}
                  disabled={isAdding}
                />
                <button
                  onClick={handleAddChannel}
                  disabled={isAdding}
                  className="p-2 rounded-md bg-blue-600 text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-gray-400"
                  aria-label="Add channel"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                </button>
              </div>
              <div className="flex items-center gap-4">
                {channels.length > 0 && (
                  <p className="text-lg font-medium text-gray-600 dark:text-gray-400">
                    Total Channels: {channels.length}
                  </p>
                )}
                <button
                  onClick={handleRefresh}
                  className="p-1 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                  aria-label="Refresh channels"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-gray-500 dark:text-gray-400"><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M3 21v-5h5"/></svg>
                </button>
              </div>
            </>
          )}
        </div>
        {addChannelNotice && (
          <div className="mb-4 p-3 bg-yellow-100 border border-yellow-400 text-yellow-700 rounded-md dark:bg-yellow-900 dark:border-yellow-700 dark:text-yellow-300">
            <p className="whitespace-pre-wrap">{addChannelNotice}</p>
          </div>
        )}
        {loading && (
          <div className="text-center p-8">
            <p>Loading channels...</p>
          </div>
        )}
        {error && (
          <div className="text-center p-8 text-red-500">
            <p>Error fetching data: {error}</p>
            <p>Please ensure the backend server is running and accessible.</p>
          </div>
        )}
        {!loading && !error && (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
              <thead className="bg-gray-50 dark:bg-gray-800">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider">
                    Title
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider">
                    Handle
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider">
                    Channel ID
                  </th>
                  <th className="px-6 py-3 text-center text-xs font-medium uppercase tracking-wider">
                    Delete
                  </th>
                </tr>
              </thead>
              <tbody className="bg-background divide-y divide-gray-200 dark:divide-gray-700">
                {channels.map((channel) => (
                  <tr key={channel.channelId}>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {channel.title}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                      <a
                        href={`https://www.youtube.com/${channel.handle}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 hover:underline"
                      >
                        {channel.handle}
                      </a>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap font-mono text-sm">
                      <a
                        href={`https://www.youtube.com/channel/${channel.channelId}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 hover:underline"
                      >
                        {channel.channelId}
                      </a>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <button
                        onClick={() => handleDelete(channel.channelId, channel.title)}
                        disabled={deletingChannels.has(channel.channelId)}
                        className="p-1 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                        aria-label={`Delete ${channel.title}`}
                      >
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          width="16"
                          height="16"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          className="text-red-500"
                        >
                          <path d="M3 6h18" />
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                          <line x1="10" y1="11" x2="10" y2="17" />
                          <line x1="14" y1="11" x2="14" y2="17" />
                        </svg>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
