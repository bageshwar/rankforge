import React from 'react';
import './HitLocationIndicator.css';

interface HitLocationIndicatorProps {
  hitGroup: string | undefined;
  size?: number; // Size of the silhouette
  className?: string;
}

/**
 * HitLocationIndicator component displays a human silhouette with a marker
 * indicating where the hit occurred on the body
 */
export const HitLocationIndicator: React.FC<HitLocationIndicatorProps> = ({ 
  hitGroup, 
  size = 40,
  className = '' 
}) => {
  if (!hitGroup) {
    return null;
  }

  // Normalize hit group
  const normalized = hitGroup.toLowerCase();

  // Map hit groups to approximate positions on the silhouette (as percentages)
  // Silhouette is 136x242 pixels
  const getHitPosition = (): { x: number; y: number; label: string } => {
    if (normalized.includes('head')) {
      return { x: 50, y: 10, label: 'HEAD' }; // Top center
    }
    if (normalized.includes('neck')) {
      return { x: 50, y: 18, label: 'NECK' }; // Just below head
    }
    if (normalized.includes('chest') || normalized.includes('torso')) {
      return { x: 50, y: 35, label: 'CHEST' }; // Upper center
    }
    if (normalized.includes('stomach') || normalized.includes('belly')) {
      return { x: 50, y: 50, label: 'STOMACH' }; // Mid center
    }
    if (normalized.includes('left arm')) {
      return { x: 25, y: 40, label: 'L.ARM' }; // Left side, mid height
    }
    if (normalized.includes('right arm')) {
      return { x: 75, y: 40, label: 'R.ARM' }; // Right side, mid height
    }
    if (normalized.includes('left leg')) {
      return { x: 40, y: 75, label: 'L.LEG' }; // Left lower
    }
    if (normalized.includes('right leg')) {
      return { x: 60, y: 75, label: 'R.LEG' }; // Right lower
    }
    
    // Default: center torso
    return { x: 50, y: 40, label: 'BODY' };
  };

  const hitPosition = getHitPosition();
  const aspectRatio = 242 / 136; // height / width
  const width = size;
  const height = size * aspectRatio;

  return (
    <div 
      className={`hit-location-indicator ${className}`}
      style={{ 
        width: `${width}px`, 
        height: `${height}px`,
        position: 'relative',
        display: 'inline-block'
      }}
    >
      {/* Human silhouette */}
      <img 
        src="/victim-sillhoute.png" 
        alt="Hit location"
        className="silhouette-image"
        style={{ 
          width: '100%', 
          height: '100%',
          objectFit: 'contain'
        }}
      />
      
      {/* Hit marker */}
      <div 
        className="hit-marker"
        style={{
          position: 'absolute',
          left: `${hitPosition.x}%`,
          top: `${hitPosition.y}%`,
          transform: 'translate(-50%, -50%)'
        }}
        title={hitPosition.label}
      >
        <div className="hit-marker-pulse"></div>
        <div className="hit-marker-dot"></div>
      </div>
    </div>
  );
};
