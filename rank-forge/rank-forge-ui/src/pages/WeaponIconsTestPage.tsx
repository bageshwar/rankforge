import { PageContainer } from '../components/Layout/PageContainer';
import { SpriteIcon } from '../components/UI/SpriteIcon';
import { WEAPON_MAP } from '../utils/weaponIcons';
import './WeaponIconsTestPage.css';

export const WeaponIconsTestPage = () => {
  // Get all weapons from WEAPON_MAP
  const allWeapons = Object.entries(WEAPON_MAP).map(([weaponId, weaponInfo]) => ({
    weaponId,
    ...weaponInfo
  }));

  // Dummy names for testing
  const dummyAttacker = 'TestAttacker';
  const dummyVictim = 'TestVictim';

  return (
    <PageContainer>
      <div className="weapon-icons-test-page">
        <div className="test-page-header">
          <h1>🎯 Weapon Icons Visual Test</h1>
          <p className="test-description">
            This page displays all supported weapons in the attack event format.
            Each weapon is shown with both headshot and no-headshot variants.
            Use this to visually inspect icon positioning and alignment.
          </p>
        </div>

        <div className="weapons-test-container">
          <div className="test-section">
            <h2>All Weapons ({allWeapons.length})</h2>
            <div className="weapons-test-list">
              {allWeapons.map((weapon) => (
                <div key={weapon.weaponId} className="weapon-test-row-group">
                  <div className="weapon-test-header">
                    <span className="weapon-name">{weapon.displayName}</span>
                    <span className="weapon-category">{weapon.category}</span>
                  </div>
                  
                  <div className="weapon-test-rows">
                    {/* Row with headshot */}
                    <div className="kill-event-line test-row">
                      <span className="player-link attacker">{dummyAttacker}</span>
                      <SpriteIcon icon={weapon.weaponId} size="small" />
                      <SpriteIcon icon="headshot" size={36} className="headshot-icon" />
                      <span className="player-link victim">{dummyVictim}</span>
                      <span className="test-label">(Headshot)</span>
                    </div>

                    {/* Row without headshot */}
                    <div className="kill-event-line test-row">
                      <span className="player-link attacker">{dummyAttacker}</span>
                      <SpriteIcon icon={weapon.weaponId} size="small" />
                      <span className="player-link victim">{dummyVictim}</span>
                      <span className="test-label">(No Headshot)</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
};
