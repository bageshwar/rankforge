/**
 * Backwards compatibility exports
 * All icon rendering now uses the unified SpriteIcon system
 */
export { 
  SpriteIcon as WeaponIcon,
  SpriteIcon,
  HeadshotIcon,
  KnifeIcon,
  C4Icon,
  type SpriteIconProps as WeaponIconProps
} from './SpriteIcon';
