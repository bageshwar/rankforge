import React, { useState, useRef, useEffect } from 'react';
import './Tooltip.css';

export interface TooltipProps {
  content: string;
  children: React.ReactNode;
  delay?: number; // Delay in milliseconds before showing (default: 200ms)
  position?: 'top' | 'bottom' | 'left' | 'right';
}

export const Tooltip: React.FC<TooltipProps> = ({ 
  content, 
  children, 
  delay = 200,
  position = 'top' 
}) => {
  const [isVisible, setIsVisible] = useState(false);
  const timeoutRef = useRef<NodeJS.Timeout | null>(null);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const showTooltip = () => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    timeoutRef.current = setTimeout(() => {
      setIsVisible(true);
    }, delay);
  };

  const hideTooltip = () => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    setIsVisible(false);
  };

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  return (
    <div 
      ref={wrapperRef}
      className="tooltip-wrapper"
      onMouseEnter={showTooltip}
      onMouseLeave={hideTooltip}
    >
      {children}
      {isVisible && (
        <div 
          className={`tooltip-content tooltip-${position}`}
        >
          {content}
          <div className={`tooltip-arrow tooltip-arrow-${position}`}></div>
        </div>
      )}
    </div>
  );
};
