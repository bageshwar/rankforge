import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { format } from 'date-fns';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { gamesApi } from '../services/api';
import { extractSteamId } from '../utils/steamId';
import type { GameDTO, GameDetailsDTO } from '../services/api';
import './GameDetailsPage.css';

// Map accolade types to icons
const getAccoladeIcon = (typeDescription: string): string => {
  const type = typeDescription.toLowerCase();
  
  if (type.includes('5 kills') || type.includes('5k')) return '💀';
  if (type.includes('4 kills') || type.includes('4k')) return '🎯';
  if (type.includes('3 kills') || type.includes('3k')) return '🔫';
  if (type.includes('first kill')) return '⚡';
  if (type.includes('death')) return '☠️';
  if (type.includes('assist')) return '🤝';
  if (type.includes('headshot')) return '🎯';
  if (type.includes('flash')) return '💡';
  if (type.includes('burn') || type.includes('fire')) return '🔥';
  if (type.includes('cash') || type.includes('money') || type.includes('spent')) return '💰';
  if (type.includes('weapon') || type.includes('unique')) return '🗡️';
  if (type.includes('mvp')) return '⭐';
  if (type.includes('damage')) return '💥';
  if (type.includes('clutch')) return '🏆';
  
  return '🎖️'; // Default medal icon
};

export const GameDetailsPage = () => {
  const { gameId } = useParams<{ gameId: string }>();
  const [game, setGame] = useState<GameDTO | null>(null);
  const [gameDetails, setGameDetails] = useState<GameDetailsDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // Sorting state - default to kills descending
  type SortColumn = 'kills' | 'deaths' | 'assists' | 'damage' | 'hs' | null;
  type SortDirection = 'asc' | 'desc';
  const [sortColumn, setSortColumn] = useState<SortColumn>('kills');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  useEffect(() => {
    if (gameId) {
      loadGameDetails();
    }
  }, [gameId]);

  const loadGameDetails = async () => {
    if (!gameId) return;

    try {
      setLoading(true);
      setError(null);
      const [gameData, detailsData] = await Promise.all([
        gamesApi.getById(gameId),
        gamesApi.getDetails(gameId),
      ]);

      if (!gameData) {
        setError('Game not found');
        return;
      }

      setGame(gameData);
      setGameDetails(detailsData);
    } catch (err) {
      setError('Failed to load game details. Please try again later.');
      console.error('Error loading game details:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString);
      return format(date, 'MMM dd, yyyy HH:mm');
    } catch {
      return dateString;
    }
  };

  const getRankIcon = (position: number) => {
    if (position === 1) return '🥇';
    if (position === 2) return '🥈';
    if (position === 3) return '🥉';
    return null;
  };

  const handleSort = (column: SortColumn) => {
    if (column === null) return;
    
    if (sortColumn === column) {
      // Toggle direction if clicking same column
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      // New column, default to descending
      setSortColumn(column);
      setSortDirection('desc');
    }
  };

  const getSortedPlayerStats = () => {
    if (!gameDetails?.playerStats) return [];
    
    // First, calculate kill rankings (for gold/silver/bronze badges)
    const playersWithKillRank = [...gameDetails.playerStats]
      .sort((a, b) => b.kills - a.kills)
      .map((player, idx) => ({
        ...player,
        killRank: idx + 1 // 1 = most kills, 2 = second most, etc.
      }));
    
    // Then sort by selected column
    if (!sortColumn) return playersWithKillRank;

    const sorted = playersWithKillRank.sort((a, b) => {
      let aValue: number;
      let bValue: number;

      switch (sortColumn) {
        case 'kills':
          aValue = a.kills;
          bValue = b.kills;
          break;
        case 'deaths':
          aValue = a.deaths;
          bValue = b.deaths;
          break;
        case 'assists':
          aValue = a.assists;
          bValue = b.assists;
          break;
        case 'damage':
          aValue = a.damage || 0;
          bValue = b.damage || 0;
          break;
        case 'hs':
          aValue = a.headshotPercentage || 0;
          bValue = b.headshotPercentage || 0;
          break;
        default:
          return 0;
      }

      if (sortDirection === 'asc') {
        return aValue - bValue;
      } else {
        return bValue - aValue;
      }
    });

    return sorted;
  };

  if (loading) {
    return (
      <PageContainer backgroundClass="bg-game-details">
        <LoadingSpinner size="lg" message="Loading game details..." />
      </PageContainer>
    );
  }

  if (error || !game) {
    return (
      <PageContainer backgroundClass="bg-game-details">
        <div className="error-message">
          {error || 'Game not found'}
        </div>
        <Link to="/games" className="back-btn">
          ← Back to Games
        </Link>
      </PageContainer>
    );
  }

  return (
    <PageContainer backgroundClass="bg-game-details">
      <Link to="/games" className="back-btn">
        ← Back to Games
      </Link>

      <div className="game-header">
        <h1 className="game-title">🎮 {game.map}</h1>
        
        {/* Score Display */}
        {gameDetails && (
          <div className="header-score">
            <span className="header-score-ct">{gameDetails.ctScore}</span>
            <span className="header-score-divider">:</span>
            <span className="header-score-t">{gameDetails.tScore}</span>
          </div>
        )}
        
        <div className="game-meta">
          <div className="meta-item">
            <span className="meta-label">Date & Time</span>
            <span className="meta-value">{formatDate(game.gameDate)}</span>
          </div>
          <div className="meta-item">
            <span className="meta-label">Duration</span>
            <span className="meta-value">{game.formattedDuration}</span>
          </div>
          {gameDetails && (
            <div className="meta-item">
              <span className="meta-label">Rounds</span>
              <span className="meta-value">{gameDetails.totalRounds}</span>
            </div>
          )}
        </div>
      </div>

      {/* Round Timeline */}
      {gameDetails?.rounds && gameDetails.rounds.length > 0 && (
        <div className="section-card card-bg rounds-section">
          <h2 className="section-title">⏱️ Round Timeline</h2>
          <p className="rounds-description">
            Round-by-round results: <span className="color-legend">Blue = CT Win</span> | <span className="color-legend">Orange = T Win</span>
          </p>
          <p className="rounds-hint">Click a round to view detailed events</p>
          <div className="rounds-timeline">
            {gameDetails.rounds.map((round, idx) => (
              <Link
                key={idx}
                to={`/games/${gameId}/rounds/${idx + 1}`}
                className={`round-badge ${
                  round.winnerTeam === 'CT' ? 'ct-win' : 't-win'
                }`}
                title={`Round ${idx + 1}: ${round.winnerTeam} Win - Click for details`}
              >
                {idx + 1}
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Player Statistics - Tabular */}
      <div className="section-card card-bg player-stats-section">
        <h2 className="section-title">👥 Player Statistics</h2>
        {gameDetails?.playerStats && gameDetails.playerStats.length > 0 ? (
          <div className="stats-table-container">
            <table className="stats-table">
              <thead>
                <tr>
                  <th>Player</th>
                  <th 
                    className="sortable"
                    onClick={() => handleSort('kills')}
                    title="Click to sort by kills"
                    data-sort-active={sortColumn === 'kills' ? 'true' : 'false'}
                    data-sort-direction={sortColumn === 'kills' ? sortDirection : undefined}
                  >
                    K
                  </th>
                  <th 
                    className="sortable"
                    onClick={() => handleSort('deaths')}
                    title="Click to sort by deaths"
                    data-sort-active={sortColumn === 'deaths' ? 'true' : 'false'}
                    data-sort-direction={sortColumn === 'deaths' ? sortDirection : undefined}
                  >
                    D
                  </th>
                  <th 
                    className="sortable"
                    onClick={() => handleSort('assists')}
                    title="Click to sort by assists"
                    data-sort-active={sortColumn === 'assists' ? 'true' : 'false'}
                    data-sort-direction={sortColumn === 'assists' ? sortDirection : undefined}
                  >
                    A
                  </th>
                  <th 
                    className="sortable"
                    onClick={() => handleSort('damage')}
                    title="Click to sort by damage"
                    data-sort-active={sortColumn === 'damage' ? 'true' : 'false'}
                    data-sort-direction={sortColumn === 'damage' ? sortDirection : undefined}
                  >
                    DMG
                  </th>
                  <th 
                    className="sortable"
                    onClick={() => handleSort('hs')}
                    title="Click to sort by headshot %"
                    data-sort-active={sortColumn === 'hs' ? 'true' : 'false'}
                    data-sort-direction={sortColumn === 'hs' ? sortDirection : undefined}
                  >
                    HS%
                  </th>
                </tr>
              </thead>
              <tbody>
                {getSortedPlayerStats().map((player, idx) => {
                  const killRank = player.killRank || 999; // Use killRank for badges (persistent based on kills)
                  const rankClass = killRank === 1 ? 'rank-gold' : killRank === 2 ? 'rank-silver' : killRank === 3 ? 'rank-bronze' : '';
                  const steamId = extractSteamId(player.playerId);
                  return (
                    <tr key={idx} className={rankClass}>
                      <td className="player-name-cell">
                        {killRank <= 3 && (
                          <span className={`rank-icon ${rankClass}`} data-testid={`testid-rank-icon-${killRank}`}>
                            {getRankIcon(killRank)}
                          </span>
                        )}
                        {steamId ? (
                          <Link 
                            to={`/players/${steamId}`} 
                            className="player-profile-link"
                            data-testid={`testid-player-link-${steamId}`}
                          >
                            {player.playerName}
                          </Link>
                        ) : (
                          player.playerName
                        )}
                      </td>
                      <td className="kills-cell">{player.kills}</td>
                      <td className="deaths-cell">{player.deaths}</td>
                      <td className="assists-cell">{player.assists}</td>
                      <td className="damage-cell">{player.damage || 0}</td>
                      <td className="headshot-cell">
                        {Math.round(player.headshotPercentage || 0)}%
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="no-data">No player statistics available.</div>
        )}
      </div>

      {/* Accolades */}
      {gameDetails?.accolades && gameDetails.accolades.length > 0 && (
        <div className="section-card card-bg accolades-section">
          <h2 className="section-title">🏆 Match Accolades</h2>
          <div className="accolades-grid">
            {gameDetails.accolades.map((accolade, idx) => {
              const steamId = extractSteamId(accolade.playerId);
              return (
                <div key={idx} className="accolade-card">
                  <div className="accolade-icon">{getAccoladeIcon(accolade.typeDescription)}</div>
                  <div className="accolade-content">
                    <div className="accolade-type">{accolade.typeDescription}</div>
                    {steamId ? (
                      <Link 
                        to={`/players/${steamId}`} 
                        className="accolade-player-link"
                        data-testid={`testid-accolade-player-link-${steamId}`}
                      >
                        {accolade.playerName}
                      </Link>
                    ) : (
                      <div className="accolade-player">{accolade.playerName}</div>
                    )}
                    <div className="accolade-value">{accolade.value.toFixed(0)}</div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </PageContainer>
  );
};
