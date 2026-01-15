import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { playersApi } from '../services/api';
import type { PlayerProfileDTO, RatingHistoryPoint } from '../services/api';
import './PlayerProfilePage.css';

import { extractSteamId } from '../utils/steamId';

export const PlayerProfilePage = () => {
  const { playerId } = useParams<{ playerId: string }>();
  const [profile, setProfile] = useState<PlayerProfileDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (playerId) {
      loadProfile();
    }
  }, [playerId]);

  const loadProfile = async () => {
    if (!playerId) return;

    try {
      setLoading(true);
      setError(null);
      const data = await playersApi.getProfile(playerId);

      if (!data) {
        setError('Player not found');
        return;
      }

      setProfile(data);
    } catch (err) {
      setError('Failed to load player profile. Please try again later.');
      console.error('Error loading player profile:', err);
    } finally {
      setLoading(false);
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
        <LoadingSpinner size="lg" message="Loading player profile..." />
      </PageContainer>
    );
  }

  if (error || !profile) {
    return (
      <PageContainer backgroundClass="bg-player-profile">
        <div className="error-message">{error || 'Player not found'}</div>
        <Link to="/rankings" className="back-btn">
          ← Back to Rankings
        </Link>
      </PageContainer>
    );
  }

  const rankTier = getRankTier(profile.currentRank);

  return (
    <PageContainer backgroundClass="bg-player-profile">
      <Link to="/rankings" className="back-btn">
        ← Back to Rankings
      </Link>

      {/* Player Header */}
      <motion.div
        className="profile-header"
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <div className="profile-avatar">
          <span className="avatar-icon">{rankTier.icon}</span>
        </div>
        <div className="profile-info">
          <h1 className="player-name">{profile.playerName}</h1>
          <div className="player-id-badge">{extractSteamId(profile.playerId)}</div>
          <div className="rank-badge" style={{ borderColor: rankTier.color }}>
            <span className="rank-tier" style={{ color: rankTier.color }}>
              {rankTier.tier}
            </span>
            <span className="rank-number">Rank #{profile.currentRank}</span>
          </div>
        </div>
        <div className="current-rating">
          <div className="rating-value">{formatNumber(profile.killDeathRatio)}</div>
          <div className="rating-label">K/D Ratio</div>
        </div>
      </motion.div>

      {/* Stats Grid */}
      <motion.div
        className="stats-grid"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.1 }}
      >
        <div className="stat-card">
          <div className="stat-icon">🎮</div>
          <div className="stat-value">{profile.totalGamesPlayed}</div>
          <div className="stat-label">Games Played</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">💀</div>
          <div className="stat-value">{profile.totalKills}</div>
          <div className="stat-label">Total Kills</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">☠️</div>
          <div className="stat-value">{profile.totalDeaths}</div>
          <div className="stat-label">Total Deaths</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">🤝</div>
          <div className="stat-value">{profile.totalAssists}</div>
          <div className="stat-label">Assists</div>
        </div>
        <div className="stat-card highlight">
          <div className="stat-icon">🎯</div>
          <div className="stat-value">{formatNumber(profile.headshotPercentage, 1)}%</div>
          <div className="stat-label">Headshot %</div>
        </div>
        <div className="stat-card highlight">
          <div className="stat-icon">🏆</div>
          <div className="stat-value">{profile.clutchesWon}</div>
          <div className="stat-label">Clutches Won</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">🔫</div>
          <div className="stat-value">{profile.totalRoundsPlayed}</div>
          <div className="stat-label">Rounds Played</div>
        </div>
        <div className="stat-card">
          <div className="stat-icon">💥</div>
          <div className="stat-value">{formatNumber(profile.totalDamageDealt, 0)}</div>
          <div className="stat-label">Total Damage</div>
        </div>
      </motion.div>

      {/* Past Nicks Section */}
      {profile.pastNicks && profile.pastNicks.length > 0 && (
        <motion.div
          className="section-card past-nicks-section"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.15 }}
        >
          <h2 className="section-title">👤 Past Nicknames</h2>
          <div className="past-nicks-list">
            {profile.pastNicks.map((nick, idx) => (
              <div key={idx} className="past-nick-item">
                {nick}
              </div>
            ))}
          </div>
        </motion.div>
      )}

      {/* Rating History Chart */}
      {profile.ratingHistory && profile.ratingHistory.length > 0 && (
        <motion.div
          className="section-card chart-section"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
        >
          <h2 className="section-title">📈 Rating Progression</h2>
          <RatingChart data={profile.ratingHistory} />
        </motion.div>
      )}

      {/* Accolades Section */}
      <motion.div
        className="section-card accolades-summary"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.3 }}
      >
        <h2 className="section-title">🏅 Accolades Overview</h2>
        <div className="accolades-stats">
          <div className="accolade-stat-card">
            <div className="accolade-stat-value">{profile.totalAccolades}</div>
            <div className="accolade-stat-label">Total Accolades</div>
          </div>
          <div className="accolade-stat-card featured">
            <div className="accolade-stat-icon">⭐</div>
            <div className="accolade-stat-value">{profile.mostFrequentAccolade}</div>
            <div className="accolade-stat-label">Most Frequent</div>
          </div>
        </div>

        {/* Accolades by Type */}
        {profile.accoladesByType && Object.keys(profile.accoladesByType).length > 0 && (
          <div className="accolades-breakdown">
            <h3 className="subsection-title">Accolades by Type</h3>
            <div className="accolades-type-grid">
              {Object.entries(profile.accoladesByType)
                .sort(([, a], [, b]) => b - a)
                .map(([type, count]) => (
                  <div key={type} className="accolade-type-item">
                    <span className="accolade-type-name">{type}</span>
                    <span className="accolade-type-count">{count}</span>
                  </div>
                ))}
            </div>
          </div>
        )}
      </motion.div>

      {/* Recent Accolades */}
      {profile.accolades && profile.accolades.length > 0 && (
        <motion.div
          className="section-card recent-accolades"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
        >
          <h2 className="section-title">🏆 Recent Accolades</h2>
          <div className="accolades-list">
            {profile.accolades.slice(0, 10).map((accolade, idx) => (
              <div key={idx} className="accolade-item">
                <div className="accolade-icon">🥇</div>
                <div className="accolade-details">
                  <div className="accolade-type">{accolade.typeDescription}</div>
                  <div className="accolade-meta">
                    <span className="accolade-position">#{accolade.position}</span>
                    <span className="accolade-date">{accolade.gameDate}</span>
                  </div>
                </div>
                <div className="accolade-values">
                  <div className="accolade-value">
                    <span className="value-label">Value</span>
                    <span className="value-number">{accolade.value.toFixed(1)}</span>
                  </div>
                  <div className="accolade-score">
                    <span className="value-label">Score</span>
                    <span className="value-number">{accolade.score.toFixed(1)}</span>
                  </div>
                </div>
                {accolade.gameId && (
                  <Link to={`/games/${accolade.gameId}`} className="view-game-link">
                    View Game →
                  </Link>
                )}
              </div>
            ))}
          </div>
        </motion.div>
      )}
    </PageContainer>
  );
};

// Custom SVG Line Chart Component
interface RatingChartProps {
  data: RatingHistoryPoint[];
}

const RatingChart = ({ data }: RatingChartProps) => {
  const width = 800;
  const height = 300;
  const padding = { top: 40, right: 40, bottom: 60, left: 60 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  // Calculate min/max for K/D ratio
  const kdValues = data.map((d) => d.killDeathRatio);
  const minKd = Math.min(...kdValues) * 0.9;
  const maxKd = Math.max(...kdValues) * 1.1;

  // Scale functions
  const xScale = (index: number) => (index / (data.length - 1)) * chartWidth + padding.left;
  const yScale = (value: number) =>
    chartHeight - ((value - minKd) / (maxKd - minKd)) * chartHeight + padding.top;

  // Generate path for the line
  const linePath = data
    .map((point, i) => {
      const x = xScale(i);
      const y = yScale(point.killDeathRatio);
      return `${i === 0 ? 'M' : 'L'} ${x} ${y}`;
    })
    .join(' ');

  // Generate area path
  const areaPath = `${linePath} L ${xScale(data.length - 1)} ${padding.top + chartHeight} L ${padding.left} ${padding.top + chartHeight} Z`;

  // Y-axis ticks
  const yTicks = 5;
  const yTickValues = Array.from({ length: yTicks }, (_, i) => minKd + ((maxKd - minKd) / (yTicks - 1)) * i);

  return (
    <div className="chart-container">
      <svg viewBox={`0 0 ${width} ${height}`} className="rating-chart">
        {/* Grid lines */}
        <g className="grid-lines">
          {yTickValues.map((tick, i) => (
            <line
              key={i}
              x1={padding.left}
              y1={yScale(tick)}
              x2={width - padding.right}
              y2={yScale(tick)}
              stroke="rgba(255,255,255,0.1)"
              strokeDasharray="4,4"
            />
          ))}
        </g>

        {/* Area fill with gradient */}
        <defs>
          <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#4a90e2" stopOpacity="0.4" />
            <stop offset="100%" stopColor="#4a90e2" stopOpacity="0.05" />
          </linearGradient>
          <linearGradient id="lineGradient" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stopColor="#4a90e2" />
            <stop offset="50%" stopColor="#ff6b35" />
            <stop offset="100%" stopColor="#4a90e2" />
          </linearGradient>
        </defs>

        {/* Area */}
        <motion.path
          d={areaPath}
          fill="url(#areaGradient)"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 1 }}
        />

        {/* Line */}
        <motion.path
          d={linePath}
          fill="none"
          stroke="url(#lineGradient)"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 1.5, ease: 'easeInOut' }}
        />

        {/* Data points */}
        {data.map((point, i) => (
          <motion.g key={i} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.5 + i * 0.05 }}>
            <circle cx={xScale(i)} cy={yScale(point.killDeathRatio)} r="6" fill="#1a1a1a" stroke="#4a90e2" strokeWidth="2" />
            <circle cx={xScale(i)} cy={yScale(point.killDeathRatio)} r="3" fill="#4a90e2" />
            {/* Tooltip trigger area */}
            <g className="chart-tooltip-trigger">
              <circle cx={xScale(i)} cy={yScale(point.killDeathRatio)} r="15" fill="transparent" />
              <foreignObject x={xScale(i) - 60} y={yScale(point.killDeathRatio) - 80} width="120" height="70">
                <div className="chart-tooltip">
                  <div className="tooltip-kd">{point.killDeathRatio.toFixed(2)} K/D</div>
                  <div className="tooltip-kda">
                    {point.kills}/{point.deaths}/{point.assists}
                  </div>
                  <div className="tooltip-game">Game #{point.gameNumber}</div>
                </div>
              </foreignObject>
            </g>
          </motion.g>
        ))}

        {/* Y-axis labels */}
        {yTickValues.map((tick, i) => (
          <text key={i} x={padding.left - 10} y={yScale(tick)} textAnchor="end" dominantBaseline="middle" fill="#b0b0b0" fontSize="12">
            {tick.toFixed(2)}
          </text>
        ))}

        {/* X-axis label */}
        <text x={width / 2} y={height - 10} textAnchor="middle" fill="#b0b0b0" fontSize="14">
          Game Number
        </text>

        {/* Y-axis label */}
        <text x={15} y={height / 2} textAnchor="middle" fill="#b0b0b0" fontSize="14" transform={`rotate(-90, 15, ${height / 2})`}>
          K/D Ratio
        </text>

        {/* X-axis game numbers */}
        {data.length <= 10 ? (
          data.map((point, i) => (
            <text key={i} x={xScale(i)} y={padding.top + chartHeight + 25} textAnchor="middle" fill="#b0b0b0" fontSize="11">
              #{point.gameNumber}
            </text>
          ))
        ) : (
          // Show fewer labels for many games
          [0, Math.floor(data.length / 4), Math.floor(data.length / 2), Math.floor((3 * data.length) / 4), data.length - 1].map((i) => (
            <text key={i} x={xScale(i)} y={padding.top + chartHeight + 25} textAnchor="middle" fill="#b0b0b0" fontSize="11">
              #{data[i].gameNumber}
            </text>
          ))
        )}
      </svg>
    </div>
  );
};
