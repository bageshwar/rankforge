import { getWeaponInfo, formatWeaponName } from '../../utils/weaponIcons';
import { getSpritePosition, SPRITE_WIDTH, SPRITE_HEIGHT } from '../../utils/weaponSpritePositions';
import { getWeaponSpriteConfig } from '../../utils/weaponSpriteConfig';
import { Tooltip } from './Tooltip';
import './WeaponIcon.css';

export interface WeaponIconProps {
  weapon: string | undefined;
  size?: 'small' | 'medium' | 'large';
  showName?: boolean;
  className?: string;
  // Props for the sprite alignment test tool - when provided, override config
  offsetX?: number;
  offsetY?: number;
  scale?: number;
  width?: number;  // Override width in pixels
  height?: number; // Override height in pixels
}

/**
 * WeaponIcon component displays CS2 weapon icons with optional weapon names
 * 
 * @param weapon - Weapon identifier (e.g., "weapon_ak47")
 * @param size - Icon size variant (default: 'medium')
 * @param showName - Whether to display weapon name alongside icon (default: false)
 * @param className - Additional CSS classes
 * @param offsetX - Override X offset (for alignment tool)
 * @param offsetY - Override Y offset (for alignment tool)
 * @param scale - Override scale (for alignment tool)
 * @param width - Override width in pixels (for alignment tool)
 * @param height - Override height in pixels (for alignment tool)
 */
export const WeaponIcon = ({ 
  weapon, 
  size = 'medium', 
  showName = false,
  className = '',
  offsetX: propOffsetX,
  offsetY: propOffsetY,
  scale: propScale,
  width: propWidth,
  height: propHeight
}: WeaponIconProps) => {
  const weaponInfo = getWeaponInfo(weapon);
  
  if (!weaponInfo) {
    return showName ? (
      <span className={`weapon-display ${className}`}>
        <span className="weapon-icon-placeholder weapon-icon-unknown">?</span>
        <span className="weapon-name-display">{formatWeaponName(weapon)}</span>
      </span>
    ) : (
      <Tooltip content={formatWeaponName(weapon)} position="top" delay={200}>
        <span className={`weapon-icon-placeholder weapon-icon-unknown ${size} ${className}`}>
          ?
        </span>
      </Tooltip>
    );
  }

  const spritePosition = getSpritePosition(weaponInfo.iconFile);
  
  // Get sprite configuration - use props if provided (test mode), otherwise use config
  const spriteConfig = getWeaponSpriteConfig(weaponInfo.iconFile);
  const finalOffsetX = propOffsetX !== undefined ? propOffsetX : spriteConfig.offsetX;
  const finalOffsetY = propOffsetY !== undefined ? propOffsetY : spriteConfig.offsetY;
  const finalScale = propScale !== undefined ? propScale : spriteConfig.scale;
  const finalWidth = propWidth !== undefined ? propWidth : spriteConfig.width;
  const finalHeight = propHeight !== undefined ? propHeight : spriteConfig.height;
  
  // Calculate background position and size for the sprite
  const getSpriteStyle = () => {
    if (!spritePosition) return {};
    
    // Apply offsets and scale from config or props
    const adjustedX = spritePosition.x + finalOffsetX;
    const adjustedY = spritePosition.y + finalOffsetY;
    
    const style: React.CSSProperties = {
      backgroundImage: 'url(/weapons-sprite-amber.png)',
      backgroundPosition: `-${adjustedX * finalScale}px -${adjustedY * finalScale}px`,
      backgroundSize: `${SPRITE_WIDTH * finalScale}px ${SPRITE_HEIGHT * finalScale}px`,
      backgroundRepeat: 'no-repeat',
    };
    
    // Apply custom dimensions if provided
    if (finalWidth !== undefined) {
      style.width = `${finalWidth}px`;
    }
    if (finalHeight !== undefined) {
      style.height = `${finalHeight}px`;
    }
    
    return style;
  };

  const spriteStyle = getSpriteStyle();
  const hasSprite = !!spritePosition;

  if (showName) {
    return (
      <div className={`weapon-display ${size} ${className}`}>
        <Tooltip content={weaponInfo.displayName} position="top" delay={200}>
          <span 
            className={`weapon-icon-sprite ${weaponInfo.category} ${hasSprite ? 'has-sprite' : 'no-sprite'}`}
            style={spriteStyle}
            aria-label={weaponInfo.displayName}
          >
            {!hasSprite && '🔫'}
          </span>
        </Tooltip>
        <span className="weapon-name-display">
          {weaponInfo.displayName}
        </span>
      </div>
    );
  }

  return (
    <Tooltip content={weaponInfo.displayName} position="top" delay={200}>
      <span 
        className={`weapon-icon-sprite ${weaponInfo.category} ${size} ${hasSprite ? 'has-sprite' : 'no-sprite'} ${className}`}
        style={spriteStyle}
        aria-label={weaponInfo.displayName}
      >
        {!hasSprite && '🔫'}
      </span>
    </Tooltip>
  );
};
