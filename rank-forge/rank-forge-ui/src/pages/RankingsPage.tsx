import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { rankingsApi } from '../services/api';
import type { PlayerRankingDTO, LeaderboardResponseDTO } from '../services/api';
import './RankingsPage.css';

import { extractSteamId } from '../utils/steamId';

type TabType = 'all-time' | 'monthly';

export const RankingsPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  
  // Read URL parameters with defaults
  const getTabFromUrl = (): TabType => {
    const tab = searchParams.get('tab');
    return (tab === 'monthly' || tab === 'all-time') ? tab : 'all-time';
  };
  
  const getYearFromUrl = (): number => {
    const year = searchParams.get('year');
    return year ? parseInt(year, 10) : new Date().getFullYear();
  };
  
  const getMonthFromUrl = (): number => {
    const month = searchParams.get('month');
    return month ? parseInt(month, 10) : new Date().getMonth() + 1;
  };
  
  const getLimitFromUrl = (): number | null => {
    const limit = searchParams.get('limit');
    return limit ? parseInt(limit, 10) : null;
  };
  
  const [rankings, setRankings] = useState<PlayerRankingDTO[]>([]);
  const [totalGames, setTotalGames] = useState<number>(0);
  const [totalRounds, setTotalRounds] = useState<number>(0);
  const [totalPlayers, setTotalPlayers] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [loadingFilter, setLoadingFilter] = useState(false);
  const [limit, setLimit] = useState<number | null>(getLimitFromUrl());
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<TabType>(getTabFromUrl());
  const [selectedYear, setSelectedYear] = useState<number>(getYearFromUrl());
  const [selectedMonth, setSelectedMonth] = useState<number>(getMonthFromUrl());

  // Update URL when state changes
  useEffect(() => {
    const params = new URLSearchParams();
    
    if (activeTab !== 'all-time') {
      params.set('tab', activeTab);
    }
    
    if (activeTab === 'monthly') {
      params.set('year', selectedYear.toString());
      params.set('month', selectedMonth.toString());
    }
    
    if (limit !== null) {
      params.set('limit', limit.toString());
    }
    
    // Only update URL if params have changed
    const currentParams = searchParams.toString();
    const newParams = params.toString();
    if (currentParams !== newParams) {
      setSearchParams(params, { replace: true });
    }
  }, [activeTab, selectedYear, selectedMonth, limit, searchParams, setSearchParams]);

  // Sync state with URL params when they change externally (e.g., browser back/forward)
  useEffect(() => {
    const urlTab = getTabFromUrl();
    const urlYear = getYearFromUrl();
    const urlMonth = getMonthFromUrl();
    const urlLimit = getLimitFromUrl();
    
    if (urlTab !== activeTab) {
      setActiveTab(urlTab);
    }
    if (urlYear !== selectedYear) {
      setSelectedYear(urlYear);
    }
    if (urlMonth !== selectedMonth) {
      setSelectedMonth(urlMonth);
    }
    if (urlLimit !== limit) {
      setLimit(urlLimit);
    }
  }, [searchParams]); // Only depend on searchParams, not the state values

  // Load rankings when dependencies change
  useEffect(() => {
    loadRankings();
  }, [limit, activeTab, selectedYear, selectedMonth]);

  const loadRankings = async (isFilterChange = false) => {
    try {
      if (isFilterChange) {
        setLoadingFilter(true);
      } else {
        setLoading(true);
      }
      setError(null);
      
      if (activeTab === 'monthly') {
        const response = await rankingsApi.getMonthlyLeaderboard(
          selectedYear,
          selectedMonth,
          limit || undefined,
          0
        );
        setRankings(response.rankings);
        setTotalGames(response.totalGames);
        setTotalRounds(response.totalRounds);
        setTotalPlayers(response.totalPlayers);
      } else {
        let response: LeaderboardResponseDTO;
        if (limit) {
          response = await rankingsApi.getTopWithStats(limit);
        } else {
          response = await rankingsApi.getAllWithStats();
        }
        setRankings(response.rankings);
        setTotalGames(response.totalGames);
        setTotalRounds(response.totalRounds);
        setTotalPlayers(response.totalPlayers);
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load rankings. Please try again later.';
      setError(errorMessage);
      console.error('Error loading rankings:', err);
    } finally {
      setLoading(false);
      setLoadingFilter(false);
    }
  };

  const getMonthName = (month: number) => {
    const months = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    return months[month - 1];
  };

  const generateYearOptions = () => {
    const currentYear = new Date().getFullYear();
    const years = [];
    // Generate years from 2020 to current year + 1
    for (let year = 2020; year <= currentYear + 1; year++) {
      years.push(year);
    }
    return years;
  };

  const generateMonthOptions = () => {
    return Array.from({ length: 12 }, (_, i) => i + 1);
  };

  const buildRankingsUrl = (updates: { tab?: TabType; year?: number; month?: number; limit?: number | null }) => {
    const params = new URLSearchParams();
    const tab = updates.tab ?? activeTab;
    const year = updates.year ?? selectedYear;
    const month = updates.month ?? selectedMonth;
    const limitValue = updates.limit !== undefined ? updates.limit : limit;
    
    if (tab !== 'all-time') {
      params.set('tab', tab);
    }
    
    if (tab === 'monthly') {
      params.set('year', year.toString());
      params.set('month', month.toString());
    }
    
    if (limitValue !== null) {
      params.set('limit', limitValue.toString());
    }
    
    return `/rankings${params.toString() ? `?${params.toString()}` : ''}`;
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
        <p className="rankings-subtitle">
          {activeTab === 'monthly' 
            ? `${getMonthName(selectedMonth)} ${selectedYear} Leaderboard`
            : 'CS2 Competitive Player Statistics'}
        </p>
        {activeTab === 'monthly' && (
          <p className="rankings-note">Shows stats accumulated during this month only</p>
        )}
      </div>

      <div className="rankings-tabs">
        <Link
          to={buildRankingsUrl({ tab: 'all-time' })}
          className={`tab-btn ${activeTab === 'all-time' ? 'active' : ''} ${loadingFilter ? 'loading' : ''}`}
          onClick={(e) => {
            e.preventDefault();
            setActiveTab('all-time');
            loadRankings(true);
          }}
          aria-disabled={loadingFilter}
        >
          All Time
        </Link>
        <Link
          to={buildRankingsUrl({ tab: 'monthly' })}
          className={`tab-btn ${activeTab === 'monthly' ? 'active' : ''} ${loadingFilter ? 'loading' : ''}`}
          onClick={(e) => {
            e.preventDefault();
            setActiveTab('monthly');
            loadRankings(true);
          }}
          aria-disabled={loadingFilter}
        >
          Monthly
        </Link>
      </div>

      {activeTab === 'monthly' && (
        <div className="month-selector">
          <label htmlFor="month-select">Month:</label>
          <select
            id="month-select"
            value={selectedMonth}
            onChange={(e) => {
              const newMonth = parseInt(e.target.value);
              setSelectedMonth(newMonth);
              loadRankings(true);
            }}
            disabled={loadingFilter}
          >
            {generateMonthOptions().map((month) => (
              <option key={month} value={month}>
                {getMonthName(month)}
              </option>
            ))}
          </select>
          <label htmlFor="year-select">Year:</label>
          <select
            id="year-select"
            value={selectedYear}
            onChange={(e) => {
              const newYear = parseInt(e.target.value);
              setSelectedYear(newYear);
              loadRankings(true);
            }}
            disabled={loadingFilter}
          >
            {generateYearOptions().map((year) => (
              <option key={year} value={year}>
                {year}
              </option>
            ))}
          </select>
          {loadingFilter && <span className="loading-indicator">Loading...</span>}
        </div>
      )}

      <div className="stats-summary">
        <div className="stat-item">
          <span className="stat-value">{totalPlayers}</span>
          <span className="stat-label">Total Players</span>
        </div>
        <div className="stat-item">
          <span className="stat-value">{totalGames.toLocaleString()}</span>
          <span className="stat-label">Total Games</span>
        </div>
        <div className="stat-item">
          <span className="stat-value">{totalRounds.toLocaleString()}</span>
          <span className="stat-label">Total Rounds</span>
        </div>
        {limit && (
          <div className="stat-item">
            <span className="stat-value">Top {limit}</span>
            <span className="stat-label">Showing</span>
          </div>
        )}
      </div>

      <div className="filter-controls">
        <Link
          to={buildRankingsUrl({ limit: null })}
          className={`filter-btn ${!limit ? 'active' : ''} ${loadingFilter ? 'loading' : ''}`}
          onClick={(e) => {
            e.preventDefault();
            setLimit(null);
            loadRankings(true);
          }}
          aria-disabled={loadingFilter}
        >
          All Players
        </Link>
        <Link
          to={buildRankingsUrl({ limit: 10 })}
          className={`filter-btn ${limit === 10 ? 'active' : ''} ${loadingFilter ? 'loading' : ''}`}
          onClick={(e) => {
            e.preventDefault();
            setLimit(10);
            loadRankings(true);
          }}
          aria-disabled={loadingFilter}
        >
          Top 10
        </Link>
        <Link
          to={buildRankingsUrl({ limit: 25 })}
          className={`filter-btn ${limit === 25 ? 'active' : ''} ${loadingFilter ? 'loading' : ''}`}
          onClick={(e) => {
            e.preventDefault();
            setLimit(25);
            loadRankings(true);
          }}
          aria-disabled={loadingFilter}
        >
          Top 25
        </Link>
      </div>

      <div className="table-container">
        <table className="rankings-table">
          <thead>
            <tr>
              <th className="rank-col">Rank</th>
              <th className="player-col">Player</th>
              <th className="stat-col">ELO</th>
              <th className="stat-col">K/D</th>
              <th className="stat-col">K</th>
              <th className="stat-col">D</th>
              <th className="stat-col">A</th>
              <th className="stat-col">HS%</th>
              <th className="stat-col">Rnd</th>
              <th className="stat-col">G</th>
              <th className="stat-col">Cl</th>
              <th className="stat-col">DMG</th>
            </tr>
          </thead>
          <tbody>
            {rankings.map((player, index) => {
              const position = index + 1;
              const rankClass = position === 1 ? 'rank-gold' : position === 2 ? 'rank-silver' : position === 3 ? 'rank-bronze' : '';
              return (
              <tr
                key={player.playerId}
                className={`player-row ${rankClass}`}
              >
                <td className="rank-cell">
                  <span className={`rank-number ${rankClass}`}>{position}</span>
                  {position <= 3 && (
                    <span className={`rank-icon ${rankClass}`}>{getRankIcon(position)}</span>
                  )}
                </td>
                <td className="player-cell">
                  <Link 
                    to={`/players/${extractSteamId(player.playerId)}`} 
                    className="player-link"
                    data-testid={`testid-rankings-player-link-${extractSteamId(player.playerId)}`}
                  >
                    <div className="player-info">
                      <span className="player-name">{player.playerName}</span>
                      <span className="player-id">{extractSteamId(player.playerId)}</span>
                    </div>
                  </Link>
                </td>
                <td className="stat-cell elo-score">{formatNumber(player.rank, 0)}</td>
                <td className="stat-cell kd-ratio">{formatNumber(player.killDeathRatio)}</td>
                <td className="stat-cell">{player.kills}</td>
                <td className="stat-cell">{player.deaths}</td>
                <td className="stat-cell">{player.assists}</td>
                <td className="stat-cell hs-percentage">
                  {formatNumber(player.headshotPercentage, 1)}%
                </td>
                <td className="stat-cell">{player.roundsPlayed}</td>
                <td className="stat-cell">{player.gamesPlayed || 0}</td>
                <td className="stat-cell">{player.clutchesWon}</td>
                <td className="stat-cell damage">
                  {formatNumber(player.damageDealt, 0)}
                </td>
              </tr>
            );
            })}
          </tbody>
        </table>
      </div>
    </PageContainer>
  );
};
