import { useState, useEffect } from 'react';
import { clansApi, type ClanDTO } from '../../services/api';
import { useAuth } from '../../contexts/AuthContext';
import './ClanSelector.css';

interface ClanSelectorProps {
  selectedClanId: number | null;
  onClanChange: (clanId: number | null) => void;
}

export const ClanSelector = ({ selectedClanId, onClanChange }: ClanSelectorProps) => {
  const { user } = useAuth();
  const [clans, setClans] = useState<ClanDTO[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user) {
      loadClans();
    } else {
      setClans([]);
      setLoading(false);
    }
  }, [user]);

  const loadClans = async () => {
    try {
      setLoading(true);
      const userClans = await clansApi.getMyClans();
      setClans(userClans);
    } catch (err) {
      console.error('Error loading clans:', err);
      setClans([]);
    } finally {
      setLoading(false);
    }
  };

  if (!user) {
    return null;
  }

  if (loading) {
    return (
      <div className="clan-selector-loading">
        Loading clans...
      </div>
    );
  }

  if (clans.length === 0) {
    return null; // Don't show selector if user has no clans
  }

  return (
    <div className="clan-selector">
      <label htmlFor="clan-select" className="clan-selector-label">
        Filter by Clan:
      </label>
      <select
        id="clan-select"
        className="clan-select"
        value={selectedClanId || ''}
        onChange={(e) => {
          const value = e.target.value;
          onClanChange(value === '' ? null : parseInt(value));
        }}
      >
        <option value="">Global (All Rankings)</option>
        {clans.map((clan) => (
          <option key={clan.id} value={clan.id}>
            {clan.name || `Clan #${clan.id}`} {clan.adminUserId === user.id ? '👑' : ''}
          </option>
        ))}
      </select>
    </div>
  );
};
