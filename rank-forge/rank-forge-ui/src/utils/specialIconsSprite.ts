/**
 * Special icons sprite positions for the more-icons.png sprite sheet
 * This includes headshot, knife, and C4 icons
 * 
 * Sprite dimensions: 154x295 pixels (vertical layout, transparent background)
 * Layout: headshot (top), knife (middle), c4 (bottom)
 * 
 * Background removed using ImageMagick:
 * magick more-icons.png -fuzz 20% -transparent "srgba(72,111,78,1)" more-icons.png
 */

export interface SpecialIconSpritePosition {
  x: number; // X position in pixels
  y: number; // Y position in pixels
  width: number; // Width of the icon
  height: number; // Height of the icon
}

export interface SpecialIconSpriteConfig {
  offsetX: number;
  offsetY: number;
  scale: number;
  width?: number;
  height?: number;
}

// Sprite dimensions for more-icons.png (actual measured dimensions)
export const SPECIAL_ICONS_SPRITE_WIDTH = 154;
export const SPECIAL_ICONS_SPRITE_HEIGHT = 295;

/**
 * Special icon sprite positions (vertical arrangement)
 * Analyzed using ImageMagick trim:
 * - Headshot: y=0-98 (73x59 actual icon at offset +48+20)
 * - Knife: y=98-196 (114x32 actual icon at offset +27+135) 
 * - C4: y=196-295 (72x62 actual icon at offset +39+217)
 */
export const SPECIAL_ICON_POSITIONS: Record<string, SpecialIconSpritePosition> = {
  'headshot': { x: 0, y: 0, width: 154, height: 98 },
  'knife': { x: 0, y: 98, width: 154, height: 98 },
  'c4': { x: 0, y: 196, width: 154, height: 99 },
};

/**
 * Per-icon alignment configuration
 * Optimized values from SpecialIconsTest page at /special-icons-test
 */
export const SPECIAL_ICON_CONFIG: Record<string, SpecialIconSpriteConfig> = {
  'headshot': { offsetX: 32, offsetY: 2, scale: 0.53, width: 55, height: 45 },
  'knife': { offsetX: 21, offsetY: 18, scale: 0.48, width: 70, height: 35 },
  'c4': { offsetX: 35, offsetY: 15, scale: 0.83, width: 70, height: 60 },
};

/**
 * Get sprite position for a special icon
 */
export function getSpecialIconPosition(iconName: string): SpecialIconSpritePosition | undefined {
  return SPECIAL_ICON_POSITIONS[iconName];
}

/**
 * Get sprite configuration for a special icon
 */
export function getSpecialIconConfig(iconName: string): SpecialIconSpriteConfig {
  return SPECIAL_ICON_CONFIG[iconName] || { offsetX: 0, offsetY: 0, scale: 1.0 };
}
