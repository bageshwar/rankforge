import { useState, useEffect } from 'react';
import { clansApi, type ClanDTO, type CreateClanRequest } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';
import './ClanManagementSection.css';

export const ClanManagementSection = () => {
  const { user } = useAuth();
  const [clans, setClans] = useState<ClanDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [creating, setCreating] = useState(false);
  
  // Form state
  const [appServerId, setAppServerId] = useState<string>('');
  const [clanName, setClanName] = useState<string>('');
  const [telegramChannelId, setTelegramChannelId] = useState<string>('');
  const [serverCheckResult, setServerCheckResult] = useState<{ claimed: boolean } | null>(null);

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
      }, 500); // Debounce
      return () => clearTimeout(timeoutId);
    } else {
      setServerCheckResult(null);
    }
  }, [appServerId]);

  const handleCreateClan = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const serverId = parseInt(appServerId);
    if (isNaN(serverId) || serverId <= 0) {
      setError('Please enter a valid app server ID');
      return;
    }

    if (serverCheckResult?.claimed) {
      setError('This app server ID is already claimed by another clan');
      return;
    }

    try {
      setCreating(true);
      setError(null);
      
      const request: CreateClanRequest = {
        appServerId: serverId,
        name: clanName.trim() || undefined,
        telegramChannelId: telegramChannelId.trim() || undefined,
      };
      
      await clansApi.create(request);
      
      // Reset form
      setAppServerId('');
      setClanName('');
      setTelegramChannelId('');
      setShowCreateForm(false);
      setServerCheckResult(null);
      
      // Reload clans
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

  if (loading) {
    return <div className="clan-management-loading">Loading clans...</div>;
  }

  return (
    <div className="clan-management-section">
      <h2 className="section-title">Clan Management</h2>
      
      {error && <div className="clan-error-message">{error}</div>}

      {/* Create Clan Form */}
      {!showCreateForm ? (
        <button
          className="create-clan-btn"
          onClick={() => setShowCreateForm(true)}
        >
          + Create New Clan
        </button>
      ) : (
        <form className="create-clan-form" onSubmit={handleCreateClan}>
          <h3>Create New Clan</h3>
          
          <div className="form-group">
            <label htmlFor="appServerId">
              App Server ID <span className="required">*</span>
            </label>
            <input
              id="appServerId"
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
                setAppServerId('');
                setClanName('');
                setTelegramChannelId('');
                setServerCheckResult(null);
                setError(null);
              }}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="submit-btn"
              disabled={creating || serverCheckResult?.claimed === true}
            >
              {creating ? 'Creating...' : 'Create Clan'}
            </button>
          </div>
        </form>
      )}

      {/* My Clans List */}
      {clans.length > 0 && (
        <div className="my-clans-list">
          <h3>My Clans</h3>
          {clans.map((clan) => (
            <div key={clan.id} className="clan-card">
              <div className="clan-header">
                <div className="clan-info">
                  <h4>{clan.name || `Clan #${clan.id}`}</h4>
                  <div className="clan-meta">
                    <span className="clan-id">App Server ID: {clan.appServerId}</span>
                    {clan.adminUserId === user?.id && (
                      <span className="admin-badge">👑 Admin</span>
                    )}
                  </div>
                  {clan.telegramChannelId && (
                    <div className="telegram-info">
                      📱 Telegram: {clan.telegramChannelId}
                    </div>
                  )}
                </div>
              </div>
              {clan.adminUserId === user?.id && (
                <div className="clan-actions">
                  <TransferAdminForm
                    clanId={clan.id}
                    onTransfer={handleTransferAdmin}
                  />
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {clans.length === 0 && !showCreateForm && (
        <div className="no-clans-message">
          <p>You haven't created or joined any clans yet.</p>
          <p>Create a clan to claim an app server ID and start managing your community!</p>
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
