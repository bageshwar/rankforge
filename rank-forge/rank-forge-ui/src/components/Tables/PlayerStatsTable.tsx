import React, { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { extractSteamId } from '../../utils/steamId';
import './PlayerStatsTable.css';

export interface PlayerStat {
  playerName: string;
  playerId: string;
  kills: number;
  deaths: number;
  assists: number;
  damage?: number;
  headshotKills?: number;
  headshotPercentage?: number;
  killRank?: number; // For game details page - rank based on kills (not position in array)
}

type SortColumn = 'kills' | 'deaths' | 'assists' | 'damage' | 'hs';
type SortDirection = 'asc' | 'desc';

interface PlayerStatsTableProps {
  players: PlayerStat[];
  showRankings?: boolean; // Show top 3 badges (default: true)
  defaultSortColumn?: SortColumn; // Default column to sort by (default: 'kills')
}

export const PlayerStatsTable: React.FC<PlayerStatsTableProps> = ({ 
  players, 
  showRankings = true,
  defaultSortColumn = 'kills'
}) => {
  const [sortColumn, setSortColumn] = useState<SortColumn>(defaultSortColumn);
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  const handleSort = (column: SortColumn) => {
    if (sortColumn === column) {
      // Toggle direction if clicking same column
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      // New column, default to descending
      setSortColumn(column);
      setSortDirection('desc');
    }
  };

  const sortedPlayers = useMemo(() => {
    return [...players].sort((a, b) => {
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
  }, [players, sortColumn, sortDirection]);

  if (players.length === 0) {
    return <div className="no-data">No player statistics available.</div>;
  }

  const getRankIcon = (position: number) => {
    if (position === 0) return '🥇';
    if (position === 1) return '🥈';
    if (position === 2) return '🥉';
    return null;
  };

  return (
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
          {sortedPlayers.map((player) => {
            const steamId = extractSteamId(player.playerId);
            // Use killRank if available (for game details), otherwise use array position
            // Note: After sorting, we need to recalculate rank based on kills, not position
            const playersSortedByKills = [...players].sort((a, b) => b.kills - a.kills);
            const killRankIndex = playersSortedByKills.findIndex(p => p.playerId === player.playerId);
            const rankPosition = player.killRank !== undefined ? player.killRank - 1 : killRankIndex;
            const rankClass = showRankings && rankPosition === 0 ? 'rank-gold' : 
                            showRankings && rankPosition === 1 ? 'rank-silver' : 
                            showRankings && rankPosition === 2 ? 'rank-bronze' : '';
            
            return (
              <tr key={player.playerId} className={rankClass}>
                <td className="player-name-cell">
                  {showRankings && rankPosition < 3 && (
                    <span className={`rank-icon ${rankClass}`}>
                      {getRankIcon(rankPosition)}
                    </span>
                  )}
                  {steamId ? (
                    <Link 
                      to={`/players/${steamId}`} 
                      className="player-profile-link"
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
  );
};
