/**
 * Per-weapon sprite alignment configuration
 * Generated from the Weapon Sprite Test tool
 */

export interface WeaponSpriteConfig {
  offsetX: number;
  offsetY: number;
  scale: number;
  width?: number;  // Icon width in pixels (optional, defaults to component size)
  height?: number; // Icon height in pixels (optional, defaults to component size)
}

export const WEAPON_SPRITE_CONFIG: Record<string, WeaponSpriteConfig> = {
  "Glock18": { offsetX: 20, offsetY: 43, scale: 1, width: 68, height: 52 },
  "P2000": { offsetX: 9, offsetY: 37, scale: 0.9, width: 71, height: 40 },
  "USP-S": { offsetX: 11, offsetY: 35, scale: 0.9, width: 74, height: 43 },
  "DualBerettas": { offsetX: 7, offsetY: 41, scale: 0.9, width: 73, height: 40 },
  "P250": { offsetX: 16, offsetY: 36, scale: 1, width: 65, height: 52 },
  "FN": { offsetX: 13, offsetY: 34, scale: 1, width: 73, height: 51 },
  "Tec9": { offsetX: 19, offsetY: 19, scale: 1, width: 65, height: 61 },
  "CZ75": { offsetX: 12, offsetY: 26, scale: 0.85, width: 75, height: 49 },
  "DesertEagle": { offsetX: 17, offsetY: 33, scale: 1, width: 75, height: 51 },
  "R8": { offsetX: 18, offsetY: 31, scale: 0.9, width: 76, height: 46 },
  "Ak47": { offsetX: 16, offsetY: 44, scale: 0.95, width: 84, height: 38 },
  "M4A4": { offsetX: 7, offsetY: 50, scale: 0.8, width: 70, height: 35 },
  "M4A4-S": { offsetX: 8, offsetY: 58, scale: 0.9, width: 72, height: 30 },
  "AUG": { offsetX: 16, offsetY: 42, scale: 0.85, width: 70, height: 35 },
  "SG553": { offsetX: 15, offsetY: 43, scale: 0.9, width: 70, height: 35 },
  "GalilAR": { offsetX: 16, offsetY: 56, scale: 0.85, width: 70, height: 30 },
  "FAMAS": { offsetX: 10, offsetY: 43, scale: 0.85, width: 70, height: 35 },
  "MP9": { offsetX: 10, offsetY: 43, scale: 0.8, width: 70, height: 35 },
  "MAC-10": { offsetX: 14, offsetY: 40, scale: 1, width: 70, height: 50 },
  "MP7": { offsetX: 17, offsetY: 40, scale: 1, width: 70, height: 45 },
  "MP5-SD": { offsetX: 16, offsetY: 41, scale: 0.9, width: 70, height: 40 },
  "UMP-45": { offsetX: 10, offsetY: 40, scale: 0.85, width: 70, height: 35 },
  "P90": { offsetX: 3, offsetY: 46, scale: 0.8, width: 70, height: 35 },
  "PP-Bizon": { offsetX: 4, offsetY: 50, scale: 0.8, width: 70, height: 30 },
  "Nova": { offsetX: 14, offsetY: 55, scale: 0.9, width: 70, height: 30 },
  "XM1014": { offsetX: 7, offsetY: 47, scale: 0.85, width: 70, height: 30 },
  "Mag7": { offsetX: 17, offsetY: 43, scale: 0.8, width: 70, height: 35 },
  "sawedoff": { offsetX: 14, offsetY: 46, scale: 0.85, width: 70, height: 30 },
  "Awp": { offsetX: 12, offsetY: 54, scale: 0.85, width: 70, height: 25 },
  "SSG-08": { offsetX: 12, offsetY: 50, scale: 0.85, width: 70, height: 25 },
  "Scar20": { offsetX: 14, offsetY: 50, scale: 0.85, width: 70, height: 25 },
  "G3SG1": { offsetX: 15, offsetY: 60, scale: 0.85, width: 70, height: 30 },
  "M249": { offsetX: 4, offsetY: 56, scale: 0.8, width: 70, height: 30 },
  "Negev": { offsetX: 10, offsetY: 43, scale: 0.85, width: 70, height: 35 },
  "HEGrenade": { offsetX: 20, offsetY: 48, scale: 1, width: 70, height: 45 },
  "Flashbang": { offsetX: 18, offsetY: 36, scale: 1, width: 70, height: 50 },
  "Smoke": { offsetX: 19, offsetY: 35, scale: 1, width: 70, height: 45 },
  "Molotov": { offsetX: 22, offsetY: 42, scale: 1.15, width: 70, height: 50 },
  "Inc": { offsetX: 19, offsetY: 45, scale: 1, width: 70, height: 50 },
  "Decoy": { offsetX: 14, offsetY: 37, scale: 1, width: 70, height: 45 },
  "Zeusx27": { offsetX: 13, offsetY: 33, scale: 1, width: 70, height: 50 },
  "DefKit": { offsetX: 14, offsetY: 18, scale: 0.9, width: 70, height: 60 }
};

/**
 * Get sprite configuration for a weapon
 */
export function getWeaponSpriteConfig(iconFile: string): WeaponSpriteConfig {
  return WEAPON_SPRITE_CONFIG[iconFile] || { offsetX: 14, offsetY: 5, scale: 1.0 };
}
