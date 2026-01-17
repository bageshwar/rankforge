import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { gamesApi } from '../services/api';
import type { GameDTO } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import './GamesPage.css';

export const GamesPage = () => {
  const { user, login } = useAuth();
  const [games, setGames] = useState<GameDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [limit, setLimit] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  
  // Get default clan ID from user
  const defaultClanId = user?.defaultClanId || null;

  useEffect(() => {
    loadGames();
  }, [limit, defaultClanId]);

  const loadGames = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Check if user is authenticated
      if (!user) {
        setError('Please log in to view games.');
        setGames([]);
        return;
      }
      
      // Require default clan if user is logged in
      if (!defaultClanId) {
        setError('Please select a default clan in your profile to view games.');
        setGames([]);
        return;
      }
      
      const data = limit ? await gamesApi.getRecent(limit, defaultClanId) : await gamesApi.getAll(defaultClanId);
      setGames(data);
    } catch (err) {
      setError('Failed to load games. Please try again later.');
      console.error('Error loading games:', err);
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

      {!user && (
        <div className="clan-required-message">
          <p>🔐 Please <button onClick={login} className="inline-link" style={{ background: 'none', border: 'none', padding: 0, cursor: 'pointer', textDecoration: 'underline', color: 'inherit' }}>log in</button> to view games.</p>
        </div>
      )}
      {user && !defaultClanId && (
        <div className="clan-required-message">
          <p>⚠️ Please select a default clan in your <Link to="/my-profile" className="inline-link">profile</Link> to view games.</p>
        </div>
      )}

      {games.length === 0 && defaultClanId ? (
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
                  <th>Score</th>
                  <th>Duration</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {games.map((game) => (
                  <tr key={game.id}>
                    <td className="game-date">{formatDate(game.gameDate)}</td>
                    <td>
                      <span className="map-badge">{game.map}</span>
                    </td>
                    <td className="score">{game.score}</td>
                    <td>{game.formattedDuration}</td>
                    <td>
                      <Link
                        to={`/games/${game.id}`}
                        className="details-btn"
                        data-testid={`testid-game-details-link-${game.id}`}
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
