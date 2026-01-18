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
  const [clanName, setClanName] = useState<string>('');
  const [telegramChannelId, setTelegramChannelId] = useState<string>('');
  const [newlyCreatedClan, setNewlyCreatedClan] = useState<ClanDTO | null>(null);

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

  const handleDismissApiKey = () => {
    setNewlyCreatedClan(null);
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
            <strong>Next steps:</strong> Configure the App Server ID after your server boots. Visit the Clan Management page for full setup.
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
            <div key={clan.id} className="clan-card">
              <div className="clan-header">
                <div className="clan-info">
                  <h4>{clan.name || `Clan #${clan.id}`}</h4>
                  <div className="clan-meta">
                    <span className={`clan-status ${!clan.appServerId || clan.status === 'PENDING' ? 'pending' : 'active'}`}>
                      {!clan.appServerId || clan.status === 'PENDING' ? '⏳ PENDING' : '✅ ACTIVE'}
                    </span>
                    {clan.appServerId && (
                      <span className="clan-id">App Server ID: {clan.appServerId}</span>
                    )}
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
