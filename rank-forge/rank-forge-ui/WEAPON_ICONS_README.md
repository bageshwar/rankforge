# Weapon Icons System

## Overview

This document describes the weapon icon system implemented for displaying CS2 weapon icons in the Round Details page and other components.

## Architecture

The weapon icon system consists of three main components:

### 1. Weapon Mapping Utility (`src/utils/weaponIcons.ts`)

Provides comprehensive mappings for all CS2 weapons:

```typescript
import { getWeaponInfo, formatWeaponName } from '../utils/weaponIcons';

// Get weapon information
const weaponInfo = getWeaponInfo('weapon_ak47');
// Returns: { name: 'ak47', displayName: 'AK-47', category: 'rifle', iconFile: 'Ak47' }

// Format weapon name for display
const displayName = formatWeaponName('weapon_ak47');
// Returns: "AK-47"
```

**Supported Weapon Categories:**
- Pistols (Glock, USP-S, Desert Eagle, etc.)
- Rifles (AK-47, M4A4, M4A1-S, AWP, etc.)
- SMGs (MP9, MP7, P90, etc.)
- Shotguns (Nova, XM1014, etc.)
- Snipers (AWP, SSG 08, SCAR-20, G3SG1)
- Heavy (M249, Negev)
- Grenades (HE, Flashbang, Smoke, Molotov, Incendiary)
- Equipment (Zeus, C4, Knife)

### 2. WeaponIcon Component (`src/components/UI/WeaponIcon.tsx`)

A React component for rendering weapon icons with category-based styling.

**Props:**
```typescript
interface WeaponIconProps {
  weapon: string | undefined;      // Weapon identifier (e.g., "weapon_ak47")
  size?: 'small' | 'medium' | 'large';  // Icon size (default: 'medium')
  showName?: boolean;               // Show weapon name alongside icon (default: false)
  className?: string;               // Additional CSS classes
}
```

**Usage Examples:**

```tsx
// Simple icon only
<WeaponIcon weapon="weapon_ak47" />

// Small icon with name
<WeaponIcon weapon="weapon_ak47" size="small" showName />

// Large icon with custom class
<WeaponIcon weapon="weapon_awp" size="large" className="my-custom-class" />
```

**Features:**
- Automatic weapon category detection
- Color-coded borders based on weapon category
- Emoji-based icons (can be replaced with actual sprites)
- Hover effects and tooltips
- Accessible with ARIA labels
- Responsive design

### 3. Styling (`src/components/UI/WeaponIcon.css`)

Category-based color scheme:
- **Rifles:** Red (#ff6b6b)
- **Pistols:** Teal (#4ecdc4)
- **SMGs:** Light teal (#95e1d3)
- **Shotguns:** Pink-red (#f38181)
- **Snipers:** Purple (#aa96da)
- **Heavy:** Pink (#fcbad3)
- **Grenades:** Yellow (#ffd93d)
- **Equipment:** Green (#6bcf7f)
- **Knife:** Gray (#c8c8c8)

## Integration

### Round Details Page

The weapon icon system is integrated into two main sections:

#### 1. Event Timeline
Shows weapons used in each event with the weapon name:

```tsx
{event.weapon && (
  <WeaponIcon weapon={event.weapon} size="small" showName />
)}
```

#### 2. Kill Feed
Displays weapon icons in the kill feed alongside headshot indicators:

```tsx
<div className="kill-weapon-display">
  {kill.isHeadshot && <span className="hs-indicator">HS</span>}
  <WeaponIcon weapon={kill.weapon} size="small" />
</div>
```

## Upgrading to Real Weapon Sprites

The current implementation uses emoji-based icons as placeholders. To upgrade to actual weapon sprites:

### Option 1: Individual Icon Files

1. Extract individual weapon icons from the source image
2. Save them as PNG files in `src/assets/images/weapons/`
3. Update `WeaponIcon.tsx` to use `<img>` tags:

```tsx
<img 
  src={`/src/assets/images/weapons/${weaponInfo.iconFile}.png`}
  alt={weaponInfo.displayName}
  className={`weapon-icon ${weaponInfo.category}`}
/>
```

### Option 2: CSS Sprite Sheet

1. Create a sprite sheet with all weapons arranged in a grid
2. Save as `src/assets/images/weapon-sprite.png`
3. Update `WeaponIcon.css` to use background-position:

```css
.weapon-icon-sprite {
  background-image: url('/src/assets/images/weapon-sprite.png');
  background-size: 800px 600px; /* Adjust based on sprite dimensions */
}

.weapon-icon-sprite.ak47 {
  background-position: 0 0;
}

.weapon-icon-sprite.m4a4 {
  background-position: -32px 0;
}
/* ... more positions ... */
```

4. Update the component to use sprite positioning

### Option 3: SVG Icons

1. Convert weapon icons to SVG format
2. Import SVGs directly in the component
3. Use as React components for best performance and scalability

## Weapon Name Normalization

The system handles various weapon name formats:

- `weapon_ak47` → AK-47
- `ak47` → AK-47
- `weapon_m4a1_silencer` → M4A1-S
- `awp` → AWP

All comparisons are case-insensitive and handle both prefixed and unprefixed formats.

## Future Enhancements

1. **Weapon Skins:** Add support for displaying weapon skin variants
2. **Animated Icons:** Add subtle animations for special weapons
3. **StatTrak:** Display StatTrak counter if available
4. **Weapon Stats:** Show weapon damage, fire rate on hover
5. **Rarity Indicators:** Add rarity borders for special weapon drops

## Testing

To test the weapon icon system:

```bash
# Navigate to the UI directory
cd rank-forge-ui

# Run the development server
npm run dev

# Navigate to a round details page
# Example: http://localhost:3000/rounds/{gameId}/{roundNumber}
```

Verify:
- ✅ Weapon icons appear in event timeline
- ✅ Weapon icons appear in kill feed
- ✅ Hover states work correctly
- ✅ Weapon names display properly
- ✅ Category colors are correct
- ✅ Icons scale properly on mobile

## Troubleshooting

### Icons not displaying
- Check browser console for import errors
- Verify `WeaponIcon` component is imported correctly
- Ensure weapon identifiers match the WEAPON_MAP keys

### Wrong weapon names
- Verify weapon ID format in API responses
- Check normalization logic in `getWeaponInfo()`
- Add missing weapons to WEAPON_MAP

### Styling issues
- Ensure `WeaponIcon.css` is imported
- Check CSS variable definitions in theme
- Verify z-index and positioning

## Contributing

To add new weapons:

1. Add entry to `WEAPON_MAP` in `weaponIcons.ts`
2. Include all required fields: name, displayName, category, iconFile
3. Update this documentation
4. Test with actual game data

## API Reference

See inline TypeScript documentation in source files for complete API reference.
