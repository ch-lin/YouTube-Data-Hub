"use client";

import React, { useState, useEffect } from 'react';

interface Tag {
  id: number;
  name: string;
}

export default function TagsPage() {
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deletingTagId, setDeletingTagId] = useState<number | null>(null);
  const [newTagName, setNewTagName] = useState("");
  const [isAdding, setIsAdding] = useState(false);

  const fetchTags = async () => {
    try {
      setLoading(true);
      setError(null);
      // Use the API proxy route
      const apiUrl = "/api";
      const res = await fetch(`${apiUrl}/tags`, {
        cache: 'no-cache',
      });

      if (!res.ok) {
        throw new Error('Failed to fetch tags');
      }

      const data: Tag[] = await res.json();
      setTags(data);
    } catch (err) {
      if (err instanceof Error) {
          setError(err.message);
      } else {
          setError("An unknown error occurred while fetching tags.");
      }
      console.error('Error fetching tags:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTags();
  }, []); // Empty dependency array means this effect runs once on mount.

  const handleCreateTag = async () => {
    if (!newTagName.trim()) {
      alert("Please enter a tag name.");
      return;
    }
    setIsAdding(true);
    try {
      // Use the API proxy route
      const apiUrl = "/api";
      const response = await fetch(`${apiUrl}/tags`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ name: newTagName.trim() }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: `HTTP error! Status: ${response.status}` }));
        throw new Error(errorData.message || 'Failed to create tag.');
      }

      setNewTagName(""); // Clear input on success
      await fetchTags(); // Refresh the list
    } catch (err) {
      alert(`Error creating tag: ${err instanceof Error ? err.message : "Unknown error"}`);
    } finally {
      setIsAdding(false);
    }
  };

  const handleDelete = async (tag: Tag) => {
    if (!window.confirm(`Are you sure you want to delete the tag "${tag.name}"?`)) {
      return;
    }
    setDeletingTagId(tag.id);
    try {
      // Use the API proxy route
      const apiUrl = "/api";
      const response = await fetch(`${apiUrl}/tags/${tag.name}`, {
        method: 'DELETE',
      });

      // A 204 No Content response is a success signal for a DELETE request.
      if (!response.ok && response.status !== 204) {
        const errorData = await response.json().catch(() => ({ message: `HTTP error! Status: ${response.status}` }));
        throw new Error(errorData.message || 'Failed to delete tag.');
      }

      // On success, remove the tag from the local state to update the UI.
      setTags((prevTags) => prevTags.filter((t) => t.id !== tag.id));
    } catch (err) {
      alert(`Error deleting tag: ${err instanceof Error ? err.message : "Unknown error"}`);
    } finally {
      setDeletingTagId(null);
    }
  };

  if (loading) {
    return (
      <div className="container mx-auto p-4">
        <h1 className="text-2xl font-bold mb-4">Tags</h1>
        <p>Loading tags...</p>
      </div>
    );
  }

  if (error) {
    return <div className="container mx-auto p-4 text-red-500">Error: {error}</div>;
  }

  return (
    <div className="container mx-auto p-4">
      <div className="flex justify-between items-center mb-6">
        <div className="flex items-center gap-2">
          <input
            type="text"
            placeholder="Add new tag..."
            className="bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2 focus:outline-none focus:ring-2 focus:ring-blue-500 w-60"
            value={newTagName}
            onChange={(e) => setNewTagName(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleCreateTag()}
            disabled={isAdding}
          />
          <button
            onClick={handleCreateTag}
            disabled={isAdding}
            className="p-2 rounded-md bg-blue-600 text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-wait"
            aria-label="Add tag"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          </button>
          <button
            onClick={fetchTags}
            disabled={loading}
            className="p-2 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            aria-label="Refresh tags"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-gray-500 dark:text-gray-400"><path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/><path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/><path d="M3 21v-5h5"/></svg>
          </button>
        </div>
      </div>
      {tags.length > 0 ? (
        <div className="flex flex-wrap gap-3">
          {tags.map((tag) => (
            <span key={tag.id} className="flex items-center bg-gray-200 dark:bg-gray-700 text-gray-800 dark:text-gray-200 px-3 py-1 rounded-full text-sm font-medium">
              <span>{tag.name}</span>
              <button onClick={() => handleDelete(tag)} disabled={deletingTagId === tag.id} className="ml-2 text-gray-500 hover:text-red-500 dark:text-gray-400 dark:hover:text-red-400 disabled:opacity-50 disabled:cursor-wait" aria-label={`Delete tag ${tag.name}`}>
                &#x2715;
              </button>
            </span>
          ))}
        </div>
      ) : (
        <p>No tags found. You can create tags via the API.</p>
      )}
    </div>
  );
}
