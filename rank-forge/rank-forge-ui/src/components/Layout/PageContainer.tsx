import type { ReactNode } from 'react';
import './PageContainer.css';

interface PageContainerProps {
  children: ReactNode;
  backgroundClass?: 'bg-home' | 'bg-rankings' | 'bg-games' | 'bg-game-details' | 'bg-player-profile' | 'bg-round-details';
  className?: string;
}

export const PageContainer = ({ 
  children, 
  backgroundClass = 'bg-home',
  className = '' 
}: PageContainerProps) => {
  return (
    <div className={`page-container ${backgroundClass} ${className}`}>
      <div className="page-content">
        {children}
      </div>
    </div>
  );
};
