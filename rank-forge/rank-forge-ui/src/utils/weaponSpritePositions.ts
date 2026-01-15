/**
 * Weapon sprite positions for the CS2 weapons sprite sheet
 * The sprite sheet is 1086x440 pixels with weapons arranged in a grid
 * Each weapon icon is approximately 99x110 pixels
 */

export interface SpritePosition {
  x: number; // X position in pixels
  y: number; // Y position in pixels
  width: number; // Width of the icon
  height: number; // Height of the icon
}

// Sprite dimensions
export const SPRITE_WIDTH = 1086;
export const SPRITE_HEIGHT = 440;
export const ICON_WIDTH = 99;
export const ICON_HEIGHT = 110;

/**
 * Weapon sprite positions based on the grid layout
 * Row 1: AK47, AUG, AWP, CZ75, Decoy, DefKit, DesertEagle, DualBerettas, FAMAS, Flashbang, FN
 * Row 2: G3SG1, GalilAR, Glock18, HEGrenade, Inc, KV&H, KV, M4A4, M4A4-S, M249, MAC-10
 * Row 3: Mag7, Molotov, MP5-SD, MP7, MP9, Negev, Nova, NVG, P90, P250, PP-Bizon
 * Row 4: R8, sawedoff, Scar20, SG553, Smoke, SSG-08, Taser, UMP-45, USP-S, XM1014, Zeusx27
 */
export const WEAPON_SPRITE_POSITIONS: Record<string, SpritePosition> = {
  // Row 1 (y=0)
  'Ak47': { x: 0, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  'AUG': { x: 99, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Awp': { x: 198, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  'CZ75': { x: 297, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Decoy': { x: 396, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  'DefKit': { x: 495, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  'DesertEagle': { x: 594, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  'DualBerettas': { x: 693, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  'FAMAS': { x: 792, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Flashbang': { x: 891, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  'FN': { x: 990, y: 0, width: ICON_WIDTH, height: ICON_HEIGHT },
  
  // Row 2 (y=110)
  'G3SG1': { x: 0, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT },
  'GalilAR': { x: 99, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Glock18': { x: 198, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT },
  'HEGrenade': { x: 297, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Inc': { x: 396, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT },
  'KV&H': { x: 495, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT }, // Kevlar + Helmet
  'KV': { x: 594, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT }, // Kevlar
  'M4A4': { x: 693, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT },
  'M4A4-S': { x: 792, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT },
  'M249': { x: 891, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT },
  'MAC-10': { x: 990, y: 110, width: ICON_WIDTH, height: ICON_HEIGHT },
  
  // Row 3 (y=220)
  'Mag7': { x: 0, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Molotov': { x: 99, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT },
  'MP5-SD': { x: 198, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT },
  'MP7': { x: 297, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT },
  'MP9': { x: 396, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Negev': { x: 495, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Nova': { x: 594, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT },
  'NVG': { x: 693, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT }, // Night Vision
  'P90': { x: 792, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT },
  'P250': { x: 891, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT },
  'PP-Bizon': { x: 990, y: 220, width: ICON_WIDTH, height: ICON_HEIGHT },
  
  // Row 4 (y=330)
  'R8': { x: 0, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  'sawedoff': { x: 99, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Scar20': { x: 198, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  'SG553': { x: 297, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Smoke': { x: 396, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  'SSG-08': { x: 495, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Tec9': { x: 594, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  'UMP-45': { x: 693, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  'USP-S': { x: 792, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  'XM1014': { x: 891, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  'Zeusx27': { x: 990, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT },
  
  // Aliases for common variations
  'P2000': { x: 792, y: 330, width: ICON_WIDTH, height: ICON_HEIGHT }, // Use USP-S as fallback
};

/**
 * Get sprite position for a weapon icon file name
 */
export function getSpritePosition(iconFile: string): SpritePosition | undefined {
  return WEAPON_SPRITE_POSITIONS[iconFile];
}
