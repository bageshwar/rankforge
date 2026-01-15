import { Link } from 'react-router-dom';
import { PageContainer } from '../components/Layout/PageContainer';
import './HomePage.css';

export const HomePage = () => {
  return (
    <PageContainer backgroundClass="bg-home">
      <div className="home-hero">
        <h1 className="hero-title">
          <span className="hero-icon">🎯</span>
          RankForge
        </h1>
        <p className="hero-subtitle">Advanced CS2 Player Rankings & Game Analytics</p>
        <p className="hero-description">
          Track player performance, analyze game statistics, and explore detailed match history
        </p>
      </div>

      <div className="nav-cards">
        <Link to="/rankings" className="nav-card card-bg hover-glow" data-testid="testid-home-rankings-link">
          <div className="nav-card-icon">🏆</div>
          <h2 className="nav-card-title">Player Rankings</h2>
          <p className="nav-card-description">
            View comprehensive player rankings with detailed statistics including K/D ratios,
            headshot percentages, and performance metrics.
          </p>
        </Link>

        <Link to="/games" className="nav-card card-bg hover-glow" data-testid="testid-home-games-link">
          <div className="nav-card-icon">🎮</div>
          <h2 className="nav-card-title">Processed Games</h2>
          <p className="nav-card-description">
            Browse all processed games with match details, scores, maps played,
            and participating players.
          </p>
        </Link>
      </div>

      <div className="home-footer">
        <p className="footer-info">
          RankForge automatically processes CS2 server logs to provide real-time player statistics and game analytics.
        </p>
      </div>
    </PageContainer>
  );
};
