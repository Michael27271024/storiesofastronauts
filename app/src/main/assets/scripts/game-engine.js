/**
 * Game Engine - Unified game engine for all Astronaut Stories stages
 * Handles game loop, state management, physics, rendering, and input
 * 
 * Usage:
 *   const engine = GameEngine.create(canvas, config);
 *   engine.addPlayer(playerData);
 *   engine.addEnemy(enemyData);
 *   engine.start();
 */

(function(global) {
    'use strict';

    const GameEngine = {
        create: function(canvas, config = {}) {
            return new Engine(canvas, config);
        }
    };

    /**
     * Main Game Engine Class
     */
    class Engine {
        constructor(canvas, config) {
            this.canvas = canvas;
            this.ctx = canvas.getContext('2d');
            this.config = {
                gravity: 0.4,
                friction: 0.85,
                worldWidth: config.worldWidth || 2000,
                worldHeight: config.worldHeight || canvas.height,
                fps: config.fps || 60,
                ...config
            };

            // Game state
            this.gameState = 'READY'; // READY, PLAYING, PAUSED, GAMEOVER, WIN
            this.isPaused = false;
            this.isRunning = false;

            // Game objects
            this.player = null;
            this.enemies = [];
            this.teammates = [];
            this.platforms = [];
            this.items = [];
            this.bullets = [];
            this.particles = [];
            this.effects = [];

            // Camera
            this.camera = { x: 0, y: 0 };

            // Input
            this.input = {
                left: false,
                right: false,
                jump: false,
                shoot: false,
                action: false
            };

            // Drawing system
            this.drawer = null;

            // Game loop
            this.frameCount = 0;
            this.lastFrameTime = 0;
            this.deltaTime = 0;

            // Callbacks
            this.onGameOver = null;
            this.onWin = null;
            this.onUpdate = null;

            this._setupInput();
            this._setupResizeListener();
        }

        /**
         * Initialize with drawing system
         */
        setDrawer(drawer) {
            this.drawer = drawer;
            drawer.ctx = this.ctx;
            return this;
        }

        /**
         * Add player to the game
         */
        addPlayer(data) {
            this.player = {
                x: data.x || 100,
                y: data.y || 0,
                width: data.width || 50,
                height: data.height || 70,
                speed: data.speed || 4,
                jumpPower: data.jumpPower || -12,
                velocityX: 0,
                velocityY: 0,
                isJumping: false,
                onGround: false,
                health: data.health || 5,
                maxHealth: data.maxHealth || 5,
                hasWeapon: false,
                ammo: 0,
                invincibleTimer: 0,
                direction: 1, // 1 for right, -1 for left
                state: 'idle', // idle, walk, jump, attack, hit, dead
                name: data.name || 'کوروش',
                sprite: data.sprite || null,
                ...data
            };
            return this;
        }

        /**
         * Add enemy/astronaut to the game
         */
        addEnemy(data) {
            const enemy = {
                x: data.x || 500,
                y: data.y || 0,
                width: data.width || 50,
                height: data.height || 70,
                speed: data.speed || 2,
                velocityX: 0,
                velocityY: 0,
                isJumping: false,
                onGround: false,
                health: data.health || 3,
                maxHealth: data.maxHealth || 3,
                attackRange: data.attackRange || 100,
                attackCooldown: 0,
                direction: 1,
                state: 'idle',
                name: data.name || 'Enemy',
                aiType: data.aiType || 'patrol', // patrol, chase, ranged
                patrolRange: data.patrolRange || 300,
                patrolStart: data.patrolStart || data.x,
                ...data
            };
            this.enemies.push(enemy);
            return this;
        }

        /**
         * Add platform to the game
         */
        addPlatform(x, y, width, height) {
            this.platforms.push({ x, y, width, height });
            return this;
        }

        /**
         * Add item to the game
         */
        addItem(x, y, type, data = {}) {
            this.items.push({
                x,
                y,
                width: data.width || 20,
                height: data.height || 20,
                type, // health, ammo, weapon
                collected: false,
                ...data
            });
            return this;
        }

        /**
         * Start the game
         */
        start() {
            this.gameState = 'PLAYING';
            this.isPaused = false;
            this.isRunning = true;
            this._gameLoop();
            return this;
        }

        /**
         * Pause the game
         */
        pause() {
            this.isPaused = true;
            this.gameState = 'PAUSED';
            return this;
        }

        /**
         * Resume the game
         */
        resume() {
            this.isPaused = false;
            this.gameState = 'PLAYING';
            return this;
        }

        /**
         * Stop the game
         */
        stop() {
            this.isRunning = false;
            return this;
        }

        /**
         * Game over
         */
        triggerGameOver() {
            this.gameState = 'GAMEOVER';
            this.isPaused = true;
            if (this.onGameOver) {
                this.onGameOver();
            }
            return this;
        }

        /**
         * Win condition
         */
        triggerWin() {
            this.gameState = 'WIN';
            this.isPaused = true;
            if (this.onWin) {
                this.onWin();
            }
            return this;
        }

        /**
         * Private: Game loop
         */
        _gameLoop = () => {
            const now = performance.now();
            this.deltaTime = (now - this.lastFrameTime) / 1000 || 0.016;
            this.lastFrameTime = now;

            if (!this.isPaused) {
                this._update();
            }

            this._draw();
            this.frameCount++;

            if (this.isRunning) {
                requestAnimationFrame(this._gameLoop);
            }
        }

        /**
         * Private: Update game logic
         */
        _update() {
            if (this.gameState !== 'PLAYING') return;

            // Update player
            if (this.player) {
                this._updatePlayer();
                this._updateCamera();
            }

            // Update enemies
            this.enemies.forEach((enemy, index) => {
                this._updateEnemy(enemy, index);
            });

            // Update bullets
            this._updateBullets();

            // Update items
            this._updateItems();

            // Update particles
            this._updateParticles();

            // Custom update callback
            if (this.onUpdate) {
                this.onUpdate({
                    deltaTime: this.deltaTime,
                    gameState: this.gameState,
                    frameCount: this.frameCount
                });
            }
        }

        /**
         * Private: Update player physics and state
         */
        _updatePlayer() {
            const p = this.player;

            // Horizontal movement
            if (this.input.left) {
                p.velocityX = -p.speed;
                p.direction = -1;
                p.state = 'walk';
            } else if (this.input.right) {
                p.velocityX = p.speed;
                p.direction = 1;
                p.state = 'walk';
            } else {
                p.velocityX *= this.config.friction;
                if (p.state === 'walk' && !p.isJumping) {
                    p.state = 'idle';
                }
            }

            // Jumping
            if (this.input.jump && p.onGround) {
                p.velocityY = p.jumpPower;
                p.isJumping = true;
                p.onGround = false;
                p.state = 'jump';
                this.input.jump = false; // Consume jump input
            }

            // Apply gravity
            p.velocityY += this.config.gravity;

            // Update position
            p.x += p.velocityX;
            p.y += p.velocityY;

            // Collision detection
            this._resolveCollisions(p);

            // World bounds
            if (p.x < 0) p.x = 0;
            if (p.x + p.width > this.config.worldWidth) {
                p.x = this.config.worldWidth - p.width;
            }

            // Check if fell off world
            if (p.y > this.canvas.height + 100) {
                this.triggerGameOver();
            }

            // Invincibility timer
            if (p.invincibleTimer > 0) {
                p.invincibleTimer--;
            }
        }

        /**
         * Private: Update enemy AI
         */
        _updateEnemy(enemy, index) {
            // Simple AI
            switch(enemy.aiType) {
                case 'patrol':
                    this._updatePatrolEnemy(enemy);
                    break;
                case 'chase':
                    this._updateChaseEnemy(enemy);
                    break;
                case 'ranged':
                    this._updateRangedEnemy(enemy);
                    break;
            }

            // Physics
            enemy.velocityY += this.config.gravity;
            enemy.x += enemy.velocityX;
            enemy.y += enemy.velocityY;

            // Collision detection
            this._resolveCollisions(enemy);

            // Remove if fallen off
            if (enemy.y > this.canvas.height + 100) {
                this.enemies.splice(index, 1);
            }

            // Check if dead
            if (enemy.health <= 0) {
                this.enemies.splice(index, 1);
            }
        }

        /**
         * Private: Patrol enemy behavior
         */
        _updatePatrolEnemy(enemy) {
            // Simple patrol
            if (enemy.x < enemy.patrolStart) {
                enemy.velocityX = enemy.speed;
                enemy.direction = 1;
            } else if (enemy.x > enemy.patrolStart + enemy.patrolRange) {
                enemy.velocityX = -enemy.speed;
                enemy.direction = -1;
            }

            // Chase player if close
            if (this.player && Math.abs(this.player.x - enemy.x) < 150) {
                this._updateChaseEnemy(enemy);
            }
        }

        /**
         * Private: Chase enemy behavior
         */
        _updateChaseEnemy(enemy) {
            if (!this.player) return;

            const dx = this.player.x - enemy.x;
            if (Math.abs(dx) > 50) {
                enemy.velocityX = Math.sign(dx) * enemy.speed;
                enemy.direction = Math.sign(dx);
                enemy.state = 'walk';
            } else {
                enemy.velocityX = 0;
                enemy.state = 'attack';
            }
        }

        /**
         * Private: Ranged enemy behavior
         */
        _updateRangedEnemy(enemy) {
            if (!this.player) return;

            const dx = this.player.x - enemy.x;
            const distance = Math.abs(dx);

            if (distance < 200) {
                enemy.velocityX = 0;
                enemy.state = 'attack';

                if (enemy.attackCooldown <= 0) {
                    this._fireEnemyBullet(enemy);
                    enemy.attackCooldown = 30;
                }
            } else {
                enemy.velocityX = Math.sign(dx) * enemy.speed;
                enemy.direction = Math.sign(dx);
                enemy.state = 'walk';
            }

            if (enemy.attackCooldown > 0) {
                enemy.attackCooldown--;
            }
        }

        /**
         * Private: Resolve collisions
         */
        _resolveCollisions(entity) {
            entity.onGround = false;

            for (let platform of this.platforms) {
                if (this._checkCollision(entity, platform)) {
                    // Resolve collision
                    if (entity.velocityY > 0) {
                        entity.y = platform.y - entity.height;
                        entity.velocityY = 0;
                        entity.onGround = true;
                        entity.isJumping = false;
                    } else if (entity.velocityY < 0) {
                        entity.y = platform.y + platform.height;
                        entity.velocityY = 0;
                    }
                }
            }
        }

        /**
         * Private: Check AABB collision
         */
        _checkCollision(a, b) {
            return a.x < b.x + b.width &&
                   a.x + a.width > b.x &&
                   a.y < b.y + b.height &&
                   a.y + a.height > b.y;
        }

        /**
         * Private: Update bullets
         */
        _updateBullets() {
            this.bullets = this.bullets.filter(bullet => {
                bullet.x += bullet.vx;
                bullet.y += bullet.vy;
                bullet.life--;

                // Check collisions with enemies
                for (let i = this.enemies.length - 1; i >= 0; i--) {
                    if (this._checkCollision(bullet, this.enemies[i])) {
                        this.enemies[i].health -= bullet.damage;
                        bullet.life = -1;
                        break;
                    }
                }

                return bullet.life > 0;
            });
        }

        /**
         * Private: Update items
         */
        _updateItems() {
            this.items.forEach((item, index) => {
                if (item.collected) return;

                if (this.player && this._checkCollision(this.player, item)) {
                    this._collectItem(item);
                    item.collected = true;
                }
            });
        }

        /**
         * Private: Collect item
         */
        _collectItem(item) {
            switch(item.type) {
                case 'health':
                    this.player.health = Math.min(this.player.health + 1, this.player.maxHealth);
                    break;
                case 'ammo':
                    this.player.ammo += 10;
                    break;
                case 'weapon':
                    this.player.hasWeapon = true;
                    break;
            }
        }

        /**
         * Private: Update particles
         */
        _updateParticles() {
            this.particles = this.particles.filter(particle => {
                particle.x += particle.vx;
                particle.y += particle.vy;
                particle.vy += 0.1; // Gravity
                particle.life--;
                return particle.life > 0;
            });
        }

        /**
         * Private: Update camera
         */
        _updateCamera() {
            const p = this.player;
            const w = this.canvas.width;
            const h = this.canvas.height;

            // Follow player
            this.camera.x = p.x - w / 4;
            this.camera.y = Math.max(0, p.y - h / 3);

            // Clamp to world
            this.camera.x = Math.max(0, Math.min(this.camera.x, this.config.worldWidth - w));
            this.camera.y = Math.max(0, Math.min(this.camera.y, this.config.worldHeight - h));
        }

        /**
         * Private: Draw everything
         */
        _draw() {
            const ctx = this.ctx;

            // Clear canvas
            ctx.fillStyle = '#0a0e1a';
            ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

            // Apply camera translation
            ctx.save();
            ctx.translate(-this.camera.x, -this.camera.y);

            // Draw game objects
            this._drawPlatforms();
            this._drawItems();
            this._drawEnemies();
            if (this.player) {
                this._drawPlayer();
            }
            this._drawBullets();
            this._drawParticles();

            ctx.restore();

            // Draw UI
            this._drawUI();
        }

        /**
         * Private: Draw platforms
         */
        _drawPlatforms() {
            const ctx = this.ctx;
            ctx.fillStyle = '#00d9ff';
            ctx.globalAlpha = 0.3;
            this.platforms.forEach(p => {
                ctx.fillRect(p.x, p.y, p.width, p.height);
            });
            ctx.globalAlpha = 1;
        }

        /**
         * Private: Draw player
         */
        _drawPlayer() {
            if (!this.player || !this.drawer) return;
            this.drawer.drawAstronaut(
                this.player.x,
                this.player.y,
                this.player.name,
                this.player.state,
                {
                    width: this.player.width,
                    height: this.player.height,
                    direction: this.player.direction,
                    health: this.player.health,
                    maxHealth: this.player.maxHealth,
                    showHealthBar: true,
                    isHit: this.player.invincibleTimer > 0
                }
            );
        }

        /**
         * Private: Draw enemies
         */
        _drawEnemies() {
            if (!this.drawer) return;
            this.enemies.forEach(enemy => {
                this.drawer.drawAstronaut(
                    enemy.x,
                    enemy.y,
                    enemy.name,
                    enemy.state,
                    {
                        width: enemy.width,
                        height: enemy.height,
                        direction: enemy.direction,
                        health: enemy.health,
                        maxHealth: enemy.maxHealth,
                        showHealthBar: true
                    }
                );
            });
        }

        /**
         * Private: Draw items
         */
        _drawItems() {
            const ctx = this.ctx;
            this.items.forEach(item => {
                if (item.collected) return;
                ctx.fillStyle = item.type === 'health' ? '#00ff88' : '#ffeb3b';
                ctx.fillRect(item.x, item.y, item.width, item.height);
            });
        }

        /**
         * Private: Draw bullets
         */
        _drawBullets() {
            const ctx = this.ctx;
            ctx.fillStyle = '#ffeb3b';
            this.bullets.forEach(bullet => {
                ctx.beginPath();
                ctx.arc(bullet.x, bullet.y, 4, 0, Math.PI * 2);
                ctx.fill();
            });
        }

        /**
         * Private: Draw particles
         */
        _drawParticles() {
            const ctx = this.ctx;
            this.particles.forEach(particle => {
                ctx.globalAlpha = particle.life / particle.maxLife;
                ctx.fillStyle = particle.color || '#ffeb3b';
                ctx.beginPath();
                ctx.arc(particle.x, particle.y, particle.size || 2, 0, Math.PI * 2);
                ctx.fill();
            });
            ctx.globalAlpha = 1;
        }

        /**
         * Private: Draw UI
         */
        _drawUI() {
            // UI will be drawn separately by the stage
        }

        /**
         * Fire bullet from player
         */
        firePlayerBullet() {
            if (!this.player || !this.player.hasWeapon || this.player.ammo <= 0) return;

            this.player.ammo--;
            const bullet = {
                x: this.player.x + this.player.width / 2,
                y: this.player.y + this.player.height / 2,
                vx: this.player.direction * 8,
                vy: 0,
                life: 100,
                damage: 1
            };
            this.bullets.push(bullet);
        }

        /**
         * Fire bullet from enemy
         */
        _fireEnemyBullet(enemy) {
            const bullet = {
                x: enemy.x + enemy.width / 2,
                y: enemy.y + enemy.height / 2,
                vx: enemy.direction * 6,
                vy: 0,
                life: 100,
                damage: 1,
                isEnemyBullet: true
            };
            this.bullets.push(bullet);
        }

        /**
         * Damage player
         */
        damagePlayer(amount = 1) {
            if (this.player.invincibleTimer > 0) return; // Already invincible
            this.player.health -= amount;
            this.player.invincibleTimer = 30;

            if (this.player.health <= 0) {
                this.triggerGameOver();
            }
        }

        /**
         * Add particle effect
         */
        addParticle(x, y, vx = 0, vy = 0, color = '#ffeb3b') {
            this.particles.push({
                x, y, vx, vy,
                color,
                life: 30,
                maxLife: 30,
                size: 3
            });
        }

        /**
         * Private: Setup input listeners
         */
        _setupInput() {
            const keys = {};

            document.addEventListener('keydown', (e) => {
                keys[e.key] = true;
                this._updateInputState(keys);
            });

            document.addEventListener('keyup', (e) => {
                keys[e.key] = false;
                this._updateInputState(keys);
            });

            // Touch controls
            this.setupTouchControls();
        }

        /**
         * Private: Update input state
         */
        _updateInputState(keys) {
            this.input.left = keys['ArrowLeft'] || keys['a'] || keys['A'];
            this.input.right = keys['ArrowRight'] || keys['d'] || keys['D'];
            this.input.jump = keys[' '] || keys['w'] || keys['W'] || keys['ArrowUp'];
            this.input.shoot = keys['Enter'] || keys['Control'];
            this.input.action = keys['e'] || keys['E'];
        }

        /**
         * Setup touch controls for mobile
         */
        setupTouchControls() {
            // This will be implemented in the stage-specific code
        }

        /**
         * Private: Setup resize listener
         */
        _setupResizeListener() {
            window.addEventListener('resize', () => {
                this.canvas.width = window.innerWidth;
                this.canvas.height = window.innerHeight;
            });
        }
    }

    // Export
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = GameEngine;
    } else {
        global.GameEngine = GameEngine;
    }

})(typeof window !== 'undefined' ? window : global);
