/*
  audio-manager.js
  Simple, robust audio manager for the WebView-hosted game.
  - Looks for audio files under ./audio/ by filename (e.g. audio/menu_bgm.mp3)
  - Supports: playMusic(looping, fade), stopMusic(fade), playSfx(one-shot), global volumes, mute, and persistence of volumes.
  - Safe no-op behavior if files are missing or autoplay blocked.

  Usage (global):
    window.AM.init({ assets: { music: {...}, sfx: {...} }, defaultMusic: 'menu_bgm' });
    window.AM.playMusic('menu_bgm');
    window.AM.playSfx('jump');
    window.AM.setMusicVolume(0.7);
    window.AM.setSfxVolume(0.9);

  Note: Place your audio files in `app/src/main/assets/audio/` and use the filenames referenced below.
*/
(function(global){
  function safeGet(obj, key, fallback){ try { return obj[key]; } catch(e){ return fallback; } }

  class AudioWrapper {
    constructor(src, opts = {}){
      this.src = src;
      this.loop = !!opts.loop;
      this.element = new Audio();
      this.element.src = src;
      this.element.preload = 'auto';
      this.element.loop = this.loop;
      this.element.volume = safeGet(opts,'volume',1);
      this.element.muted = true; // start muted to satisfy autoplay, unmute after start
      this.isPlaying = false;
      this._fadeInterval = null;
    }
    play(){
      this.cancelFade();
      try{ this.element.load(); }catch(e){}
      const p = this.element.play();
      if(p && p.then) p.then(()=>{
        this.isPlaying = true;
        // unmute shortly after playback begins
        setTimeout(() => { try{ this.element.muted = false; this.element.volume = this.element.volume || 1; }catch(e){} }, 150);
      }).catch(()=>{ /* autoplay likely blocked, will play on user gesture */ });
    }
    pause(){ this.element.pause(); this.isPlaying = false; }
    stop(){ this.pause(); try{ this.element.currentTime = 0; }catch(e){} }
    setVolume(v){ this.element.volume = Math.max(0, Math.min(1, v)); }
    fadeTo(target, ms=400){
      this.cancelFade();
      const start = this.element.volume;
      const steps = Math.max(6, Math.floor(ms / 40));
      let i = 0;
      this._fadeInterval = setInterval(()=>{
        i++;
        const t = i / steps;
        const v = start + (target - start) * t;
        this.setVolume(v);
        if(i >= steps){ this.cancelFade(); if(target === 0) this.pause(); }
      }, Math.max(20, Math.floor(ms / steps)));
    }
    cancelFade(){ if(this._fadeInterval){ clearInterval(this._fadeInterval); this._fadeInterval = null; } }
  }

  class AudioManager {
    constructor(){
      this.music = {}; // name -> AudioWrapper
      this.sfx = {};
      this.currentMusic = null;
      this._playlist = [];
      this._playlistIndex = 0;
      this._playlistTimer = null;
      this._audibleWatchers = [];
      // Force full-volume playback (no user controls)
      this.musicVolume = 1.0;
      this.sfxVolume = 1.0;
      this.preloadList = [];
      this.userGestureAttached = false;
      this._ensureUserGesture();
      // Preferences and mute controls intentionally removed per project settings
    }

    _attachStartEvents(fn){
      const once = () => { try{ fn(); }catch(e){} events.forEach(ev => document.removeEventListener(ev, once)); };
      const events = ['click','touchstart','pointerdown','keydown','mousemove'];
      events.forEach(ev => document.addEventListener(ev, once, { once:true, passive:true }));
    }

    _attachRepeatingStart(fn, until){
      const handler = () => {
        try{
          if(!until || !until()) fn();
          if(until && until()) {
            events.forEach(ev => document.removeEventListener(ev, handler));
          }
        }catch(e){}
      };
      const events = ['click','touchstart','pointerdown','keydown','mousemove'];
      events.forEach(ev => document.addEventListener(ev, handler, { passive:true }));
      this._audibleWatchers.push({ events, handler });
    }

    _ensureUserGesture(){
      // mobile webviews often allow programmatic audio; still, resume on first touch/click
      const resume = ()=>{ try{ for(const k in this.music) { const m = this.music[k]; if(m && !m.isPlaying && m.element.paused) { /* no-op */ } } }catch(e){} document.removeEventListener('touchstart', resume); document.removeEventListener('click', resume); };
      document.addEventListener('touchstart', resume, { once: true });
      document.addEventListener('click', resume, { once: true });
    }

  // Preferences storage removed - audio volume is fixed at maximum and cannot be changed

    init(opts = {}){
      // opts.assets: { music: {name: 'file.mp3', ...}, sfx: {name:'file.wav' }}
      const assets = opts.assets || {};
      const music = assets.music || {};
      const sfx = assets.sfx || {};
      for(const k in music){ this._createMusic(k, music[k]); }
      for(const k in sfx){ this._createSfx(k, sfx[k]); }
      // volumes are fixed at 1.0 (no-op setters)
      if(opts.autoplay && opts.defaultMusic) this.playMusic(opts.defaultMusic);
    }

    _createMusic(name, filename){
      try{
        const path = `audio/${filename}`;
        this.music[name] = new AudioWrapper(path, { loop: true, volume: this.musicVolume });
      }catch(e){ console.warn('audio:createMusic', e); }
    }

    _createSfx(name, filename){
      try{
        const path = `audio/${filename}`;
        this.sfx[name] = filename; // store filename, create element per-play for multiple overlapping
      }catch(e){ console.warn('audio:createSfx', e); }
    }

    playMusic(name, { fade = true, fadeMs = 400 } = {}){
      if(!this.music[name]) return;
      const sameTrack = this.currentMusic === name;
      // If same track but not actually playing (e.g., autoplay blocked), retry
      if(sameTrack && this.music[name].isPlaying) return;
      const next = this.music[name];
      if(this.currentMusic && this.music[this.currentMusic]){
        const prev = this.music[this.currentMusic];
        if(fade) prev.fadeTo(0, fadeMs); else prev.stop();
      }
      next.setVolume(0);
      next.element.loop = true;
      next.play();
      try{ next.element.muted = false; }catch(e){}
      if(fade) next.fadeTo(this.musicVolume, fadeMs); else next.setVolume(this.musicVolume);
      // If play was blocked, retry once on next user gesture
      const retry = () => {
        try{
          if(next.element.paused){
            next.play();
            next.element.muted = false;
            next.setVolume(this.musicVolume);
          }
        }catch(e){}
        document.removeEventListener('click', retry);
        document.removeEventListener('touchstart', retry);
      };
      document.addEventListener('click', retry, { once: true });
      document.addEventListener('touchstart', retry, { once: true, passive: true });
      this.currentMusic = name;
    }

    isMusicAudible(){
      try{
        if(!this.currentMusic) return false;
        const m = this.music[this.currentMusic];
        if(!m) return false;
        return !m.element.paused && m.element.currentTime > 0;
      }catch(e){ return false; }
    }

    forcePlay(names = [], rotateMs = 60000, maxAttempts = 10){
      try{
        const list = (names || []).filter(n => !!this.music[n]);
        if(list.length === 0) return;
        let attempts = 0;
        const kick = () => {
          attempts++;
          this.startPlaylist(list, rotateMs);
          if(this.isMusicAudible() || attempts >= maxAttempts){
            clearInterval(timer);
            return;
          }
        };
        kick();
        const timer = setInterval(kick, 2500);
        this._attachRepeatingStart(() => this.startPlaylist(list, rotateMs), () => this.isMusicAudible());
      }catch(e){ console.warn('audio:forcePlay', e); }
    }

    ensurePlayback(names = [], { rotateMs = 60000, intervalMs = 1500, durationMs = 45000 } = {}){
      try{
        const list = (names || []).filter(n => !!this.music[n]);
        if(list.length === 0) return;
        let elapsed = 0;
        const tick = () => {
          if(this.isMusicAudible() || elapsed >= durationMs){
            clearInterval(timer);
            return;
          }
          this.forcePlay(list, rotateMs, 3);
          // also force direct play of first track as a fallback
          const first = list[0];
          if(first) this.playMusic(first, { fade: true, fadeMs: 400 });
          elapsed += intervalMs;
        };
        tick();
        const timer = setInterval(tick, intervalMs);
        this._attachRepeatingStart(() => this.forcePlay(list, rotateMs, 3), () => this.isMusicAudible());
      }catch(e){ console.warn('audio:ensurePlayback', e); }
    }

    stopMusic({ fade = true, fadeMs = 400 } = {}){
      this.stopPlaylist();
      if(!this.currentMusic) return;
      const c = this.music[this.currentMusic];
      if(!c) return;
      if(fade) c.fadeTo(0, fadeMs); else c.stop();
      this.currentMusic = null;
    }

    startPlaylist(names = [], rotateMs = 65000){
      try{
        this.stopPlaylist();
        const valid = (names || []).filter(n => !!this.music[n]);
        if(valid.length === 0) return;
        this._playlist = valid;
        this._playlistIndex = 0;
        const playNext = () => {
          if(this._playlist.length === 0) return;
          const nextName = this._playlist[this._playlistIndex % this._playlist.length];
          this._playlistIndex = (this._playlistIndex + 1) % this._playlist.length;
          this.playMusic(nextName, { fade: true, fadeMs: 600 });
        };
        playNext();
        this._playlistTimer = setInterval(playNext, Math.max(20000, rotateMs || 65000));
      }catch(e){ console.warn('audio:startPlaylist', e); }
    }

    stopPlaylist(){
      if(this._playlistTimer){
        clearInterval(this._playlistTimer);
        this._playlistTimer = null;
      }
      this._playlist = [];
    }

    playSfx(name){
      // play at fixed sfx volume
      const filename = this.sfx[name];
      if(!filename) return;
      try{
        const a = new Audio();
        a.src = `audio/${filename}`;
        a.preload = 'auto';
        a.volume = this.sfxVolume;
        // allow multiple overlapping effects
        const p = a.play();
        if(p && p.catch) p.catch(()=>{});
      }catch(e){ console.warn('audio:playSfx', e); }
    }
    // Volume setters are intentionally no-ops to keep audio at full volume
    setMusicVolume(v){ /* no-op - volumes are fixed at 1.0 */ }
    setSfxVolume(v){ /* no-op - volumes are fixed at 1.0 */ }
    toggleMute(){ /* no-op - mute disabled */ }
    isMuted(){ return false; }
  }

  // Export singleton
  const singleton = new AudioManager();
  global.AM = singleton;

  /**
   * Convenience helper for stages to register music/SFX and auto-start the default loop
   * after the first user gesture (to satisfy mobile autoplay rules).
   * Usage:
   *   const stageAudio = useStageAudio({ music:{bg:'stage1_loop.wav'}, sfx:{shoot:'sfx_shoot.wav'}, defaultMusic:'bg' });
   *   // later: AM.playSfx('shoot'); stageAudio.stop();
   */
  global.useStageAudio = function(config = {}){
    try{
      if(!global.AM) return { start: ()=>{}, stop: ()=>{} };
      const music = config.music || {};
      const sfx = config.sfx || {};
      const playlist = Array.isArray(config.playlist) ? config.playlist.filter(n => !!music[n]) : [];
      const defaultMusic = config.defaultMusic || playlist[0];
      const autoplay = !!config.autoplay;
      const rotateMs = config.rotateMs || 65000;
      global.AM.init({ assets:{ music, sfx }, autoplay: autoplay && !!defaultMusic, defaultMusic });
      let retryTimer = null;

      const startWithPlaylist = () => {
        try{
          if(playlist.length > 0){
            const list = [...playlist];
            if(defaultMusic && !list.includes(defaultMusic)) list.unshift(defaultMusic);
            global.AM.startPlaylist(list, rotateMs);
          } else if (defaultMusic){
            global.AM.playMusic(defaultMusic);
          }
        }catch(e){ console.warn('useStageAudio:startWithPlaylist', e); }
      };

      const ensureAudible = () => {
        let attempts = 0;
        if(retryTimer) clearInterval(retryTimer);
        retryTimer = setInterval(() => {
          attempts++;
          if(global.AM.isMusicAudible() || attempts > 8){
            clearInterval(retryTimer);
            retryTimer = null;
            return;
          }
          startWithPlaylist();
        }, 4000);
      };

      const hasMusicToPlay = (defaultMusic || playlist.length);
      const autoAttempt = () => {
        try{
          startWithPlaylist();
          const list = playlist.length ? playlist : (defaultMusic ? [defaultMusic] : []);
          if(list.length){
            global.AM.forcePlay(list, rotateMs, 12);
            global.AM.ensurePlayback(list, { rotateMs, intervalMs: 1500, durationMs: 60000 });
          }
          ensureAudible();
        }catch(e){}
      };

      if (hasMusicToPlay && !autoplay) {
        const resume = () => { startWithPlaylist(); ensureAudible(); document.removeEventListener('touchstart', resume); document.removeEventListener('click', resume); };
        document.addEventListener('touchstart', resume, { once: true });
        document.addEventListener('click', resume, { once: true });
        setTimeout(autoAttempt, 800); // fallback attempt; will be ignored if browser blocks
        document.addEventListener('visibilitychange', function once(){
          if(!document.hidden){ autoAttempt(); document.removeEventListener('visibilitychange', once); }
        });
        this._attachStartEvents(startWithPlaylist);
        this._attachRepeatingStart(startWithPlaylist, () => global.AM.isMusicAudible());
      } else if (hasMusicToPlay && autoplay) {
        autoAttempt();
        this._attachStartEvents(startWithPlaylist);
        this._attachRepeatingStart(startWithPlaylist, () => global.AM.isMusicAudible());
      }

      return {
        start: startWithPlaylist,
        stop: () => { try{ global.AM.stopPlaylist(); global.AM.stopMusic(); }catch(e){} }
      };
    } catch (e) {
      console.warn('useStageAudio failed', e);
      return { start: ()=>{}, stop: ()=>{} };
    }
  };

})(window);
