/**
 * Unified Sprite Icon System
 * Handles all sprite-based icons including weapons, headshot, knife, and C4
 */

export interface SpriteIconInfo {
  name: string;
  displayName: string;
  category: 'rifle' | 'pistol' | 'smg' | 'shotgun' | 'sniper' | 'heavy' | 'grenade' | 'equipment' | 'knife' | 'special';
  iconFile: string;
  spriteSheet: 'weapons' | 'special'; // Which sprite sheet to use
}

export interface SpritePosition {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface SpriteConfig {
  offsetX: number;
  offsetY: number;
  scale: number;
  width?: number;
  height?: number;
}

// Re-export weapon map with sprite sheet info
import { WEAPON_MAP as ORIGINAL_WEAPON_MAP } from './weaponIcons';

// Augment weapon map with sprite sheet information
export const ICON_MAP: Record<string, SpriteIconInfo> = {
  ...Object.entries(ORIGINAL_WEAPON_MAP).reduce((acc, [key, info]) => ({
    ...acc,
    [key]: {
      ...info,
      spriteSheet: (info.iconFile === 'Knife' || info.name === 'c4') ? 'special' as const : 'weapons' as const
    }
  }), {}),
  // Add special event icons
  'headshot': {
    name: 'headshot',
    displayName: 'Headshot',
    category: 'special',
    iconFile: 'headshot',
    spriteSheet: 'special'
  },
};

// Sprite sheet dimensions
export const SPRITE_SHEETS = {
  weapons: {
    width: 1086,
    height: 440,
    url: '/weapons-sprite-amber.png'
  },
  special: {
    width: 154,
    height: 295,
    url: '/more-icons.png'
  }
};

// Import existing sprite positions
import { WEAPON_SPRITE_POSITIONS } from './weaponSpritePositions';
import { SPECIAL_ICON_POSITIONS } from './specialIconsSprite';

// Merge all sprite positions
export const SPRITE_POSITIONS: Record<string, SpritePosition> = {
  ...WEAPON_SPRITE_POSITIONS,
  'headshot': SPECIAL_ICON_POSITIONS['headshot'],
  'Knife': SPECIAL_ICON_POSITIONS['knife'],
  'C4': SPECIAL_ICON_POSITIONS['c4'],
};

// Import existing sprite configs
import { WEAPON_SPRITE_CONFIG } from './weaponSpriteConfig';
import { SPECIAL_ICON_CONFIG } from './specialIconsSprite';

// Merge all sprite configs
export const SPRITE_CONFIGS: Record<string, SpriteConfig> = {
  ...WEAPON_SPRITE_CONFIG,
  'headshot': SPECIAL_ICON_CONFIG['headshot'],
  'Knife': SPECIAL_ICON_CONFIG['knife'],
  'C4': SPECIAL_ICON_CONFIG['c4'],
};

/**
 * Get icon information
 */
export function getIconInfo(iconId: string | undefined): SpriteIconInfo | undefined {
  if (!iconId) return undefined;
  
  const normalizedId = iconId.toLowerCase();
  
  // Try direct lookup
  if (ICON_MAP[normalizedId]) {
    return ICON_MAP[normalizedId];
  }
  
  // Try with weapon_ prefix
  const withPrefix = normalizedId.startsWith('weapon_') ? normalizedId : `weapon_${normalizedId}`;
  if (ICON_MAP[withPrefix]) {
    return ICON_MAP[withPrefix];
  }
  
  // Try without prefix
  const withoutPrefix = normalizedId.replace('weapon_', '');
  const matchingKey = Object.keys(ICON_MAP).find(key => 
    key.replace('weapon_', '') === withoutPrefix
  );
  
  return matchingKey ? ICON_MAP[matchingKey] : undefined;
}

/**
 * Get sprite position for an icon
 */
export function getSpritePosition(iconFile: string): SpritePosition | undefined {
  return SPRITE_POSITIONS[iconFile];
}

/**
 * Get sprite configuration for an icon
 */
export function getSpriteConfig(iconFile: string): SpriteConfig {
  return SPRITE_CONFIGS[iconFile] || { offsetX: 0, offsetY: 0, scale: 1.0 };
}

/**
 * Format icon name for display
 */
export function formatIconName(iconId: string | undefined): string {
  if (!iconId) return 'Unknown';
  
  const iconInfo = getIconInfo(iconId);
  return iconInfo?.displayName || iconId.replace('weapon_', '').replace(/_/g, '-').toUpperCase();
}
