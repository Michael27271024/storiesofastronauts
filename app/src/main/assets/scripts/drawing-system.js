/**
 * Drawing System Module - Unified canvas rendering system for all stages
 * Provides professional astronaut character drawing, animations, and VFX
 * 
 * Usage:
 *   const drawer = DrawingSystem.create({ width: 1024, height: 768 });
 *   drawer.drawAstronaut(x, y, characterName, state);
 */

(function(global) {
    'use strict';

    const DrawingSystem = {
        create: function(config) {
            return new CanvasRenderer(config);
        }
    };

    /**
     * Main Canvas Renderer Class
     */
    class CanvasRenderer {
        constructor(config) {
            this.width = config.width || window.innerWidth;
            this.height = config.height || window.innerHeight;
            this.ctx = config.ctx || null;
            this.scale = config.scale || 1;
            this.quality = config.quality || 'high';
            
            // Color palette for professional rendering
            this.colors = {
                astronaut_suit: '#FF6B6B',
                astronaut_visor: '#00D9FF',
                astronaut_accent: '#FFD700',
                astronaut_shadow: 'rgba(0, 0, 0, 0.4)',
                selection_glow: '#00FF88',
                damage_flash: '#FF4444'
            };

            // Animation state
            this.animations = new Map();
            this.particles = [];
            this.effects = [];
        }

        /**
         * Draw professional astronaut character
         * @param {number} x - X position
         * @param {number} y - Y position
         * @param {string} characterName - Character name (کوروش، داریوش، سهراب، رستم)
         * @param {string} state - State: idle, walk, jump, attack, hit, dead
         * @param {object} options - Additional options
         */
        drawAstronaut(x, y, characterName, state = 'idle', options = {}) {
            const ctx = this.ctx;
            if (!ctx) return;

            ctx.save();

            const width = options.width || 50;
            const height = options.height || 70;
            const direction = options.direction || 1;
            const health = options.health || 5;
            const maxHealth = options.maxHealth || 5;
            const isSelected = options.isSelected || false;
            const isHit = options.isHit || false;

            // Translate for direction
            if (direction === -1) {
                ctx.translate(x + width, y);
                ctx.scale(-1, 1);
                x = 0;
            } else {
                ctx.translate(x, y);
            }

            // Draw shadow
            ctx.fillStyle = this.colors.astronaut_shadow;
            ctx.beginPath();
            ctx.ellipse(width / 2, height - 5, width / 2, 8, 0, 0, Math.PI * 2);
            ctx.fill();

            // Draw astronaut body based on state
            this._drawAstronautBody(width, height, characterName, state, isHit);

            // Draw selection glow if selected
            if (isSelected) {
                ctx.strokeStyle = this.colors.selection_glow;
                ctx.lineWidth = 3;
                ctx.shadowColor = this.colors.selection_glow;
                ctx.shadowBlur = 10;
                ctx.beginPath();
                ctx.rect(-5, -5, width + 10, height + 10);
                ctx.stroke();
                ctx.shadowColor = 'transparent';
            }

            // Draw health bar if needed
            if (options.showHealthBar && health < maxHealth) {
                this._drawHealthBar(width, height, health, maxHealth);
            }

            ctx.restore();
        }

        /**
         * Draw astronaut body with professional details
         */
        _drawAstronautBody(width, height, characterName, state, isHit) {
            const ctx = this.ctx;
            const flashColor = isHit ? 'rgba(255, 255, 255, 0.6)' : 'transparent';

            // Helmet visor
            ctx.fillStyle = this.colors.astronaut_visor;
            ctx.globalAlpha = 0.7;
            ctx.beginPath();
            ctx.arc(width / 2, height * 0.25, width * 0.35, 0, Math.PI * 2);
            ctx.fill();
            ctx.globalAlpha = 1;

            // Helmet glint
            ctx.fillStyle = 'rgba(255, 255, 255, 0.4)';
            ctx.beginPath();
            ctx.arc(width * 0.35, height * 0.15, width * 0.1, 0, Math.PI * 2);
            ctx.fill();

            // Suit body
            ctx.fillStyle = this._getCharacterColor(characterName);
            if (state === 'jump') {
                // Crouched jump pose
                ctx.beginPath();
                ctx.ellipse(width / 2, height * 0.55, width * 0.35, height * 0.25, 0, 0, Math.PI * 2);
                ctx.fill();
            } else if (state === 'attack') {
                // Aggressive stance
                ctx.beginPath();
                ctx.rect(width * 0.1, height * 0.35, width * 0.8, height * 0.5);
                ctx.fill();
            } else {
                // Normal standing pose
                ctx.beginPath();
                ctx.rect(width * 0.15, height * 0.35, width * 0.7, height * 0.5);
                ctx.fill();
            }

            // Accent stripe
            ctx.fillStyle = this.colors.astronaut_accent;
            ctx.beginPath();
            ctx.rect(width * 0.45, height * 0.35, width * 0.1, height * 0.5);
            ctx.fill();

            // Gloves
            ctx.fillStyle = '#FFFFFF';
            ctx.beginPath();
            ctx.arc(width * 0.15, height * 0.45, width * 0.08, 0, Math.PI * 2);
            ctx.fill();
            ctx.beginPath();
            ctx.arc(width * 0.85, height * 0.45, width * 0.08, 0, Math.PI * 2);
            ctx.fill();

            // Boots
            ctx.fillStyle = '#333333';
            ctx.beginPath();
            ctx.rect(width * 0.2, height * 0.85, width * 0.25, height * 0.15);
            ctx.fill();
            ctx.beginPath();
            ctx.rect(width * 0.55, height * 0.85, width * 0.25, height * 0.15);
            ctx.fill();

            // Flash effect when hit
            if (isHit) {
                ctx.fillStyle = flashColor;
                ctx.fillRect(0, 0, width, height);
            }
        }

        /**
         * Get character-specific suit color
         */
        _getCharacterColor(characterName) {
            const colors = {
                'کوروش': '#FF6B6B',      // Red
                'داریوش': '#00D9FF',    // Cyan
                'سهراب': '#FFD700',     // Gold
                'رستم': '#00FF88'       // Green
            };
            return colors[characterName] || this.colors.astronaut_suit;
        }

        /**
         * Draw health bar above character
         */
        _drawHealthBar(width, height, health, maxHealth) {
            const ctx = this.ctx;
            const barWidth = width;
            const barHeight = 4;
            const barX = 0;
            const barY = -12;

            // Background
            ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
            ctx.fillRect(barX, barY, barWidth, barHeight);

            // Health
            const healthPercent = health / maxHealth;
            const healthColor = healthPercent > 0.5 ? '#00FF88' : healthPercent > 0.25 ? '#FFD700' : '#FF6B6B';
            ctx.fillStyle = healthColor;
            ctx.fillRect(barX, barY, barWidth * healthPercent, barHeight);

            // Border
            ctx.strokeStyle = '#FFFFFF';
            ctx.lineWidth = 1;
            ctx.strokeRect(barX, barY, barWidth, barHeight);
        }

        /**
         * Draw professional particle effect
         */
        drawParticle(x, y, type = 'spark', options = {}) {
            const ctx = this.ctx;
            ctx.save();
            ctx.globalAlpha = options.alpha || 0.8;

            switch(type) {
                case 'spark':
                    ctx.fillStyle = options.color || '#FFD700';
                    ctx.beginPath();
                    ctx.arc(x, y, options.size || 2, 0, Math.PI * 2);
                    ctx.fill();
                    break;
                case 'hit':
                    ctx.fillStyle = 'rgba(255, 100, 100, 0.8)';
                    ctx.beginPath();
                    ctx.rect(x - 3, y - 3, 6, 6);
                    ctx.fill();
                    break;
                case 'heal':
                    ctx.fillStyle = 'rgba(0, 255, 136, 0.8)';
                    ctx.beginPath();
                    ctx.arc(x, y, 4, 0, Math.PI * 2);
                    ctx.fill();
                    break;
            }

            ctx.restore();
        }

        /**
         * Create damage flash effect
         */
        createDamageFlash(duration = 100) {
            return {
                startTime: Date.now(),
                duration: duration,
                type: 'damage'
            };
        }

        /**
         * Draw screen effect
         */
        drawScreenEffect(effect) {
            if (!effect) return;

            const ctx = this.ctx;
            const elapsed = Date.now() - effect.startTime;
            const progress = Math.min(elapsed / effect.duration, 1);

            if (effect.type === 'damage') {
                ctx.fillStyle = `rgba(255, 100, 100, ${(1 - progress) * 0.3})`;
                ctx.fillRect(0, 0, this.width, this.height);
            }
        }

        /**
         * Utility: Draw background with stars
         */
        drawStarfield(options = {}) {
            const ctx = this.ctx;
            const numStars = options.count || 200;
            const seed = options.seed || 42;

            // Seeded random for consistent star pattern
            for (let i = 0; i < numStars; i++) {
                const x = ((seed + i * 7919) % this.width);
                const y = ((seed + i * 11) % this.height);
                const size = ((seed + i * 13) % 3) + 0.5;
                const brightness = ((seed + i * 17) % 100) / 100;

                ctx.fillStyle = `rgba(255, 255, 255, ${brightness})`;
                ctx.beginPath();
                ctx.arc(x, y, size, 0, Math.PI * 2);
                ctx.fill();
            }
        }

        /**
         * Utility: Draw gradient background
         */
        drawGradientBackground(color1, color2) {
            const ctx = this.ctx;
            const gradient = ctx.createLinearGradient(0, 0, 0, this.height);
            gradient.addColorStop(0, color1);
            gradient.addColorStop(1, color2);
            ctx.fillStyle = gradient;
            ctx.fillRect(0, 0, this.width, this.height);
        }

        /**
         * Utility: Draw text with glow effect
         */
        drawGlowText(text, x, y, options = {}) {
            const ctx = this.ctx;
            const fontSize = options.fontSize || 24;
            const color = options.color || '#00D9FF';
            const glowColor = options.glowColor || color;
            const glowBlur = options.glowBlur || 10;

            ctx.save();
            ctx.font = `bold ${fontSize}px 'Vazirmatn', sans-serif`;
            ctx.textAlign = options.textAlign || 'center';
            ctx.textBaseline = options.textBaseline || 'middle';

            // Glow effect
            ctx.shadowColor = glowColor;
            ctx.shadowBlur = glowBlur;
            ctx.shadowOffsetX = 0;
            ctx.shadowOffsetY = 0;

            // Draw text
            ctx.fillStyle = color;
            ctx.fillText(text, x, y);

            ctx.restore();
        }

        /**
         * Clear canvas
         */
        clear(color = '#000000') {
            const ctx = this.ctx;
            ctx.fillStyle = color;
            ctx.fillRect(0, 0, this.width, this.height);
        }

        /**
         * Resize canvas
         */
        resize(width, height) {
            this.width = width;
            this.height = height;
            if (this.ctx && this.ctx.canvas) {
                this.ctx.canvas.width = width;
                this.ctx.canvas.height = height;
            }
        }
    };

    // Export
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = DrawingSystem;
    } else {
        global.DrawingSystem = DrawingSystem;
    }

})(typeof window !== 'undefined' ? window : global);
