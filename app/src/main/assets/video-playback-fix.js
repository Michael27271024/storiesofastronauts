// video-playback-fix.js
// Robust video helpers + simple localStorage level-unlock API.

export function attachVideoHandlers(opts = {}){
  const videoSelector = opts.videoSelector || '#video-player';
  const overlaySelector = opts.overlaySelector || null;
  const redirectTo = opts.redirectTo || 'level-select.html';
  const currentLevel = typeof opts.currentLevel !== 'undefined' ? opts.currentLevel : null;

  const video = document.querySelector(videoSelector);
  const overlay = overlaySelector ? document.querySelector(overlaySelector) : null;
  if(!video) console.warn('video-playback-fix: video element not found for', videoSelector);

  let endTimeout = null;
  let lastTime = 0;
  let endedHandled = false;
  let startTimeout = null;

  function handleEnded(){
    if(endedHandled) return;
    endedHandled = true;
    setTimeout(()=>{
      try{ if(currentLevel !== null) unlockNext(currentLevel); }catch(e){}
      try{ if(currentLevel !== null){ const next = Number(currentLevel)+1; localStorage.setItem('unlockedLevel', String(next)); } }catch(e){}
      try{ window.location.href = redirectTo; }catch(e){ console.error(e); }
    }, 200);
  }

  function handleError(){ handleEnded(); }

  function handleTimeUpdate(){ if(!video || !video.duration) return; const remain = video.duration - video.currentTime; if(remain > 0 && remain < 0.3) handleEnded(); lastTime = video.currentTime; }

  function handleVisibility(){ if(document.hidden && video && !video.paused){ try{ video.pause(); }catch(e){} } }

  function attach(){
    if(!video) return;
    endedHandled = false;
    video.addEventListener('ended', handleEnded);
    video.addEventListener('error', handleError);
    video.addEventListener('timeupdate', handleTimeUpdate);
    document.addEventListener('visibilitychange', handleVisibility);
    clearTimeout(endTimeout);
    endTimeout = setTimeout(()=>{
      if(video && !video.ended){ if(Math.abs((video.currentTime||0)-lastTime) < 0.01){ handleEnded(); } }
    }, 8000);
  }

  function detach(){
    if(!video) return;
    video.removeEventListener('ended', handleEnded);
    video.removeEventListener('error', handleError);
    video.removeEventListener('timeupdate', handleTimeUpdate);
    document.removeEventListener('visibilitychange', handleVisibility);
    clearTimeout(endTimeout); clearTimeout(startTimeout);
    try{ if(video) video.style.visibility = 'hidden'; }catch(e){}
    try{ if(overlay) overlay.style.display = 'none'; }catch(e){}
  }

  function ensurePlayButton(){ if(!overlay) return; try{ let btn = overlay.querySelector('.vp-play-btn'); if(!btn){ btn = document.createElement('button'); btn.className='vp-play-btn'; btn.style.cssText='width:96px;height:96px;border-radius:50%;border:none;background:linear-gradient(135deg,#00d9ff,#0066ff);box-shadow:0 8px 24px rgba(0,0,0,0.6);display:flex;align-items:center;justify-content:center;color:#fff;font-size:36px;z-index:9001;cursor:pointer;'; btn.innerHTML='▶'; btn.addEventListener('click', (e)=>{ e.stopPropagation(); try{ const p = video.play(); if(p && typeof p.then === 'function'){ p.then(()=>{ btn.remove(); }).catch(err=>{ console.warn('vp: user play failed', err); }); } else { btn.remove(); } }catch(err){ console.warn('vp: play() error', err); } }); overlay.appendChild(btn); } btn.style.display='flex'; }catch(e){ console.warn('vp: ensurePlayButton error', e); } }

  function onPlaying(){ try{ if(video){ video.style.visibility='visible'; } if(overlay){ try{ overlay.style.background='transparent'; overlay.style.pointerEvents='none'; }catch(e){} try{ const sp = overlay.querySelectorAll('.vp-spinner'); sp.forEach(s=>s.style.display='none'); }catch(e){} try{ const pb = overlay.querySelector('.vp-play-btn'); if(pb) pb.style.display='none'; }catch(e){} try{ if(getComputedStyle(overlay).display==='none') overlay.style.display='flex'; }catch(e){} } try{ if(video && video.parentElement !== document.body){ document.body.appendChild(video); } video.style.position='fixed'; video.style.left='0'; video.style.top='0'; video.style.width='100%'; video.style.height='100%'; video.style.objectFit='cover'; video.style.zIndex='99999'; }catch(e){ console.warn('vp: onPlaying reposition error', e); } clearTimeout(startTimeout); }catch(e){} }

  // localStorage helpers
  function getUnlocked(){ try{ const v = parseInt(localStorage.getItem('unlockedLevel')||'1',10); return isNaN(v)?1:v; }catch(e){ return 1; } }
  function unlockNext(cur){ try{ const unlocked = getUnlocked(); const next = Number(cur)+1; if(next>unlocked) localStorage.setItem('unlockedLevel', String(next)); }catch(e){ console.error(e); } }

  function playSrc(src){ if(!video) return; detach(); endedHandled=false; try{ video.style.visibility='hidden'; }catch(e){} if(overlay) try{ overlay.style.display='flex'; }catch(e){} video.src = src; video.load(); const p = (function(){ try{ return video.play(); }catch(e){ return null; }})(); if(p && typeof p.then === 'function'){ p.then(()=>{ attach(); onPlaying(); }).catch(err=>{ attach(); ensurePlayButton(); }); } else { attach(); setTimeout(()=>{ if(video && video.paused) ensurePlayButton(); },800); } clearTimeout(startTimeout); startTimeout = setTimeout(()=>{ try{ if(video && video.paused){ const p2 = video.play(); if(p2 && typeof p2.then === 'function'){ p2.then(()=> onPlaying()).catch(()=> ensurePlayButton()); } else { setTimeout(()=>{ if(video && video.paused) ensurePlayButton(); },200); } } }catch(e){} },2000); }

  function freeVideo(){ try{ if(video){ try{ video.pause(); }catch(e){} try{ video.removeAttribute('src'); video.load(); }catch(e){} try{ video.style.visibility='hidden'; }catch(e){} } }catch(e){} detach(); }

  if(video) attach();

  // expose API
  return { playSrc, freeVideo, unlockNext, getUnlocked };
}

// expose global for non-module environments
try{ if(typeof window !== 'undefined') window.attachVideoHandlers = attachVideoHandlers; }catch(e){}
// video-playback-fix.js
// Small module to attach robust video handlers and manage level progression via localStorage.

export function attachVideoHandlers(opts = {}) {
  const videoSelector = opts.videoSelector || '#video-player';
  const overlaySelector = opts.overlaySelector || null;
  const redirectTo = opts.redirectTo || 'level-select.html';

  const videoEl = document.querySelector(videoSelector);
  const overlayEl = overlaySelector ? document.querySelector(overlaySelector) : null;

  let redirected = false;
  let fallbackTimer = null;

  function safeRedirect(to) {
    if (redirected) return;
    redirected = true;
    // small fade if possible
    try { document.body.style.transition = 'opacity 0.35s'; document.body.style.opacity = '0'; } catch(e){}
    setTimeout(() => {
      window.location.href = to || redirectTo;
    }, 300);
  }

  function onEndedHandler() { safeRedirect(redirectTo); }
  function onErrorHandler() { safeRedirect(redirectTo); }
  function onTimeUpdateHandler() {
    if (!videoEl.duration) return;
    if (videoEl.duration - videoEl.currentTime < 0.25) safeRedirect(redirectTo);
  }

  function clearHandlers() {
    if (!videoEl) return;
    videoEl.removeEventListener('ended', onEndedHandler);
    videoEl.removeEventListener('error', onErrorHandler);
    videoEl.removeEventListener('timeupdate', onTimeUpdateHandler);
    if (fallbackTimer) { clearTimeout(fallbackTimer); fallbackTimer = null; }
  }

  function attachHandlers() {
    if (!videoEl) return;
    redirected = false;
    videoEl.addEventListener('ended', onEndedHandler);
    videoEl.addEventListener('error', onErrorHandler);
    videoEl.addEventListener('timeupdate', onTimeUpdateHandler);
    // fallback in case events don't fire
    fallbackTimer = setTimeout(() => safeRedirect(redirectTo), 12000);
  }

  function playSrc(src) {
    if (!videoEl) return Promise.resolve();
    clearHandlers();
    if (overlayEl) overlayEl.style.display = 'flex';
    try {
      videoEl.src = src;
      attachHandlers();
      const p = videoEl.play();
      if (p && p.catch) {
        return p.catch(err => { safeRedirect(redirectTo); });
      }
      return Promise.resolve();
    } catch (e) {
      safeRedirect(redirectTo);
      return Promise.reject(e);
    }
  }

  function freeVideo() {
    try { videoEl.pause(); videoEl.removeAttribute('src'); videoEl.load(); } catch(e){}
    clearHandlers();
    if (overlayEl) overlayEl.style.display = 'none';
  }

  // Level progression helpers
  const STORAGE_KEY = 'unlockedLevel';
  function getUnlocked() {
    try {
      const v = parseInt(localStorage.getItem(STORAGE_KEY));
      return isNaN(v) ? 1 : v;
    } catch (e) { return 1; }
  }
  function setUnlocked(n) {
    try { localStorage.setItem(STORAGE_KEY, String(n)); } catch(e){}
  }
  function unlockNext(currentLevel) {
    try {
      const unlocked = getUnlocked();
      const next = Math.max(unlocked, currentLevel + 1);
      if (next > unlocked) setUnlocked(next);
      return next;
    } catch (e) { return getUnlocked(); }
  }

  // expose API
  return {
    playSrc,
    freeVideo,
    unlockNext,
    getUnlocked
  };
}
// video-playback-fix.js
// Lightweight helper to make HTML5 video playback more reliable across platforms
// Exports attachVideoHandlers(options) -> { playSrc, freeVideo, unlockNext, getUnlocked }

export function attachVideoHandlers(opts = {}){
  const videoSelector = opts.videoSelector || '#video-player';
  const overlaySelector = opts.overlaySelector || null;
  const redirectTo = opts.redirectTo || 'level-select.html';
  const currentLevel = opts.currentLevel || null;
  const video = document.querySelector(videoSelector);
  const overlay = overlaySelector ? document.querySelector(overlaySelector) : null;
  if(!video) {
    console.warn('video-playback-fix: video element not found for', videoSelector);
  }

  let endTimeout = null;
  let lastTime = 0;
  let endedHandled = false;
  let startTimeout = null;
  let playingBound = null;

  function handleEnded() {
    if(endedHandled) return;
    endedHandled = true;
    // small delay to allow UI to update
    setTimeout(() => {
      // optional: unlock next level if currentLevel provided
      try{ if(currentLevel !== null) unlockNext(currentLevel); }catch(e){}
      // extra-robust: write unlockedLevel directly to localStorage in case helper isn't available elsewhere
      try{ if(currentLevel !== null){ const next = Number(currentLevel)+1; localStorage.setItem('unlockedLevel', String(next)); } }catch(e){}
      // default behaviour is to go back to level-select (or provided redirect)
      try { window.location.href = redirectTo; } catch(e){ console.error(e); }
    }, 200);
  }

  function handleError() {
    // If video errors, fallback to redirect
    handleEnded();
  }

  function handleTimeUpdate(){
    if(!video || !video.duration) return;
    const remain = video.duration - video.currentTime;
    if(remain > 0 && remain < 0.3){
      handleEnded();
    }
    lastTime = video.currentTime;
  }

  function handleVisibility(){
    // if tab hidden for long time, try to resume or finalize
    if(document.hidden && video && !video.paused){
      try { video.pause(); } catch(e){}
    }
  }

  function attach(){
    if(!video) return;
    endedHandled = false;
    video.addEventListener('ended', handleEnded);
    video.addEventListener('error', handleError);
    video.addEventListener('timeupdate', handleTimeUpdate);
    video.addEventListener('playing', onPlaying);
    document.addEventListener('visibilitychange', handleVisibility);
    // additional timeout fallback
    clearTimeout(endTimeout);
    endTimeout = setTimeout(() => {
      if(video && !video.ended){
        // If the video hasn't progressed in 7s, consider it stuck and redirect
        if(Math.abs((video.currentTime || 0) - lastTime) < 0.01){
          handleEnded();
        }
      }
    }, 8000);
  }

  function detach(){
    if(!video) return;
    video.removeEventListener('ended', handleEnded);
    video.removeEventListener('error', handleError);
    video.removeEventListener('timeupdate', handleTimeUpdate);
    video.removeEventListener('playing', onPlaying);
    document.removeEventListener('visibilitychange', handleVisibility);
    clearTimeout(endTimeout);
    clearTimeout(startTimeout);
    try{ if(video) video.style.visibility = 'hidden'; }catch(e){}
    try{ if(overlay) overlay.style.display = 'none'; }catch(e){}
    // remove play button if present
    try{ const btn = overlay && overlay.querySelector && overlay.querySelector('.vp-play-btn'); if(btn) btn.remove(); }catch(e){}
  }

  function onPlaying(){
    // video has started rendering; reveal it and hide any overlay
    try{
      if(video){
        video.style.visibility = 'visible';
      }
      if(overlay){
        // hide spinner/play button but keep overlay present so video (child) remains visible
        try{ overlay.style.background = 'transparent'; }catch(e){}
        try{ overlay.style.pointerEvents = 'none'; }catch(e){}
        try{ const sp = overlay.querySelectorAll('.vp-spinner'); sp.forEach(s=>s.style.display='none'); }catch(e){}
        try{ const pb = overlay.querySelector('.vp-play-btn'); if(pb) pb.style.display = 'none'; }catch(e){}
        // ensure overlay remains displayed so positioned video inside it continues rendering
        try{ if(getComputedStyle(overlay).display === 'none') overlay.style.display = 'flex'; }catch(e){}
      }
      // Move the video element to document.body and force it to the front to avoid WebView surface stacking issues
      try{
        if(video && video.parentElement !== document.body){
          document.body.appendChild(video);
        }
        video.style.position = 'fixed';
        video.style.left = '0';
        video.style.top = '0';
        video.style.width = '100%';
        video.style.height = '100%';
        video.style.objectFit = 'cover';
        video.style.zIndex = '99999';
      }catch(e){ console.warn('vp: onPlaying reposition error', e); }
    }catch(e){}
    clearTimeout(startTimeout);
  }

  // Show a large tappable play button inside the overlay to allow user-start when autoplay is blocked
  function ensurePlayButton(){
    if(!overlay) return;
    try{
      let btn = overlay.querySelector('.vp-play-btn');
      if(!btn){
        btn = document.createElement('button');
        btn.className = 'vp-play-btn';
        btn.style.cssText = 'width:96px;height:96px;border-radius:50%;border:none;background:linear-gradient(135deg,#00d9ff,#0066ff);box-shadow:0 8px 24px rgba(0,0,0,0.6);display:flex;align-items:center;justify-content:center;color:#fff;font-size:36px;z-index:9001;cursor:pointer;';
        btn.innerHTML = '▶';
        btn.addEventListener('click', (e)=>{
          e.stopPropagation();
          try{
            const p = video.play();
            if(p && typeof p.then === 'function'){
              p.then(()=>{
                onPlaying();
                btn.remove();
              }).catch(err=>{ console.warn('vp: user play failed', err); });
            } else {
              onPlaying();
              btn.remove();
            }
          }catch(err){ console.warn('vp: play() error', err); }
        });
        overlay.appendChild(btn);
      }
      btn.style.display = 'flex';
    }catch(e){ console.warn('vp: ensurePlayButton error', e); }
  }

  // Helpers for localStorage based level unlocking
  function getUnlocked(){
    try{
      const v = parseInt(localStorage.getItem('unlockedLevel') || '1', 10);
      return isNaN(v) ? 1 : v;
    }catch(e){ return 1; }
  }
  function unlockNext(currentLevel){
    try{
      const unlocked = getUnlocked();
      const next = Number(currentLevel) + 1;
      if(next > unlocked){
        localStorage.setItem('unlockedLevel', String(next));
      }
    }catch(e){ console.error(e); }
  }

  function playSrc(src){
    if(!video) return;
    detach();
    endedHandled = false;
    // hide the video element initially to avoid showing the default poster / thumbnail
    try{ video.style.visibility = 'hidden'; }catch(e){}
    if(overlay) try{ overlay.style.display = 'flex'; }catch(e){}

    console.log('vp: playSrc()', src);

    video.src = src;
    video.load();
    // try to play; many mobile browsers block autoplay
    const p = (function(){ try{ return video.play(); }catch(e){ return null; } })();
    if(p && typeof p.then === 'function'){
      p.then(()=>{
        console.log('vp: autoplay succeeded');
        attach();
        // when play succeeds, ensure we reveal the video
        onPlaying();
      }).catch(err=>{
        // autoplay prevented; attach handlers and show a user play button
        console.warn('vp: autoplay prevented', err);
        attach();
        ensurePlayButton();
      });
    } else {
      attach();
      // if play returned no promise, likely synchronous or not allowed; show play button after a short delay
      setTimeout(()=>{ if(video && video.paused) ensurePlayButton(); }, 800);
    }

    // start timeout: if video hasn't fired 'playing' within 2s, try to resume once; if still stuck after 8s, fallback to redirect
    clearTimeout(startTimeout);
    startTimeout = setTimeout(() => {
      // if not playing yet, attempt a play() again and reveal if possible
      try{
        if(video && video.paused){
          const p2 = video.play();
          if(p2 && typeof p2.then === 'function'){
            p2.then(()=> onPlaying()).catch(()=>{ ensurePlayButton(); });
          } else {
            // if play() didn't return a promise and still paused, show play button
            setTimeout(()=>{ if(video && video.paused) ensurePlayButton(); }, 200);
          }
        }
      }catch(e){}
    }, 2000);
  }

  function freeVideo(){
    try{
      if(video){
        try{ video.pause(); } catch(e){}
        try{ video.removeAttribute('src'); video.load(); } catch(e){}
        try{ video.style.visibility = 'hidden'; }catch(e){}
      }
    }catch(e){ console.error(e); }
    detach();
  }

  // auto-attach if video element present
  if(video) attach();

  return { playSrc, freeVideo, unlockNext, getUnlocked };
}

// Also expose as a global for non-module environments (Android WebView older versions)
try{
  if(typeof window !== 'undefined') window.attachVideoHandlers = attachVideoHandlers;
}catch(e){}
