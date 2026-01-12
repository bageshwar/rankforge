import type { ReactNode } from 'react';
import { useState, useEffect } from 'react';
import { getMapImageUrl, getFallbackImageUrl } from '../../utils/mapImages';
import './PageContainer.css';

interface PageContainerProps {
  children: ReactNode;
  backgroundClass?: 'bg-home' | 'bg-rankings' | 'bg-games' | 'bg-game-details' | 'bg-player-profile' | 'bg-round-details';
  className?: string;
  mapName?: string;
}

export const PageContainer = ({ 
  children, 
  backgroundClass = 'bg-home',
  className = '',
  mapName
}: PageContainerProps) => {
  const [backgroundImage, setBackgroundImage] = useState<string | null>(null);
  const [imageLoaded, setImageLoaded] = useState(false);

  useEffect(() => {
    if (!mapName) {
      setBackgroundImage(null);
      setImageLoaded(false);
      return;
    }

    // Reset loading state
    setImageLoaded(false);

    // Preload the map image
    const img = new Image();
    const url = getMapImageUrl(mapName);

    img.onload = () => {
      setBackgroundImage(url);
      setImageLoaded(true);
    };

    img.onerror = () => {
      // Try fallback image
      const fallbackImg = new Image();
      const fallbackUrl = getFallbackImageUrl();

      fallbackImg.onload = () => {
        setBackgroundImage(fallbackUrl);
        setImageLoaded(true);
      };

      fallbackImg.onerror = () => {
        // If even fallback fails, just use gradient
        setBackgroundImage(null);
        setImageLoaded(false);
      };

      fallbackImg.src = fallbackUrl;
    };

    img.src = url;
  }, [mapName]);

  // Determine which background class to use - always use combat style for maps
  const finalBackgroundClass = backgroundImage 
    ? `bg-map-combat ${imageLoaded ? 'image-loaded' : ''}`
    : backgroundClass;

  return (
    <div 
      className={`page-container ${finalBackgroundClass} ${className}`}
      style={backgroundImage ? {
        '--map-bg-image': `url(${backgroundImage})`
      } as React.CSSProperties : undefined}
    >
      <div className="page-content">
        {children}
      </div>
    </div>
  );
};
