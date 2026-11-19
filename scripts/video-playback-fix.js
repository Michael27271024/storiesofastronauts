// Updated video-playback-fix.js - Auto Handles All 10 Stages
(function (root) {
  function attachVideoHandlers(opts = {}) {
    const videoSelector = opts.videoSelector || '#video-player';
    const overlaySelector = opts.overlaySelector || '#video-overlay';
    const redirectTo = opts.redirectTo || '../level-select.html';
    const currentLevel = typeof opts.currentLevel !== 'undefined' ? opts.currentLevel : null;

    const video = document.querySelector(videoSelector);
    const overlay = overlaySelector ? document.querySelector(overlaySelector) : null;

    let endedHandled = false;

    function unlockNext(cur) {
      try {
        const unlocked = parseInt(localStorage.getItem('unlockedLevel') || '1', 10);
        const next = Math.max(unlocked, Number(cur) + 1);
        localStorage.setItem('unlockedLevel', String(next));
      } catch (e) {
        console.error("Unlock Error:", e);
      }
    }

    function safeRedirect() {
      if (currentLevel !== null) {
        unlockNext(currentLevel);
      }
      window.location.href = redirectTo;
    }

    function handleEnded() {
      if (endedHandled) return;
      endedHandled = true;
      safeRedirect();
    }

    function playSrc(src, stageNum) {
      if (stageNum !== undefined) unlockNext(stageNum);
      if (!video) {
        safeRedirect();
        return;
      }

      if (overlay) {
        overlay.style.display = 'flex';
        overlay.style.zIndex = '99999';
      }

      video.style.display = 'block';
      video.style.visibility = 'visible';
      video.style.opacity = '1';
      video.src = src;

      video.onended = handleEnded;
      video.onerror = handleEnded;

      const promise = video.play();
      if (promise !== undefined) {
        promise.catch(err => {
          console.warn("Autoplay blocked, attempting muted play:", err);
          video.muted = true;
          video.play().catch(handleEnded);
        });
      }
    }

    return { playSrc, unlockNext };
  }

  root.attachVideoHandlers = attachVideoHandlers;
  root.stageVideoHelper = attachVideoHandlers();
})(typeof globalThis !== 'undefined' ? globalThis : window);
