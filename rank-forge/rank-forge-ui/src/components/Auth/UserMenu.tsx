import { useState, useRef, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { PlayerAvatar } from '../UI/PlayerAvatar';
import './UserMenu.css';

export const UserMenu = () => {
  const { user, logout } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  if (!user) {
    return null;
  }

  return (
    <div className="user-menu" ref={menuRef}>
      <button
        className="user-menu-trigger"
        onClick={() => setIsOpen(!isOpen)}
        aria-label="User menu"
      >
        <PlayerAvatar steamId={user.steamId64} size="small" />
        <span className="user-name">{user.personaName}</span>
        <svg
          className={`dropdown-arrow ${isOpen ? 'open' : ''}`}
          width="12"
          height="12"
          viewBox="0 0 12 12"
          fill="none"
        >
          <path
            d="M2 4L6 8L10 4"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      {isOpen && (
        <div className="user-menu-dropdown">
          <Link
            to="/my-profile"
            className="user-menu-item"
            onClick={() => setIsOpen(false)}
          >
            <span className="menu-icon">👤</span>
            <span>My Profile</span>
          </Link>
          <Link
            to="/clan-management"
            className="user-menu-item"
            onClick={() => setIsOpen(false)}
          >
            <span className="menu-icon">👥</span>
            <span>Clan Management</span>
          </Link>
          <a
            href={user.profileUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="user-menu-item"
            onClick={() => setIsOpen(false)}
          >
            <span className="menu-icon">🔗</span>
            <span>Steam Profile</span>
          </a>
          <div className="user-menu-divider"></div>
          <button
            className="user-menu-item logout"
            onClick={() => {
              logout();
              setIsOpen(false);
            }}
          >
            <span className="menu-icon">🚪</span>
            <span>Logout</span>
          </button>
        </div>
      )}
    </div>
  );
};
