import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { format } from 'date-fns';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { gamesApi } from '../services/api';
import type { GameDTO, GameDetailsDTO } from '../services/api';
import './GameDetailsPage.css';

export const GameDetailsPage = () => {
  const { gameId } = useParams<{ gameId: string }>();
  const [game, setGame] = useState<GameDTO | null>(null);
  const [gameDetails, setGameDetails] = useState<GameDetailsDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
        <div className="game-meta">
          <div className="meta-item">
            <span className="meta-label">Date & Time</span>
            <span className="meta-value">{formatDate(game.gameDate)}</span>
          </div>
          <div className="meta-item">
            <span className="meta-label">Mode</span>
            <span className="meta-value">{game.mode}</span>
          </div>
          <div className="meta-item">
            <span className="meta-label">Final Score</span>
            <span className="meta-value">{game.score}</span>
          </div>
          <div className="meta-item">
            <span className="meta-label">Duration</span>
            <span className="meta-value">{game.formattedDuration}</span>
          </div>
        </div>
      </div>

      <div className="content-grid">
        {/* Player Statistics */}
        <div className="section-card card-bg">
          <h2 className="section-title">👥 Player Statistics</h2>
          {gameDetails?.playerStats && gameDetails.playerStats.length > 0 ? (
            <div className="players-grid">
              {gameDetails.playerStats.map((player, idx) => (
                <div key={idx} className="player-card">
                  <div className="player-name">{player.playerName}</div>
                  <div className="player-stats">
                    <div className="stat-item">
                      <span className="stat-number">{player.kills}</span>
                      <span className="stat-label">K</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-number">{player.deaths}</span>
                      <span className="stat-label">D</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-number">{player.assists}</span>
                      <span className="stat-label">A</span>
                    </div>
                    <div className="stat-item">
                      <span className="stat-number">
                        {player.rating.toFixed(2)}
                      </span>
                      <span className="stat-label">Rating</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="no-data">No detailed player statistics available.</div>
          )}
        </div>

        {/* Game Summary */}
        <div className="section-card card-bg">
          <h2 className="section-title">📊 Game Summary</h2>
          {gameDetails ? (
            <>
              <div className="team-section">
                <div className="team-header team-ct">
                  Counter-Terrorists: <span>{gameDetails.ctScore}</span> rounds
                </div>
              </div>
              <div className="team-section">
                <div className="team-header team-t">
                  Terrorists: <span>{gameDetails.tScore}</span> rounds
                </div>
              </div>
              <div className="total-rounds">
                <span className="stat-number">{gameDetails.totalRounds}</span>
                <span className="stat-label">Total Rounds Played</span>
              </div>
            </>
          ) : (
            <div className="no-data">Game summary data is being processed...</div>
          )}
        </div>

        {/* Round Timeline */}
        {gameDetails?.rounds && gameDetails.rounds.length > 0 && (
          <div className="section-card card-bg rounds-section">
            <h2 className="section-title">⏱️ Round Timeline</h2>
            <p className="rounds-description">
              Round-by-round results (Green = CT Win, Orange = T Win)
            </p>
            <div className="rounds-timeline">
              {gameDetails.rounds.map((round, idx) => (
                <div
                  key={idx}
                  className={`round-badge ${
                    round.winnerTeam === 'CT' ? 'ct-win' : 't-win'
                  }`}
                  title={`Round ${idx + 1}: ${round.winnerTeam} Win`}
                >
                  {idx + 1}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Accolades */}
        {gameDetails?.accolades && gameDetails.accolades.length > 0 && (
          <div className="section-card card-bg accolades-section">
            <h2 className="section-title">🏆 Match Accolades</h2>
            <p className="accolades-description">
              Player achievements and awards from this match
            </p>
            <div className="accolades-grid">
              {gameDetails.accolades.map((accolade, idx) => (
                <div key={idx} className="accolade-card">
                  <div className="accolade-header">
                    <span className="accolade-type">{accolade.typeDescription}</span>
                    <span className="accolade-position">#{accolade.position}</span>
                  </div>
                  <div className="accolade-player">{accolade.playerName}</div>
                  <div className="accolade-stats">
                    <div className="accolade-stat">
                      <span className="stat-label">Value:</span>
                      <span className="stat-value">{accolade.value.toFixed(1)}</span>
                    </div>
                    <div className="accolade-stat">
                      <span className="stat-label">Score:</span>
                      <span className="stat-value">{accolade.score.toFixed(1)}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </PageContainer>
  );
};
