/**
 * CS2 Weapon Icon Mappings and Utilities
 * 
 * This file provides mappings between weapon identifiers from the game logs
 * and their display properties including names and icon identifiers.
 */

export interface WeaponInfo {
  name: string;
  displayName: string;
  category: 'rifle' | 'pistol' | 'smg' | 'shotgun' | 'sniper' | 'heavy' | 'grenade' | 'equipment' | 'knife';
  iconFile: string; // Reference to icon file or sprite position
}

/**
 * Comprehensive CS2 weapon mapping
 * Maps weapon identifiers (as they appear in logs) to weapon information
 */
export const WEAPON_MAP: Record<string, WeaponInfo> = {
  // Pistols
  'weapon_glock': { name: 'glock', displayName: 'Glock-18', category: 'pistol', iconFile: 'Glock18' },
  'weapon_hkp2000': { name: 'hkp2000', displayName: 'P2000', category: 'pistol', iconFile: 'P2000' },
  'weapon_usp_silencer': { name: 'usp_silencer', displayName: 'USP-S', category: 'pistol', iconFile: 'USP-S' },
  'weapon_elite': { name: 'elite', displayName: 'Dual Berettas', category: 'pistol', iconFile: 'DualBerettas' },
  'weapon_p250': { name: 'p250', displayName: 'P250', category: 'pistol', iconFile: 'P250' },
  'weapon_fiveseven': { name: 'fiveseven', displayName: 'Five-SeveN', category: 'pistol', iconFile: 'FN' },
  'weapon_tec9': { name: 'tec9', displayName: 'Tec-9', category: 'pistol', iconFile: 'Tec9' },
  'weapon_cz75a': { name: 'cz75a', displayName: 'CZ75-Auto', category: 'pistol', iconFile: 'CZ75' },
  'weapon_deagle': { name: 'deagle', displayName: 'Desert Eagle', category: 'pistol', iconFile: 'DesertEagle' },
  'weapon_revolver': { name: 'revolver', displayName: 'R8 Revolver', category: 'pistol', iconFile: 'R8' },

  // Rifles
  'weapon_ak47': { name: 'ak47', displayName: 'AK-47', category: 'rifle', iconFile: 'Ak47' },
  'weapon_m4a1': { name: 'm4a1', displayName: 'M4A4', category: 'rifle', iconFile: 'M4A4' },
  'weapon_m4a1_silencer': { name: 'm4a1_silencer', displayName: 'M4A1-S', category: 'rifle', iconFile: 'M4A4-S' },
  'weapon_aug': { name: 'aug', displayName: 'AUG', category: 'rifle', iconFile: 'AUG' },
  'weapon_sg556': { name: 'sg556', displayName: 'SG 553', category: 'rifle', iconFile: 'SG553' },
  'weapon_galilar': { name: 'galilar', displayName: 'Galil AR', category: 'rifle', iconFile: 'GalilAR' },
  'weapon_famas': { name: 'famas', displayName: 'FAMAS', category: 'rifle', iconFile: 'FAMAS' },

  // SMGs
  'weapon_mp9': { name: 'mp9', displayName: 'MP9', category: 'smg', iconFile: 'MP9' },
  'weapon_mac10': { name: 'mac10', displayName: 'MAC-10', category: 'smg', iconFile: 'MAC-10' },
  'weapon_mp7': { name: 'mp7', displayName: 'MP7', category: 'smg', iconFile: 'MP7' },
  'weapon_mp5sd': { name: 'mp5sd', displayName: 'MP5-SD', category: 'smg', iconFile: 'MP5-SD' },
  'weapon_ump45': { name: 'ump45', displayName: 'UMP-45', category: 'smg', iconFile: 'UMP-45' },
  'weapon_p90': { name: 'p90', displayName: 'P90', category: 'smg', iconFile: 'P90' },
  'weapon_bizon': { name: 'bizon', displayName: 'PP-Bizon', category: 'smg', iconFile: 'PP-Bizon' },

  // Shotguns
  'weapon_nova': { name: 'nova', displayName: 'Nova', category: 'shotgun', iconFile: 'Nova' },
  'weapon_xm1014': { name: 'xm1014', displayName: 'XM1014', category: 'shotgun', iconFile: 'XM1014' },
  'weapon_mag7': { name: 'mag7', displayName: 'MAG-7', category: 'shotgun', iconFile: 'Mag7' },
  'weapon_sawedoff': { name: 'sawedoff', displayName: 'Sawed-Off', category: 'shotgun', iconFile: 'sawedoff' },

  // Snipers
  'weapon_awp': { name: 'awp', displayName: 'AWP', category: 'sniper', iconFile: 'Awp' },
  'weapon_ssg08': { name: 'ssg08', displayName: 'SSG 08', category: 'sniper', iconFile: 'SSG-08' },
  'weapon_scar20': { name: 'scar20', displayName: 'SCAR-20', category: 'sniper', iconFile: 'Scar20' },
  'weapon_g3sg1': { name: 'g3sg1', displayName: 'G3SG1', category: 'sniper', iconFile: 'G3SG1' },

  // Heavy
  'weapon_m249': { name: 'm249', displayName: 'M249', category: 'heavy', iconFile: 'M249' },
  'weapon_negev': { name: 'negev', displayName: 'Negev', category: 'heavy', iconFile: 'Negev' },

  // Grenades
  'weapon_hegrenade': { name: 'hegrenade', displayName: 'HE Grenade', category: 'grenade', iconFile: 'HEGrenade' },
  'weapon_flashbang': { name: 'flashbang', displayName: 'Flashbang', category: 'grenade', iconFile: 'Flashbang' },
  'weapon_smokegrenade': { name: 'smokegrenade', displayName: 'Smoke Grenade', category: 'grenade', iconFile: 'Smoke' },
  'weapon_molotov': { name: 'molotov', displayName: 'Molotov', category: 'grenade', iconFile: 'Molotov' },
  'weapon_incgrenade': { name: 'incgrenade', displayName: 'Incendiary Grenade', category: 'grenade', iconFile: 'Inc' },
  'weapon_inferno': { name: 'inferno', displayName: 'Incendiary Grenade', category: 'grenade', iconFile: 'Inc' },
  'weapon_decoy': { name: 'decoy', displayName: 'Decoy Grenade', category: 'grenade', iconFile: 'Decoy' },

  // Equipment
  'weapon_taser': { name: 'taser', displayName: 'Zeus x27', category: 'equipment', iconFile: 'Zeusx27' },
  'weapon_c4': { name: 'c4', displayName: 'C4 Explosive', category: 'equipment', iconFile: 'C4' },
  'weapon_knife': { name: 'knife', displayName: 'Knife', category: 'knife', iconFile: 'Knife' },
  'weapon_knife_t': { name: 'knife_t', displayName: 'Knife', category: 'knife', iconFile: 'Knife' },
};

/**
 * Get weapon information from weapon identifier
 * @param weaponId - Weapon identifier (e.g., "weapon_ak47" or "ak47")
 * @returns WeaponInfo object or undefined if not found
 */
export function getWeaponInfo(weaponId: string | undefined): WeaponInfo | undefined {
  if (!weaponId) return undefined;
  
  // Normalize the weapon ID
  const normalizedId = weaponId.toLowerCase();
  
  // Try with "weapon_" prefix first
  if (WEAPON_MAP[normalizedId]) {
    return WEAPON_MAP[normalizedId];
  }
  
  // Try adding "weapon_" prefix if not present
  const withPrefix = normalizedId.startsWith('weapon_') ? normalizedId : `weapon_${normalizedId}`;
  if (WEAPON_MAP[withPrefix]) {
    return WEAPON_MAP[withPrefix];
  }
  
  // Try without prefix
  const withoutPrefix = normalizedId.replace('weapon_', '');
  const matchingKey = Object.keys(WEAPON_MAP).find(key => 
    key.replace('weapon_', '') === withoutPrefix
  );
  
  return matchingKey ? WEAPON_MAP[matchingKey] : undefined;
}

/**
 * Format weapon name for display
 * @param weaponId - Weapon identifier
 * @returns Formatted display name
 */
export function formatWeaponName(weaponId: string | undefined): string {
  if (!weaponId) return 'Unknown';
  
  const weaponInfo = getWeaponInfo(weaponId);
  return weaponInfo?.displayName || weaponId.replace('weapon_', '').replace(/_/g, '-').toUpperCase();
}

/**
 * Get weapon category color for styling
 * @param category - Weapon category
 * @returns CSS color value
 */
export function getWeaponCategoryColor(category: WeaponInfo['category']): string {
  const colorMap: Record<WeaponInfo['category'], string> = {
    rifle: '#ff6b6b',
    pistol: '#4ecdc4',
    smg: '#95e1d3',
    shotgun: '#f38181',
    sniper: '#aa96da',
    heavy: '#fcbad3',
    grenade: '#ffd93d',
    equipment: '#6bcf7f',
    knife: '#c8c8c8',
  };
  
  return colorMap[category] || '#888';
}

/**
 * Get weapon icon file path
 * @param weaponId - Weapon identifier
 * @returns Path to weapon icon file
 */
export function getWeaponIconPath(weaponId: string | undefined): string {
  const weaponInfo = getWeaponInfo(weaponId);
  if (!weaponInfo) return '';
  
  return `/src/assets/images/weapons/${weaponInfo.iconFile}.tga`;
}
