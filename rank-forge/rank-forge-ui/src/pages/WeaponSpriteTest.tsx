import { useState, useEffect, useRef } from 'react';
import { WEAPON_MAP } from '../utils/weaponIcons';
import { WEAPON_SPRITE_POSITIONS, SPRITE_WIDTH, SPRITE_HEIGHT } from '../utils/weaponSpritePositions';
import './WeaponSpriteTest.css';

interface WeaponOffset {
  offsetX: number;
  offsetY: number;
  scale: number;
  width?: number;
  height?: number;
}

export const WeaponSpriteTest = () => {
  const [iconSize, setIconSize] = useState(70);
  const [spriteScale, setSpriteScale] = useState(1.0);
  const [selectedWeapon, setSelectedWeapon] = useState<string | null>(null);
  const [jsonInput, setJsonInput] = useState('');
  const [showImport, setShowImport] = useState(false);
  
  // Store per-weapon offsets
  const [weaponOffsets, setWeaponOffsets] = useState<Record<string, WeaponOffset>>({});

  // Drag and resize state
  const [isDragging, setIsDragging] = useState(false);
  const [isResizing, setIsResizing] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [resizeStart, setResizeStart] = useState({ width: 0, height: 0, mouseX: 0, mouseY: 0 });
  const dragRef = useRef<HTMLDivElement>(null);

  // LocalStorage key
  const STORAGE_KEY = 'weapon-sprite-config';

  // Initialize weapon offsets from localStorage or defaults
  useEffect(() => {
    // Try to load from localStorage first
    const savedConfig = localStorage.getItem(STORAGE_KEY);
    
    if (savedConfig) {
      try {
        const parsed = JSON.parse(savedConfig);
        setWeaponOffsets(parsed);
        console.log('✅ Loaded weapon config from localStorage');
        return;
      } catch (error) {
        console.error('Failed to parse saved config:', error);
      }
    }
    
    // Fallback to default config
    const initialOffsets: Record<string, WeaponOffset> = {};
    Object.entries(WEAPON_MAP).forEach(([key, info]) => {
      if (WEAPON_SPRITE_POSITIONS[info.iconFile]) {
        initialOffsets[info.iconFile] = { offsetX: 14, offsetY: 5, scale: 1.0, width: 70, height: 70 };
      }
    });
    setWeaponOffsets(initialOffsets);
  }, []);

  // Save to localStorage whenever weaponOffsets changes
  useEffect(() => {
    if (Object.keys(weaponOffsets).length > 0) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(weaponOffsets));
      console.log('💾 Saved weapon config to localStorage');
    }
  }, [weaponOffsets]);

  // Import JSON configuration
  const importJson = () => {
    try {
      const parsed = JSON.parse(jsonInput);
      setWeaponOffsets(parsed);
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
    if (confirm('⚠️ This will reset ALL weapons to default positions and clear browser storage. Continue?')) {
      localStorage.removeItem(STORAGE_KEY);
      const initialOffsets: Record<string, WeaponOffset> = {};
      Object.entries(WEAPON_MAP).forEach(([key, info]) => {
        if (WEAPON_SPRITE_POSITIONS[info.iconFile]) {
          initialOffsets[info.iconFile] = { offsetX: 14, offsetY: 5, scale: 1.0, width: 70, height: 70 };
        }
      });
      setWeaponOffsets(initialOffsets);
      setSelectedWeapon(null);
      alert('✅ Reset to defaults complete!');
    }
  };

  // Store iconFile during drag/resize operations
  const [activeIconFile, setActiveIconFile] = useState<string | null>(null);

  // Drag handlers
  const handleDragStart = (e: React.MouseEvent, iconFile: string, weaponKey: string) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
    setDragStart({ x: e.clientX, y: e.clientY });
    setActiveIconFile(iconFile);
    setSelectedWeapon(weaponKey);
  };

  const handleDragMove = (e: React.MouseEvent) => {
    if (!isDragging || !activeIconFile) return;

    const deltaX = e.clientX - dragStart.x;
    const deltaY = e.clientY - dragStart.y;
    
    const currentOffset = getWeaponOffset(activeIconFile);
    updateWeaponOffset(activeIconFile, {
      offsetX: currentOffset.offsetX + deltaX,
      offsetY: currentOffset.offsetY + deltaY
    });
    
    setDragStart({ x: e.clientX, y: e.clientY });
  };

  const handleDragEnd = () => {
    setIsDragging(false);
    setActiveIconFile(null);
  };

  // Resize handlers
  const handleResizeStart = (e: React.MouseEvent, iconFile: string, weaponKey: string) => {
    e.preventDefault();
    e.stopPropagation();
    setIsResizing(true);
    
    const currentOffset = getWeaponOffset(iconFile);
    setResizeStart({
      width: currentOffset.width || 70,
      height: currentOffset.height || 70,
      mouseX: e.clientX,
      mouseY: e.clientY
    });
    setActiveIconFile(iconFile);
    setSelectedWeapon(weaponKey);
  };

  const handleResizeMove = (e: React.MouseEvent) => {
    if (!isResizing || !activeIconFile) return;

    const deltaX = e.clientX - resizeStart.mouseX;
    const deltaY = e.clientY - resizeStart.mouseY;
    
    const newWidth = Math.max(40, Math.min(200, resizeStart.width + deltaX));
    const newHeight = Math.max(40, Math.min(200, resizeStart.height + deltaY));
    
    updateWeaponOffset(activeIconFile, {
      width: newWidth,
      height: newHeight
    });
  };

  const handleResizeEnd = () => {
    setIsResizing(false);
    setActiveIconFile(null);
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
  }, [isDragging, isResizing, dragStart, resizeStart, activeIconFile]);

  // Get all unique weapons that have sprite positions
  const weaponsWithSprites = Object.entries(WEAPON_MAP)
    .map(([key, info]) => ({
      key,
      info,
      spritePos: WEAPON_SPRITE_POSITIONS[info.iconFile]
    }))
    .filter(w => w.spritePos);

  const getWeaponOffset = (iconFile: string): WeaponOffset => {
    return weaponOffsets[iconFile] || { offsetX: 14, offsetY: 5, scale: 1.0, width: 70, height: 70 };
  };

  const updateWeaponOffset = (iconFile: string, updates: Partial<WeaponOffset>) => {
    setWeaponOffsets(prev => ({
      ...prev,
      [iconFile]: {
        ...getWeaponOffset(iconFile),
        ...updates
      }
    }));
  };

  const getSpriteStyle = (iconFile: string) => {
    const pos = WEAPON_SPRITE_POSITIONS[iconFile];
    if (!pos) return {};

    const offset = getWeaponOffset(iconFile);
    const adjustedX = pos.x + offset.offsetX;
    const adjustedY = pos.y + offset.offsetY;
    // Combine global sprite scale with per-weapon scale
    const combinedScale = spriteScale * offset.scale;
    const scaledWidth = SPRITE_WIDTH * combinedScale;
    const scaledHeight = SPRITE_HEIGHT * combinedScale;

    return {
      backgroundImage: 'url(/weapons-sprite-amber.png)',
      backgroundPosition: `-${adjustedX * offset.scale}px -${adjustedY * offset.scale}px`,
      backgroundSize: `${scaledWidth}px ${scaledHeight}px`,
      backgroundRepeat: 'no-repeat',
      width: `${offset.width || iconSize}px`,
      height: `${offset.height || iconSize}px`,
    };
  };

  const selectedWeaponInfo = selectedWeapon 
    ? weaponsWithSprites.find(w => w.key === selectedWeapon)
    : null;
  
  const currentOffset = selectedWeaponInfo 
    ? getWeaponOffset(selectedWeaponInfo.info.iconFile)
    : { offsetX: 14, offsetY: 5, scale: 1.0, width: 70, height: 70 };

  return (
    <div className="weapon-sprite-test">
      <div className="test-header">
        <h1>🎯 Weapon Sprite Alignment Tool</h1>
        <p>Click a weapon to select • Drag icon to reposition • Drag corner to resize</p>
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
          {selectedWeaponInfo ? (
            <>
              <div className="selected-weapon-header">
                <h2>Adjusting: {selectedWeaponInfo.info.displayName}</h2>
                <button 
                  className="deselect-btn"
                  onClick={() => setSelectedWeapon(null)}
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
                    onChange={(e) => updateWeaponOffset(
                      selectedWeaponInfo.info.iconFile,
                      { offsetX: Number(e.target.value) }
                    )}
                  />
                </label>
                <div className="button-row">
                  <button onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
                    { offsetX: currentOffset.offsetX - 1 }
                  )}>← Left</button>
                  <button onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
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
                    onChange={(e) => updateWeaponOffset(
                      selectedWeaponInfo.info.iconFile,
                      { offsetY: Number(e.target.value) }
                    )}
                  />
                </label>
                <div className="button-row">
                  <button onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
                    { offsetY: currentOffset.offsetY - 1 }
                  )}>↑ Up</button>
                  <button onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
                    { offsetY: currentOffset.offsetY + 1 }
                  )}>Down ↓</button>
                </div>
              </div>

              <div className="control-group">
                <label>
                  <strong>Weapon Zoom:</strong> {currentOffset.scale.toFixed(2)}x
                  <input
                    type="range"
                    min="0.5"
                    max="2.0"
                    step="0.05"
                    value={currentOffset.scale}
                    onChange={(e) => updateWeaponOffset(
                      selectedWeaponInfo.info.iconFile,
                      { scale: Number(e.target.value) }
                    )}
                  />
                </label>
                <div className="button-row">
                  <button onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
                    { scale: Math.max(0.5, currentOffset.scale - 0.05) }
                  )}>🔍- Zoom Out</button>
                  <button onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
                    { scale: Math.min(2.0, currentOffset.scale + 0.05) }
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
                    onChange={(e) => updateWeaponOffset(
                      selectedWeaponInfo.info.iconFile,
                      { width: Number(e.target.value) }
                    )}
                  />
                </label>
                <div className="button-row">
                  <button onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
                    { width: (currentOffset.width || 70) - 5 }
                  )}>◀ Narrower</button>
                  <button onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
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
                    onChange={(e) => updateWeaponOffset(
                      selectedWeaponInfo.info.iconFile,
                      { height: Number(e.target.value) }
                    )}
                  />
                </label>
                <div className="button-row">
                  <button onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
                    { height: (currentOffset.height || 70) - 5 }
                  )}>▼ Shorter</button>
                  <button onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
                    { height: (currentOffset.height || 70) + 5 }
                  )}>Taller ▲</button>
                </div>
              </div>

              <div className="control-group">
                <button 
                  className="reset-btn"
                  onClick={() => updateWeaponOffset(
                    selectedWeaponInfo.info.iconFile,
                    { offsetX: 14, offsetY: 5, scale: 1.0, width: 70, height: 70 }
                  )}
                >
                  🔄 Reset This Weapon
                </button>
              </div>
            </>
          ) : (
            <div className="no-selection">
              <p>👆 Click a weapon icon to adjust its position</p>
            </div>
          )}

          <div className="control-group global-controls">
            <h3>Global Settings</h3>
            <label>
              <strong>Icon Size:</strong> {iconSize}px
              <input
                type="range"
                min="50"
                max="150"
                value={iconSize}
                onChange={(e) => setIconSize(Number(e.target.value))}
              />
            </label>

            <label>
              <strong>Sprite Scale:</strong> {spriteScale.toFixed(2)}x
              <input
                type="range"
                min="0.8"
                max="1.5"
                step="0.05"
                value={spriteScale}
                onChange={(e) => setSpriteScale(Number(e.target.value))}
              />
            </label>
          </div>

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
{JSON.stringify(weaponOffsets, null, 2)}
            </pre>
            <button onClick={() => {
              navigator.clipboard.writeText(JSON.stringify(weaponOffsets, null, 2));
              alert('All weapon positions copied to clipboard!');
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

        {/* Weapons Grid - Right Side */}
        <div className="weapons-grid">
          <h2>All Weapons ({weaponsWithSprites.length})</h2>
          <div className="grid-container">
            {weaponsWithSprites.map(({ key, info, spritePos }) => {
              const offset = getWeaponOffset(info.iconFile);
              const isAdjusted = offset.offsetX !== 14 || offset.offsetY !== 5 || offset.scale !== 1.0 || 
                                 (offset.width && offset.width !== 70) || (offset.height && offset.height !== 70);
              const isSelected = selectedWeapon === key;
              
              return (
                <div 
                  key={key} 
                  className={`weapon-item ${isSelected ? 'selected' : ''} ${isAdjusted ? 'adjusted' : ''}`}
                  onClick={() => setSelectedWeapon(key)}
                >
                  <div 
                    className="weapon-icon-box-wrapper"
                    style={{ position: 'relative' }}
                  >
                    <div 
                      className={`weapon-icon-box ${isDragging && isSelected ? 'dragging' : ''}`}
                      style={getSpriteStyle(info.iconFile)}
                      onMouseDown={(e) => handleDragStart(e, info.iconFile, key)}
                    >
                      {isSelected && (
                        <>
                          <div className="drag-overlay" title="Drag to reposition">
                            <span className="drag-hint">🖐️ Drag</span>
                          </div>
                          <div 
                            className="resize-handle"
                            onMouseDown={(e) => handleResizeStart(e, info.iconFile, key)}
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
            <h3>🖼️ Full Sprite Reference</h3>
            <img src="/weapons-sprite-amber.png" alt="Full weapon sprite" style={{ maxWidth: '100%', border: '2px solid #333' }} />
          </div>
        </div>
      </div>
    </div>
  );
};
