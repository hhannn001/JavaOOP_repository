(() => {
  'use strict';
  const loginScene = document.getElementById('loginScene');
  const mapScene   = document.getElementById('mapScene');
  const roomStage  = document.getElementById('roomStage');
  const loginForm  = document.getElementById('loginForm');
  const loginId    = document.getElementById('loginId');
  const password   = document.getElementById('loginPassword');
  const error      = document.getElementById('loginError');
  let player       = '';

  function show(scene) {
    loginScene.hidden = scene !== loginScene;
    mapScene.hidden   = scene !== mapScene;
    roomStage.hidden  = scene !== roomStage;
  }

  loginForm.addEventListener('submit', event => {
    event.preventDefault();
    if (!loginId.value.trim() || !password.value.trim()) {
      error.textContent = '플레이어 ID와 입장 암호를 모두 적어주세요.';
      (!loginId.value.trim() ? loginId : password).focus();
      return;
    }
    player = loginId.value.trim();
    error.textContent = '';
    password.value = '';
    document.getElementById('playerGreeting').textContent = `${player} 님의 맵`;
    show(mapScene);
    document.getElementById('libraryPoster').focus();
  });

  document.getElementById('libraryPoster').addEventListener('click', () => {
    show(roomStage);
    window.dispatchEvent(new Event('code-room:enter'));
    document.getElementById('drawerHotspot').focus({ preventScroll: true });
  });

  document.getElementById('backToMaps').addEventListener('click', () => {
    show(mapScene);
    document.getElementById('libraryPoster').focus();
  });

  document.getElementById('logoutButton').addEventListener('click', () => {
    player = '';
    loginForm.reset();
    show(loginScene);
    loginId.focus();
  });

  const endMapButton = document.createElement('button');
  endMapButton.type = 'button';
  endMapButton.className = 'text-button';
  endMapButton.textContent = '맵 선택으로 돌아가기';
  document.getElementById('endDialog').append(endMapButton);
  endMapButton.addEventListener('click', () => {
    document.getElementById('endDialog').close();
    show(mapScene);
    document.getElementById('libraryPoster').focus();
  });

  show(loginScene);
})();
