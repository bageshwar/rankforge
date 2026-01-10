import { useState, useEffect } from 'react';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { rankingsApi } from '../services/api';
import type { PlayerRankingDTO } from '../services/api';
import './RankingsPage.css';

export const RankingsPage = () => {
  const [rankings, setRankings] = useState<PlayerRankingDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [limit, setLimit] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadRankings();
  }, [limit]);

  const loadRankings = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = limit ? await rankingsApi.getTop(limit) : await rankingsApi.getAll();
      setRankings(data);
    } catch (err) {
      setError('Failed to load rankings. Please try again later.');
      console.error('Error loading rankings:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatNumber = (num: number, decimals: number = 2) => {
    return num.toFixed(decimals);
  };

  const getRankIcon = (rank: number) => {
    if (rank === 1) return '🥇';
    if (rank === 2) return '🥈';
    if (rank === 3) return '🥉';
    return null;
  };

  if (loading) {
    return (
      <PageContainer backgroundClass="bg-rankings">
        <LoadingSpinner size="lg" message="Loading rankings..." />
      </PageContainer>
    );
  }

  if (error) {
    return (
      <PageContainer backgroundClass="bg-rankings">
        <div className="error-message">{error}</div>
      </PageContainer>
    );
  }

  return (
    <PageContainer backgroundClass="bg-rankings">
      <div className="rankings-header">
        <h1 className="rankings-title">🏆 RankForge Player Rankings</h1>
        <p className="rankings-subtitle">CS2 Competitive Player Statistics</p>
      </div>

      <div className="stats-summary">
        <div className="stat-item">
          <span className="stat-value">{rankings.length}</span>
          <span className="stat-label">Total Players</span>
        </div>
        {limit && (
          <div className="stat-item">
            <span className="stat-value">Top {limit}</span>
            <span className="stat-label">Showing</span>
          </div>
        )}
      </div>

      <div className="filter-controls">
        <button
          className={`filter-btn ${!limit ? 'active' : ''}`}
          onClick={() => setLimit(null)}
        >
          All Players
        </button>
        <button
          className={`filter-btn ${limit === 10 ? 'active' : ''}`}
          onClick={() => setLimit(10)}
        >
          Top 10
        </button>
        <button
          className={`filter-btn ${limit === 25 ? 'active' : ''}`}
          onClick={() => setLimit(25)}
        >
          Top 25
        </button>
      </div>

      <div className="table-container">
        <table className="rankings-table">
          <thead>
            <tr>
              <th className="rank-col">Rank</th>
              <th className="player-col">Player</th>
              <th className="stat-col">K/D</th>
              <th className="stat-col">Kills</th>
              <th className="stat-col">Deaths</th>
              <th className="stat-col">Assists</th>
              <th className="stat-col">HS%</th>
              <th className="stat-col">Rounds</th>
              <th className="stat-col">Clutches</th>
              <th className="stat-col">Damage</th>
            </tr>
          </thead>
          <tbody>
            {rankings.map((player) => (
              <tr
                key={player.playerId}
                className={`player-row ${player.rank <= 3 ? 'top-player' : ''}`}
              >
                <td className="rank-cell">
                  <span className="rank-number">{player.rank}</span>
                  {getRankIcon(player.rank) && (
                    <span className="rank-icon">{getRankIcon(player.rank)}</span>
                  )}
                </td>
                <td className="player-cell">
                  <div className="player-info">
                    <span className="player-name">{player.playerName}</span>
                    <span className="player-id">{player.playerId}</span>
                  </div>
                </td>
                <td className="stat-cell kd-ratio">{formatNumber(player.killDeathRatio)}</td>
                <td className="stat-cell">{player.kills}</td>
                <td className="stat-cell">{player.deaths}</td>
                <td className="stat-cell">{player.assists}</td>
                <td className="stat-cell hs-percentage">
                  {formatNumber(player.headshotPercentage, 1)}%
                </td>
                <td className="stat-cell">{player.roundsPlayed}</td>
                <td className="stat-cell">{player.clutchesWon}</td>
                <td className="stat-cell damage">
                  {formatNumber(player.damageDealt, 0)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </PageContainer>
  );
};
