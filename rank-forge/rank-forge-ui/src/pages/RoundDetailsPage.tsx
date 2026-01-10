import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { gamesApi } from '../services/api';
import type { RoundDetailsDTO, RoundEventDTO } from '../services/api';
import './RoundDetailsPage.css';

// Event type icons and labels
const getEventIcon = (eventType: string, details?: RoundEventDTO): string => {
  switch (eventType) {
    case 'KILL':
      return details?.isHeadshot ? '🎯' : '💀';
    case 'ASSIST':
      return '🤝';
    case 'BOMB_EVENT':
      switch (details?.bombEventType?.toLowerCase()) {
        case 'planted':
          return '💣';
        case 'defused':
          return '🔧';
        case 'exploded':
          return '💥';
        default:
          return '💣';
      }
    case 'ATTACK':
      return '⚔️';
    default:
      return '📌';
  }
};

const getEventLabel = (eventType: string, details?: RoundEventDTO): string => {
  switch (eventType) {
    case 'KILL':
      return details?.isHeadshot ? 'Headshot Kill' : 'Kill';
    case 'ASSIST':
      return details?.assistType === 'flash_assist' ? 'Flash Assist' : 'Assist';
    case 'BOMB_EVENT':
      switch (details?.bombEventType?.toLowerCase()) {
        case 'planted':
          return 'Bomb Planted';
        case 'defused':
          return 'Bomb Defused';
        case 'exploded':
          return 'Bomb Exploded';
        case 'begindefuse':
          return 'Defusing Started';
        case 'dropped':
          return 'Bomb Dropped';
        case 'pickup':
          return 'Bomb Picked Up';
        default:
          return `Bomb ${details?.bombEventType || 'Event'}`;
      }
    case 'ATTACK':
      return 'Damage';
    default:
      return eventType;
  }
};

const getEventColorClass = (eventType: string, details?: RoundEventDTO): string => {
  switch (eventType) {
    case 'KILL':
      return details?.isHeadshot ? 'event-headshot' : 'event-kill';
    case 'ASSIST':
      return 'event-assist';
    case 'BOMB_EVENT':
      switch (details?.bombEventType?.toLowerCase()) {
        case 'planted':
          return 'event-bomb-planted';
        case 'defused':
          return 'event-bomb-defused';
        case 'exploded':
          return 'event-bomb-exploded';
        default:
          return 'event-bomb';
      }
    case 'ATTACK':
      return 'event-attack';
    default:
      return 'event-default';
  }
};

const formatWeaponName = (weapon: string | undefined): string => {
  if (!weapon) return '';
  // Clean up weapon names (e.g., "weapon_ak47" -> "AK-47")
  const cleanName = weapon.replace('weapon_', '').replace('_', '-').toUpperCase();
  return cleanName;
};

const formatTimeOffset = (ms: number): string => {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
};

export const RoundDetailsPage = () => {
  const { gameId, roundNumber } = useParams<{ gameId: string; roundNumber: string }>();
  const [roundDetails, setRoundDetails] = useState<RoundDetailsDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (gameId && roundNumber) {
      loadRoundDetails();
    }
  }, [gameId, roundNumber]);

  const loadRoundDetails = async () => {
    if (!gameId || !roundNumber) return;

    try {
      setLoading(true);
      setError(null);
      const data = await gamesApi.getRoundDetails(gameId, parseInt(roundNumber, 10));

      if (!data) {
        setError('Round not found');
        return;
      }

      setRoundDetails(data);
    } catch (err) {
      setError('Failed to load round details. Please try again later.');
      console.error('Error loading round details:', err);
    } finally {
      setLoading(false);
    }
  };

  // Filter events to show only significant ones (kills, assists, bombs)
  const getSignificantEvents = (events: RoundEventDTO[]): RoundEventDTO[] => {
    return events.filter(e => 
      e.eventType === 'KILL' || 
      e.eventType === 'ASSIST' || 
      e.eventType === 'BOMB_EVENT'
    );
  };

  if (loading) {
    return (
      <PageContainer backgroundClass="bg-round-details">
        <LoadingSpinner size="lg" message="Loading round details..." />
      </PageContainer>
    );
  }

  if (error || !roundDetails) {
    return (
      <PageContainer backgroundClass="bg-round-details">
        <div className="error-message">
          {error || 'Round not found'}
        </div>
        <Link to={`/games/${gameId}`} className="back-btn">
          ← Back to Game
        </Link>
      </PageContainer>
    );
  }

  const significantEvents = getSignificantEvents(roundDetails.events);

  return (
    <PageContainer backgroundClass="bg-round-details">
      <Link to={`/games/${gameId}`} className="back-btn">
        ← Back to Game
      </Link>

      {/* Round Header */}
      <div className={`round-header ${roundDetails.winnerTeam === 'CT' ? 'ct-win' : 't-win'}`}>
        <div className="round-header-content">
          <h1 className="round-title">Round {roundDetails.roundNumber}</h1>
          <div className="round-winner-badge">
            <span className="winner-icon">{roundDetails.winnerTeam === 'CT' ? '🛡️' : '💀'}</span>
            <span className="winner-text">{roundDetails.winnerTeam} Victory</span>
          </div>
        </div>
        
        <div className="round-stats-row">
          <div className="round-stat">
            <span className="stat-value">{roundDetails.totalKills}</span>
            <span className="stat-label">Kills</span>
          </div>
          <div className="round-stat">
            <span className="stat-value">{roundDetails.headshotKills}</span>
            <span className="stat-label">Headshots</span>
          </div>
          <div className="round-stat">
            <span className="stat-value">{roundDetails.totalAssists}</span>
            <span className="stat-label">Assists</span>
          </div>
          <div className="round-stat">
            <span className="stat-value">
              {Math.floor(roundDetails.durationMs / 1000)}s
            </span>
            <span className="stat-label">Duration</span>
          </div>
        </div>

        {/* Bomb Status */}
        {(roundDetails.bombPlanted || roundDetails.bombDefused || roundDetails.bombExploded) && (
          <div className="bomb-status-bar">
            {roundDetails.bombPlanted && (
              <span className="bomb-status planted">💣 Planted</span>
            )}
            {roundDetails.bombDefused && (
              <span className="bomb-status defused">🔧 Defused</span>
            )}
            {roundDetails.bombExploded && (
              <span className="bomb-status exploded">💥 Exploded</span>
            )}
          </div>
        )}
      </div>

      {/* Event Timeline */}
      <div className="section-card card-bg events-section">
        <h2 className="section-title">⏱️ Event Timeline</h2>
        <p className="events-description">
          All significant events that happened in this round, sorted chronologically
        </p>

        {significantEvents.length > 0 ? (
          <div className="events-timeline">
            {significantEvents.map((event, idx) => (
              <div 
                key={event.id || idx} 
                className={`event-card ${getEventColorClass(event.eventType, event)}`}
              >
                <div className="event-time">
                  <span className="time-badge">{formatTimeOffset(event.timeOffsetMs)}</span>
                </div>
                
                <div className="event-connector">
                  <div className="connector-line"></div>
                  <div className="connector-dot">
                    <span className="event-icon">{getEventIcon(event.eventType, event)}</span>
                  </div>
                </div>
                
                <div className="event-content">
                  <div className="event-header">
                    <span className="event-type-badge">
                      {getEventLabel(event.eventType, event)}
                    </span>
                    {event.weapon && (
                      <span className="weapon-badge">
                        🔫 {formatWeaponName(event.weapon)}
                      </span>
                    )}
                    {event.isHeadshot && (
                      <span className="headshot-badge">
                        🎯 Headshot
                      </span>
                    )}
                  </div>
                  
                  <div className="event-players">
                    {event.eventType === 'KILL' && (
                      <>
                        <Link 
                          to={`/players/${encodeURIComponent(event.player1Name || event.player1Id || '')}`}
                          className="player-link attacker"
                        >
                          {event.player1Name || event.player1Id || 'Unknown'}
                        </Link>
                        <span className="kill-arrow">→</span>
                        <Link 
                          to={`/players/${encodeURIComponent(event.player2Name || event.player2Id || '')}`}
                          className="player-link victim"
                        >
                          {event.player2Name || event.player2Id || 'Unknown'}
                        </Link>
                      </>
                    )}
                    
                    {event.eventType === 'ASSIST' && (
                      <>
                        <Link 
                          to={`/players/${encodeURIComponent(event.player1Name || event.player1Id || '')}`}
                          className="player-link assister"
                        >
                          {event.player1Name || event.player1Id || 'Unknown'}
                        </Link>
                        <span className="assist-text">assisted killing</span>
                        <Link 
                          to={`/players/${encodeURIComponent(event.player2Name || event.player2Id || '')}`}
                          className="player-link victim"
                        >
                          {event.player2Name || event.player2Id || 'Unknown'}
                        </Link>
                      </>
                    )}
                    
                    {event.eventType === 'BOMB_EVENT' && event.player1Name && (
                      <Link 
                        to={`/players/${encodeURIComponent(event.player1Name || event.player1Id || '')}`}
                        className="player-link bomber"
                      >
                        {event.player1Name || event.player1Id || 'Unknown'}
                      </Link>
                    )}
                  </div>
                  
                  {event.eventType === 'ATTACK' && event.damage && (
                    <div className="damage-info">
                      <span className="damage-value">-{event.damage} HP</span>
                      {event.hitGroup && (
                        <span className="hit-location">({event.hitGroup})</span>
                      )}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="no-events">
            <span className="no-events-icon">📭</span>
            <p>No significant events recorded for this round.</p>
          </div>
        )}
      </div>

      {/* Kill Feed Summary */}
      {roundDetails.totalKills > 0 && (
        <div className="section-card card-bg kill-feed-section">
          <h2 className="section-title">💀 Kill Feed</h2>
          <div className="kill-feed">
            {significantEvents
              .filter(e => e.eventType === 'KILL')
              .map((kill, idx) => (
                <div key={kill.id || idx} className={`kill-feed-item ${kill.isHeadshot ? 'headshot' : ''}`}>
                  <Link 
                    to={`/players/${encodeURIComponent(kill.player1Name || kill.player1Id || '')}`}
                    className="killer-name"
                  >
                    {kill.player1Name || kill.player1Id || 'Unknown'}
                  </Link>
                  <div className="kill-weapon-icon">
                    {kill.isHeadshot && <span className="hs-indicator">HS</span>}
                    <span className="weapon-name">{formatWeaponName(kill.weapon)}</span>
                  </div>
                  <Link 
                    to={`/players/${encodeURIComponent(kill.player2Name || kill.player2Id || '')}`}
                    className="victim-name"
                  >
                    {kill.player2Name || kill.player2Id || 'Unknown'}
                  </Link>
                </div>
              ))}
          </div>
        </div>
      )}
    </PageContainer>
  );
};
