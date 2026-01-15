import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { NavigationBar } from './components/Layout/NavigationBar';
import { Footer } from './components/Layout/Footer';
import { HomePage } from './pages/HomePage';
import { RankingsPage } from './pages/RankingsPage';
import { GamesPage } from './pages/GamesPage';
import { GameDetailsPage } from './pages/GameDetailsPage';
import { RoundDetailsPage } from './pages/RoundDetailsPage';
import { PlayerProfilePage } from './pages/PlayerProfilePage';
import { WeaponSpriteTest } from './pages/WeaponSpriteTest';
import { SpecialIconsTest } from './pages/SpecialIconsTest';
import { WeaponIconsTestPage } from './pages/WeaponIconsTestPage';
import './App.css';

function App() {
  return (
    <Router>
      <div className="app">
        <NavigationBar />
        <main className="app-main">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/rankings" element={<RankingsPage />} />
            <Route path="/players/:playerId" element={<PlayerProfilePage />} />
            <Route path="/games" element={<GamesPage />} />
            <Route path="/games/:gameId" element={<GameDetailsPage />} />
            <Route path="/games/:gameId/rounds/:roundNumber" element={<RoundDetailsPage />} />
            <Route path="/weapon-sprite-test" element={<WeaponSpriteTest />} />
            <Route path="/special-icons-test" element={<SpecialIconsTest />} />
            <Route path="/weapon-icons-test" element={<WeaponIconsTestPage />} />
          </Routes>
        </main>
        <Footer />
      </div>
    </Router>
  );
}

export default App;
