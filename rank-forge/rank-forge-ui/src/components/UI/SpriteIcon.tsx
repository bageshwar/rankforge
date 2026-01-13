import { 
  getIconInfo, 
  getSpritePosition, 
  getSpriteConfig,
  formatIconName,
  SPRITE_SHEETS 
} from '../../utils/spriteIcons';
import { Tooltip } from './Tooltip';
import './WeaponIcon.css';

export interface SpriteIconProps {
  icon: string | undefined;
  size?: 'small' | 'medium' | 'large' | number; // Can be preset or pixel value
  showName?: boolean;
  className?: string;
  status?: 'planted' | 'defused' | 'exploded'; // For C4 icon
  // Props for the sprite alignment test tool - when provided, override config
  offsetX?: number;
  offsetY?: number;
  scale?: number;
  width?: number;
  height?: number;
}

/**
 * Unified SpriteIcon component for all sprite-based icons
 * Handles weapons, headshot, knife, C4, and other special icons
 */
export const SpriteIcon = ({ 
  icon, 
  size = 'medium', 
  showName = false,
  className = '',
  status = 'planted',
  offsetX: propOffsetX,
  offsetY: propOffsetY,
  scale: propScale,
  width: propWidth,
  height: propHeight
}: SpriteIconProps) => {
  const iconInfo = getIconInfo(icon);
  
  if (!iconInfo) {
    return showName ? (
      <span className={`weapon-display ${className}`}>
        <span className="weapon-icon-placeholder weapon-icon-unknown">?</span>
        <span className="weapon-name-display">{formatIconName(icon)}</span>
      </span>
    ) : (
      <Tooltip content={formatIconName(icon)} position="top" delay={200}>
        <span className={`weapon-icon-placeholder weapon-icon-unknown ${typeof size === 'string' ? size : ''} ${className}`}>
          ?
        </span>
      </Tooltip>
    );
  }

  const spritePosition = getSpritePosition(iconInfo.iconFile);
  const spriteConfig = getSpriteConfig(iconInfo.iconFile);
  
  // Get sprite sheet info
  const spriteSheet = SPRITE_SHEETS[iconInfo.spriteSheet];
  
  // Get config values (use props if provided, otherwise use config)
  const finalOffsetX = propOffsetX !== undefined ? propOffsetX : spriteConfig.offsetX;
  const finalOffsetY = propOffsetY !== undefined ? propOffsetY : spriteConfig.offsetY;
  const configScale = propScale !== undefined ? propScale : spriteConfig.scale;
  const configWidth = propWidth !== undefined ? propWidth : (spriteConfig.width || 70);
  const configHeight = propHeight !== undefined ? propHeight : (spriteConfig.height || 70);
  
  // Determine display size based on icon type
  let displayWidth: number;
  let displayHeight: number;
  let finalScale: number;
  
  if (iconInfo.spriteSheet === 'weapons') {
    // Weapons: Use fixed configured dimensions (they're already optimized per weapon)
    displayWidth = configWidth;
    displayHeight = configHeight;
    finalScale = configScale;
  } else {
    // Special icons: Scale dynamically based on size prop
    const sizeMap = { small: 16, medium: 20, large: 24 };
    const pixelSize = typeof size === 'number' ? size : sizeMap[size];
    displayWidth = pixelSize;
    displayHeight = pixelSize;
    // Calculate scale factor to fit icon within the display size
    const scaleFactor = pixelSize / Math.max(configWidth, configHeight);
    finalScale = configScale * scaleFactor;
  }
  
  // Calculate background position and size for the sprite
  const getSpriteStyle = () => {
    if (!spritePosition) return {};
    
    const adjustedX = spritePosition.x + finalOffsetX;
    const adjustedY = spritePosition.y + finalOffsetY;
    
    const style: React.CSSProperties = {
      backgroundImage: `url(${spriteSheet.url})`,
      backgroundPosition: `-${adjustedX * finalScale}px -${adjustedY * finalScale}px`,
      backgroundSize: `${spriteSheet.width * finalScale}px ${spriteSheet.height * finalScale}px`,
      backgroundRepeat: 'no-repeat',
      width: `${displayWidth}px`,
      height: `${displayHeight}px`,
    };
    
    return style;
  };

  const spriteStyle = getSpriteStyle();
  const hasSprite = !!spritePosition;
  
  // Get status class for C4 icon
  const getStatusClass = () => {
    if (iconInfo.name === 'c4') {
      switch (status) {
        case 'defused': return 'c4-defused';
        case 'exploded': return 'c4-exploded';
        default: return 'c4-planted';
      }
    }
    return '';
  };

  const iconClass = `weapon-icon-sprite ${iconInfo.category} ${getStatusClass()} ${hasSprite ? 'has-sprite' : 'no-sprite'}`;

  if (showName) {
    return (
      <div className={`weapon-display ${typeof size === 'string' ? size : ''} ${className}`}>
        <Tooltip content={iconInfo.displayName} position="top" delay={200}>
          <span 
            className={iconClass}
            style={spriteStyle}
            aria-label={iconInfo.displayName}
          >
            {!hasSprite && '🔫'}
          </span>
        </Tooltip>
        <span className="weapon-name-display">
          {iconInfo.displayName}
        </span>
      </div>
    );
  }

  return (
    <Tooltip content={iconInfo.displayName} position="top" delay={200}>
      <span 
        className={`${iconClass} ${typeof size === 'string' ? size : ''} ${className}`}
        style={spriteStyle}
        aria-label={iconInfo.displayName}
      >
        {!hasSprite && '🔫'}
      </span>
    </Tooltip>
  );
};

// Export backwards-compatible aliases
export const WeaponIcon = SpriteIcon;
export const HeadshotIcon = (props: Omit<SpriteIconProps, 'icon'>) => 
  <SpriteIcon {...props} icon="headshot" />;
export const KnifeIcon = (props: Omit<SpriteIconProps, 'icon'>) => 
  <SpriteIcon {...props} icon="weapon_knife" />;
export const C4Icon = (props: Omit<SpriteIconProps, 'icon'>) => 
  <SpriteIcon {...props} icon="weapon_c4" />;
