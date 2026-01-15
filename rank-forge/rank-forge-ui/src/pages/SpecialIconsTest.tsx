import { useState, useEffect } from 'react';
import { 
  SPECIAL_ICON_POSITIONS, 
  SPECIAL_ICONS_SPRITE_WIDTH, 
  SPECIAL_ICONS_SPRITE_HEIGHT,
  type SpecialIconSpriteConfig 
} from '../utils/specialIconsSprite';
import './WeaponSpriteTest.css';

const SPECIAL_ICONS = {
  'headshot': { displayName: 'Headshot Icon', category: 'special' },
  'knife': { displayName: 'Knife Icon', category: 'special' },
  'c4': { displayName: 'C4 Icon', category: 'special' },
};

export const SpecialIconsTest = () => {
  const [selectedIcon, setSelectedIcon] = useState<string | null>(null);
  const [jsonInput, setJsonInput] = useState('');
  const [showImport, setShowImport] = useState(false);
  
  // Store per-icon offsets
  const [iconOffsets, setIconOffsets] = useState<Record<string, SpecialIconSpriteConfig>>({});

  // Drag and resize state
  const [isDragging, setIsDragging] = useState(false);
  const [isResizing, setIsResizing] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [resizeStart, setResizeStart] = useState({ width: 0, height: 0, mouseX: 0, mouseY: 0 });
  const [activeIcon, setActiveIcon] = useState<string | null>(null);

  // LocalStorage key
  const STORAGE_KEY = 'special-icons-sprite-config';

  // Initialize icon offsets from localStorage or defaults
  useEffect(() => {
    const savedConfig = localStorage.getItem(STORAGE_KEY);
    
    if (savedConfig) {
      try {
        const parsed = JSON.parse(savedConfig);
        setIconOffsets(parsed);
        console.log('✅ Loaded special icons config from localStorage');
        return;
      } catch (error) {
        console.error('Failed to parse saved config:', error);
      }
    }
    
    // Fallback to default config
    const initialOffsets: Record<string, SpecialIconSpriteConfig> = {};
    Object.keys(SPECIAL_ICONS).forEach(key => {
      initialOffsets[key] = { offsetX: 0, offsetY: 0, scale: 1.0, width: 70, height: 70 };
    });
    setIconOffsets(initialOffsets);
  }, []);

  // Save to localStorage whenever iconOffsets changes
  useEffect(() => {
    if (Object.keys(iconOffsets).length > 0) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(iconOffsets));
      console.log('💾 Saved special icons config to localStorage');
    }
  }, [iconOffsets]);

  // Import JSON configuration
  const importJson = () => {
    try {
      const parsed = JSON.parse(jsonInput);
      setIconOffsets(parsed);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(parsed));
      setJsonInput('');
      setShowImport(false);
      alert('✅ Configuration imported and saved to browser storage!');
    } catch (error) {
      alert('❌ Invalid JSON format. Please check your input.');
    }
  };

  // Clear localStorage and reset to defaults
  const resetToDefaults = () => {
    if (confirm('⚠️ This will reset ALL icons to default positions and clear browser storage. Continue?')) {
      localStorage.removeItem(STORAGE_KEY);
      const initialOffsets: Record<string, SpecialIconSpriteConfig> = {};
      Object.keys(SPECIAL_ICONS).forEach(key => {
        initialOffsets[key] = { offsetX: 0, offsetY: 0, scale: 1.0, width: 70, height: 70 };
      });
      setIconOffsets(initialOffsets);
      setSelectedIcon(null);
      alert('✅ Reset to defaults complete!');
    }
  };

  // Drag handlers
  const handleDragStart = (e: React.MouseEvent, iconKey: string) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
    setDragStart({ x: e.clientX, y: e.clientY });
    setActiveIcon(iconKey);
    setSelectedIcon(iconKey);
  };

  const handleDragMove = (e: React.MouseEvent) => {
    if (!isDragging || !activeIcon) return;

    const deltaX = e.clientX - dragStart.x;
    const deltaY = e.clientY - dragStart.y;
    
    const currentOffset = getIconOffset(activeIcon);
    updateIconOffset(activeIcon, {
      offsetX: currentOffset.offsetX + deltaX,
      offsetY: currentOffset.offsetY + deltaY
    });
    
    setDragStart({ x: e.clientX, y: e.clientY });
  };

  const handleDragEnd = () => {
    setIsDragging(false);
    setActiveIcon(null);
  };

  // Resize handlers
  const handleResizeStart = (e: React.MouseEvent, iconKey: string) => {
    e.preventDefault();
    e.stopPropagation();
    setIsResizing(true);
    
    const currentOffset = getIconOffset(iconKey);
    setResizeStart({
      width: currentOffset.width || 70,
      height: currentOffset.height || 70,
      mouseX: e.clientX,
      mouseY: e.clientY
    });
    setActiveIcon(iconKey);
    setSelectedIcon(iconKey);
  };

  const handleResizeMove = (e: React.MouseEvent) => {
    if (!isResizing || !activeIcon) return;

    const deltaX = e.clientX - resizeStart.mouseX;
    const deltaY = e.clientY - resizeStart.mouseY;
    
    const newWidth = Math.max(40, Math.min(200, resizeStart.width + deltaX));
    const newHeight = Math.max(40, Math.min(200, resizeStart.height + deltaY));
    
    updateIconOffset(activeIcon, {
      width: newWidth,
      height: newHeight
    });
  };

  const handleResizeEnd = () => {
    setIsResizing(false);
    setActiveIcon(null);
  };

  // Global mouse handlers
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (isDragging) {
        handleDragMove(e as any);
      } else if (isResizing) {
        handleResizeMove(e as any);
      }
    };

    const handleMouseUp = () => {
      if (isDragging) handleDragEnd();
      if (isResizing) handleResizeEnd();
    };

    if (isDragging || isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      return () => {
        document.removeEventListener('mousemove', handleMouseMove);
        document.removeEventListener('mouseup', handleMouseUp);
      };
    }
  }, [isDragging, isResizing, dragStart, resizeStart, activeIcon]);

  const getIconOffset = (iconKey: string): SpecialIconSpriteConfig => {
    return iconOffsets[iconKey] || { offsetX: 0, offsetY: 0, scale: 1.0, width: 70, height: 70 };
  };

  const updateIconOffset = (iconKey: string, updates: Partial<SpecialIconSpriteConfig>) => {
    setIconOffsets(prev => ({
      ...prev,
      [iconKey]: {
        ...getIconOffset(iconKey),
        ...updates
      }
    }));
  };

  const getSpriteStyle = (iconKey: string) => {
    const pos = SPECIAL_ICON_POSITIONS[iconKey];
    if (!pos) return {};

    const offset = getIconOffset(iconKey);
    const adjustedX = pos.x + offset.offsetX;
    const adjustedY = pos.y + offset.offsetY;
    const scaledWidth = SPECIAL_ICONS_SPRITE_WIDTH * offset.scale;
    const scaledHeight = SPECIAL_ICONS_SPRITE_HEIGHT * offset.scale;

    return {
      backgroundImage: 'url(/more-icons.png)',
      backgroundPosition: `-${adjustedX * offset.scale}px -${adjustedY * offset.scale}px`,
      backgroundSize: `${scaledWidth}px ${scaledHeight}px`,
      backgroundRepeat: 'no-repeat',
      width: `${offset.width || 70}px`,
      height: `${offset.height || 70}px`,
    };
  };

  const selectedIconInfo = selectedIcon ? SPECIAL_ICONS[selectedIcon as keyof typeof SPECIAL_ICONS] : null;
  const currentOffset = selectedIcon 
    ? getIconOffset(selectedIcon)
    : { offsetX: 0, offsetY: 0, scale: 1.0, width: 70, height: 70 };

  return (
    <div className="weapon-sprite-test">
      <div className="test-header">
        <h1>🎯 Special Icons Sprite Alignment Tool</h1>
        <p>Click an icon to select • Drag icon to reposition • Drag corner to resize</p>
        <div className="storage-status">
          💾 Auto-saving to browser storage
        </div>
        {(isDragging || isResizing) && (
          <div className="active-action-badge">
            {isDragging ? '🖐️ Dragging...' : '↔️ Resizing...'}
          </div>
        )}
      </div>

      <div className="test-layout">
        {/* Controls Panel - Left Side */}
        <div className="controls-panel">
          {selectedIconInfo ? (
            <>
              <div className="selected-weapon-header">
                <h2>Adjusting: {selectedIconInfo.displayName}</h2>
                <button 
                  className="deselect-btn"
                  onClick={() => setSelectedIcon(null)}
                >
                  ✕ Deselect
                </button>
              </div>

              <div className="control-group">
                <label>
                  <strong>X Offset:</strong> {currentOffset.offsetX}px
                  <input
                    type="range"
                    min="-50"
                    max="50"
                    value={currentOffset.offsetX}
                    onChange={(e) => updateIconOffset(
                      selectedIcon!,
                      { offsetX: Number(e.target.value) }
                    )}
                  />
                </label>
                <div className="button-row">
                  <button onClick={() => updateIconOffset(
                    selectedIcon!,
                    { offsetX: currentOffset.offsetX - 1 }
                  )}>← Left</button>
                  <button onClick={() => updateIconOffset(
                    selectedIcon!,
                    { offsetX: currentOffset.offsetX + 1 }
                  )}>Right →</button>
                </div>
              </div>

              <div className="control-group">
                <label>
                  <strong>Y Offset:</strong> {currentOffset.offsetY}px
                  <input
                    type="range"
                    min="-50"
                    max="50"
                    value={currentOffset.offsetY}
                    onChange={(e) => updateIconOffset(
                      selectedIcon!,
                      { offsetY: Number(e.target.value) }
                    )}
                  />
                </label>
                <div className="button-row">
                  <button onClick={() => updateIconOffset(
                    selectedIcon!,
                    { offsetY: currentOffset.offsetY - 1 }
                  )}>↑ Up</button>
                  <button onClick={() => updateIconOffset(
                    selectedIcon!,
                    { offsetY: currentOffset.offsetY + 1 }
                  )}>Down ↓</button>
                </div>
              </div>

              <div className="control-group">
                <label>
                  <strong>Icon Zoom:</strong> {currentOffset.scale.toFixed(3)}x
                  <input
                    type="range"
                    min="0.1"
                    max="3.0"
                    step="0.01"
                    value={currentOffset.scale}
                    onChange={(e) => updateIconOffset(
                      selectedIcon!,
                      { scale: Number(e.target.value) }
                    )}
                  />
                </label>
                <div className="button-row">
                  <button onClick={() => updateIconOffset(
                    selectedIcon!,
                    { scale: Math.max(0.1, currentOffset.scale - 0.05) }
                  )}>🔍- Zoom Out</button>
                  <button onClick={() => updateIconOffset(
                    selectedIcon!,
                    { scale: Math.min(3.0, currentOffset.scale + 0.05) }
                  )}>🔍+ Zoom In</button>
                </div>
              </div>

              <div className="control-group">
                <label>
                  <strong>Icon Width:</strong> {currentOffset.width || 70}px
                  <input
                    type="range"
                    min="40"
                    max="200"
                    value={currentOffset.width || 70}
                    onChange={(e) => updateIconOffset(
                      selectedIcon!,
                      { width: Number(e.target.value) }
                    )}
                  />
                </label>
                <div className="button-row">
                  <button onClick={() => updateIconOffset(
                    selectedIcon!,
                    { width: (currentOffset.width || 70) - 5 }
                  )}>◀ Narrower</button>
                  <button onClick={() => updateIconOffset(
                    selectedIcon!,
                    { width: (currentOffset.width || 70) + 5 }
                  )}>Wider ▶</button>
                </div>
              </div>

              <div className="control-group">
                <label>
                  <strong>Icon Height:</strong> {currentOffset.height || 70}px
                  <input
                    type="range"
                    min="40"
                    max="200"
                    value={currentOffset.height || 70}
                    onChange={(e) => updateIconOffset(
                      selectedIcon!,
                      { height: Number(e.target.value) }
                    )}
                  />
                </label>
                <div className="button-row">
                  <button onClick={() => updateIconOffset(
                    selectedIcon!,
                    { height: (currentOffset.height || 70) - 5 }
                  )}>▼ Shorter</button>
                  <button onClick={() => updateIconOffset(
                    selectedIcon!,
                    { height: (currentOffset.height || 70) + 5 }
                  )}>Taller ▲</button>
                </div>
              </div>

              <div className="control-group">
                <button 
                  className="reset-btn"
                  onClick={() => updateIconOffset(
                    selectedIcon!,
                    { offsetX: 0, offsetY: 0, scale: 1.0, width: 70, height: 70 }
                  )}
                >
                  🔄 Reset This Icon
                </button>
              </div>
            </>
          ) : (
            <div className="no-selection">
              <p>👆 Click an icon to adjust its position</p>
            </div>
          )}

          <div className="import-section">
            <h3>📥 Import Configuration</h3>
            <button 
              className="import-toggle-btn"
              onClick={() => setShowImport(!showImport)}
            >
              {showImport ? '✕ Cancel' : '📥 Import JSON'}
            </button>
            
            {showImport && (
              <div className="import-controls">
                <textarea
                  placeholder='Paste your JSON configuration here...'
                  value={jsonInput}
                  onChange={(e) => setJsonInput(e.target.value)}
                  rows={10}
                />
                <button 
                  className="import-btn"
                  onClick={importJson}
                  disabled={!jsonInput.trim()}
                >
                  ✅ Load Configuration
                </button>
              </div>
            )}
          </div>

          <div className="export-values">
            <h3>📋 Export All Positions</h3>
            <p className="export-hint">Auto-saved to browser • Copy this JSON to share</p>
            <pre>
{JSON.stringify(iconOffsets, null, 2)}
            </pre>
            <button onClick={() => {
              navigator.clipboard.writeText(JSON.stringify(iconOffsets, null, 2));
              alert('All icon positions copied to clipboard!');
            }}>
              📋 Copy JSON
            </button>
          </div>

          <div className="reset-section">
            <button 
              className="reset-all-btn"
              onClick={resetToDefaults}
            >
              🔄 Reset All to Defaults
            </button>
            <p className="reset-hint">⚠️ This will clear all adjustments and browser storage</p>
          </div>
        </div>

        {/* Icons Grid - Right Side */}
        <div className="weapons-grid">
          <h2>Special Icons ({Object.keys(SPECIAL_ICONS).length})</h2>
          <div className="grid-container">
            {Object.entries(SPECIAL_ICONS).map(([key, info]) => {
              const offset = getIconOffset(key);
              const isAdjusted = offset.offsetX !== 0 || offset.offsetY !== 0 || offset.scale !== 1.0 || 
                                 (offset.width && offset.width !== 70) || (offset.height && offset.height !== 70);
              const isSelected = selectedIcon === key;
              
              return (
                <div 
                  key={key} 
                  className={`weapon-item ${isSelected ? 'selected' : ''} ${isAdjusted ? 'adjusted' : ''}`}
                  onClick={() => setSelectedIcon(key)}
                >
                  <div 
                    className="weapon-icon-box-wrapper"
                    style={{ position: 'relative' }}
                  >
                    <div 
                      className={`weapon-icon-box ${isDragging && isSelected ? 'dragging' : ''}`}
                      style={getSpriteStyle(key)}
                      onMouseDown={(e) => handleDragStart(e, key)}
                    >
                      {isSelected && (
                        <>
                          <div className="drag-overlay" title="Drag to reposition">
                            <span className="drag-hint">🖐️ Drag</span>
                          </div>
                          <div 
                            className="resize-handle"
                            onMouseDown={(e) => handleResizeStart(e, key)}
                            title="Drag to resize"
                          >
                            ↘️
                          </div>
                        </>
                      )}
                    </div>
                  </div>
                  <div className="weapon-info">
                    <strong>{info.displayName}</strong>
                    <small>{info.category}</small>
                    {isAdjusted && <span className="adjusted-badge">✓ Adjusted</span>}
                  </div>
                </div>
              );
            })}
          </div>

          <div className="reference-sprite">
            <h3>🖼️ Full Sprite Reference (534x1024)</h3>
            <p style={{ color: '#888', fontSize: '0.9rem', marginBottom: '1rem' }}>
              Vertical layout: Headshot (top) • Knife (middle) • C4 (bottom)
            </p>
            <img src="/more-icons.png" alt="Full special icons sprite" style={{ maxWidth: '100%', border: '2px solid #333' }} />
          </div>
        </div>
      </div>
    </div>
  );
};
