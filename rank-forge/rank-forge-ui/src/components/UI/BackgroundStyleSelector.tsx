import { useState, useEffect } from 'react';
import { 
  type BackgroundStyle, 
  getStoredBackgroundStyle, 
  setStoredBackgroundStyle 
} from '../../utils/mapImages';
import './BackgroundStyleSelector.css';

interface BackgroundStyleSelectorProps {
  onStyleChange?: (style: BackgroundStyle) => void;
}

const STYLE_OPTIONS: { value: BackgroundStyle; label: string; icon: string; description: string }[] = [
  { 
    value: 'combat', 
    label: 'Combat', 
    icon: '🎯',
    description: 'Sharp focus with dark overlay'
  },
  { 
    value: 'intel', 
    label: 'Intel', 
    icon: '🔍',
    description: 'Blurred recon view'
  },
  { 
    value: 'ghost', 
    label: 'Ghost', 
    icon: '👻',
    description: 'Stealth mode with heavy fade'
  },
];

export const BackgroundStyleSelector = ({ onStyleChange }: BackgroundStyleSelectorProps) => {
  const [selectedStyle, setSelectedStyle] = useState<BackgroundStyle>(getStoredBackgroundStyle());
  const [isExpanded, setIsExpanded] = useState(false);

  useEffect(() => {
    // Notify parent of initial style
    if (onStyleChange) {
      onStyleChange(selectedStyle);
    }
  }, []);

  const handleStyleChange = (style: BackgroundStyle) => {
    setSelectedStyle(style);
    setStoredBackgroundStyle(style);
    if (onStyleChange) {
      onStyleChange(style);
    }
    // Auto-collapse after selection
    setTimeout(() => setIsExpanded(false), 300);
  };

  const currentOption = STYLE_OPTIONS.find(opt => opt.value === selectedStyle) || STYLE_OPTIONS[0];

  return (
    <div className={`bg-style-selector ${isExpanded ? 'expanded' : ''}`}>
      <button
        className="bg-style-toggle"
        onClick={() => setIsExpanded(!isExpanded)}
        title="Change background style"
        aria-label="Toggle background style selector"
      >
        <span className="toggle-icon">{currentOption.icon}</span>
        <span className="toggle-label">{currentOption.label}</span>
        <span className="toggle-arrow">{isExpanded ? '▲' : '▼'}</span>
      </button>

      {isExpanded && (
        <div className="bg-style-options">
          {STYLE_OPTIONS.map((option) => (
            <button
              key={option.value}
              className={`bg-style-option ${selectedStyle === option.value ? 'active' : ''}`}
              onClick={() => handleStyleChange(option.value)}
              title={option.description}
            >
              <span className="option-icon">{option.icon}</span>
              <div className="option-content">
                <span className="option-label">{option.label}</span>
                <span className="option-description">{option.description}</span>
              </div>
              {selectedStyle === option.value && (
                <span className="option-checkmark">✓</span>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
