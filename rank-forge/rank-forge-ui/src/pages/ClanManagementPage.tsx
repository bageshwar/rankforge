import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { clansApi, type ClanDTO, type CreateClanRequest, type UpdateClanRequest } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import './ClanManagementPage.css';

export const ClanManagementPage = () => {
  const { user } = useAuth();
  const [clans, setClans] = useState<ClanDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [creating, setCreating] = useState(false);
  
  // Step 1 form state (create clan)
  const [clanName, setClanName] = useState<string>('');
  const [telegramChannelId, setTelegramChannelId] = useState<string>('');
  const [newlyCreatedClan, setNewlyCreatedClan] = useState<ClanDTO | null>(null);
  // Store API keys by clan ID (persisted in localStorage)
  const [apiKeys, setApiKeys] = useState<Map<number, string>>(new Map());

  // Load API keys from localStorage on mount
  useEffect(() => {
    const storedKeys = localStorage.getItem('clanApiKeys');
    if (storedKeys) {
      try {
        const keysMap = new Map<number, string>(JSON.parse(storedKeys));
        setApiKeys(keysMap);
      } catch (e) {
        console.error('Error loading API keys from localStorage:', e);
      }
    }
  }, []);

  useEffect(() => {
    if (user) {
      loadClans();
    }
  }, [user]);

  const loadClans = async () => {
    try {
      setLoading(true);
      setError(null);
      const userClans = await clansApi.getMyClans();
      setClans(userClans);
    } catch (err) {
      console.error('Error loading clans:', err);
      setError('Failed to load your clans. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateClan = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      setCreating(true);
      setError(null);
      
      const request: CreateClanRequest = {
        name: clanName.trim() || undefined,
        telegramChannelId: telegramChannelId.trim() || undefined,
      };
      
      const createdClan = await clansApi.create(request);
      
      // Store the newly created clan with API key
      setNewlyCreatedClan(createdClan);
      
      // Store API key in state and localStorage
      if (createdClan.apiKey) {
        const newKeys = new Map(apiKeys);
        newKeys.set(createdClan.id, createdClan.apiKey);
        setApiKeys(newKeys);
        localStorage.setItem('clanApiKeys', JSON.stringify(Array.from(newKeys.entries())));
      }
      
      // Reset form
      setClanName('');
      setTelegramChannelId('');
      setShowCreateForm(false);
      
      // Reload clans to get updated list
      await loadClans();
    } catch (err: any) {
      console.error('Error creating clan:', err);
      const errorMessage = err.response?.data?.error || 'Failed to create clan. Please try again.';
      setError(errorMessage);
    } finally {
      setCreating(false);
    }
  };

  const handleTransferAdmin = async (clanId: number, newAdminId: number) => {
    try {
      await clansApi.transferAdmin(clanId, newAdminId);
      await loadClans();
    } catch (err: any) {
      console.error('Error transferring admin:', err);
      alert(err.response?.data?.error || 'Failed to transfer admin');
    }
  };

  const handleDismissApiKey = () => {
    setNewlyCreatedClan(null);
  };

  if (!user) {
    return (
      <PageContainer>
        <div className="error-message">Please log in to manage clans.</div>
        <Link to="/" className="back-btn">← Back to Home</Link>
      </PageContainer>
    );
  }

  if (loading) {
    return (
      <PageContainer>
        <LoadingSpinner size="lg" message="Loading clans..." />
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <Link to="/my-profile" className="back-btn">← Back to Profile</Link>

      <div className="clan-management-page">
        <h1 className="page-title">Clan Management</h1>
        
        {error && <div className="clan-error-message">{error}</div>}

        {/* Show API Key for newly created clan */}
        {newlyCreatedClan && newlyCreatedClan.apiKey && (
          <div className="api-key-display">
            <h3>✅ Clan Created Successfully!</h3>
            <p className="api-key-instructions">
              Your API key has been generated. <strong>Copy it now</strong> - you won't be able to see it again!
            </p>
            <div className="api-key-box">
              <code className="api-key-value">{newlyCreatedClan.apiKey}</code>
              <button
                className="copy-api-key-btn"
                onClick={() => {
                  navigator.clipboard.writeText(newlyCreatedClan.apiKey!);
                  alert('API key copied to clipboard!');
                }}
              >
                📋 Copy
              </button>
            </div>
            <p className="api-key-next-steps">
              <strong>Next steps:</strong>
              <ol>
                <li>Set up log ingestion on your CS2 server using this API key</li>
                <li>Boot your CS2 server and note the App Server ID from the logs</li>
                <li>Come back here and configure the App Server ID for this clan (see below)</li>
              </ol>
            </p>
            <button className="dismiss-api-key-btn" onClick={handleDismissApiKey}>
              Got it, continue
            </button>
          </div>
        )}

        {/* Create Clan Form (Step 1) */}
        {!showCreateForm ? (
          <button
            className="create-clan-btn"
            onClick={() => setShowCreateForm(true)}
          >
            + Create New Clan
          </button>
        ) : (
          <form className="create-clan-form" onSubmit={handleCreateClan}>
            <h3>Create New Clan (Step 1 of 2)</h3>
            <p className="form-description">
              Create your clan and get an API key. You'll configure the App Server ID in step 2 after your server boots.
            </p>
            
            <div className="form-group">
              <label htmlFor="clanName">Clan Name (Optional)</label>
              <input
                id="clanName"
                type="text"
                value={clanName}
                onChange={(e) => setClanName(e.target.value)}
                placeholder="Enter clan name"
                maxLength={255}
              />
            </div>

            <div className="form-group">
              <label htmlFor="telegramChannelId">Telegram Channel ID (Optional)</label>
              <input
                id="telegramChannelId"
                type="text"
                value={telegramChannelId}
                onChange={(e) => setTelegramChannelId(e.target.value)}
                placeholder="Enter Telegram channel ID for notifications"
                maxLength={255}
              />
            </div>

            <div className="form-actions">
              <button
                type="button"
                className="cancel-btn"
                onClick={() => {
                  setShowCreateForm(false);
                  setClanName('');
                  setTelegramChannelId('');
                  setError(null);
                }}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="submit-btn"
                disabled={creating}
              >
                {creating ? 'Creating...' : 'Create Clan & Generate API Key'}
              </button>
            </div>
          </form>
        )}

        {/* My Clans List */}
        {clans.length > 0 && (
          <div className="my-clans-list">
            <h3>My Clans</h3>
            {clans.map((clan) => (
              <ClanCard
                key={clan.id}
                clan={clan}
                user={user}
                apiKey={apiKeys.get(clan.id)}
                onTransferAdmin={handleTransferAdmin}
                onClanUpdated={loadClans}
                onApiKeyStored={(clanId: number, key: string) => {
                  const newKeys = new Map(apiKeys);
                  newKeys.set(clanId, key);
                  setApiKeys(newKeys);
                  localStorage.setItem('clanApiKeys', JSON.stringify(Array.from(newKeys.entries())));
                }}
              />
            ))}
          </div>
        )}

        {clans.length === 0 && !showCreateForm && !newlyCreatedClan && (
          <div className="no-clans-message">
            <p>You haven't created or joined any clans yet.</p>
            <p>Create a clan to get started with log ingestion and rankings!</p>
          </div>
        )}
      </div>
    </PageContainer>
  );
};

const ClanCard = ({ 
  clan, 
  user, 
  apiKey,
  onTransferAdmin, 
  onClanUpdated,
  onApiKeyStored
}: { 
  clan: ClanDTO; 
  user: any; 
  apiKey?: string;
  onTransferAdmin: (clanId: number, newAdminId: number) => void;
  onClanUpdated: () => void;
  onApiKeyStored: (clanId: number, key: string) => void;
}) => {
  const [showConfigureForm, setShowConfigureForm] = useState(false);
  const [appServerId, setAppServerId] = useState<string>('');
  const [configuring, setConfiguring] = useState(false);
  const [configError, setConfigError] = useState<string | null>(null);
  const [serverCheckResult, setServerCheckResult] = useState<{ claimed: boolean } | null>(null);
  const [showRegenerateKey, setShowRegenerateKey] = useState(false);
  const [regenerating, setRegenerating] = useState(false);
  const [showApiKey, setShowApiKey] = useState(!!apiKey); // Show by default if API key is available
  const [isEditingName, setIsEditingName] = useState(false);
  const [editedName, setEditedName] = useState(clan.name || '');
  const [updatingName, setUpdatingName] = useState(false);

  const isPending = !clan.appServerId || clan.status === 'PENDING';
  const isAdmin = clan.adminUserId === user.id;

  // Sync editedName when clan prop changes
  useEffect(() => {
    setEditedName(clan.name || '');
  }, [clan.name]);

  const checkServer = async () => {
    const serverId = parseInt(appServerId);
    if (isNaN(serverId) || serverId <= 0) {
      setServerCheckResult(null);
      return;
    }

    try {
      const result = await clansApi.checkAppServer(serverId);
      setServerCheckResult(result);
    } catch (err) {
      console.error('Error checking server:', err);
      setServerCheckResult({ claimed: false });
    }
  };

  useEffect(() => {
    if (appServerId) {
      const timeoutId = setTimeout(() => {
        checkServer();
      }, 500);
      return () => clearTimeout(timeoutId);
    } else {
      setServerCheckResult(null);
    }
  }, [appServerId]);

  const handleConfigureAppServer = async (e: React.FormEvent) => {
    e.preventDefault();
    const serverId = parseInt(appServerId);
    if (isNaN(serverId) || serverId <= 0) {
      setConfigError('Please enter a valid app server ID');
      return;
    }

    if (serverCheckResult?.claimed) {
      setConfigError('This app server ID is already claimed by another clan');
      return;
    }

    try {
      setConfiguring(true);
      setConfigError(null);
      await clansApi.configureAppServerId(clan.id, serverId);
      setAppServerId('');
      setShowConfigureForm(false);
      setServerCheckResult(null);
      onClanUpdated();
    } catch (err: any) {
      console.error('Error configuring app server:', err);
      setConfigError(err.response?.data?.error || 'Failed to configure app server ID');
    } finally {
      setConfiguring(false);
    }
  };

  const handleRegenerateApiKey = async () => {
    if (!confirm('Are you sure you want to regenerate the API key? The old key will stop working after a short grace period.')) {
      return;
    }

    try {
      setRegenerating(true);
      const response = await clansApi.regenerateApiKey(clan.id);
      // Store the new API key
      onApiKeyStored(clan.id, response.apiKey);
      setShowRegenerateKey(false);
      setShowApiKey(true); // Show the new key
      onClanUpdated();
    } catch (err: any) {
      console.error('Error regenerating API key:', err);
      alert(err.response?.data?.error || 'Failed to regenerate API key');
    } finally {
      setRegenerating(false);
    }
  };

  const handleUpdateName = async () => {
    if (editedName.trim() === (clan.name || '').trim()) {
      setIsEditingName(false);
      return;
    }

    try {
      setUpdatingName(true);
      const updateData: UpdateClanRequest = {
        name: editedName.trim() || undefined,
      };
      await clansApi.update(clan.id, updateData);
      setIsEditingName(false);
      onClanUpdated();
    } catch (err: any) {
      console.error('Error updating clan name:', err);
      alert(err.response?.data?.error || 'Failed to update clan name');
      setEditedName(clan.name || '');
    } finally {
      setUpdatingName(false);
    }
  };

  const handleCancelEdit = () => {
    setEditedName(clan.name || '');
    setIsEditingName(false);
  };

  return (
    <div className="clan-card">
      <div className="clan-header">
        <div className="clan-info">
          {isEditingName && isAdmin ? (
            <div className="edit-name-form">
              <input
                type="text"
                value={editedName}
                onChange={(e) => setEditedName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    handleUpdateName();
                  } else if (e.key === 'Escape') {
                    handleCancelEdit();
                  }
                }}
                disabled={updatingName}
                maxLength={255}
                className="edit-name-input"
                autoFocus
              />
              <button
                onClick={handleUpdateName}
                disabled={updatingName}
                className="save-name-btn"
              >
                {updatingName ? 'Saving...' : '✓'}
              </button>
              <button
                onClick={handleCancelEdit}
                disabled={updatingName}
                className="cancel-name-btn"
              >
                ✕
              </button>
            </div>
          ) : (
            <h4>
              {clan.name || `Clan #${clan.id}`}
              {isAdmin && (
                <button
                  onClick={() => setIsEditingName(true)}
                  className="edit-name-btn"
                  title="Edit clan name"
                >
                  ✏️
                </button>
              )}
            </h4>
          )}
          <div className="clan-meta">
            <span className={`clan-status ${isPending ? 'pending' : 'active'}`}>
              {isPending ? '⏳ PENDING' : '✅ ACTIVE'}
            </span>
            {clan.appServerId && (
              <span className="clan-id">App Server ID: {clan.appServerId}</span>
            )}
            {isAdmin && <span className="admin-badge">👑 Admin</span>}
          </div>
          {clan.telegramChannelId && (
            <div className="telegram-info">
              📱 Telegram: {clan.telegramChannelId}
            </div>
          )}
        </div>
      </div>

      {/* API Key Display */}
      {isAdmin && (apiKey || clan.hasApiKey) && (
        <div className="api-key-section">
          {apiKey ? (
            <div className="api-key-display-inline">
              <div className="api-key-header">
                <strong>API Key</strong>
                <button
                  className="toggle-api-key-btn"
                  onClick={() => setShowApiKey(!showApiKey)}
                >
                  {showApiKey ? '👁️ Hide' : '👁️ Show'}
                </button>
              </div>
              {showApiKey && (
                <div className="api-key-box">
                  <code className="api-key-value">{apiKey}</code>
                  <button
                    className="copy-api-key-btn"
                    onClick={() => {
                      navigator.clipboard.writeText(apiKey);
                      alert('API key copied to clipboard!');
                    }}
                  >
                    📋 Copy
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="api-key-missing">
              <p>API key not available. Regenerate to get a new key.</p>
            </div>
          )}
        </div>
      )}

      {/* Step 2: Configure App Server ID (for PENDING clans) */}
      {isPending && isAdmin && (
        <div className="configure-app-server-section">
          {!showConfigureForm ? (
            <button
              className="configure-app-server-btn"
              onClick={() => setShowConfigureForm(true)}
            >
              ⚙️ Configure App Server ID (Step 2)
            </button>
          ) : (
            <form className="configure-app-server-form" onSubmit={handleConfigureAppServer}>
              <h4>Configure App Server ID</h4>
              <p className="form-description">
                Enter the App Server ID from your CS2 server logs (found in ResetBreakpadAppId log line).
              </p>
              {configError && <div className="clan-error-message">{configError}</div>}
              <div className="form-group">
                <label htmlFor={`appServerId-${clan.id}`}>
                  App Server ID <span className="required">*</span>
                </label>
                <input
                  id={`appServerId-${clan.id}`}
                  type="number"
                  value={appServerId}
                  onChange={(e) => setAppServerId(e.target.value)}
                  placeholder="Enter app server ID"
                  required
                  min="1"
                />
                {serverCheckResult !== null && (
                  <div className={`server-check-result ${serverCheckResult.claimed ? 'claimed' : 'available'}`}>
                    {serverCheckResult.claimed ? '⚠️ Already claimed' : '✓ Available'}
                  </div>
                )}
              </div>
              <div className="form-actions">
                <button
                  type="button"
                  className="cancel-btn"
                  onClick={() => {
                    setShowConfigureForm(false);
                    setAppServerId('');
                    setServerCheckResult(null);
                    setConfigError(null);
                  }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="submit-btn"
                  disabled={configuring || serverCheckResult?.claimed === true}
                >
                  {configuring ? 'Configuring...' : 'Configure'}
                </button>
              </div>
            </form>
          )}
        </div>
      )}

      {/* API Key Management (for ACTIVE clans) */}
      {!isPending && isAdmin && (
        <div className="api-key-management">
          {!showRegenerateKey ? (
            <button
              className="regenerate-api-key-btn"
              onClick={() => setShowRegenerateKey(true)}
            >
              🔑 Regenerate API Key
            </button>
          ) : (
            <div className="regenerate-api-key-form">
              <p>Regenerating the API key will invalidate the current key after a grace period.</p>
              <div className="form-actions">
                <button
                  className="cancel-btn"
                  onClick={() => setShowRegenerateKey(false)}
                >
                  Cancel
                </button>
                <button
                  className="submit-btn"
                  onClick={handleRegenerateApiKey}
                  disabled={regenerating}
                >
                  {regenerating ? 'Regenerating...' : 'Confirm Regenerate'}
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Transfer Admin (for admins) */}
      {isAdmin && (
        <div className="clan-actions">
          <TransferAdminForm
            clanId={clan.id}
            onTransfer={onTransferAdmin}
          />
        </div>
      )}
    </div>
  );
};

const TransferAdminForm = ({ clanId, onTransfer }: { clanId: number; onTransfer: (clanId: number, newAdminId: number) => void }) => {
  const [showForm, setShowForm] = useState(false);
  const [newAdminId, setNewAdminId] = useState<string>('');
  const [transferring, setTransferring] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const adminId = parseInt(newAdminId);
    if (isNaN(adminId) || adminId <= 0) {
      alert('Please enter a valid user ID');
      return;
    }

    try {
      setTransferring(true);
      await onTransfer(clanId, adminId);
      setShowForm(false);
      setNewAdminId('');
    } catch (err) {
      // Error handled by parent
    } finally {
      setTransferring(false);
    }
  };

  if (!showForm) {
    return (
      <button
        className="transfer-admin-btn"
        onClick={() => setShowForm(true)}
      >
        Transfer Admin
      </button>
    );
  }

  return (
    <form className="transfer-admin-form" onSubmit={handleSubmit}>
      <input
        type="number"
        value={newAdminId}
        onChange={(e) => setNewAdminId(e.target.value)}
        placeholder="New admin user ID"
        required
        min="1"
      />
      <div className="transfer-actions">
        <button type="submit" disabled={transferring}>
          {transferring ? 'Transferring...' : 'Transfer'}
        </button>
        <button type="button" onClick={() => {
          setShowForm(false);
          setNewAdminId('');
        }}>
          Cancel
        </button>
      </div>
    </form>
  );
};
