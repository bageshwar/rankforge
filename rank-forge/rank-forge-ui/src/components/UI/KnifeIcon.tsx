import React from 'react';

interface KnifeIconProps {
  size?: number;
  className?: string;
}

export const KnifeIcon: React.FC<KnifeIconProps> = ({ size = 20, className = '' }) => {
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
      {/* Knife handle */}
      <path 
        d="M 2 18 L 2 22 L 6 22 L 6 18 Z" 
        fill="#8B4513" 
        stroke="#654321" 
        strokeWidth="0.5"
      />
      
      {/* Handle grip lines */}
      <line x1="2.5" y1="19" x2="5.5" y2="19" stroke="#654321" strokeWidth="0.3"/>
      <line x1="2.5" y1="20" x2="5.5" y2="20" stroke="#654321" strokeWidth="0.3"/>
      <line x1="2.5" y1="21" x2="5.5" y2="21" stroke="#654321" strokeWidth="0.3"/>
      
      {/* Guard */}
      <rect 
        x="5.5" 
        y="17" 
        width="1.5" 
        height="6" 
        fill="#7a7a7a" 
        stroke="#5a5a5a" 
        strokeWidth="0.5"
        rx="0.3"
      />
      
      {/* Blade */}
      <path 
        d="M 7 20 L 22 5 L 21 4 L 6 19 Z" 
        fill="#c0c0c0" 
        stroke="#a0a0a0" 
        strokeWidth="0.8"
      />
      
      {/* Blade edge (sharper side) */}
      <path 
        d="M 7 20 L 22 5 L 21.5 4.5 L 6.5 19.5 Z" 
        fill="#e8e8e8" 
        stroke="none"
      />
      
      {/* Blade shine/reflection */}
      <path 
        d="M 10 17 L 20 7 L 19.5 6.5 L 9.5 16.5 Z" 
        fill="rgba(255, 255, 255, 0.4)" 
        stroke="none"
      />
      
      {/* Sharp tip highlight */}
      <circle cx="21.5" cy="4.5" r="0.8" fill="#fff" opacity="0.6"/>
      
      {/* Blood droplet (optional, for CS2 theme) */}
      <circle cx="14" cy="12" r="0.6" fill="#ff0000" opacity="0.7"/>
      <circle cx="15.5" cy="10.5" r="0.4" fill="#ff0000" opacity="0.5"/>
    </svg>
  );
};
