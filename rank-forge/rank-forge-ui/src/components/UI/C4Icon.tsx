import React from 'react';

interface C4IconProps {
  size?: number;
  className?: string;
  status?: 'planted' | 'defused' | 'exploded';
}

export const C4Icon: React.FC<C4IconProps> = ({ size = 20, className = '', status = 'planted' }) => {
  const getColor = () => {
    switch (status) {
      case 'planted':
        return { primary: '#ff4444', secondary: '#cc0000', accent: '#ffff00' };
      case 'defused':
        return { primary: '#4CAF50', secondary: '#2e7d32', accent: '#81C784' };
      case 'exploded':
        return { primary: '#ff9800', secondary: '#e65100', accent: '#ffeb3b' };
      default:
        return { primary: '#ff4444', secondary: '#cc0000', accent: '#ffff00' };
    }
  };

  const colors = getColor();

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      style={{ display: 'inline-block', verticalAlign: 'middle' }}
    >
      {/* C4 explosive block */}
      <rect 
        x="6" 
        y="8" 
        width="12" 
        height="10" 
        rx="1" 
        fill={colors.primary} 
        stroke={colors.secondary} 
        strokeWidth="1.5"
      />
      
      {/* Wires/cables */}
      <path 
        d="M 10 8 L 10 5 M 14 8 L 14 5" 
        stroke={colors.accent} 
        strokeWidth="1.5" 
        strokeLinecap="round"
      />
      
      {/* Timer display */}
      <rect 
        x="8" 
        y="10" 
        width="8" 
        height="4" 
        rx="0.5" 
        fill="#000" 
        opacity="0.6"
      />
      
      {/* Timer digits */}
      <text 
        x="12" 
        y="13.5" 
        fontSize="3" 
        fill={colors.accent} 
        textAnchor="middle" 
        fontFamily="monospace"
        fontWeight="bold"
      >
        {status === 'exploded' ? '💥' : status === 'defused' ? '✓' : 'C4'}
      </text>
      
      {/* Keypad buttons */}
      <circle cx="9" cy="16" r="0.8" fill={colors.secondary}/>
      <circle cx="12" cy="16" r="0.8" fill={colors.secondary}/>
      <circle cx="15" cy="16" r="0.8" fill={colors.secondary}/>
      
      {/* Antenna */}
      <line 
        x1="17" 
        y1="8" 
        x2="17" 
        y2="4" 
        stroke={colors.secondary} 
        strokeWidth="1"
      />
      <circle cx="17" cy="3" r="1" fill={colors.accent}/>
      
      {/* Explosion effect for exploded status */}
      {status === 'exploded' && (
        <>
          <circle cx="12" cy="13" r="10" fill={colors.accent} opacity="0.3"/>
          <circle cx="12" cy="13" r="7" fill={colors.primary} opacity="0.4"/>
        </>
      )}
      
      {/* Checkmark for defused */}
      {status === 'defused' && (
        <path 
          d="M 8 12 L 11 15 L 16 9" 
          stroke={colors.accent} 
          strokeWidth="2" 
          strokeLinecap="round"
          strokeLinejoin="round"
          fill="none"
        />
      )}
    </svg>
  );
};
