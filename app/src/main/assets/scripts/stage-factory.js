/**
 * Stage Factory - Generates all refactored game stages
 * Use this to create stages 3-10 with consistent structure
 */

const STAGES_CONFIG = {
    3: {
        name: 'محطه ۳: مریخ',
        title: 'مرحله ۳: سپاهی مریخ',
        story: '🔴 سرخ سیاره!\n\nلشکر فضانوردان مریخی ما را تهاجم کرده!\nتمام دشمنان را شکست بده و سیاره را نجات بده!',
        worldWidth: 2800,
        enemyCount: 6,
        difficulty: 'medium'
    },
    4: {
        name: 'محطه ۴: اریس',
        title: 'مرحله ۴: کهکشان اریس',
        story: '⚡ کهکشان باطل!\n\nدشمنان تاریک در اریس منتظر هستند.\nبرای پیروزی، تمام دشمنان را شکست بده!',
        worldWidth: 3200,
        enemyCount: 7,
        difficulty: 'hard'
    },
    5: {
        name: 'محطه ۵: ستاره سوار',
        title: 'مرحله ۵: ستاره سوار',
        story: '⭐ در میان ستارگان!\n\nآخرین مستعمره ایران در خطر!\nتمام فضانوردان دشمن را شکست بده!',
        worldWidth: 3400,
        enemyCount: 8,
        difficulty: 'very-hard'
    },
    6: {
        name: 'محطه ۶: فناتوس',
        title: 'مرحله ۶: سیاره فناتوس',
        story: '🌙 سیاره مرموز!\n\nتاریک‌ترین سیاره‌ای که تا کنون دیده‌ام.\nدشمنان قوی‌تری در انتظار هستند!',
        worldWidth: 3600,
        enemyCount: 9,
        difficulty: 'insane'
    },
    7: {
        name: 'محطه ۷: عملیات جوخه فضایی',
        title: 'مرحله ۷: عملیات جوخه',
        story: '🎯 تیراندازی دقیق!\n\nفضانوردان دشمن را با دقت شکست بده.\nهر شلیک شمار می‌رود!',
        worldWidth: 2600,
        enemyCount: 5,
        difficulty: 'expert'
    },
    8: {
        name: 'محطه ۸: پایگاه تاریک',
        title: 'مرحله ۸: پایگاه تاریک',
        story: '🌑 تاریکی مطلق!\n\nپایگاه سری دشمن کشف شد.\nتمام دفاعات را از بین بر!',
        worldWidth: 3800,
        enemyCount: 10,
        difficulty: 'nightmare'
    },
    9: {
        name: 'محطه ۹: دوستی ابدی',
        title: 'مرحله ۹: دوستی ابدی',
        story: '💫 پایانی نزدیک!\n\nدرخت‌های دوستی حتی در جنگ برقرار است.\nآخرین دشمنان را شکست بده!',
        worldWidth: 3200,
        enemyCount: 6,
        difficulty: 'final'
    },
    10: {
        name: 'محطه ۱۰: بازگشت به خانه',
        title: 'مرحله ۱۰: بازگشت به خانه',
        story: '🏠 بازگشت!\n\nآخرین مرحله سفر.\nتمام دشمنان را شکست بده و به خانه برگرد!',
        worldWidth: 4000,
        enemyCount: 12,
        difficulty: 'ultimate'
    }
};

/**
 * Generate stage HTML
 */
function generateStageHTML(stageNumber) {
    const config = STAGES_CONFIG[stageNumber];
    if (!config) return null;

    const nextStage = stageNumber < 10 ? stageNumber + 1 : 'level-select';
    const nextHref = stageNumber < 10 ? `stage${nextStage}.html` : '../level-select.html';

    const html = `<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
    <title>${config.title}</title>
    <link rel="stylesheet" href="../styles/professional-theme.css">
    <link rel="stylesheet" href="stage-style.css">
</head>
<body>
<div class="stage-container">
    <canvas id="gameCanvas" class="stage-canvas"></canvas>
    <div class="stage-ui">
        <div class="ui-panel ui-top-left">
            <div class="ui-row"><span class="ui-label">مرحله:</span><span class="ui-value">${stageNumber}</span></div>
            <div class="ui-row"><span class="ui-label">سلامتی:</span><span class="ui-value" id="ui-health">❤️❤️❤️</span></div>
            <div class="ui-row"><span class="ui-label">دشمنان:</span><span class="ui-value" id="ui-enemies">0</span></div>
        </div>
    </div>
    <div id="dialogBackdrop" class="dialog-backdrop">
        <div class="dialog-box">
            <div class="dialog-title" id="dialogTitle">عنوان</div>
            <div class="dialog-content" id="dialogContent">محتوا</div>
            <div class="dialog-buttons" id="dialogButtons"></div>
        </div>
    </div>
    <div class="controls-panel">
        <div class="control-group">
            <button class="control-btn" id="btnLeft">◄</button>
            <button class="control-btn" id="btnRight">►</button>
            <button class="control-btn" id="btnJump" style="width: 60px; height: 60px;">△</button>
        </div>
        <div class="control-group">
            <button class="control-btn" id="btnShoot">●</button>
            <button class="control-btn" id="btnPause">⚙</button>
        </div>
    </div>
</div>

<script src="../scripts/drawing-system.js"></script>
<script src="../scripts/game-engine.js"></script>
<script src="../audio-manager.js"></script>
<script>
    'use strict';

    const STAGE_CONFIG = {
        stageNumber: ${stageNumber},
        stageName: '${config.name}',
        storyText: '${config.story}',
        worldWidth: ${config.worldWidth},
        worldHeight: 800
    };

    let gameEngine = null, drawingSystem = null, gameRunning = false;
    const canvas = document.getElementById('gameCanvas');
    const ctx = canvas.getContext('2d');

    function initialize() {
        resizeCanvas();
        drawingSystem = DrawingSystem.create({ width: canvas.width, height: canvas.height, ctx: ctx });
        gameEngine = GameEngine.create(canvas, { worldWidth: STAGE_CONFIG.worldWidth, worldHeight: STAGE_CONFIG.worldHeight, gravity: 0.4 });
        gameEngine.setDrawer(drawingSystem);
        gameEngine.onUpdate = updateUI;
        gameEngine.onGameOver = () => showDialog('❌ شکست!', 'دوباره تلاش کن', ['باشه'], () => location.reload());
        gameEngine.onWin = () => showDialog('✅ برنده!', 'مرحله بعدی', ['باشه'], () => window.location.href = '${nextHref}');

        const playerName = sessionStorage.getItem('selectedCharacter') || 'کوروش';
        gameEngine.addPlayer({
            x: 100, y: 0, width: 50, height: 70,
            health: 3, maxHealth: 3, name: playerName,
            speed: 4, jumpPower: -12, hasWeapon: true, ammo: 50
        });

        setupWorld();
        setupControls();
        showDialog('🎮 ' + STAGE_CONFIG.stageName, STAGE_CONFIG.storyText, ['شروع'], () => {
            closeDialog();
            gameEngine.start();
            gameRunning = true;
        });
    }

    function setupWorld() {
        gameEngine.addPlatform(0, canvas.height - 60, STAGE_CONFIG.worldWidth, 60);

        // Dynamic platform generation based on world width
        const platformCount = Math.floor(STAGE_CONFIG.worldWidth / 500);
        for (let i = 0; i < platformCount; i++) {
            const x = i * 500 + Math.random() * 200;
            const y = canvas.height - 80 - Math.random() * 150;
            const width = 150 + Math.random() * 150;
            gameEngine.addPlatform(x, y, width, 30);
        }

        // Dynamic enemy generation based on difficulty
        const characterNames = ['کوروش', 'داریوش', 'سهراب', 'رستم'];
        const aiTypes = ['patrol', 'chase', 'ranged'];
        
        for (let i = 0; i < ${config.enemyCount}; i++) {
            const x = 300 + (i * (STAGE_CONFIG.worldWidth - 400) / ${config.enemyCount});
            const character = characterNames[i % characterNames.length];
            const aiType = aiTypes[i % aiTypes.length];
            const health = Math.min(2 + Math.floor(i / 3), 5);
            
            gameEngine.addEnemy({
                x, y: 0, name: character,
                health, aiType,
                patrolRange: 250 + (i * 50),
                speed: 1.5 + (i * 0.3)
            });
        }

        // Random items
        for (let i = 0; i < 3; i++) {
            const x = 300 + Math.random() * (STAGE_CONFIG.worldWidth - 600);
            const type = Math.random() > 0.5 ? 'health' : 'ammo';
            gameEngine.addItem(x, canvas.height - 150 - Math.random() * 200, type);
        }
    }

    function showDialog(title, content, buttons, onComplete) {
        document.getElementById('dialogTitle').textContent = title;
        document.getElementById('dialogContent').textContent = content;
        const buttonsDiv = document.getElementById('dialogButtons');
        buttonsDiv.innerHTML = '<button class="btn btn-primary">باشه</button>';
        buttonsDiv.firstChild.onclick = () => { closeDialog(); if (onComplete) onComplete(); };
        document.getElementById('dialogBackdrop').classList.add('active');
        if (gameRunning) gameEngine.pause();
    }

    function closeDialog() {
        document.getElementById('dialogBackdrop').classList.remove('active');
        if (gameRunning) gameEngine.resume();
    }

    function updateUI() {
        if (!gameEngine.player) return;
        document.getElementById('ui-health').textContent = '❤️'.repeat(gameEngine.player.health) + '🖤'.repeat(Math.max(0, gameEngine.player.maxHealth - gameEngine.player.health));
        document.getElementById('ui-enemies').textContent = gameEngine.enemies.length;
        if (gameEngine.enemies.length === 0 && gameRunning) {
            gameRunning = false;
            setTimeout(() => gameEngine.triggerWin(), 500);
        }
    }

    function setupControls() {
        const setupBtn = (btn, onDown, onUp) => {
            btn.addEventListener('pointerdown', onDown);
            btn.addEventListener('pointerup', onUp || (() => {}));
            btn.addEventListener('pointerleave', onUp || (() => {}));
        };

        setupBtn(document.getElementById('btnLeft'), () => { gameEngine.input.left = true; gameEngine.input.right = false; }, () => gameEngine.input.left = false);
        setupBtn(document.getElementById('btnRight'), () => { gameEngine.input.right = true; gameEngine.input.left = false; }, () => gameEngine.input.right = false);
        setupBtn(document.getElementById('btnJump'), () => gameEngine.input.jump = true, () => gameEngine.input.jump = false);
        setupBtn(document.getElementById('btnShoot'), () => gameEngine.firePlayerBullet());
        document.getElementById('btnPause').addEventListener('click', () => {
            if (gameEngine.gameState === 'PLAYING') {
                gameEngine.pause();
                showDialog('⏸ توقف', 'بازی متوقف', ['ادامه'], () => gameEngine.resume());
            }
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowLeft') gameEngine.input.left = true;
            if (e.key === 'ArrowRight') gameEngine.input.right = true;
            if (e.key === ' ') gameEngine.input.jump = true;
            if (e.key === 'Enter') gameEngine.firePlayerBullet();
            if (e.key === 'Escape') document.getElementById('btnPause').click();
        });

        document.addEventListener('keyup', (e) => {
            if (e.key === 'ArrowLeft') gameEngine.input.left = false;
            if (e.key === 'ArrowRight') gameEngine.input.right = false;
            if (e.key === ' ') gameEngine.input.jump = false;
        });
    }

    function resizeCanvas() {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
        if (drawingSystem) drawingSystem.resize(canvas.width, canvas.height);
    }

    window.addEventListener('DOMContentLoaded', initialize);
    window.addEventListener('resize', resizeCanvas);
</script>
</body>
</html>`;

    return html;
}

// Export for Node.js or use in browser
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { generateStageHTML, STAGES_CONFIG };
}
