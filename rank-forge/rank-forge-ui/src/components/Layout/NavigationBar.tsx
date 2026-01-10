import { Link, useLocation } from 'react-router-dom';
import './NavigationBar.css';

export const NavigationBar = () => {
  const location = useLocation();

  const isActive = (path: string) => {
    return location.pathname === path;
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <Link to="/" className="navbar-brand">
          <span className="brand-icon">🎯</span>
          <span className="brand-text">RankForge</span>
        </Link>
        
        <div className="navbar-links">
          <Link 
            to="/" 
            className={`nav-link ${isActive('/') ? 'active' : ''}`}
          >
            🏠 Home
          </Link>
          <Link 
            to="/rankings" 
            className={`nav-link ${isActive('/rankings') ? 'active' : ''}`}
          >
            🏆 Rankings
          </Link>
          <Link 
            to="/games" 
            className={`nav-link ${isActive('/games') ? 'active' : ''}`}
          >
            🎮 Games
          </Link>
        </div>
      </div>
    </nav>
  );
};
