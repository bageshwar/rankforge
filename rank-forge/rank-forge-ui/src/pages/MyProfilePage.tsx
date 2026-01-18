import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { PlayerAvatar } from '../components/UI/PlayerAvatar';
import { useAuth } from '../contexts/AuthContext';
import { playersApi, clansApi, usersApi, type ClanDTO } from '../services/api';
import type { PlayerProfileDTO } from '../services/api';
import './MyProfilePage.css';

export const MyProfilePage = () => {
  const { user, refreshUser } = useAuth();
  const [profile, setProfile] = useState<PlayerProfileDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [clans, setClans] = useState<ClanDTO[]>([]);
  const [updatingClan, setUpdatingClan] = useState(false);

  useEffect(() => {
    if (!user) {
      setLoading(false);
      return;
    }

    const loadProfile = async () => {
      try {
        setLoading(true);
        setError(null);

        // Load player stats (don't refresh user - it causes infinite loop)
        const data = await playersApi.getProfile(user.steamId3);
        setProfile(data);
        
        // Load clans
        const userClans = await clansApi.getMyClans();
        setClans(userClans);
      } catch (err) {
        console.error('Error loading profile:', err);
        setError('Failed to load your profile. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, [user?.steamId3]); // Only depend on steamId3, not entire user object

  const handleDefaultClanChange = async (clanId: number | null) => {
    try {
      setUpdatingClan(true);
      await usersApi.updateDefaultClan(clanId);
      await refreshUser(); // Refresh user to get updated defaultClanId
    } catch (err) {
      console.error('Error updating default clan:', err);
      alert('Failed to update default clan. Please try again.');
    } finally {
      setUpdatingClan(false);
    }
  };

  const formatNumber = (num: number, decimals: number = 2) => {
    return num.toFixed(decimals);
  };

  const getRankTier = (rank: number) => {
    if (rank <= 3) return { tier: 'Elite', color: '#ffd700', icon: '👑' };
    if (rank <= 10) return { tier: 'Diamond', color: '#b9f2ff', icon: '💎' };
    if (rank <= 25) return { tier: 'Platinum', color: '#e5e4e2', icon: '⚡' };
    if (rank <= 50) return { tier: 'Gold', color: '#ffa500', icon: '🏅' };
    return { tier: 'Rising', color: '#4a90e2', icon: '📈' };
  };

  if (loading) {
    return (
      <PageContainer backgroundClass="bg-player-profile">
        <LoadingSpinner size="lg" message="Loading your profile..." />
      </PageContainer>
    );
  }

  if (!user) {
    return (
      <PageContainer backgroundClass="bg-player-profile">
        <div className="error-message">Please log in to view your profile.</div>
        <Link to="/" className="back-btn">
          ← Back to Home
        </Link>
      </PageContainer>
    );
  }

  const rankTier = profile ? getRankTier(profile.currentRank) : null;

  return (
    <PageContainer backgroundClass="bg-player-profile">
      <Link to="/" className="back-btn">
        ← Back to Home
      </Link>

      {/* User Header */}
      <motion.div
        className="my-profile-header"
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <div className="profile-avatar-section">
          <PlayerAvatar steamId={user.steamId64} size="large" showBadge={true} />
          {rankTier && (
            <div className="rank-icon-badge" style={{ borderColor: rankTier.color }}>
              {rankTier.icon}
            </div>
          )}
        </div>
        <div className="profile-info">
          <h1 className="player-name">{user.personaName}</h1>
          <div className="player-id-badge">Steam ID: {user.steamId64}</div>
          {user.vacBanned && (
            <div className="vac-banned-badge">⚠️ VAC Banned</div>
          )}
          {profile && (
            <div className="rank-badge" style={{ borderColor: rankTier?.color }}>
              <span className="rank-tier" style={{ color: rankTier?.color }}>
                {rankTier?.tier}
              </span>
              <span className="rank-number">#{profile.currentRank}</span>
            </div>
          )}
        </div>
      </motion.div>

      {error && <div className="error-message">{error}</div>}

      {profile ? (
        <>
          {/* Stats Grid */}
          <motion.div
            className="stats-grid"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
          >
            <div className="stat-card">
              <div className="stat-label">Total Kills</div>
              <div className="stat-value">{profile.totalKills.toLocaleString()}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Total Deaths</div>
              <div className="stat-value">{profile.totalDeaths.toLocaleString()}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">K/D Ratio</div>
              <div className="stat-value">{formatNumber(profile.killDeathRatio)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Headshot %</div>
              <div className="stat-value">{formatNumber(profile.headshotPercentage)}%</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Games Played</div>
              <div className="stat-value">{profile.totalGamesPlayed}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Rounds Played</div>
              <div className="stat-value">{profile.totalRoundsPlayed.toLocaleString()}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Clutches Won</div>
              <div className="stat-value">{profile.clutchesWon}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Total Damage</div>
              <div className="stat-value">{Math.round(profile.totalDamageDealt).toLocaleString()}</div>
            </div>
          </motion.div>

          {/* Quick Actions */}
          <motion.div
            className="quick-actions"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.4 }}
          >
            <Link
              to={`/players/${encodeURIComponent(profile.playerId)}`}
              className="action-btn"
            >
              View Public Profile →
            </Link>
            {user.profileUrl && (
              <a
                href={user.profileUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="action-btn secondary"
              >
                Open Steam Profile →
              </a>
            )}
          </motion.div>

          {/* Default Clan Selector */}
          <motion.div
            className="default-clan-section"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.6 }}
          >
            <h3 className="section-subtitle">Default Clan</h3>
            <p className="section-description">
              Select a default clan to filter rankings and games. You can manage clans on the{' '}
              <Link to="/clan-management" className="inline-link">Clan Management</Link> page.
            </p>
            <div className="clan-selector-wrapper">
              <select
                className="default-clan-select"
                value={user.defaultClanId || ''}
                onChange={(e) => {
                  const value = e.target.value;
                  handleDefaultClanChange(value === '' ? null : parseInt(value));
                }}
                disabled={updatingClan || clans.length === 0}
              >
                <option value="">No Default Clan</option>
                {clans.map((clan) => (
                  <option key={clan.id} value={clan.id}>
                    {clan.name || `Clan #${clan.id}`} {clan.adminUserId === user.id ? '👑' : ''}
                  </option>
                ))}
              </select>
              {clans.length === 0 && (
                <p className="no-clans-hint">
                  You don't have any clans yet.{' '}
                  <Link to="/clan-management" className="inline-link">Create one</Link> to get started.
                </p>
              )}
            </div>
          </motion.div>
        </>
      ) : (
        <div className="no-stats-message">
          <p>You haven't played any games yet. Start playing to see your stats here!</p>
          <Link to="/rankings" className="action-btn">
            View Rankings →
          </Link>
        </div>
      )}

      {/* Default Clan Selector (shown even without stats) */}
      {!profile && (
        <motion.div
          className="default-clan-section"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
        >
          <h3 className="section-subtitle">Default Clan</h3>
          <p className="section-description">
            Select a default clan to filter rankings and games. You can manage clans on the{' '}
            <Link to="/clan-management" className="inline-link">Clan Management</Link> page.
          </p>
          <div className="clan-selector-wrapper">
            <select
              className="default-clan-select"
              value={user.defaultClanId || ''}
              onChange={(e) => {
                const value = e.target.value;
                handleDefaultClanChange(value === '' ? null : parseInt(value));
              }}
              disabled={updatingClan || clans.length === 0}
            >
              <option value="">No Default Clan</option>
              {clans.map((clan) => (
                <option key={clan.id} value={clan.id}>
                  {clan.name || `Clan #${clan.id}`} {clan.adminUserId === user.id ? '👑' : ''}
                </option>
              ))}
            </select>
            {clans.length === 0 && (
              <p className="no-clans-hint">
                You don't have any clans yet.{' '}
                <Link to="/clan-management" className="inline-link">Create one</Link> to get started.
              </p>
            )}
          </div>
        </motion.div>
      )}
    </PageContainer>
  );
};
