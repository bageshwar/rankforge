// CS2-inspired dark theme color palette
export const cs2Theme = {
  colors: {
    // Primary dark grays
    dark: {
      primary: '#0d0d0d',
      secondary: '#1a1a1a',
      tertiary: '#2d2d2d',
      quaternary: '#3a3a3a',
    },
    // Accent colors
    accent: {
      orange: {
        primary: '#ff6b35',
        secondary: '#ff8c42',
        light: '#ffa366',
        dark: '#cc5529',
      },
      blue: {
        primary: '#4a90e2',
        secondary: '#357abd',
        light: '#6ba3e8',
        dark: '#2c5a8a',
      },
    },
    // Text colors
    text: {
      primary: '#ffffff',
      secondary: '#e0e0e0',
      tertiary: '#b0b0b0',
      disabled: '#808080',
    },
    // Status colors
    status: {
      success: '#4caf50',
      warning: '#ff9800',
      error: '#f44336',
      info: '#2196f3',
    },
    // Team colors
    team: {
      ct: '#4caf50', // Counter-Terrorists (Green)
      t: '#ff9800',   // Terrorists (Orange)
    },
  },
  // Typography
  typography: {
    fontFamily: {
      primary: "'Inter', 'Roboto', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
      mono: "'JetBrains Mono', 'Courier New', monospace",
    },
    fontSize: {
      xs: '0.75rem',
      sm: '0.875rem',
      base: '1rem',
      lg: '1.125rem',
      xl: '1.25rem',
      '2xl': '1.5rem',
      '3xl': '1.875rem',
      '4xl': '2.25rem',
    },
    fontWeight: {
      normal: 400,
      medium: 500,
      semibold: 600,
      bold: 700,
    },
  },
  // Spacing
  spacing: {
    xs: '0.25rem',
    sm: '0.5rem',
    md: '1rem',
    lg: '1.5rem',
    xl: '2rem',
    '2xl': '3rem',
    '3xl': '4rem',
  },
  // Border radius
  borderRadius: {
    sm: '4px',
    md: '8px',
    lg: '12px',
    xl: '16px',
    full: '9999px',
  },
  // Shadows and glows
  effects: {
    glow: {
      orange: '0 0 20px rgba(255, 107, 53, 0.3)',
      blue: '0 0 20px rgba(74, 144, 226, 0.3)',
      green: '0 0 20px rgba(76, 175, 80, 0.3)',
    },
    shadow: {
      sm: '0 2px 4px rgba(0, 0, 0, 0.3)',
      md: '0 4px 8px rgba(0, 0, 0, 0.4)',
      lg: '0 8px 16px rgba(0, 0, 0, 0.5)',
      xl: '0 16px 32px rgba(0, 0, 0, 0.6)',
    },
  },
  // Transitions
  transitions: {
    fast: '150ms ease-in-out',
    normal: '250ms ease-in-out',
    slow: '350ms ease-in-out',
  },
};

export type CS2Theme = typeof cs2Theme;
