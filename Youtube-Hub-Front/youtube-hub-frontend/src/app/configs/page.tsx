'use client';

import { useEffect, useState } from 'react';

type AllConfigsResponse = {
  enabledConfigName: string;
  allConfigNames: string[];
};

type OverwriteOption = 'DEFAULT' | 'FORCE' | 'SKIP';
type YtDlpConfig = {
  name: string;
  formatFiltering: string;
  formatSorting: string;
  remuxVideo: string;
  writeDescription: boolean;
  writeSubs: boolean;
  subLang: string;
  writeAutoSubs: boolean;
  subFormat: string;
  outputTemplate: string;
  overwrite: OverwriteOption;
  keepVideo: boolean;
  extractAudio: boolean;
  audioFormat: string;
  audioQuality: number;
  cookie: string;
  noProgress: boolean;
  useCookie: boolean;
};

type DownloaderConfig = {
  name: string;
  enabled: boolean;
  duration: number;
  startDownloadAutomatically: boolean;
  removeCompletedJobAutomatically: boolean;
  clientId?: string;
  clientSecret?: string;
  threadPoolSize?: number;
  ytDlpConfig: YtDlpConfig;
};

type HubConfig = {
  name: string;
  enabled: boolean;
  youtubeApiKey: string;
  clientId: string;
  clientSecret: string;
  autoStartFetchScheduler?: boolean;
  schedulerType?: 'FIXED_RATE' | 'CRON';
  fixedRate?: number;
  cronExpression?: string;
  cronTimeZone?: string;
  quota?: number;
  quotaSafetyThreshold?: number;
};

const DOWNLOADER_API_URL = "/api";
const HUB_API_URL = "/api/hub"; // Assuming a separate proxy path for the Hub API

type TimeZoneOption = {
  id: string;
  displayName: string;
};

export default function ConfigsPage() {
  const [activeTab, setActiveTab] = useState<'downloader' | 'hub'>('downloader');

  useEffect(() => {
    const savedTab = localStorage.getItem('activeConfigTab');
    if (savedTab === 'downloader' || savedTab === 'hub') {
      setActiveTab(savedTab as 'downloader' | 'hub');
    }
  }, []);

  const handleTabChange = (tab: 'downloader' | 'hub') => {
    setActiveTab(tab);
    localStorage.setItem('activeConfigTab', tab);
  };

  // --- Downloader State ---
  const [downloaderConfigs, setDownloaderConfigs] = useState<string[]>([]);
  const [selectedDownloaderConfig, setSelectedDownloaderConfig] = useState<string>('');
  const [downloaderError, setDownloaderError] = useState<string | null>(null);
  const [downloaderLoading, setDownloaderLoading] = useState(true);

  const [downloaderConfigDetails, setDownloaderConfigDetails] = useState<DownloaderConfig | null>(null);
  const [downloaderDetailsLoading, setDownloaderDetailsLoading] = useState(false);
  const [downloaderDetailsError, setDownloaderDetailsError] = useState<string | null>(null);
  const [isDownloaderSaving, setIsDownloaderSaving] = useState(false);
  const [downloaderSaveStatus, setDownloaderSaveStatus] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const [isDownloaderDeleting, setIsDownloaderDeleting] = useState(false);
  const [isCreatingNewDownloader, setIsCreatingNewDownloader] = useState(false);
  const [previousSelectedDownloaderConfig, setPreviousSelectedDownloaderConfig] = useState<string>('');

  // --- Hub State ---
  const [hubConfigs, setHubConfigs] = useState<string[]>([]);
  const [selectedHubConfig, setSelectedHubConfig] = useState<string>('');
  const [hubError, setHubError] = useState<string | null>(null);
  const [hubLoading, setHubLoading] = useState(true);

  const [hubConfigDetails, setHubConfigDetails] = useState<HubConfig | null>(null);
  const [hubDetailsLoading, setHubDetailsLoading] = useState(false);
  const [hubDetailsError, setHubDetailsError] = useState<string | null>(null);
  const [isHubSaving, setIsHubSaving] = useState(false);
  const [hubSaveStatus, setHubSaveStatus] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const [isHubDeleting, setIsHubDeleting] = useState(false);
  const [isCreatingNewHub, setIsCreatingNewHub] = useState(false);
  const [previousSelectedHubConfig, setPreviousSelectedHubConfig] = useState<string>('');
  const [timeZones, setTimeZones] = useState<TimeZoneOption[]>([]);

  // --- Downloader Effects & Handlers ---

  // Fetch the list of all downloader configurations
  useEffect(() => {
    const fetchDownloaderConfigs = async () => {
      try {
        const response = await fetch(`${DOWNLOADER_API_URL}/configs`);
        if (!response.ok) {
          throw new Error(`Failed to fetch configurations. Status: ${response.status}`);
        }
        const data: AllConfigsResponse = await response.json();
        setDownloaderConfigs(data.allConfigNames || []);
        setSelectedDownloaderConfig(data.enabledConfigName || '');
        setPreviousSelectedDownloaderConfig(data.enabledConfigName || '');
      } catch (err) {
        setDownloaderError(err instanceof Error ? err.message : 'An unknown error occurred');
      } finally {
        setDownloaderLoading(false);
      }
    };

    fetchDownloaderConfigs();
  }, []);

  // Fetch the details of the selected downloader configuration
  useEffect(() => {
    const fetchDownloaderConfigDetails = async () => {
      if (!isCreatingNewDownloader) {
        if (!selectedDownloaderConfig) {
          setDownloaderConfigDetails(null);
          return;
        }

        setDownloaderDetailsLoading(true);
        setDownloaderDetailsError(null);
        try {
          const response = await fetch(`${DOWNLOADER_API_URL}/configs/${selectedDownloaderConfig}`);
          if (!response.ok) {
            throw new Error(`Failed to fetch details for config: ${selectedDownloaderConfig}`);
          }
          const data: DownloaderConfig = await response.json();
          setDownloaderConfigDetails(data);
        } catch (err) {
          setDownloaderDetailsError(err instanceof Error ? err.message : 'An unknown error occurred');
        } finally {
          setDownloaderDetailsLoading(false);
        }
      }
    };
    fetchDownloaderConfigDetails();
  }, [selectedDownloaderConfig, isCreatingNewDownloader]);

  const handleDownloaderDetailChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    if (!downloaderConfigDetails) return;

    const { name, value } = e.target;
    const isCheckbox = e.target instanceof HTMLInputElement && e.target.type === 'checkbox';
    const checked = isCheckbox ? (e.target as HTMLInputElement).checked : false;
    const finalValue = isCheckbox ? checked : value;

    const rootKeys = ['name', 'enabled', 'duration', 'startDownloadAutomatically', 'removeCompletedJobAutomatically', 'clientId', 'clientSecret', 'threadPoolSize'];
    // Check if the property belongs to the root DownloaderConfig or the nested YtDlpConfig
    if (rootKeys.includes(name)) {
      setDownloaderConfigDetails({
        ...downloaderConfigDetails,
        [name]: finalValue,
        // Also update the nested ytDlpConfig name to keep them in sync.
        // This is crucial for creating new configs, as the name is shared.
        ytDlpConfig: {
          ...downloaderConfigDetails.ytDlpConfig,
          name: name === 'name' ? String(finalValue) : downloaderConfigDetails.ytDlpConfig.name,
        },
      });
    } else {
      setDownloaderConfigDetails({
        ...downloaderConfigDetails,
        ytDlpConfig: { ...downloaderConfigDetails.ytDlpConfig, [name]: finalValue },
      });
    }
  };

  const handleNewDownloaderClick = () => {
    setPreviousSelectedDownloaderConfig(selectedDownloaderConfig);
    setIsCreatingNewDownloader(true);
    setSelectedDownloaderConfig('');
    setDownloaderConfigDetails({
      name: '',
      enabled: false,
      duration: 10,
      startDownloadAutomatically: true,
      removeCompletedJobAutomatically: true,
      clientId: '',
      clientSecret: '',
      threadPoolSize: 3,
      ytDlpConfig: {
        name: '',
        formatFiltering: 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best',
        formatSorting: '',
        remuxVideo: 'mp4',
        noProgress: true,
        writeDescription: true,
        writeSubs: true,
        subLang: 'ja.*',
        writeAutoSubs: true,
        subFormat: 'srt',
        outputTemplate: '',
        overwrite: 'DEFAULT',
        keepVideo: false,
        extractAudio: false,
        audioFormat: 'm4a',
        audioQuality: 0,
        cookie: '',
        useCookie: false,
      }
    });
    setDownloaderSaveStatus(null);
  };

  const handleDownloaderCancel = () => {
    setIsCreatingNewDownloader(false);
    setSelectedDownloaderConfig(previousSelectedDownloaderConfig);
  };

  const handleDownloaderDelete = async () => {
    if (!downloaderConfigDetails || downloaderConfigDetails.name === 'default') {
      setDownloaderSaveStatus({ message: "The 'default' configuration cannot be deleted.", type: 'error' });
      setTimeout(() => setDownloaderSaveStatus(null), 3000);
      return;
    }

    if (!window.confirm(`Are you sure you want to delete the "${downloaderConfigDetails.name}" configuration? This action cannot be undone.`)) {
      return;
    }

    setIsDownloaderDeleting(true);
    setDownloaderSaveStatus(null);

    try {
      const response = await fetch(`${DOWNLOADER_API_URL}/configs/${downloaderConfigDetails.name}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error(`Failed to delete configuration. Status: ${response.status}`);
      }

      setDownloaderSaveStatus({ message: `Configuration "${downloaderConfigDetails.name}" deleted successfully!`, type: 'success' });
      setTimeout(() => setDownloaderSaveStatus(null), 3000);

      // Refresh the list and select the first available config
      const newConfigs = downloaderConfigs.filter(c => c !== downloaderConfigDetails.name);
      setDownloaderConfigs(newConfigs);
      setSelectedDownloaderConfig(newConfigs.length > 0 ? newConfigs[0] : '');

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An unknown error occurred during deletion.';
      setDownloaderSaveStatus({ message: errorMessage, type: 'error' });
    } finally {
      setIsDownloaderDeleting(false);
    }
  };

  const handleDownloaderSave = async () => {
    if (!downloaderConfigDetails) return;

    setIsDownloaderSaving(true);
    setDownloaderSaveStatus(null);

    const method = isCreatingNewDownloader ? 'POST' : 'PATCH';
    const url = isCreatingNewDownloader
      ? `${DOWNLOADER_API_URL}/configs`
      : `${DOWNLOADER_API_URL}/configs/${downloaderConfigDetails.name}`;

    const body = downloaderConfigDetails;

    try {
      const response = await fetch(url, {
        method: method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
      });

      const responseData = await response.json();
      if (!response.ok) {
        throw new Error(responseData.message || `Failed to save configuration. Status: ${response.status}`);
      }

      const savedData: DownloaderConfig = responseData.data;

      setDownloaderConfigDetails(savedData);
      setIsCreatingNewDownloader(false);

      if (savedData.enabled) {
        setSelectedDownloaderConfig(savedData.name);
        setPreviousSelectedDownloaderConfig(savedData.name);
      }

      if (method === 'POST') {
        setDownloaderConfigs(prev => [...prev, savedData.name].sort());
        setSelectedDownloaderConfig(savedData.name);
        setPreviousSelectedDownloaderConfig(savedData.name);
      }

      setDownloaderSaveStatus({ message: 'Configuration saved successfully!', type: 'success' });
      setTimeout(() => setDownloaderSaveStatus(null), 3000);

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An unknown error occurred during save.';
      setDownloaderSaveStatus({ message: errorMessage, type: 'error' });
    } finally {
      setIsDownloaderSaving(false);
    }
  };

  // --- Hub Effects & Handlers ---

  // Fetch the list of all hub configurations
  useEffect(() => {
    const fetchHubConfigs = async () => {
      try {
        const response = await fetch(`${HUB_API_URL}/configs`);
        if (!response.ok) {
          throw new Error(`Failed to fetch hub configurations. Status: ${response.status}`);
        }
        const data: AllConfigsResponse = await response.json();
        setHubConfigs(data.allConfigNames || []);
        setSelectedHubConfig(data.enabledConfigName || '');
        setPreviousSelectedHubConfig(data.enabledConfigName || '');
      } catch (err) {
        setHubError(err instanceof Error ? err.message : 'An unknown error occurred');
      } finally {
        setHubLoading(false);
      }
    };

    fetchHubConfigs();
  }, []);

  // Fetch time zones when the hub tab is active
  useEffect(() => {
    if (activeTab === 'hub') {
      const fetchTimeZones = async () => {
        try {
          const response = await fetch(`${HUB_API_URL}/configs/timezones`);
          if (response.ok) {
            const data = await response.json();
            setTimeZones(data);
          }
        } catch (error) {
          console.error("Failed to fetch time zones", error);
        }
      };
      fetchTimeZones();
    }
  }, [activeTab]);

  // Fetch the details of the selected hub configuration
  useEffect(() => {
    const fetchHubConfigDetails = async () => {
      if (!isCreatingNewHub) {
        if (!selectedHubConfig) {
          setHubConfigDetails(null);
          return;
        }

        setHubDetailsLoading(true);
        setHubDetailsError(null);
        try {
          const response = await fetch(`${HUB_API_URL}/configs/${selectedHubConfig}`);
          if (!response.ok) {
            throw new Error(`Failed to fetch details for hub config: ${selectedHubConfig}`);
          }
          const data: HubConfig = await response.json();
          setHubConfigDetails(data);
        } catch (err) {
          setHubDetailsError(err instanceof Error ? err.message : 'An unknown error occurred');
        } finally {
          setHubDetailsLoading(false);
        }
      }
    };
    fetchHubConfigDetails();
  }, [selectedHubConfig, isCreatingNewHub]);

  const handleHubDetailChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    if (!hubConfigDetails) return;

    const { name, value } = e.target;
    const isCheckbox = e.target instanceof HTMLInputElement && e.target.type === 'checkbox';
    const checked = isCheckbox ? (e.target as HTMLInputElement).checked : false;
    const finalValue = isCheckbox ? checked : value;

    setHubConfigDetails({
      ...hubConfigDetails,
      [name]: finalValue,
    });
  };

  const handleNewHubClick = () => {
    setPreviousSelectedHubConfig(selectedHubConfig);
    setIsCreatingNewHub(true);
    setSelectedHubConfig('');
    setHubConfigDetails({
      name: '',
      enabled: false,
      youtubeApiKey: '',
      clientId: '',
      clientSecret: '',
      autoStartFetchScheduler: false,
      schedulerType: 'CRON',
      fixedRate: 86400000,
      cronExpression: '0 0 9,15,21 * * *',
      cronTimeZone: 'Asia/Taipei',
      quota: 10000,
      quotaSafetyThreshold: 500
    });
    setHubSaveStatus(null);
  };

  const handleHubCancel = () => {
    setIsCreatingNewHub(false);
    setSelectedHubConfig(previousSelectedHubConfig);
  };

  const handleHubDelete = async () => {
    if (!hubConfigDetails || hubConfigDetails.name === 'default') {
      setHubSaveStatus({ message: "The 'default' configuration cannot be deleted.", type: 'error' });
      setTimeout(() => setHubSaveStatus(null), 3000);
      return;
    }

    if (!window.confirm(`Are you sure you want to delete the "${hubConfigDetails.name}" configuration? This action cannot be undone.`)) {
      return;
    }

    setIsHubDeleting(true);
    setHubSaveStatus(null);

    try {
      const response = await fetch(`${HUB_API_URL}/configs/${hubConfigDetails.name}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error(`Failed to delete configuration. Status: ${response.status}`);
      }

      setHubSaveStatus({ message: `Configuration "${hubConfigDetails.name}" deleted successfully!`, type: 'success' });
      setTimeout(() => setHubSaveStatus(null), 3000);

      const newConfigs = hubConfigs.filter(c => c !== hubConfigDetails.name);
      setHubConfigs(newConfigs);
      setSelectedHubConfig(newConfigs.length > 0 ? newConfigs[0] : '');

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An unknown error occurred during deletion.';
      setHubSaveStatus({ message: errorMessage, type: 'error' });
    } finally {
      setIsHubDeleting(false);
    }
  };

  const handleHubSave = async () => {
    if (!hubConfigDetails) return;

    setIsHubSaving(true);
    setHubSaveStatus(null);

    const method = isCreatingNewHub ? 'POST' : 'PATCH';
    const url = isCreatingNewHub
      ? `${HUB_API_URL}/configs`
      : `${HUB_API_URL}/configs/${hubConfigDetails.name}`;

    const body = hubConfigDetails;

    try {
      const response = await fetch(url, {
        method: method,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
      });

      const responseData = await response.json();
      if (!response.ok) {
        throw new Error(responseData.message || `Failed to save configuration. Status: ${response.status}`);
      }

      const savedData: HubConfig = responseData.data;

      setHubConfigDetails(savedData);
      setIsCreatingNewHub(false);

      if (savedData.enabled) {
        setSelectedHubConfig(savedData.name);
        setPreviousSelectedHubConfig(savedData.name);
      }

      if (method === 'POST') {
        setHubConfigs(prev => [...prev, savedData.name].sort());
        setSelectedHubConfig(savedData.name);
        setPreviousSelectedHubConfig(savedData.name);
      }

      setHubSaveStatus({ message: 'Configuration saved successfully!', type: 'success' });
      setTimeout(() => setHubSaveStatus(null), 3000);

    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An unknown error occurred during save.';
      setHubSaveStatus({ message: errorMessage, type: 'error' });
    } finally {
      setIsHubSaving(false);
    }
  };

  return (
    <div className="container mx-auto p-4">
      {/* Tabs */}
      <div className="border-b border-gray-200 dark:border-gray-700 mb-6">
        <nav className="-mb-px flex space-x-8" aria-label="Tabs">
          <button
            onClick={() => handleTabChange('downloader')}
            className={`${activeTab === 'downloader' ? 'border-indigo-500 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'} whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
          >
            Downloader Configs
          </button>
          <button
            onClick={() => handleTabChange('hub')}
            className={`${activeTab === 'hub' ? 'border-indigo-500 text-indigo-600' : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'} whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm`}
          >
            YouTube Hub Configs
          </button>
        </nav>
      </div>

      {activeTab === 'downloader' ? (
        <>
      <div className="flex items-center gap-4 max-w-full">
        <label htmlFor="config-select" className="text-sm font-medium text-gray-700 dark:text-gray-300 whitespace-nowrap">
          Configuration
        </label>
        {downloaderLoading ? <p>Loading configurations...</p> : downloaderError ? <p className="text-red-500">Error: {downloaderError}</p> : <select id="config-select" value={selectedDownloaderConfig} onChange={(e) => setSelectedDownloaderConfig(e.target.value)} disabled={isCreatingNewDownloader} className="block w-full pl-3 pr-10 py-2 text-base border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md disabled:bg-gray-200 dark:disabled:bg-gray-800 disabled:cursor-not-allowed">
          {downloaderConfigs.map((config) => (<option key={config} value={config}>
              {config}
            </option>))}
        </select>}
        <div className="flex gap-2">
          <button
            onClick={handleDownloaderSave}
            disabled={isDownloaderSaving || !downloaderConfigDetails}
            className="w-20 justify-center px-4 py-2 bg-green-600 text-white font-semibold rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            Save
          </button>
          <button
            onClick={handleDownloaderDelete}
            disabled={isDownloaderDeleting || !downloaderConfigDetails || isCreatingNewDownloader || selectedDownloaderConfig === 'default' || downloaderConfigDetails?.name === 'default'}
            className="w-20 justify-center px-4 py-2 bg-red-600 text-white font-semibold rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            Delete
          </button>
          {isCreatingNewDownloader ? (
            <button
              onClick={handleDownloaderCancel}
              className="w-20 justify-center px-4 py-2 bg-gray-500 text-white font-semibold rounded-md hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-400"
            >
              Cancel
            </button>
          ) : (
            <button
              onClick={handleNewDownloaderClick}
              disabled={isCreatingNewDownloader}
              className="w-20 justify-center px-4 py-2 bg-blue-600 text-white font-semibold rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              New
            </button>
          )
          }
        </div>
      </div>

      {downloaderDetailsLoading && <p className="mt-4">Loading details...</p>}
      {downloaderDetailsError && <p className="mt-4 text-red-500">Error: {downloaderDetailsError}</p>}
      {downloaderConfigDetails && !downloaderDetailsLoading && !downloaderDetailsError && (
        <div className="mt-8 max-full overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg">
          <table className="min-w-full divide-y divide-gray-300 dark:divide-gray-700">
            {downloaderSaveStatus && (
              <caption className={`p-2 text-sm ${downloaderSaveStatus.type === 'success' ? 'text-green-600' : 'text-red-600'}`} >
                {downloaderSaveStatus.message}
              </caption>
            )}
            <tbody className="divide-y divide-gray-200 dark:divide-gray-800 bg-white dark:bg-gray-900">
              <tr>
                <td className="w-1/3 px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Name</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="text"
                    name="name"
                    value={downloaderConfigDetails.name}
                    readOnly={!isCreatingNewDownloader}
                    onChange={handleDownloaderDetailChange}
                    className={`w-full border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2 ${!isCreatingNewDownloader ? 'bg-gray-100 dark:bg-gray-800' : 'bg-white dark:bg-gray-700'}`}
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Enabled</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="checkbox"
                    name="enabled"
                    checked={downloaderConfigDetails.enabled}
                    onChange={handleDownloaderDetailChange}
                    className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Start Download Automatically</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="checkbox" name="startDownloadAutomatically"
                    checked={downloaderConfigDetails.startDownloadAutomatically}
                    onChange={handleDownloaderDetailChange}
                    className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Remove Completed Job Automatically</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="checkbox"
                    name="removeCompletedJobAutomatically"
                    checked={downloaderConfigDetails.removeCompletedJobAutomatically}
                    onChange={handleDownloaderDetailChange}
                    className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Client ID to Access Main APIs</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="text"
                    name="clientId"
                    value={downloaderConfigDetails.clientId || ''}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Client Secret to Access Main APIs</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="text"
                    name="clientSecret"
                    value={downloaderConfigDetails.clientSecret || ''}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Duration</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="number"
                    name="duration"
                    value={downloaderConfigDetails.duration}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Thread Pool Size</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="number"
                    name="threadPoolSize"
                    value={downloaderConfigDetails.threadPoolSize || ''}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Format Filtering</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="text"
                    name="formatFiltering"
                    value={downloaderConfigDetails.ytDlpConfig.formatFiltering}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Format Sorting</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="text"
                    name="formatSorting"
                    value={downloaderConfigDetails.ytDlpConfig.formatSorting}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Remux Video</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="text"
                    name="remuxVideo"
                    value={downloaderConfigDetails.ytDlpConfig.remuxVideo}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Disable Progress Bar</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="checkbox"
                    name="noProgress"
                    checked={downloaderConfigDetails.ytDlpConfig.noProgress || false}
                    onChange={handleDownloaderDetailChange}
                    className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Write Description</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="checkbox"
                    name="writeDescription"
                    checked={downloaderConfigDetails.ytDlpConfig.writeDescription}
                    onChange={handleDownloaderDetailChange}
                    className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Write Subtitles</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="checkbox"
                    name="writeSubs"
                    checked={downloaderConfigDetails.ytDlpConfig.writeSubs}
                    onChange={handleDownloaderDetailChange}
                    className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Subtitle Language</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="text"
                    name="subLang"
                    value={downloaderConfigDetails.ytDlpConfig.subLang}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Write Auto Subtitles</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="checkbox"
                    name="writeAutoSubs"
                    checked={downloaderConfigDetails.ytDlpConfig.writeAutoSubs}
                    onChange={handleDownloaderDetailChange}
                    className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Subtitle Format</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="text"
                    name="subFormat"
                    value={downloaderConfigDetails.ytDlpConfig.subFormat}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Output Template</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="text"
                    name="outputTemplate"
                    value={downloaderConfigDetails.ytDlpConfig.outputTemplate}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Overwrite Files</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <select
                    name="overwrite"
                    value={downloaderConfigDetails.ytDlpConfig.overwrite}
                    onChange={handleDownloaderDetailChange}
                    className="w-full bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  >
                    <option value="DEFAULT">Default (Overwrite if from different URL)</option>
                    <option value="FORCE">Force Overwrite</option>
                    <option value="SKIP">Skip Download (Do not overwrite)</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Keep Video File</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="checkbox"
                    name="keepVideo"
                    checked={downloaderConfigDetails.ytDlpConfig.keepVideo}
                    onChange={handleDownloaderDetailChange}
                    className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Extract Audio</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="checkbox"
                    name="extractAudio"
                    checked={downloaderConfigDetails.ytDlpConfig.extractAudio}
                    onChange={handleDownloaderDetailChange}
                    className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Audio Format</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="text"
                    name="audioFormat"
                    value={downloaderConfigDetails.ytDlpConfig.audioFormat}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Audio Quality</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="number"
                    name="audioQuality"
                    value={downloaderConfigDetails.ytDlpConfig.audioQuality}
                    onChange={handleDownloaderDetailChange}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Use Cookie File</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <input
                    type="checkbox"
                    name="useCookie"
                    checked={downloaderConfigDetails.ytDlpConfig.useCookie || false}
                    onChange={handleDownloaderDetailChange}
                    className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                  />
                </td>
              </tr>
              <tr>
                <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100 align-top">Cookie Content</td>
                <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                  <textarea
                    name="cookie"
                    value={downloaderConfigDetails.ytDlpConfig.cookie || ''}
                    onChange={handleDownloaderDetailChange}
                    rows={8}
                    className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                    placeholder="Paste Netscape cookie file content here..."
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
        </>
      ) : (
        <>
          {/* Hub Config View */}
          <div className="flex items-center gap-4 max-w-full">
            <label htmlFor="hub-config-select" className="text-sm font-medium text-gray-700 dark:text-gray-300 whitespace-nowrap">
              Configuration
            </label>
            {hubLoading ? <p>Loading configurations...</p> : hubError ? <p className="text-red-500">Error: {hubError}</p> : <select id="hub-config-select" value={selectedHubConfig} onChange={(e) => setSelectedHubConfig(e.target.value)} disabled={isCreatingNewHub} className="block w-full pl-3 pr-10 py-2 text-base border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md disabled:bg-gray-200 dark:disabled:bg-gray-800 disabled:cursor-not-allowed">
              {hubConfigs.map((config) => (<option key={config} value={config}>
                {config}
              </option>))}
            </select>}
            <div className="flex gap-2">
              <button
                onClick={handleHubSave}
                disabled={isHubSaving || !hubConfigDetails}
                className="w-20 justify-center px-4 py-2 bg-green-600 text-white font-semibold rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                Save
              </button>
              <button
                onClick={handleHubDelete}
                disabled={isHubDeleting || !hubConfigDetails || isCreatingNewHub || selectedHubConfig === 'default' || hubConfigDetails?.name === 'default'}
                className="w-20 justify-center px-4 py-2 bg-red-600 text-white font-semibold rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                Delete
              </button>
              {isCreatingNewHub ? (
                <button
                  onClick={handleHubCancel}
                  className="w-20 justify-center px-4 py-2 bg-gray-500 text-white font-semibold rounded-md hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-400"
                >
                  Cancel
                </button>
              ) : (
                <button
                  onClick={handleNewHubClick}
                  disabled={isCreatingNewHub}
                  className="w-20 justify-center px-4 py-2 bg-blue-600 text-white font-semibold rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
                >
                  New
                </button>
              )
              }
            </div>
          </div>

          {hubDetailsLoading && <p className="mt-4">Loading details...</p>}
          {hubDetailsError && <p className="mt-4 text-red-500">Error: {hubDetailsError}</p>}
          {hubConfigDetails && !hubDetailsLoading && !hubDetailsError && (
            <div className="mt-8 max-full overflow-hidden shadow ring-1 ring-black ring-opacity-5 sm:rounded-lg">
              <table className="min-w-full divide-y divide-gray-300 dark:divide-gray-700">
                {hubSaveStatus && (
                  <caption className={`p-2 text-sm ${hubSaveStatus.type === 'success' ? 'text-green-600' : 'text-red-600'}`} >
                    {hubSaveStatus.message}
                  </caption>
                )}
                <tbody className="divide-y divide-gray-200 dark:divide-gray-800 bg-white dark:bg-gray-900">
                  <tr>
                    <td className="w-1/3 px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Name</td>
                    <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                      <input
                        type="text"
                        name="name"
                        value={hubConfigDetails.name}
                        readOnly={!isCreatingNewHub}
                        onChange={handleHubDetailChange}
                        className={`w-full border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2 ${!isCreatingNewHub ? 'bg-gray-100 dark:bg-gray-800' : 'bg-white dark:bg-gray-700'}`}
                      />
                    </td>
                  </tr>
                  <tr>
                    <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Enabled</td>
                    <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                      <input
                        type="checkbox"
                        name="enabled"
                        checked={hubConfigDetails.enabled}
                        onChange={handleHubDetailChange}
                        className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                      />
                    </td>
                  </tr>
                  <tr>
                    <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Youtube API Key</td>
                    <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                      <input
                        type="text"
                        name="youtubeApiKey"
                        value={hubConfigDetails.youtubeApiKey || ''}
                        onChange={handleHubDetailChange}
                        className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                      />
                    </td>
                  </tr>
                  <tr>
                    <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Daily Quota</td>
                    <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                      <input
                        type="number"
                        name="quota"
                        value={hubConfigDetails.quota || ''}
                        onChange={handleHubDetailChange}
                        className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                      />
                    </td>
                  </tr>
                  <tr>
                    <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Quota Safety Threshold</td>
                    <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                      <input
                        type="number"
                        name="quotaSafetyThreshold"
                        value={hubConfigDetails.quotaSafetyThreshold || ''}
                        onChange={handleHubDetailChange}
                        className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                      />
                    </td>
                  </tr>
                  <tr>
                    <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Client ID to Access Downloader API</td>
                    <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                      <input
                        type="text"
                        name="clientId"
                        value={hubConfigDetails.clientId || ''}
                        onChange={handleHubDetailChange}
                        className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                      />
                    </td>
                  </tr>
                  <tr>
                    <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Client Secret to Access Downloader API</td>
                    <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                      <input
                        type="text"
                        name="clientSecret"
                        value={hubConfigDetails.clientSecret || ''}
                        onChange={handleHubDetailChange}
                        className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                      />
                    </td>
                  </tr>
                  <tr>
                    <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Auto Start Fetch Scheduler</td>
                    <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                      <input
                        type="checkbox"
                        name="autoStartFetchScheduler"
                        checked={hubConfigDetails.autoStartFetchScheduler || false}
                        onChange={handleHubDetailChange}
                        className="h-5 w-5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"
                      />
                    </td>
                  </tr>
                  <tr>
                    <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Scheduler Type</td>
                    <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                      <select
                        name="schedulerType"
                        value={hubConfigDetails.schedulerType || 'FIXED_RATE'}
                        onChange={handleHubDetailChange}
                        className="w-full bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                      >
                        <option value="FIXED_RATE">Fixed Rate</option>
                        <option value="CRON">Cron</option>
                      </select>
                    </td>
                  </tr>
                  {(!hubConfigDetails.schedulerType || hubConfigDetails.schedulerType === 'FIXED_RATE') && (
                    <tr>
                      <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Fixed Rate (ms)</td>
                      <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                        <input
                          type="number"
                          name="fixedRate"
                          value={hubConfigDetails.fixedRate || ''}
                          onChange={handleHubDetailChange}
                          className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                        />
                      </td>
                    </tr>
                  )}
                  {hubConfigDetails.schedulerType === 'CRON' && (
                    <>
                      <tr>
                        <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Cron Expression</td>
                        <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                          <input
                            type="text"
                            name="cronExpression"
                            value={hubConfigDetails.cronExpression || ''}
                            onChange={handleHubDetailChange}
                            className="w-full font-mono bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                            placeholder="0 0 8 * * *"
                          />
                        </td>
                      </tr>
                      <tr>
                        <td className="px-3 py-4 text-sm font-medium text-gray-900 dark:text-gray-100">Cron Time Zone</td>
                        <td className="px-3 py-4 text-sm text-gray-500 dark:text-gray-300">
                          <select
                            name="cronTimeZone"
                            value={hubConfigDetails.cronTimeZone || ''}
                            onChange={handleHubDetailChange}
                            className="w-full bg-white dark:bg-gray-700 border-gray-300 dark:border-gray-600 rounded-md shadow-sm p-2"
                          >
                            {timeZones.map((tz) => (
                              <option key={tz.id} value={tz.id}>{tz.displayName}</option>
                            ))}
                          </select>
                        </td>
                      </tr>
                    </>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}