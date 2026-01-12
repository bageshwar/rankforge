/**
 * Utility functions for CS2 map images from GitHub repository
 * Repository: https://github.com/ghostcap-gaming/cs2-map-images
 */

const GITHUB_RAW_BASE_URL = 'https://raw.githubusercontent.com/ghostcap-gaming/cs2-map-images/main/cs2';
const FALLBACK_IMAGE_URL = 'https://raw.githubusercontent.com/ghostcap-gaming/cs2-map-images/main/cs2/de_dust2.png';

/**
 * Normalizes a map name to match the GitHub repository naming convention
 * Examples:
 * - "de_dust2" → "de_dust2"
 * - "Dust2" → "de_dust2"
 * - "DE_MIRAGE" → "de_mirage"
 * - "ancient" → "de_ancient"
 */
export const normalizeMapName = (mapName: string): string => {
  if (!mapName) return '';
  
  // Convert to lowercase and trim
  let normalized = mapName.toLowerCase().trim();
  
  // Remove any extra spaces
  normalized = normalized.replace(/\s+/g, '_');
  
  // If it doesn't start with a prefix (de_, cs_, etc.), add de_
  if (!normalized.match(/^(de|cs|ar|dz|gd)_/)) {
    normalized = `de_${normalized}`;
  }
  
  return normalized;
};

/**
 * Generates the GitHub raw URL for a given map name
 */
export const getMapImageUrl = (mapName: string): string => {
  const normalized = normalizeMapName(mapName);
  return `${GITHUB_RAW_BASE_URL}/${normalized}.png`;
};

/**
 * Gets the fallback image URL (defaults to de_dust2)
 */
export const getFallbackImageUrl = (): string => {
  return FALLBACK_IMAGE_URL;
};

/**
 * Preloads a map image to improve UX
 * Returns a promise that resolves when the image is loaded
 */
export const preloadMapImage = (mapName: string): Promise<string> => {
  return new Promise((resolve, reject) => {
    const img = new Image();
    const url = getMapImageUrl(mapName);
    
    img.onload = () => resolve(url);
    img.onerror = () => {
      // Try fallback image
      const fallbackImg = new Image();
      const fallbackUrl = getFallbackImageUrl();
      
      fallbackImg.onload = () => resolve(fallbackUrl);
      fallbackImg.onerror = () => reject(new Error(`Failed to load map image for ${mapName}`));
      
      fallbackImg.src = fallbackUrl;
    };
    
    img.src = url;
  });
};

/**
 * Checks if a map image exists (without preloading)
 */
export const checkMapImageExists = async (mapName: string): Promise<boolean> => {
  try {
    const url = getMapImageUrl(mapName);
    const response = await fetch(url, { method: 'HEAD' });
    return response.ok;
  } catch {
    return false;
  }
};

/**
 * Map background style types
 */
export type BackgroundStyle = 'combat' | 'intel' | 'ghost';

/**
 * Gets the stored background style preference from localStorage
 */
export const getStoredBackgroundStyle = (): BackgroundStyle => {
  const stored = localStorage.getItem('rankforge-bg-style');
  if (stored === 'combat' || stored === 'intel' || stored === 'ghost') {
    return stored;
  }
  return 'combat'; // Default
};

/**
 * Stores the background style preference to localStorage
 */
export const setStoredBackgroundStyle = (style: BackgroundStyle): void => {
  localStorage.setItem('rankforge-bg-style', style);
};
