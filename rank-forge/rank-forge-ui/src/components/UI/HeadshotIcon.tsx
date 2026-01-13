import React from 'react';

interface HeadshotIconProps {
  size?: number;
  className?: string;
}

export const HeadshotIcon: React.FC<HeadshotIconProps> = ({ size = 20, className = '' }) => {
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
      {/* Head outline */}
      <circle cx="12" cy="8" r="5" fill="#ff4444" stroke="#cc0000" strokeWidth="1.5"/>
      
      {/* Crosshair horizontal */}
      <line x1="4" y1="8" x2="20" y2="8" stroke="#ffff00" strokeWidth="2" strokeLinecap="round"/>
      
      {/* Crosshair vertical */}
      <line x1="12" y1="0" x2="12" y2="16" stroke="#ffff00" strokeWidth="2" strokeLinecap="round"/>
      
      {/* Center dot */}
      <circle cx="12" cy="8" r="1.5" fill="#ffff00"/>
      
      {/* Neck/shoulders */}
      <path 
        d="M 8 12 L 7 16 L 9 16 L 9 20 M 16 12 L 17 16 L 15 16 L 15 20" 
        stroke="#ff4444" 
        strokeWidth="2" 
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
      
      {/* Body indication */}
      <line x1="9" y1="20" x2="15" y2="20" stroke="#ff4444" strokeWidth="2" strokeLinecap="round"/>
    </svg>
  );
};
