import { useState, useEffect } from 'react';
import { usersApi } from '../../services/api';
import './PlayerAvatar.css';

interface PlayerAvatarProps {
  steamId: string;
  size?: 'small' | 'medium' | 'large';
  showBadge?: boolean;
  className?: string;
}

const sizeMap = {
  small: '32px',
  medium: '48px',
  large: '96px',
};

export const PlayerAvatar: React.FC<PlayerAvatarProps> = ({
  steamId,
  size = 'medium',
  showBadge = false,
  className = '',
}) => {
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAvatar = async () => {
      if (!steamId) {
        setAvatarUrl('/default-avatar.png');
        setLoading(false);
        return;
      }

      try {
        const url = await usersApi.getAvatar(steamId);
        setAvatarUrl(url);
      } catch (error) {
        console.error('Failed to fetch avatar:', error);
        setAvatarUrl('/default-avatar.png');
      } finally {
        setLoading(false);
      }
    };

    fetchAvatar();
  }, [steamId]);

  const sizeValue = sizeMap[size];

  if (loading) {
    return (
      <div
        className={`player-avatar loading ${className}`}
        style={{ width: sizeValue, height: sizeValue }}
      >
        <div className="avatar-skeleton"></div>
      </div>
    );
  }

  return (
    <div
      className={`player-avatar ${size} ${showBadge ? 'with-badge' : ''} ${className}`}
      style={{ width: sizeValue, height: sizeValue }}
    >
      <img
        src={avatarUrl || '/default-avatar.png'}
        alt="Player avatar"
        className="avatar-image"
        onError={(e) => {
          (e.target as HTMLImageElement).src = '/default-avatar.png';
        }}
      />
      {showBadge && <div className="avatar-badge">✓</div>}
    </div>
  );
};
