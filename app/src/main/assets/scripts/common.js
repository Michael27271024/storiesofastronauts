// common.js - provides shared menu overlay, routes, game audio, and video cutscenes
(function(){
  const isStagePage = /\/stages\//i.test(window.location.pathname) || window.location.pathname.includes('/stages/');
  const isVideoPage = window.location.pathname.includes('video');
  const audioBase = isStagePage ? '../' : '';

  const themeMap = {
    menu: 'audio/music/menu_bgm_loop.wav',
    level_select: 'audio/music/level_select_loop.wav',
    stage1: 'audio/music/stage1_loop.wav',
    stage2: 'audio/music/stage2_loop.wav',
    stage3: 'audio/music/stage3_loop.wav',
    stage4: 'audio/music/stage4_loop.wav',
    stage5: 'audio/music/stage5_loop.wav',
    stage6: 'audio/music/stage6_loop.wav',
    stage7: 'audio/music/stage7_loop.wav',
    stage8: 'audio/music/stage8_loop.wav',
    stage9: 'audio/music/stage9_loop.wav',
    boss: 'audio/music/boss_theme_loop.wav',
    victory: 'audio/music/victory_fanfare.wav',
    game_over: 'audio/music/game_over_theme.wav',
    ambient: 'audio/music/ambient_space_loop.wav'
  };

  const sfxMap = {
    click: 'audio/sfx/sfx_ui_click.wav',
    select: 'audio/sfx/sfx_ui_select.wav',
    locked: 'audio/sfx/sfx_ui_locked.wav',
    hover: 'audio/sfx/sfx_button_hover.wav',
    shoot: 'audio/sfx/sfx_shoot.wav',
    hit: 'audio/sfx/sfx_hit.wav',
    explosion: 'audio/sfx/sfx_explosion.wav',
    jump: 'audio/sfx/sfx_jump.wav',
    footstep: 'audio/sfx/sfx_footstep_1.wav',
    battery: 'audio/sfx/sfx_pickup_battery.wav',
    gun: 'audio/sfx/sfx_pickup_gun.wav',
    part: 'audio/sfx/sfx_pickup_part.wav',
    powerup: 'audio/sfx/sfx_powerup.wav',
    boss_hit: 'audio/sfx/sfx_boss_hit.wav',
    rocket_launch: 'audio/sfx/sfx_rocket_launch.wav',
    ambient_beep: 'audio/sfx/sfx_ambient_beep.wav',
    enemy_die: 'audio/sfx/sfx_enemy_die.wav'
  };

  // نقشه ویدیوهای انتقال بین مراحل
    // Dynamic cutscenes map supporting stages 1 to 10
  const STAGE_CUTSCENES = {
    1: '../videos/moon_to_mars.mp4',
    2: '../videos/mars_to_saturn.mp4',
    3: '../videos/saturn_to_jupiter.mp4',
    4: '../videos/jupiter_to_mercury.mp4',
    5: '../videos/mercury_to_stage6.mp4',
    6: '../videos/stage6_to_stage7.mp4',
    7: '../videos/stage7_to_stage8.mp4',
    8: '../videos/stage8_to_stage9.mp4',
    9: '../videos/stage9_betrayal.mp4',
    10: '../videos/victory_ending.mp4'
  };

  function resolveAssetPath(path) {
    if (!path) return path;
    if (/^(https?:\/\/|data:|blob:|#)/i.test(path)) return path;
    const cleaned = path.replace(/^\.\/+/i, '').replace(/^\/+/, '');
    if (cleaned.startsWith('styles/') || cleaned.startsWith('scripts/') || cleaned.startsWith('audio/') || cleaned.startsWith('images/') || cleaned.startsWith('videos/')) {
      return `${audioBase}${cleaned}`;
    }
    if (cleaned.startsWith('../') || cleaned.startsWith('./')) return cleaned;
    return `${audioBase}${cleaned}`;
  }

  function ensureStylesheet(href, id) {
    if (document.getElementById(id)) return;
    const link = document.createElement('link');
    link.id = id;
    link.rel = 'stylesheet';
    link.href = resolveAssetPath(href);
    document.head.appendChild(link);
  }

  function createMusicElement() {
    const existing = document.getElementById('game-audio-music');
    if (existing) return existing;
    const musicEl = document.createElement('audio');
    musicEl.id = 'game-audio-music';
    musicEl.loop = true;
    musicEl.preload = 'auto';
    musicEl.volume = 0.45;
    document.body.appendChild(musicEl);
    return musicEl;
  }

  const audioState = { musicEl: null, currentTheme: null, unlocked: false, muted: false };

  function playPageTheme(themeKey, volume = 0.42) {
    if (!window.GameAudio) return;
    const resolvedTheme = themeKey || detectThemeKey();
    try {
      window.GameAudio.unlockAudio();
      window.GameAudio.playTheme(resolvedTheme, volume);
      window.setTimeout(() => {
        try { window.GameAudio.playTheme(resolvedTheme, volume); } catch (_) {}
      }, 250);
      window.setTimeout(() => {
        try { window.GameAudio.playTheme(resolvedTheme, volume); } catch (_) {}
      }, 700);
    } catch (_) {}
  }

  function unlockAudio() {
    if (audioState.unlocked) return true;
    const musicEl = document.getElementById('game-audio-music') || createMusicElement();
    if (musicEl) {
      try { musicEl.muted = audioState.muted; } catch (_) {}
    }
    audioState.unlocked = true;
    if (audioState.currentTheme) {
      try {
        if (audioState.currentTheme) {
          musicEl.src = audioState.currentTheme;
          musicEl.play().catch(() => {});
        }
      } catch (_) {}
    }
    return true;
  }

  window.GameAudio = {
    themes: themeMap,
    sfx: sfxMap,
    unlockAudio,
    playPageTheme,
    setMuted(muted) {
      audioState.muted = muted;
      const musicEl = audioState.musicEl || document.getElementById('game-audio-music');
      if (musicEl) {
        try { musicEl.muted = muted; } catch (_) {}
      }
      const toggle = document.getElementById('game-audio-toggle');
      if (toggle) toggle.textContent = muted ? '🔈' : '🔊';
    },
    toggleMute() {
      window.GameAudio.setMuted(!audioState.muted);
      return audioState.muted;
    },
    playTheme(themeKey, volume = 0.45) {
      const musicEl = createMusicElement();
      const themePath = themeMap[themeKey] || themeKey;
      if (!themePath) return;
      const resolved = resolveAssetPath(themePath);
      if (audioState.currentTheme === resolved && !musicEl.paused) {
        if (typeof volume === 'number') musicEl.volume = volume;
        return;
      }
      musicEl.src = resolved;
      musicEl.loop = true;
      musicEl.volume = volume;
      audioState.currentTheme = resolved;
      audioState.musicEl = musicEl;
      if (audioState.unlocked && !audioState.muted) {
        musicEl.play().catch(() => {});
      } else {
        musicEl.pause();
      }
    },
    stopTheme() {
      const musicEl = audioState.musicEl || document.getElementById('game-audio-music');
      if (!musicEl) return;
      musicEl.pause();
      musicEl.currentTime = 0;
      audioState.currentTheme = null;
    },
    playSfx(sfxKey, volume = 0.8) {
      const sfxPath = sfxMap[sfxKey] || sfxKey;
      if (!sfxPath || audioState.muted) return;
      const audioEl = new Audio(resolveAssetPath(sfxPath));
      audioEl.volume = volume;
      if (audioState.unlocked) {
        audioEl.play().catch(() => {});
      }
    },
    resolveAssetPath
  };

  /**
   * پخش ویدیوی انتقال بین مراحل (سفر بین سیارات)
   */
  window.playStageCutscene = function(currentStageNumber, nextStageUrl) {
    const videoSrc = STAGE_CUTSCENES[currentStageNumber];

    if (!videoSrc) {
      window.location.href = nextStageUrl;
      return;
    }

    if (window.GameAudio) window.GameAudio.stopTheme();

    const overlay = document.createElement('div');
    overlay.id = 'cutscene-overlay';
    overlay.style.cssText = `
      position: fixed;
      inset: 0;
      width: 100vw;
      height: 100vh;
      background-color: #000000;
      z-index: 99999;
      display: flex;
      align-items: center;
      justify-content: center;
    `;

    const video = document.createElement('video');
    video.src = videoSrc;
    video.autoplay = true;
    video.muted = true;
    video.playsInline = true;
    video.setAttribute('playsinline', '');
    video.style.cssText = `
      width: 100%;
      height: 100%;
      object-fit: cover;
      background: #000;
    `;

    overlay.appendChild(video);
    document.body.appendChild(overlay);

    function goToNext() {
      if (overlay.parentNode) {
        overlay.parentNode.removeChild(overlay);
      }
      window.location.href = nextStageUrl;
    }

    video.addEventListener('ended', goToNext);
    video.addEventListener('error', (e) => {
      console.warn("Cutscene video error:", e);
      goToNext();
    });

    video.play().catch(err => {
      console.warn("Autoplay blocked, skipping video:", err);
      goToNext();
    });
  };

  function detectThemeKey() {
    const pathname = window.location.pathname.toLowerCase();
    if (pathname.endsWith('/index.html') || pathname === '/' || pathname.endsWith('index.html')) return 'menu';
    if (pathname.endsWith('/level-select.html') || pathname.includes('level-select')) return 'level_select';
    if (pathname.includes('/stages/stage')) {
      const match = pathname.match(/stage(\d+)/i);
      if (match) return `stage${parseInt(match[1], 10)}`;
    }
    if (pathname.includes('video')) return 'ambient';
    return 'ambient';
  }

  function createMenu(){
    if(isVideoPage || document.getElementById('common-menu-overlay')) return;
    const overlay = document.createElement('div');
    overlay.id = 'common-menu-overlay';
    overlay.className = 'common-menu-overlay';
    overlay.style.display = 'none';
    overlay.innerHTML = `
      <div class="common-menu-card">
        <h3>منو بازی</h3>
        <p style="margin:0 0 0.7rem; color:#9db1ca;">کنترل سریع برای ادامه، ری‌استارت و بازگشت</p>
        <div class="common-menu-actions">
          <button id="menu-continue" class="common-btn primary">ادامه</button>
          <button id="menu-restart" class="common-btn secondary">ری‌استارت مرحله</button>
          <button id="menu-back" class="common-btn secondary">بازگشت به منو</button>
        </div>
      </div>
    `;
    document.body.appendChild(overlay);

    document.getElementById('menu-continue').addEventListener('click', ()=>{ overlay.style.display='none'; if(window.GameAudio){ window.GameAudio.unlockAudio(); window.GameAudio.playSfx('select', 0.7); } if(window.onMenuContinue) window.onMenuContinue(); });
    document.getElementById('menu-restart').addEventListener('click', ()=>{ overlay.style.display='none'; if(window.GameAudio){ window.GameAudio.unlockAudio(); window.GameAudio.playSfx('click', 0.7); } if(window.onMenuRestart) window.onMenuRestart(); });
    document.getElementById('menu-back').addEventListener('click', ()=>{ overlay.style.display='none'; if(window.GameAudio){ window.GameAudio.unlockAudio(); window.GameAudio.playSfx('click', 0.7); } if(window.onMenuBack) window.onMenuBack(); });
  }

  function createOpenButton(){
    if(isVideoPage || document.getElementById('common-menu-open')) return;
    const btn = document.createElement('button');
    btn.id = 'common-menu-open';
    btn.className = 'common-menu-open-btn';
    btn.innerHTML = '&#9776;';
    btn.setAttribute('aria-label', 'باز کردن منو');
    btn.addEventListener('click', ()=>{
      if(window.GameAudio){ window.GameAudio.unlockAudio(); window.GameAudio.playSfx('click', 0.7); }
      try{ if(window.onMenuOpen) window.onMenuOpen(); else { window.dispatchEvent(new Event('gamePause')); if(typeof gamePaused !== 'undefined') gamePaused = true; } }catch(e){}
      const o=document.getElementById('common-menu-overlay'); if(o) o.style.display='flex';
    });
    document.body.appendChild(btn);
  }

  function createAudioToggleButton(){
    if(isVideoPage || document.getElementById('game-audio-toggle')) return;
    const btn = document.createElement('button');
    btn.id = 'game-audio-toggle';
    btn.className = 'game-audio-toggle';
    btn.innerHTML = '🔊';
    btn.setAttribute('aria-label', 'تغییر صدای بازی');
    btn.addEventListener('click', ()=>{
      const muted = window.GameAudio.toggleMute();
      if (!muted) {
        window.GameAudio.unlockAudio();
        window.GameAudio.playSfx('click', 0.35);
      }
    });
    document.body.appendChild(btn);
  }

  function attachInteractionAudio(){
    document.addEventListener('pointerenter', (event) => {
      const target = event.target;
      if (target && target.closest && target.closest('button, a, .character-card, .level-card, .back-btn')) {
        if (window.GameAudio && !audioState.muted) window.GameAudio.playSfx('hover', 0.22);
      }
    }, { passive: true });

    document.addEventListener('click', (event) => {
      const target = event.target;
      if (!target || !target.closest) return;
      const interactive = target.closest('button, a, .character-card, .level-card, .back-btn');
      if (interactive) {
        if (window.GameAudio && !audioState.muted) {
          const isCard = target.closest('.character-card, .level-card');
          window.GameAudio.playSfx(isCard ? 'select' : 'click', 0.34);
        }
      }
    }, { passive: true });
  }

  function applyShell(){
    if (isVideoPage) return;
    document.body.classList.add('game-shell');
    const badge = document.createElement('div');
    badge.className = 'page-pill';
    badge.id = 'page-shell-badge';
    badge.innerHTML = '<span>🚀</span><span>نسخه موبایل‌فرندلی</span>';
    badge.style.position = 'fixed';
    badge.style.left = '50%';
    badge.style.bottom = '16px';
    badge.style.transform = 'translateX(-50%)';
    badge.style.zIndex = '9998';
    if (!document.getElementById('page-shell-badge')) document.body.appendChild(badge);
  }

  function createStageHud(){
    if (!isStagePage || isVideoPage || document.getElementById('stage-top-hud')) return;
    const match = window.location.pathname.match(/stage(\d+)/i);
    const stageNumber = match ? parseInt(match[1], 10) : null;
    const unlocked = parseInt(localStorage.getItem('unlockedLevel') || '1', 10) || 1;
    const hud = document.createElement('div');
    hud.id = 'stage-top-hud';
    hud.className = 'stage-top-hud';
    hud.innerHTML = `
      <span class="hud-pill">🚀 ${stageNumber ? `مرحله ${stageNumber}` : 'مرحله'}</span>
      <span class="hud-pill">پیشرفت ${Math.min(unlocked, 10)}/10</span>
      <button class="hud-btn" id="stage-hud-back" type="button">بازگشت</button>
      <button class="hud-btn" id="stage-hud-restart" type="button">ری‌استارت</button>
    `;
    document.body.appendChild(hud);

    const backBtn = document.getElementById('stage-hud-back');
    const restartBtn = document.getElementById('stage-hud-restart');
    if (backBtn) {
      backBtn.addEventListener('click', () => {
        if (window.GameAudio) window.GameAudio.playSfx('click', 0.55);
        window.location.href = '../level-select.html';
      });
    }
    if (restartBtn) {
      restartBtn.addEventListener('click', () => {
        if (window.GameAudio) window.GameAudio.playSfx('select', 0.55);
        window.location.reload();
      });
    }
  }

  function init(){
    if (isVideoPage) return;

    ensureStylesheet('styles/common.css', 'shared-common-css');
    ensureStylesheet('styles/modern-shell.css', 'shared-modern-shell-css');
    applyShell();
    createMenu();
    createOpenButton();
    createAudioToggleButton();
    attachInteractionAudio();
    createStageHud();

    ['pointerdown','touchstart','keydown','click'].forEach((eventName) => {
      window.addEventListener(eventName, () => {
        if (!audioState.unlocked) {
          unlockAudio();
          const themeKey = detectThemeKey();
          if (window.GameAudio && (window.GameAudio.themes[themeKey] || themeKey)) {
            window.GameAudio.playPageTheme(themeKey, 0.42);
          }
        }
      }, { once: true, passive: true });
    });

    const themeKey = detectThemeKey();
    if (window.GameAudio) {
      const themePath = window.GameAudio.themes[themeKey] || themeKey;
      if (themePath) {
        window.GameAudio.playPageTheme(themeKey, 0.42);
      }
    }

    ['pageshow', 'visibilitychange'].forEach((eventName) => {
      window.addEventListener(eventName, () => {
        if (document.visibilityState === 'hidden') return;
        const activeTheme = detectThemeKey();
        if (window.GameAudio && (window.GameAudio.themes[activeTheme] || activeTheme)) {
          window.GameAudio.playPageTheme(activeTheme, 0.42);
        }
      });
    });

    window.onMenuOpen = window.onMenuOpen || function(){ try{ window.dispatchEvent(new Event('gamePause')); if(typeof gamePaused !== 'undefined') gamePaused = true; }catch(e){} };
    window.onMenuContinue = window.onMenuContinue || function(){ try{ const resumeEvent = new Event('gameResume'); window.dispatchEvent(resumeEvent); if(typeof gamePaused !== 'undefined') gamePaused = false; }catch(e){} };
    window.onMenuRestart = window.onMenuRestart || function(){ window.location.reload(); };

    window.onMenuBack = window.onMenuBack || function(){
      const target = isStagePage ? '../level-select.html' : 'level-select.html';
      window.location.href = target;
    };
  }

  if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
