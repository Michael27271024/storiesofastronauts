// common.js - provides shared menu overlay and utilities
(function(){
  function createMenu(){
    if(document.getElementById('common-menu-overlay')) return;
    const overlay = document.createElement('div');
    overlay.id = 'common-menu-overlay';
    overlay.className = 'common-menu-overlay';
    overlay.style.display = 'none';
    overlay.innerHTML = `
      <div class="common-menu-card">
        <h3>منو بازی</h3>
        <div class="common-menu-actions">
          <button id="menu-continue" class="common-btn">ادامه</button>
          <button id="menu-restart" class="common-btn">ری‌استارت مرحله</button>
          <button id="menu-back" class="common-btn ghost">بازگشت به منو</button>
        </div>
      </div>
    `;
    document.body.appendChild(overlay);

    document.getElementById('menu-continue').addEventListener('click', ()=>{ overlay.style.display='none'; if(window.onMenuContinue) window.onMenuContinue(); });
    document.getElementById('menu-restart').addEventListener('click', ()=>{ overlay.style.display='none'; if(window.onMenuRestart) window.onMenuRestart(); });
    document.getElementById('menu-back').addEventListener('click', ()=>{ overlay.style.display='none'; if(window.onMenuBack) window.onMenuBack(); });
  }

  function createOpenButton(){
    if(document.getElementById('common-menu-open')) return;
    const btn = document.createElement('button');
    btn.id = 'common-menu-open';
    btn.className = 'common-menu-open-btn';
    btn.innerHTML = '&#9776;';
    btn.addEventListener('click', ()=>{
      // Pause the game (page can override onMenuOpen to implement custom pause behavior)
      try{ if(window.onMenuOpen) window.onMenuOpen(); else { window.dispatchEvent(new Event('gamePause')); if(typeof gamePaused !== 'undefined') gamePaused = true; } }catch(e){}
      const o=document.getElementById('common-menu-overlay'); if(o) o.style.display='flex';
    });
    document.body.appendChild(btn);
  }

  function init(){
    createMenu(); createOpenButton();
    // default callbacks if not defined by page
    window.onMenuOpen = window.onMenuOpen || function(){ try{ window.dispatchEvent(new Event('gamePause')); if(typeof gamePaused !== 'undefined') gamePaused = true; }catch(e){} };
    window.onMenuContinue = window.onMenuContinue || function(){ try{ const resumeEvent = new Event('gameResume'); window.dispatchEvent(resumeEvent); if(typeof gamePaused !== 'undefined') gamePaused = false; }catch(e){} };
    window.onMenuRestart = window.onMenuRestart || function(){ window.location.reload(); };
    window.onMenuBack = window.onMenuBack || function(){ window.location.href = 'level-select.html'; };

    // Hide/remove any existing per-stage menu elements to avoid duplicate menus
    try{
      const idsToHide = ['menu-overlay','menu-btn','menu-open','hamburger-btn'];
      idsToHide.forEach(id => { const el = document.getElementById(id); if(el) el.style.display = 'none'; });
      // also hide elements with class 'menu-overlay' or 'menu-btn' if present
      const els = document.querySelectorAll('.menu-overlay, .menu-btn');
      els.forEach(e=>{ try{ e.style.display='none'; }catch(_){} });
    }catch(e){}
  }

  if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();

// Audio UI intentionally removed: audio plays at full volume and cannot be changed by user.
