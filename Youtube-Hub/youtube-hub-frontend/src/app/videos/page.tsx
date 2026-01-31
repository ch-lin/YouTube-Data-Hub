"use client";

import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import Image from "next/image";
import { useCallback, useEffect, useMemo, useState } from "react";

type ProcessingStatus = "NEW" | "PENDING" | "DOWNLOADING" | "DOWNLOADED" | "MANUALLY_DOWNLOADED" | "WATCHED" | "FAILED" | "DELETED" | "IGNORE";

interface Item {
  videoId: string;
  title: string;
  kind: string;
  videoPublishedAt: string;
  scheduledStartTime: string;
  playlistId: string;
  channelId: string;
  channelTitle: string;
  thumbnailUrl: string;
  status: ProcessingStatus;
}

interface Channel {
  channelId: string;
  title: string;
}

export default function Home() {
  const [copiedVideoId, setCopiedVideoId] = useState<string | null>(null);
  const [hoveredThumbnail, setHoveredThumbnail] = useState<{
    url: string;
    x: number;
    y: number;
  } | null>(null);

  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updatingItems, setUpdatingItems] = useState<Set<string>>(new Set());
  const [downloadingItems, setDownloadingItems] = useState<Set<string>>(new Set());
  const [viewMode, setViewMode] = useState<"available" | "upcoming" | "all" | "deleted">(() => {
    if (typeof window === "undefined") return "available";
    return (localStorage.getItem("videos_viewMode") as "available" | "upcoming" | "all" | "deleted") || "available";
  });
  const [itemsPerPage, setItemsPerPage] = useState(() => {
    if (typeof window === "undefined") return 100;
    const saved = localStorage.getItem("videos_itemsPerPage");
    return saved ? Number(saved) : 100;
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [isFetching, setIsFetching] = useState(false);
  const [isMarkingDone, setIsMarkingDone] = useState(false);
  const [banner, setBanner] = useState<{
    message: string;
    type: "error" | "warning" | "success";
  } | null>(null);
  const [channels, setChannels] = useState<Channel[]>([]);
  const [selectedChannel, setSelectedChannel] = useState<string>(() => {
    if (typeof window === "undefined") return "all";
    return localStorage.getItem("videos_selectedChannel") || "all";
  });
  const [forcePublishedAfter, setForcePublishedAfter] = useState(false);
  const [publishedAfterDate, setPublishedAfterDate] = useState<Date | null>(null);

  const processingStatuses: ProcessingStatus[] = [
    "NEW",
    "PENDING",
    "DOWNLOADING",
    "DOWNLOADED",
    "MANUALLY_DOWNLOADED",
    "WATCHED",
    "FAILED",
    "DELETED",
    "IGNORE",
  ];

  useEffect(() => {
    const fetchChannels = async () => {
      try {
        const apiUrl = "/api";
        const response = await fetch(`${apiUrl}/channels`);
        if (!response.ok) {
          throw new Error(`HTTP error! Status: ${response.status}`);
        }
        const data: Channel[] = await response.json();
        const sortedData = data.sort((a, b) => a.title.localeCompare(b.title));
        setChannels(sortedData);

        // After fetching channels, validate the stored selectedChannel
        if (typeof window !== "undefined") {
          const savedChannel = localStorage.getItem("videos_selectedChannel");
          if (savedChannel && savedChannel !== "all" && !data.some(c => c.channelId === savedChannel)) {
            // The saved channel no longer exists, reset to default
            setSelectedChannel("all");
          }
        }
      } catch (e) {
        console.error("Failed to fetch channels:", e);
      }
    };
    fetchChannels();
  }, []);

  // Effects to save filter values to localStorage
  useEffect(() => {
    localStorage.setItem("videos_selectedChannel", selectedChannel);
  }, [selectedChannel]);

  useEffect(() => {
    localStorage.setItem("videos_viewMode", viewMode);
  }, [viewMode]);

  useEffect(() => {
    localStorage.setItem("videos_itemsPerPage", String(itemsPerPage));
  }, [itemsPerPage]);

  const fetchItems = useCallback(async (mode: "available" | "upcoming" | "all" | "deleted", channelId: string) => {
    try {
      setLoading(true);
      setError(null);
      // Use the API proxy route
      const apiUrl = "/api";
      const params = new URLSearchParams();

      let url = `${apiUrl}/items`;
      if (mode === "available") {
        params.append("notDownloaded", "true");
      } else if (mode === "upcoming") {
        params.append("liveBroadcastContent", "not_none");
        params.append("scheduledTimeIsInThePast", "false");
      } else if (mode === "deleted") {
        params.append("filterDeleted", "true");
      }
      if (channelId !== "all") {
        params.append("channelIds", channelId);
      }
      const queryString = params.toString();
      if (queryString) {
        url += `?${queryString}`;
      }
      const response = await fetch(url);

      if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
      }
      const data: Item[] = await response.json();
      setItems(data);
    } catch (e) {
      if (e instanceof Error) {
        setError(e.message);
      } else {
        setError("An unknown error occurred while fetching data.");
      }
      console.error("Failed to fetch items:", e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchItems(viewMode, selectedChannel);
    setCurrentPage(1); // Reset to first page on view mode change
  }, [fetchItems, viewMode, selectedChannel]);

  const handleCopy = (videoId: string) => {
    const url = `https://www.youtube.com/watch?v=${videoId}`;
    navigator.clipboard.writeText(url).then(() => {
      setCopiedVideoId(videoId);
      setTimeout(() => {
        setCopiedVideoId(null);
      }, 2000); // Reset after 2 seconds
    });
  };

  const handleRefresh = () => {
    fetchItems(viewMode, selectedChannel);
  };

  const handleMarkAllDone = async () => {
    const channelInfo =
      selectedChannel === "all"
        ? "all available"
        : channels.find((c) => c.channelId === selectedChannel)?.title || "the selected";

    if (!window.confirm(`Are you sure you want to mark all videos from ${channelInfo} channel(s) as done?`)) {
      return;
    }

    setIsMarkingDone(true);
    setBanner(null);

    try {
      const apiUrl = "/api";
      const requestOptions: RequestInit = {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
      };

      if (selectedChannel !== "all") {
        requestOptions.body = JSON.stringify({ channelIds: [selectedChannel] });
      } else {
        // Sending an empty JSON object for "all channels" case
        requestOptions.body = JSON.stringify({});
      }

      const response = await fetch(`${apiUrl}/tasks/mark-all-manually-downloaded`, requestOptions);
      const result = await response.json();

      if (!response.ok) {
        const errorMessage = result.message || `Failed to mark items as done. Status: ${response.status}`;
        throw new Error(errorMessage);
      }

      setBanner({ message: `Successfully marked ${result.data.updatedItems} videos as done.`, type: "success" });
      fetchItems(viewMode, selectedChannel); // Refresh list
    } catch (err) {
      const message = err instanceof Error ? err.message : "An unknown error occurred.";
      setBanner({ message, type: "error" });
    } finally {
      setIsMarkingDone(false);
      setTimeout(() => {
        setBanner(null);
      }, 20000);
    }
  };

  const handleFetch = async () => {
    setIsFetching(true);
    try {
      // Use the API proxy route
      const apiUrl = "/api";

      const requestBody: {
        delayInMilliseconds: number;
        channelIds?: string[];
        publishedAfter?: string;
        forcePublishedAfter?: boolean;
      } = {
        delayInMilliseconds: 50,
      };

      if (forcePublishedAfter && publishedAfterDate) {
        const date = new Date(publishedAfterDate.setUTCHours(0, 0, 0, 0));
        requestBody.publishedAfter = date.toISOString();
        requestBody.forcePublishedAfter = true;
      }

      if (selectedChannel !== "all") {
        requestBody.channelIds = [selectedChannel];
      }

      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 300000); // 5 minutes timeout

      const response = await fetch(`${apiUrl}/tasks/fetch`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(requestBody),
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      const result = await response.json();

      if (!response.ok) {
        // The backend sends a structured error response with a correlationId
        const apiError = result.data;
        const correlationId = result.correlationId;
        let errorMessage = `Failed to start fetch task. Status: ${response.status}`;
        if (apiError && apiError.message) {
          errorMessage = `Error: ${apiError.message} (Code: ${apiError.code}, ID: ${correlationId})`;
        }
        throw new Error(errorMessage);
      }

      const {
        newItems = 0,
        standardVideoCount = 0,
        upcomingVideoCount = 0,
        liveVideoCount = 0,
        updatedItemsCount = 0,
        processedChannels = 0,
        failures = [],
      } = result.data || {};

      if (failures.length > 0) {
        const failureCount = failures.length;
        setBanner({
          message: `Fetch completed with ${failureCount} failure(s). Check logs for details.`,
          type: "warning",
        });
      } else {
        let message;
        if (newItems > 0 || updatedItemsCount > 0) {
          const parts: string[] = [];
          const details: string[] = [];
          if (newItems > 0) {
            if (standardVideoCount > 0) details.push(`${standardVideoCount} video${standardVideoCount > 1 ? "s" : ""}`);
            if (upcomingVideoCount > 0) details.push(`${upcomingVideoCount} upcoming`);
            if (liveVideoCount > 0) details.push(`${liveVideoCount} live`);
            
            parts.push(`Found ${newItems} new item${newItems > 1 ? "s" : ""}${details.length > 0 ? ` (${details.join(", ")})` : ""}`);
          }
          if (updatedItemsCount > 0) {
            parts.push(`Updated ${updatedItemsCount} existing item${updatedItemsCount > 1 ? "s" : ""}`);
          }

          message = `Fetch complete. ${parts.join(" and ")} across ${processedChannels} channel${processedChannels > 1 ? "s" : ""}.`;
        } else {
          message = `Fetch complete. No new items or updates found across ${processedChannels} channel${processedChannels > 1 ? "s" : ""}.`;
        }

        setBanner({ message, type: "success" });
      }

      fetchItems(viewMode, selectedChannel); // Refresh the list on success
    } catch (err) {
      let message = "An unknown error occurred.";
      if (err instanceof Error) {
        message = err.name === 'AbortError' ? 'The fetch operation timed out after 5 minutes.' : err.message;
      }

      setBanner({ message, type: "error" });
    } finally {
      setIsFetching(false);
      setTimeout(() => {
        setBanner(null);
      }, 20000);
    }
  };

  const handleDownload = useCallback(async (videoId: string, title: string) => {
    if (!window.confirm(`Are you sure you want to download "${title}"?`)) {
      return;
    }

    setDownloadingItems((prev) => new Set(prev).add(videoId));
    setBanner(null);

    try {
      // Use the API proxy route
      const apiUrl = "/api";
      const response = await fetch(`${apiUrl}/tasks/download`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ videoIds: [videoId] }),
      });

      const result = await response.json();

      if (!response.ok) {
        throw new Error(result.data.message || `HTTP error! Status: ${response.status}`);
      }

      const createdTasksCount = result.data?.createdTasks;
      if (typeof createdTasksCount === 'number' && createdTasksCount > 0) {
        const message = `Successfully created ${createdTasksCount} download task(s).`;
        setBanner({ message, type: "success" });
      } else {
        // This case handles if 0 tasks were created or the response format is unexpected.
        throw new Error('Failed to create download tasks. The server did not confirm task creation.');
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "An unknown error occurred while submitting the download job.";
      setBanner({ message, type: "error" });
    } finally {
      setDownloadingItems((prev) => {
        const newSet = new Set(prev);
        newSet.delete(videoId);
        return newSet;
      });
      setTimeout(() => {
        setBanner(null);
      }, 20000);
    }
  }, []);

  const handleStatusChange = async (videoId: string, newStatus: ProcessingStatus) => {
    setUpdatingItems((prev) => new Set(prev).add(videoId));

    try {
      const apiUrl = "/api";
      const response = await fetch(`${apiUrl}/items/${videoId}`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ status: newStatus }),
      });

      if (!response.ok) {
        throw new Error(`Failed to update status. Status: ${response.status}`);
      }

      // Update the local state to reflect the change immediately
      setItems((prevItems) =>
        prevItems.map((item) => (item.videoId === videoId ? { ...item, status: newStatus } : item))
      );
    } catch (err) {
      console.error("Failed to update item status:", err);
      alert(`Failed to update status for video ${videoId}. Please try again.`);
    } finally {
      setUpdatingItems((prev) => {
        const newSet = new Set(prev);
        newSet.delete(videoId);
        return newSet;
      });
    }
  };

  const sortedItems = useMemo(() => {
    return [...items].sort((a, b) => {
      // Primary sort: channelId in ascending order to group channels
      const channelCompare = a.channelId.localeCompare(b.channelId);
      if (channelCompare !== 0) {
        return channelCompare;
      }
      // Secondary sort: videoPublishedAt in descending order (newest first)
      return b.videoPublishedAt.localeCompare(a.videoPublishedAt);
    });
  }, [items]);

  const totalPages = useMemo(() => {
    return Math.ceil(sortedItems.length / itemsPerPage);
  }, [sortedItems.length, itemsPerPage]);

  const paginatedItems = useMemo(() => {
    const effectiveItemsPerPage = itemsPerPage === Number.MAX_SAFE_INTEGER ? sortedItems.length : itemsPerPage;
    const startIndex = (currentPage - 1) * effectiveItemsPerPage;
    return sortedItems.slice(startIndex, startIndex + effectiveItemsPerPage);
  }, [sortedItems, currentPage, itemsPerPage]);

  const handleNextPage = () => {
    setCurrentPage((prev) => Math.min(prev + 1, totalPages));
  };

  const handlePrevPage = () => {
    setCurrentPage((prev) => Math.max(prev - 1, 1));
  };
  return (
    <div className="font-sans flex justify-center min-h-screen p-4 sm:p-8">
      {hoveredThumbnail && (
        <div
          className="fixed z-50 pointer-events-none transition-transform duration-200 ease-out"
          style={{
            top: hoveredThumbnail.y + 15,
            left: hoveredThumbnail.x + 15,
          }}
        >
          <Image
            src={hoveredThumbnail.url.replace("maxresdefault", "hqdefault")} // Use a slightly smaller high-quality image for preview
            alt="Enlarged thumbnail"
            width={480}
            height={360}
            className="rounded-lg shadow-2xl border-2 border-white dark:border-gray-800"
          />
        </div>
      )}
      <main className="w-full">
        <div className="flex justify-between items-center mb-4">
          {!loading && !error && (
            <>
              <div className="flex items-center gap-4">
                <select
                  className="bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={selectedChannel}
                  onChange={(e) => {
                    setSelectedChannel(e.target.value);
                  }}
                >
                  <option value="all">All Channels</option>
                  {channels.map((channel) => (
                    <option key={channel.channelId} value={channel.channelId}>
                      {channel.title}
                    </option>
                  ))}
                </select>
                <select
                  className="bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={viewMode}
                  onChange={(e) => { // prettier-ignore
                    setViewMode(e.target.value as "available" | "upcoming" | "all" | "deleted");
                  }}
                >
                  <option value="all">All</option>
                  <option value="available">Available</option>
                  <option value="upcoming">Upcoming</option>
                  <option value="deleted">Deleted</option>
                </select>
                <select
                  className="bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={itemsPerPage}
                  onChange={(e) => {
                    setItemsPerPage(Number(e.target.value));
                    setCurrentPage(1);
                  }}
                >
                  <option value={10}>10 per page</option>
                  <option value={50}>50 per page</option>
                  <option value={100}>100 per page</option>
                  <option value={Number.MAX_SAFE_INTEGER}>All</option>
                </select>
                <div className="flex items-center gap-4 ml-4">
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="forcePublishedAfter"
                      checked={forcePublishedAfter}
                      onChange={(e) => setForcePublishedAfter(e.target.checked)}
                      className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 dark:border-gray-600 dark:bg-gray-700 dark:focus:ring-blue-600 dark:ring-offset-gray-800"
                    />
                    <label htmlFor="forcePublishedAfter" className="text-sm font-medium text-gray-700 dark:text-gray-300 whitespace-nowrap">
                      Force Published After
                    </label>
                  </div>
                  <DatePicker
                    selected={publishedAfterDate}
                    onChange={(date: Date | null) => setPublishedAfterDate(date)}
                    className="bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2 focus:outline-none focus:ring-2 focus:ring-blue-500 w-36"
                    placeholderText="Select a date"
                    dateFormat="yyyy-MM-dd"
                    disabled={!forcePublishedAfter}
                  />
                </div>
              </div>
              <div className="flex items-center gap-4">
                {items.length > 0 && (
                  <p className="text-lg font-medium text-gray-600 dark:text-gray-400">
                    Total Videos: {items.length}
                  </p>
                )}
                  <button
                    onClick={handleMarkAllDone}
                    disabled={isMarkingDone}
                    className="p-1 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500 disabled:opacity-50 disabled:cursor-wait"
                    aria-label="Mark all as processed"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-yellow-500 dark:text-yellow-400"><polyline points="9 11 12 14 22 4"></polyline><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path></svg>
                  </button>
                  <button
                    onClick={handleFetch}
                    disabled={isFetching}
                    className="p-1 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-wait"
                    aria-label="Fetch new videos"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-gray-500 dark:text-gray-400">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
                    </svg>
                  </button>
                  <button
                    onClick={handleRefresh}
                    className="p-1 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                    aria-label="Refresh items"
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="16"
                      height="16"
                      viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                      className="text-gray-500 dark:text-gray-400"
                    ><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M3 21v-5h5"/></svg>
                  </button>
              </div>
            </>
          )}
        </div>
        {loading && (
          <div className="text-center p-8">
            <p>Loading items...</p>
          </div>
        )}
        {error && (
          <div className="text-center p-8 text-red-500">
            <p>Error fetching data: {error}</p>
            <p>Please ensure the backend server is running and accessible.</p>
          </div>
        )}
                {banner && (
          <div
            className={`p-4 mb-4 text-sm rounded-lg ${
              banner.type === "error"
                ? "bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300"
                : banner.type === "warning"
                  ? "bg-yellow-100 text-yellow-700 dark:bg-yellow-900 dark:text-yellow-300"
                  : "bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300"
            }`}
            role="alert"
          >
            <span className="font-medium">
              {banner.message}
            </span>
          </div>
        )}
        {!loading && !error && (
        <div>
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-800">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider w-60">Channel Title</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider">Thumbnail</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider">Video ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider w-1/2">Title</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider w-52">Published At</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider w-52">Scheduled Start Time</th>
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-center text-xs font-medium uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-background divide-y divide-gray-200 dark:divide-gray-700">
              {paginatedItems.map((item) => (
                <tr key={item.videoId}>
                  <td className="px-6 py-4 whitespace-normal break-words font-mono text-sm">
                    <a
                      href={`https://www.youtube.com/channel/${item.channelId}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 hover:underline"
                    >
                      {item.channelTitle}
                    </a>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div
                      className="w-[120px] h-[90px]"
                      onMouseEnter={(e) =>
                        setHoveredThumbnail({
                          url: item.thumbnailUrl,
                          x: e.clientX,
                          y: e.clientY,
                        })
                      }
                      onMouseMove={(e) => {
                        if (hoveredThumbnail) {
                          setHoveredThumbnail({
                            ...hoveredThumbnail,
                            x: e.clientX,
                            y: e.clientY,
                          });
                        }
                      }}
                      onMouseLeave={() => setHoveredThumbnail(null)}
                    >
                      <Image
                        src={item.thumbnailUrl}
                        alt={`Thumbnail for ${item.title}`}
                        width={120}
                        height={90}
                        className="object-cover rounded"
                      />
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap font-mono text-sm">
                    <div className="flex items-center gap-2">
                      <a
                        href={`https://www.youtube.com/watch?v=${item.videoId}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 hover:underline"
                      >
                        {item.videoId}
                      </a>
                      <button
                        onClick={() => handleCopy(item.videoId)}
                        className="p-1 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                        aria-label="Copy video link"
                      >
                        {copiedVideoId === item.videoId ? (
                          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-green-500">
                            <path d="M20 6 9 17l-5-5"/>
                          </svg>
                        ) : (
                          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-gray-500 dark:text-gray-400">
                            <rect width="14" height="14" x="8" y="8" rx="2" ry="2"/>
                            <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/>
                          </svg>
                        )}
                      </button>
                    </div>
                  </td>
                  <td className="px-6 py-4 max-w-sm whitespace-normal break-words">
                    <a
                      href={`https://www.youtube.com/watch?v=${item.videoId}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 hover:underline"
                    >
                      {item.title}
                    </a>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">{new Date(item.videoPublishedAt).toLocaleString()}</td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {item.scheduledStartTime ? new Date(item.scheduledStartTime).toLocaleString() : 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <select
                      value={item.status}
                      onChange={(e) => handleStatusChange(item.videoId, e.target.value as ProcessingStatus)}
                      disabled={updatingItems.has(item.videoId)}
                      className="bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-wait"
                    >
                      {processingStatuses.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-center">
                    <button
                      onClick={() => handleDownload(item.videoId, item.title)}
                      disabled={downloadingItems.has(item.videoId)}
                      className="p-1.5 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-wait"
                      title={`Download "${item.title}"`}
                    >
                      {downloadingItems.has(item.videoId) ? (
                        <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                      ) : (
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                      )}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        )}
        {!loading && !error && totalPages > 1 && (
          <div className="flex justify-center items-center gap-4 mt-4">
            <button
              onClick={handlePrevPage}
              disabled={currentPage === 1}
              className="px-4 py-2 bg-gray-200 dark:bg-gray-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <span className="text-gray-700 dark:text-gray-300">
              Page {currentPage} of {totalPages}
            </span>
            <button
              onClick={handleNextPage}
              disabled={currentPage === totalPages}
              className="px-4 py-2 bg-gray-200 dark:bg-gray-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
