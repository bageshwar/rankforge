import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { gamesApi } from '../services/api';
import type { GameDTO } from '../services/api';
import './GamesPage.css';

export const GamesPage = () => {
  const [games, setGames] = useState<GameDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [limit, setLimit] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [expandedPlayers, setExpandedPlayers] = useState<Set<string>>(new Set());

  useEffect(() => {
    loadGames();
  }, [limit]);

  const loadGames = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = limit ? await gamesApi.getRecent(limit) : await gamesApi.getAll();
      setGames(data);
    } catch (err) {
      setError('Failed to load games. Please try again later.');
      console.error('Error loading games:', err);
    } finally {
      setLoading(false);
    }
  };

  const togglePlayers = (gameId: string) => {
    const newExpanded = new Set(expandedPlayers);
    if (newExpanded.has(gameId)) {
      newExpanded.delete(gameId);
    } else {
      newExpanded.add(gameId);
    }
    setExpandedPlayers(newExpanded);
  };

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString);
      return format(date, 'MMM dd, yyyy HH:mm');
    } catch {
      return dateString;
    }
  };

  if (loading) {
    return (
      <PageContainer backgroundClass="bg-games">
        <LoadingSpinner size="lg" message="Loading games..." />
      </PageContainer>
    );
  }

  if (error) {
    return (
      <PageContainer backgroundClass="bg-games">
        <div className="error-message">{error}</div>
      </PageContainer>
    );
  }

  return (
    <PageContainer backgroundClass="bg-games">
      <div className="games-header">
        <h1 className="games-title">🎮 Processed Games</h1>
        <div className="games-stats">
          <div className="stat-item">
            <span className="stat-number">{games.length}</span>
            <span className="stat-label">Total Games</span>
          </div>
          {limit && (
            <div className="stat-item">
              <span className="stat-number">Top {limit}</span>
              <span className="stat-label">Displayed</span>
            </div>
          )}
        </div>
      </div>

      {games.length === 0 ? (
        <div className="no-games">
          <h3>No games processed yet</h3>
          <p>Games will appear here once the CS2 log processing pipeline has analyzed completed matches.</p>
        </div>
      ) : (
        <>
          <div className="filter-controls">
            <button
              className={`filter-btn ${!limit ? 'active' : ''}`}
              onClick={() => setLimit(null)}
            >
              All Games
            </button>
            <button
              className={`filter-btn ${limit === 10 ? 'active' : ''}`}
              onClick={() => setLimit(10)}
            >
              Recent 10
            </button>
            <button
              className={`filter-btn ${limit === 25 ? 'active' : ''}`}
              onClick={() => setLimit(25)}
            >
              Recent 25
            </button>
          </div>

          <div className="games-table-container">
            <table className="games-table">
              <thead>
                <tr>
                  <th>Date & Time</th>
                  <th>Map</th>
                  <th>Mode</th>
                  <th>Score</th>
                  <th>Duration</th>
                  <th>Players</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {games.map((game) => (
                  <tr key={game.gameId}>
                    <td className="game-date">{formatDate(game.gameDate)}</td>
                    <td>
                      <span className="map-badge">{game.map}</span>
                    </td>
                    <td>{game.mode}</td>
                    <td className="score">{game.score}</td>
                    <td>{game.formattedDuration}</td>
                    <td>
                      <div className="players-container">
                        {game.players && game.players.length > 0 ? (
                          <>
                            <div
                              className="players-summary"
                              onClick={() => togglePlayers(game.gameId)}
                            >
                              <span className="players-count">
                                {game.players.length} players
                              </span>
                              <span
                                className={`expand-icon ${
                                  expandedPlayers.has(game.gameId) ? 'expanded' : ''
                                }`}
                              >
                                ▼
                              </span>
                            </div>
                            {expandedPlayers.has(game.gameId) && (
                              <div className="players-dropdown">
                                {game.players.map((player, idx) => (
                                  <div key={idx} className="player-item">
                                    {player}
                                  </div>
                                ))}
                              </div>
                            )}
                          </>
                        ) : (
                          <span className="no-players">No players</span>
                        )}
                      </div>
                    </td>
                    <td>
                      <Link
                        to={`/games/${game.gameId}`}
                        className="details-btn"
                      >
                        📊 Details
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </PageContainer>
  );
};
