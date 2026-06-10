// stage-audio-overlay.js
// Lightweight, opt-in audio bootstrap for stages that don't have bespoke audio wiring.
(function(){
  const match = (location.pathname.match(/stage(\d+)\.html/i) || [])[1];
  const stageNumber = match ? parseInt(match, 10) : null;
  if (!stageNumber || stageNumber === 3 || stageNumber === 9) return; // custom audio already present
  if (!window.useStageAudio) return;

  const stageMusic = {
    1: {
      tracks: {
        stage1: 'stage1_loop.wav',
        ambient: 'ambient_space_loop.wav',
        select: 'level_select_loop.wav',
        menu: 'menu_bgm_loop.wav'
      },
      playlist: ['stage1', 'ambient', 'menu', 'select']
    },
    2: {
      tracks: {
        stage2: 'stage2_loop.wav',
        ambient: 'ambient_space_loop.wav',
        select: 'level_select_loop.wav',
        menu: 'menu_bgm_loop.wav'
      },
      playlist: ['stage2', 'ambient', 'select', 'menu']
    },
    4: {
      tracks: {
        stage4: 'stage4_loop.wav',
        ambient: 'ambient_space_loop.wav',
        boss: 'boss_theme_loop.wav'
      },
      playlist: ['stage4', 'ambient', 'boss']
    },
    5: {
      tracks: {
        stage5: 'stage5_loop.wav',
        ambient: 'ambient_space_loop.wav',
        select: 'level_select_loop.wav'
      },
      playlist: ['stage5', 'ambient', 'select']
    },
    6: {
      tracks: {
        stage6: 'stage6_loop.wav',
        ambient: 'ambient_space_loop.wav',
        stage1: 'stage1_loop.wav'
      },
      playlist: ['stage6', 'ambient', 'stage1']
    },
    7: {
      tracks: {
        stage7: 'stage7_loop.wav',
        ambient: 'ambient_space_loop.wav',
        boss: 'boss_theme_loop.wav'
      },
      playlist: ['stage7', 'boss', 'ambient']
    },
    8: {
      tracks: {
        stage8: 'stage8_loop.wav',
        ambient: 'ambient_space_loop.wav',
        select: 'level_select_loop.wav'
      },
      playlist: ['stage8', 'ambient', 'select']
    }
  };

  const music = {};
  const config = stageMusic[stageNumber] || {};
  const tracks = config.tracks || {};
  const playlist = (config.playlist || Object.keys(tracks)).filter(Boolean);
  const defaultMusic = playlist[0];
  for(const key in tracks){ music[key] = tracks[key]; }
  music.game_over = 'game_over_theme.wav';
  music.victory = 'victory_fanfare.wav';

  const sfx = {
    shoot: 'sfx_shoot.wav',
    jump: 'sfx_jump.wav',
    ui_click: 'sfx_ui_click.wav',
    ui_hover: 'sfx_button_hover.wav'
  };

  const stageAudio = useStageAudio({ music, sfx, defaultMusic, playlist, rotateMs: 60000, autoplay: true });
  const playSfx = (key) => { try { window.AM && window.AM.playSfx(key); } catch (e) {} };

  function wire(id, key) {
    const el = document.getElementById(id);
    if (!el) return;
    ['click','touchstart'].forEach(evt => el.addEventListener(evt, () => playSfx(key), { passive: true }));
  }

  // Controls + UI buttons
  wire('shoot-btn', 'shoot');
  wire('jump-btn', 'jump');
  wire('dialogue-next-btn', 'ui_click');
  wire('message-ok-btn', 'ui_click');
  wire('next-button', 'ui_click');
  document.querySelectorAll('.dialogue-btn, .message-btn, .menu-button, .next-btn').forEach(btn => {
    ['click','touchstart'].forEach(evt => btn.addEventListener(evt, () => playSfx('ui_click'), { passive: true }));
    ['mouseenter'].forEach(evt => btn.addEventListener(evt, () => playSfx('ui_hover')));
  });

  // Kick off music on first meaningful interaction as a safety net.
  const startOnce = () => { try{ stageAudio.start && stageAudio.start(); }catch(e){} };
  ['touchstart','click','pointerdown','keydown','mousemove'].forEach(evt => {
    document.addEventListener(evt, startOnce, { once: true, passive: true });
  });
  window.addEventListener('load', startOnce, { once: true });
  setTimeout(startOnce, 800); // fallback attempt
})(); 
