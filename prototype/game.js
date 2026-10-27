(() => {
  'use strict';
  const STORAGE_KEY = 'code-room-study-v1';
  const $           = (selector) => document.querySelector(selector);
  const icon        = (name, className = '') => `<svg class="icon ${className}" aria-hidden="true"><use href="#i-${name}"/></svg>`;
  const initial     = () => ({ stage: 0, selected: 'note1', startedAt: Date.now(), finishedAt: null, attempts: 0 });
  const available   = (stage) => ['note1', ...(stage >= 1 ? ['note2'] : []), ...(stage >= 2 ? ['key'] : [])];
  const clues       = {
    note1: {
      title: '접힌 메모', short: '접힌 메모', stamp: 'NOTE 01', icon: 'note',
      description: '“하나씩 더해진 숫자들이\n첫 번째 서랍을 열어줄 거야.”',
      code: ['int[] digits = {1, 2, 0, 1};', '', 'for (int digit : digits) {', '  System.out.print(digit + 1);', '}'],
      shape: '네 자리 숫자',
      note: '숫자 사이에 공백이나 줄바꿈은 없다.',
      hint: '배열의 숫자를 왼쪽부터 하나씩 꺼내 1을 더하세요. print는 줄을 바꾸지 않으므로 결과를 이어 붙이면 됩니다.'
    },
    note2: {
      title: '격자에 관한 기록', short: '격자 메모', stamp: 'NOTE 02', icon: 'note',
      description: '“세 줄, 세 칸.\n벽에 걸린 아홉 개의 눈을 깨워라.”',
      code: ['int[][] grid = {', '  {3, 1, 2},', '  {2, 0, 1},', '  {0, 2, 2}', '};', '', 'for (int[] row : grid) {', '  for (int n : row) {', '    System.out.print(n + 1);', '  }', '  System.out.println();', '}'],
      shape: '세 줄 × 세 칸',
      note: 'println()을 만나면 다음 줄로 이동한다.',
      hint: '모든 칸의 숫자에 각각 1을 더하세요. 첫 번째 줄은 4, 2, 3입니다. 같은 방법으로 나머지 두 줄을 채워보세요.'
    },
    key: {
      title: '작은 황동 열쇠', short: '황동 열쇠', stamp: 'EXIT KEY', icon: 'key',
      description: '오래된 황동 열쇠.\n문에 달린 자물쇠와 꼭 맞을 것 같다.',
      note: '이제 코드 대신, 열쇠를 사용할 차례.',
      hint: '황동 열쇠를 선택한 상태로 방 오른쪽의 출입문을 누르세요.'
    }
  };
  const drafts   = { drawer: '', grid: Array(9).fill('') };
  let state      = initial();
  let panel      = null;
  let lastFocus  = null;
  let toastTimer = null;

  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (saved && Number.isInteger(saved.stage) && saved.stage >= 0 && saved.stage <= 3 &&
        Number.isFinite(saved.startedAt) && saved.startedAt > 0 && saved.startedAt <= Date.now() &&
        (saved.stage < 3 || (Number.isFinite(saved.finishedAt) && saved.finishedAt >= saved.startedAt))) {
      state = {
        stage: saved.stage,
        selected: available(saved.stage).includes(saved.selected) ? saved.selected : 'note1',
        startedAt: saved.startedAt,
        finishedAt: saved.stage === 3 ? saved.finishedAt : null,
        attempts: Number.isInteger(saved.attempts) && saved.attempts >= 0 ? saved.attempts : 0
      };
    }
  } catch { /* Gameplay also works when local storage is unavailable. */ }

  function save() {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch { /* Optional persistence. */ }
  }

  function escapeHTML(value) {
    return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
  }

  function highlight(code) {
    return escapeHTML(code).replace(/\b(int|for)\b|\b(\d+)\b|\b(print|println)\b/g, (match, keyword, number) => {
      const type = keyword ? 'keyword' : number ? 'number' : 'method';
      return `<span class="token-${type}">${match}</span>`;
    });
  }

  function renderClue() {
    const clue   = clues[state.selected];
    const items  = available(state.stage);
    const solved = state.selected === 'note1' ? state.stage >= 1 : state.selected === 'note2' ? state.stage >= 2 : state.stage === 3;
    const code   = clue.code ? `<pre class="code-block" aria-label="Java 단서 코드"><code>${clue.code.map((line, index) => `<span class="code-line"><span class="line-number" aria-hidden="true">${index + 1}</span>${highlight(line) || ' '}</span>`).join('')}</code></pre>` : `<div class="key-art">${icon('key')}</div>`;
    const shape  = state.selected === 'note1' ? `<div class="mini-digits" aria-hidden="true">${'<i>?</i>'.repeat(4)}</div>` : state.selected === 'note2' ? `<div class="mini-grid" aria-hidden="true">${'<i></i>'.repeat(9)}</div>` : icon('door');
    $('#heldItem').innerHTML = `<div class="paper-top">${icon(clue.icon)}<span>${clue.stamp}${solved ? ' / SOLVED' : ''}</span></div><h2>${clue.title}</h2><p class="paper-description">${clue.description.replaceAll('\n', '<br>')}</p>${code}<div class="answer-shape"><span>${clue.shape || '출입문에 사용'}${solved ? ' · 사용 완료' : ''}</span>${shape}</div><p class="paper-note">${clue.note}</p>`;
    $('#clueIndex').textContent  = `${String(items.indexOf(state.selected) + 1).padStart(2, '0')} / ${String(items.length).padStart(2, '0')}`;
    $('#hintText').textContent   = clue.hint;
  }

  function renderInventory() {
    const items = available(state.stage);
    $('#inventorySlots').innerHTML = items.map((id, index) => {
      const solved = id === 'note1' ? state.stage >= 1 : id === 'note2' ? state.stage >= 2 : state.stage === 3;
      return `<button class="inventory-slot${id === state.selected ? ' active' : ''}" data-item="${id}" aria-label="${clues[id].short} 선택" aria-pressed="${id === state.selected}"><span class="slot-number">0${index + 1}</span>${icon(clues[id].icon)}<span>${clues[id].short}</span>${solved ? icon('check', 'collected-check') : ''}</button>`;
    }).join('') + Array.from({ length: 5 - items.length }, () => '<div class="inventory-slot empty" aria-label="빈 인벤토리 칸"><i class="empty-mark" aria-hidden="true"></i></div>').join('');
    $('#itemCount').textContent = `${items.length}개의 ${state.stage >= 2 ? '물건' : '단서'}`;
  }

  function render() {
    renderClue();
    renderInventory();
    window.drawRoom(state.stage);
    $('#progressText').innerHTML               = `잠금 장치 <b>${Math.min(state.stage, 2)} / 2</b>`;
    $('#roomStatus').textContent               = state.stage === 3 ? '서재 · 탈출 성공' : '서재 · 문이 잠겨 있습니다';
    $('#drawerHotspot .spot-marker').innerHTML  = state.stage >= 1 ? icon('check') : '01';
    $('#gridHotspot .spot-marker').innerHTML    = state.stage >= 2 ? icon('check') : '02';
    $('#doorHotspot .door-marker').innerHTML    = icon(state.stage >= 2 ? 'key' : 'lock');
    $('#drawerHotspot .spot-label').textContent = state.stage >= 1 ? '열린 서랍' : '네 자리 잠금 서랍';
    $('#gridHotspot .spot-label').textContent   = state.stage >= 2 ? '해제된 다이얼' : '3 × 3 다이얼';
    $('#doorHotspot .spot-label').textContent   = state.stage === 3 ? '열린 출입문' : '잠긴 출입문';
    updateTimer();
  }

  function caption(message, name = 'note') {
    $('#sceneCaption').innerHTML = `${icon(name)}<span>${message}</span>`;
  }

  function selectItem(id) {
    if (!available(state.stage).includes(id)) return;
    if (panel && matchMedia('(max-width: 700px)').matches) closePanel(false);
    state.selected         = id;
    $('#clueColumn').hidden = false;
    $('#hintBox').open      = false;
    renderClue();
    renderInventory();
    save();
    $(`[data-item="${id}"]`)?.focus({ preventScroll: true });
  }

  function closePanel(restoreFocus = true) {
    if (panel) {
      const values = [...$('#puzzlePanel').querySelectorAll('input')].map(input => input.value);
      drafts[panel] = panel === 'drawer' ? values[0] || '' : values;
    }
    $('#puzzlePanel').hidden = true;
    $('#hotspots').inert    = false;
    panel                  = null;
    if (restoreFocus) lastFocus?.focus({ preventScroll: true });
  }

  function reward(id, title) {
    closePanel(false);
    state.selected         = id;
    $('#clueColumn').hidden = false;
    $('#hintBox').open      = false;
    render();
    save();
    clearTimeout(toastTimer);
    $('#rewardToast').innerHTML = `${icon(clues[id].icon)}<strong>${title}</strong><span>인벤토리에 추가되었습니다.</span>`;
    $('#rewardToast').hidden = false;
    toastTimer = setTimeout(() => { $('#rewardToast').hidden = true; }, 2300);
    $(`[data-item="${id}"]`).focus({ preventScroll: true });
  }

  function openPanel(type) {
    if (type === 'drawer' && state.stage >= 1) {
      caption('서랍은 비어 있다. 발견한 격자 메모는 인벤토리에서 다시 볼 수 있다.');
      return;
    }
    if (type === 'grid' && state.stage >= 2) {
      caption('벽의 잠금 장치가 열려 있다. 황동 열쇠를 출입문에 사용하자.', 'key');
      return;
    }
    if (type === 'grid' && state.stage === 0) {
      caption('아홉 칸짜리 다이얼이다. 지금 가진 네 자리 메모는 서랍에 맞을 것 같다.', 'light');
      return;
    }
    if (panel) closePanel(false);
    panel     = type;
    lastFocus = document.activeElement;
    const isDrawer = type === 'drawer';
    const fields   = isDrawer
      ? '<label class="lock-label" for="drawerCode">네 자리 암호</label><input class="digit-input" id="drawerCode" name="code" inputmode="numeric" autocomplete="off" maxlength="4" placeholder="····" aria-describedby="puzzleError">'
      : `<fieldset class="grid-fieldset" style="border:0;padding:0;margin:0"><legend class="lock-label">메모의 출력값을 행 순서대로 입력하세요.</legend><div class="grid-inputs">${Array.from({ length: 9 }, (_, index) => `<input inputmode="numeric" autocomplete="off" maxlength="1" aria-label="${Math.floor(index / 3) + 1}행 ${index % 3 + 1}열" placeholder="·" aria-describedby="puzzleError" data-cell="${index}">`).join('')}</div></fieldset><p class="panel-footnote">숫자를 입력하면 다음 칸으로 이동합니다.</p>`;
    $('#puzzlePanel').innerHTML = `<button class="icon-button panel-close" id="closePanel" aria-label="잠금 장치 닫기">${icon('close')}</button><h2 id="puzzleTitle">${isDrawer ? '잠긴 서랍' : '벽의 3 × 3 다이얼'}</h2><p>${isDrawer ? '손잡이 아래, 네 자리 숫자 잠금 장치가 있다.' : '세 줄의 숫자가 작은 금고의 암호인 것 같다.'}</p><form id="puzzleForm" novalidate>${fields}<p class="puzzle-error" id="puzzleError" role="status" aria-live="polite"></p><button class="primary-button" type="submit">${isDrawer ? '서랍 열기' : '다이얼 확인'}${icon('arrow')}</button></form>`;
    $('#puzzlePanel').hidden = false;
    $('#hotspots').inert     = true;
    $('#closePanel').addEventListener('click', () => closePanel());
    $('#puzzleForm').addEventListener('submit', submitPuzzle);
    $('#puzzlePanel').querySelectorAll('input').forEach((input, index, inputs) => {
      input.value = isDrawer ? drafts.drawer : drafts.grid[index] || '';
      input.addEventListener('input', () => {
        input.value = input.value.replace(/[^0-9]/g, '');
        input.removeAttribute('aria-invalid');
        $('#puzzleError').textContent = '';
        if (!isDrawer && input.value && index < 8) inputs[index + 1].focus();
      });
      input.addEventListener('keydown', (event) => {
        if (!isDrawer && event.key === 'Backspace' && !input.value && index > 0) inputs[index - 1].focus();
      });
      if (!isDrawer) input.addEventListener('paste', (event) => {
        const digits = event.clipboardData.getData('text').replace(/[^0-9]/g, '');
        if (digits.length > 1) {
          event.preventDefault();
          [...digits].slice(0, 9 - index).forEach((digit, offset) => { inputs[index + offset].value = digit; });
          inputs[Math.min(8, index + digits.length - 1)].focus();
          $('#puzzleError').textContent = '';
        }
      });
    });
    $('#puzzlePanel input').focus({ preventScroll: true });
  }

  function submitPuzzle(event) {
    event.preventDefault();
    const inputs = [...$('#puzzlePanel').querySelectorAll('input')];
    const answer = inputs.map(input => input.value).join('');
    const size   = panel === 'drawer' ? 4 : 9;
    if (answer.length !== size || inputs.some(input => !input.value)) {
      $('#puzzleError').textContent = panel === 'drawer' ? '네 자리 숫자를 모두 입력해 주세요.' : '아홉 칸에 숫자를 하나씩 입력해 주세요.';
      (inputs.find(input => !input.value) || inputs[0]).focus();
      return;
    }
    state.attempts += 1;
    if ((panel === 'drawer' && answer === '2312') || (panel === 'grid' && answer === '423312133')) {
      if (panel === 'drawer') {
        state.stage = 1;
        reward('note2', '서랍 속에서 새 메모를 찾았다.');
        caption('서랍이 열렸다. 새 메모에는 벽의 다이얼과 같은 격자가 그려져 있다.');
      } else {
        state.stage = 2;
        reward('key', '작은 황동 열쇠를 얻었다.');
        caption('찰칵. 작은 금고에서 열쇠를 찾았다. 이제 출입문으로 가자.', 'key');
      }
    } else {
      $('#puzzleError').textContent = '아직 열리지 않아요. 코드의 출력값을 다시 확인해 보세요.';
      inputs.forEach(input => input.setAttribute('aria-invalid', 'true'));
      inputs[0].focus();
      inputs[0].select();
      save();
    }
  }

  function door() {
    if (state.stage === 3) {
      $('#endDialog').showModal();
      return;
    }
    if (state.stage < 2) {
      caption('문이 굳게 잠겨 있다. 숫자 암호가 아니라, 작은 열쇠가 필요하다.', 'lock');
      return;
    }
    if (state.selected !== 'key') {
      caption('열쇠를 찾았다. 아래 인벤토리에서 황동 열쇠를 손에 들고 다시 문을 누르자.', 'key');
      $('[data-item="key"]').focus({ preventScroll: true });
      return;
    }
    state.stage      = 3;
    state.finishedAt = Date.now();
    clearTimeout(toastTimer);
    $('#rewardToast').hidden = true;
    render();
    save();
    caption('문이 열렸다. 코드의 끝에서, 새로운 시작을 만났다.', 'door');
    $('#endDialog').showModal();
  }

  function timeText() {
    const seconds = Math.max(0, Math.floor(((state.finishedAt || Date.now()) - state.startedAt) / 1000));
    const minutes = Math.floor(seconds / 60);
    return `${String(minutes).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
  }

  function updateTimer() {
    $('#timer').textContent   = timeText();
    $('#endTime').textContent = timeText();
  }

  function reset() {
    document.querySelectorAll('dialog[open]').forEach(dialog => dialog.close());
    closePanel(false);
    clearTimeout(toastTimer);
    $('#rewardToast').hidden = true;
    $('#hintBox').open       = false;
    state                   = initial();
    drafts.drawer           = '';
    drafts.grid             = Array(9).fill('');
    $('#clueColumn').hidden  = false;
    render();
    save();
    caption('주머니에서 접힌 메모를 발견했다. 방 안의 물건을 살펴보자.');
    $('#drawerHotspot').focus({ preventScroll: true });
  }

  $('#inventorySlots').addEventListener('click', event => {
    const item = event.target.closest('[data-item]');
    if (item) selectItem(item.dataset.item);
  });
  $('#drawerHotspot').addEventListener('click', () => openPanel('drawer'));
  $('#gridHotspot').addEventListener('click', () => openPanel('grid'));
  $('#doorHotspot').addEventListener('click', door);
  $('#booksHotspot').addEventListener('click', () => caption('『배열과 반복문』, 『Java의 정석』… 이 방의 주인은 숫자에 이야기를 숨겼다.'));
  $('#windowHotspot').addEventListener('click', () => caption('창밖에는 조용히 비가 내린다. 창문은 열리지 않는다. 출입문을 찾아보자.'));
  $('#putAway').addEventListener('click', () => {
    $('#clueColumn').hidden = true;
    $(`[data-item="${state.selected}"]`).focus({ preventScroll: true });
  });
  $('#panLeft').addEventListener('click', () => $('#worldViewport').scrollBy({ left: -innerWidth * .7, behavior: 'smooth' }));
  $('#panRight').addEventListener('click', () => $('#worldViewport').scrollBy({ left: innerWidth * .7, behavior: 'smooth' }));
  $('#helpButton').addEventListener('click', () => $('#helpDialog').showModal());
  $('#resetButton').addEventListener('click', () => $('#resetDialog').showModal());
  $('#confirmReset').addEventListener('click', reset);
  $('#playAgain').addEventListener('click', reset);
  document.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => document.getElementById(button.dataset.close).close()));
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && panel && !document.querySelector('dialog[open]')) closePanel();
  });
  render();
  save();
  if (state.stage === 1) caption('다시 서재에 돌아왔다. 격자 메모로 벽의 다이얼을 열어보자.');
  if (state.stage === 2) caption('황동 열쇠가 인벤토리에 있다. 열쇠를 손에 들고 문을 열자.', 'key');
  if (state.stage === 3) caption('이미 열린 문. 언제든 처음부터 다시 탐색할 수 있다.', 'door');
  setInterval(updateTimer, 1000);
})();
