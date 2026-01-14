import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import { PageContainer } from '../components/Layout/PageContainer';
import { LoadingSpinner } from '../components/Layout/LoadingSpinner';
import { SpriteIcon } from '../components/UI/SpriteIcon';
import { HitLocationIndicator } from '../components/UI/HitLocationIndicator';
import { PlayerStatsTable, type PlayerStat } from '../components/Tables/PlayerStatsTable';
import { Tooltip } from '../components/UI/Tooltip';
import { gamesApi } from '../services/api';
import { extractSteamId } from '../utils/steamId';
import type { RoundDetailsDTO, RoundEventDTO, GameDTO } from '../services/api';
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

// Hit location rendering now handled by HitLocationIndicator component

const formatTimeOffset = (ms: number): string => {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
};

export const RoundDetailsPage = () => {
  const { gameId, roundNumber } = useParams<{ gameId: string; roundNumber: string }>();
  const [roundDetails, setRoundDetails] = useState<RoundDetailsDTO | null>(null);
  const [game, setGame] = useState<GameDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadRoundDetails = useCallback(async () => {
    if (!gameId || !roundNumber) return;

    try {
      setLoading(true);
      setError(null);
      
      // Fetch both round details and game data (for map name)
      const [roundData, gameData] = await Promise.all([
        gamesApi.getRoundDetails(gameId, parseInt(roundNumber, 10)),
        gamesApi.getById(gameId),
      ]);

      if (!roundData) {
        setError('Round not found');
        return;
      }

      setRoundDetails(roundData);
      setGame(gameData);
    } catch (err) {
      setError('Failed to load round details. Please try again later.');
      console.error('Error loading round details:', err);
    } finally {
      setLoading(false);
    }
  }, [gameId, roundNumber]);

  useEffect(() => {
    if (gameId && roundNumber) {
      loadRoundDetails();
    }
  }, [gameId, roundNumber, loadRoundDetails]);

  // Calculate player statistics for this round (memoized for performance)
  // Must be called before any early returns to follow Rules of Hooks
  const roundPlayerStats = useMemo(() => {
    if (!roundDetails) return [];
    
    const playerStatsMap = new Map<string, PlayerStat>();

    roundDetails.events.forEach(event => {
      // Process kills
      if (event.eventType === 'KILL') {
        // Attacker gets a kill
        if (event.player1Id) {
          const attacker = playerStatsMap.get(event.player1Id) || {
            playerName: event.player1Name || event.player1Id,
            playerId: event.player1Id,
            kills: 0,
            deaths: 0,
            assists: 0,
            damage: 0,
            headshotKills: 0,
          };
          attacker.kills++;
          if (event.isHeadshot) {
            attacker.headshotKills = (attacker.headshotKills || 0) + 1;
          }
          playerStatsMap.set(event.player1Id, attacker);
        }

        // Victim gets a death
        if (event.player2Id) {
          const victim = playerStatsMap.get(event.player2Id) || {
            playerName: event.player2Name || event.player2Id,
            playerId: event.player2Id,
            kills: 0,
            deaths: 0,
            assists: 0,
            damage: 0,
            headshotKills: 0,
          };
          victim.deaths++;
          playerStatsMap.set(event.player2Id, victim);
        }
      }

      // Process assists
      if (event.eventType === 'ASSIST' && event.player1Id) {
        const assister = playerStatsMap.get(event.player1Id) || {
          playerName: event.player1Name || event.player1Id,
          playerId: event.player1Id,
          kills: 0,
          deaths: 0,
          assists: 0,
          damage: 0,
          headshotKills: 0,
        };
        assister.assists++;
        playerStatsMap.set(event.player1Id, assister);
      }

      // Process damage
      if (event.eventType === 'ATTACK' && event.player1Id && event.damage) {
        const attacker = playerStatsMap.get(event.player1Id) || {
          playerName: event.player1Name || event.player1Id,
          playerId: event.player1Id,
          kills: 0,
          deaths: 0,
          assists: 0,
          damage: 0,
          headshotKills: 0,
        };
        attacker.damage = (attacker.damage || 0) + event.damage;
        playerStatsMap.set(event.player1Id, attacker);
      }
    });

    return Array.from(playerStatsMap.values())
      .map(player => ({
        ...player,
        headshotPercentage: player.kills > 0 ? ((player.headshotKills || 0) / player.kills) * 100 : 0,
      }))
      .sort((a, b) => {
        // Sort by kills descending, then damage descending
        if (b.kills !== a.kills) return b.kills - a.kills;
        return (b.damage || 0) - (a.damage || 0);
      });
  }, [roundDetails]);

  // Build player ID to team mapping from round events
  // Note: GameDTO doesn't have player stats, so team mapping is not available
  // Team colors will not be applied to player links
  const getPlayerTeamMap = (): Map<string, 'CT' | 'T'> => {
    return new Map<string, 'CT' | 'T'>();
  };

  const playerTeamMap = getPlayerTeamMap();

  // Filter events to show only significant ones (kills, assists, bombs, attacks)
  const getSignificantEvents = (events: RoundEventDTO[]): RoundEventDTO[] => {
    return events.filter(e => 
      e.eventType === 'KILL' || 
      e.eventType === 'ASSIST' || 
      e.eventType === 'BOMB_EVENT' ||
      e.eventType === 'ATTACK'
    );
  };

  // Group assists and attacks with their corresponding kills
  const groupEventsWithAssists = (events: RoundEventDTO[]) => {
    const result: Array<{ event: RoundEventDTO; assist?: RoundEventDTO; attack?: RoundEventDTO }> = [];
    const assistMap = new Map<string, RoundEventDTO>();
    const attackMap = new Map<string, RoundEventDTO>();
    const killTimestamps = new Set<string>();
    
    // First pass: collect all assists, attacks, and kill timestamps
    events.forEach(event => {
      if (event.eventType === 'ASSIST') {
        // Key by victim ID and approximate time to match with kills
        const key = `${event.player2Id}_${Math.floor(event.timeOffsetMs / 100)}`;
        assistMap.set(key, event);
      } else if (event.eventType === 'ATTACK') {
        // Store attack events to merge with kills
        const key = `${event.player2Id}_${Math.floor(event.timeOffsetMs / 100)}`;
        attackMap.set(key, event);
      } else if (event.eventType === 'KILL') {
        killTimestamps.add(`${event.player2Id}_${Math.floor(event.timeOffsetMs / 100)}`);
      }
    });
    
    // Second pass: group kills with assists and attacks, filter out merged attacks
    events.forEach(event => {
      if (event.eventType === 'KILL') {
        const key = `${event.player2Id}_${Math.floor(event.timeOffsetMs / 100)}`;
        const assist = assistMap.get(key);
        const attack = attackMap.get(key);
        result.push({ event, assist, attack });
        if (assist) {
          assistMap.delete(key); // Remove matched assist
        }
        if (attack) {
          attackMap.delete(key); // Remove matched attack
        }
      } else if (event.eventType === 'ATTACK') {
        // Check if this attack is followed by an immediate kill (within 100ms)
        const attackKey = `${event.player2Id}_${Math.floor(event.timeOffsetMs / 100)}`;
        const hasImmediateKill = killTimestamps.has(attackKey);
        
        // Only add attack if it doesn't result in immediate death
        if (!hasImmediateKill) {
          result.push({ event });
        }
      } else if (event.eventType !== 'ASSIST') {
        // Add other events (BOMB_EVENT, etc.)
        result.push({ event });
      }
    });
    
    return result;
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

  const significantEvents = roundDetails ? getSignificantEvents(roundDetails.events) : [];
  const groupedEvents = roundDetails ? groupEventsWithAssists(significantEvents) : [];

  return (
    <PageContainer mapName={game?.map}>
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

      {/* Round Scorecard */}
      {roundPlayerStats.length > 0 && (
        <div className="section-card card-bg player-stats-section">
          <h2 className="section-title">📊 Round Scorecard</h2>
          <p className="events-description">
            Player statistics for this round
          </p>
          <PlayerStatsTable 
            players={roundPlayerStats} 
            showRankings={true}
            defaultSortColumn="kills"
          />
        </div>
      )}

      {/* Event Timeline */}
      <div className="section-card card-bg events-section">
        <h2 className="section-title">⏱️ Event Timeline</h2>
        <p className="events-description">
          All significant events that happened in this round, sorted chronologically
        </p>

        {groupedEvents.length > 0 ? (
          <div className="events-timeline">
            {groupedEvents.map(({ event, assist, attack }, idx) => (
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
                    <Tooltip content={getEventLabel(event.eventType, event)} position="right" delay={200}>
                      <span className="event-icon">
                        {getEventIcon(event.eventType, event)}
                      </span>
                    </Tooltip>
                  </div>
                </div>
                
                <div className="event-content">
                  {/* KILL Events - Format: Attacker <weapon> <headshot?> Victim OR Assister + Attacker <weapon> <headshot?> Victim */}
                  {event.eventType === 'KILL' && (
                    <div className="kill-event-line">
                      {assist && (
                        <>
                          <Link 
                            to={`/players/${extractSteamId(assist.player1Id)}`}
                            className={`player-link assister ${playerTeamMap.get(assist.player1Id || '') === 'CT' ? 'team-ct' : playerTeamMap.get(assist.player1Id || '') === 'T' ? 'team-t' : ''}`}
                            data-testid={`testid-round-event-player-link-${event.id || idx}-assister`}
                          >
                            {assist.player1Name || assist.player1Id || 'Unknown'}
                          </Link>
                          <span className="assist-plus">+</span>
                        </>
                      )}
                      <Link 
                        to={`/players/${extractSteamId(event.player1Id)}`}
                        className={`player-link attacker ${playerTeamMap.get(event.player1Id || '') === 'CT' ? 'team-ct' : playerTeamMap.get(event.player1Id || '') === 'T' ? 'team-t' : ''}`}
                        data-testid={`testid-round-event-player-link-${event.id || idx}-attacker`}
                      >
                        {event.player1Name || event.player1Id || 'Unknown'}
                      </Link>
                      {event.weapon && <SpriteIcon icon={event.weapon} size="small" />}
                      {event.isHeadshot && <SpriteIcon icon="headshot" size={36} className="headshot-icon" />}
                      <Link 
                        to={`/players/${extractSteamId(event.player2Id)}`}
                        className={`player-link victim ${playerTeamMap.get(event.player2Id || '') === 'CT' ? 'team-ct' : playerTeamMap.get(event.player2Id || '') === 'T' ? 'team-t' : ''}`}
                        data-testid={`testid-round-event-player-link-${event.id || idx}-victim`}
                      >
                        {event.player2Name || event.player2Id || 'Unknown'}
                      </Link>
                      {/* Show hit location from the merged attack event if available */}
                      {attack && attack.damage && (
                        <span className="damage-value">-{attack.damage} HP</span>
                      )}
                      <HitLocationIndicator hitGroup={attack?.hitGroup || event.hitGroup} size={32} />
                    </div>
                  )}
                  
                  {/* BOMB Events */}
                  {event.eventType === 'BOMB_EVENT' && (
                    <div className="bomb-event-line">
                      <SpriteIcon 
                        icon="weapon_c4"
                        size={40} 
                        status={
                          event.bombEventType?.toLowerCase() === 'defused' ? 'defused' :
                          event.bombEventType?.toLowerCase() === 'exploded' ? 'exploded' :
                          'planted'
                        } 
                        className="c4-icon"
                      />
                      <span className="bomb-event-text">{getEventLabel(event.eventType, event)}</span>
                      {event.player1Id && (
                        <>
                          <span className="by-text">by</span>
                          <Link 
                            to={`/players/${extractSteamId(event.player1Id)}`}
                            className={`player-link bomber ${playerTeamMap.get(event.player1Id) === 'CT' ? 'team-ct' : playerTeamMap.get(event.player1Id) === 'T' ? 'team-t' : ''}`}
                          >
                            {event.player1Name || event.player1Id || 'Unknown'}
                          </Link>
                        </>
                      )}
                    </div>
                  )}
                  
                  {/* ATTACK Events */}
                  {event.eventType === 'ATTACK' && (
                    <div className="attack-event-line">
                      <Link 
                        to={`/players/${extractSteamId(event.player1Id)}`}
                        className={`player-link attacker ${playerTeamMap.get(event.player1Id || '') === 'CT' ? 'team-ct' : playerTeamMap.get(event.player1Id || '') === 'T' ? 'team-t' : ''}`}
                      >
                        {event.player1Name || event.player1Id || 'Unknown'}
                      </Link>
                      {event.weapon && <SpriteIcon icon={event.weapon} size="small" />}
                      <Link 
                        to={`/players/${extractSteamId(event.player2Id)}`}
                        className={`player-link victim ${playerTeamMap.get(event.player2Id || '') === 'CT' ? 'team-ct' : playerTeamMap.get(event.player2Id || '') === 'T' ? 'team-t' : ''}`}
                      >
                        {event.player2Name || event.player2Id || 'Unknown'}
                      </Link>
                      {event.damage && (
                        <span className="damage-value">-{event.damage} HP</span>
                      )}
                      <HitLocationIndicator hitGroup={event.hitGroup} size={32} />
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
    </PageContainer>
  );
};
