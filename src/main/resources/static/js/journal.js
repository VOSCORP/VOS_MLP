// journal.js — журнал: правка ячеек, панель ячейки, панель стипендии
(function () {
  var scroll = document.querySelector('.journal-scroll');
  if (!scroll) { return; }
  var url = scroll.dataset.url;
  var schUrl = scroll.dataset.schUrl;
  var editable = scroll.dataset.editable === 'true';
  var panel = document.getElementById('cellPanel');
  var f = {
    who: document.getElementById('panelWho'),
    what: document.getElementById('panelWhat'),
    status: document.getElementById('fStatus'),
    score: document.getElementById('fScore'),
    minutes: document.getElementById('fMinutes'),
    excused: document.getElementById('fExcused'),
    minutesBox: document.getElementById('boxMinutes'),
    excusedBox: document.getElementById('boxExcused'),
    hint: document.getElementById('panelHint'),
    msg: document.getElementById('panelMsg')
  };
  var sp = {
    panel: document.getElementById('studentPanel'),
    name: document.getElementById('spName'),
    status: document.getElementById('spStatus'),
    list: document.getElementById('spList')
  };
  var current = null;
  var VERIFIED = '<svg class="verified-icon" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">' +
    '<circle cx="12" cy="12" r="11"></circle>' +
    '<path d="M7 12.5l3.2 3.2L17 9" fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"></path></svg>';

  function toast(text, bad) {
    var el = document.createElement('div');
    el.className = 'toast' + (bad ? ' bad' : '');
    el.textContent = text;
    document.body.appendChild(el);
    setTimeout(function () { el.remove(); }, 2800);
  }

  function plural(n) {
    var m10 = n % 10, m100 = n % 100;
    if (m10 === 1 && m100 !== 11) { return 'оценка'; }
    if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) { return 'оценки'; }
    return 'оценок';
  }

  function labelOf(td) {
    var head = document.querySelectorAll('.journal thead tr:first-child th')[td.cellIndex];
    var task = document.querySelectorAll('.journal .task-row td')[td.cellIndex];
    var text = head ? head.textContent : '';
    if (task && task.textContent) { text += ' · ' + task.textContent; }
    return text;
  }

  function syncFields() {
    var s = f.status.value;
    f.minutesBox.hidden = s !== 'LATE';
    f.excusedBox.hidden = s !== 'ABSENT';
    f.hint.hidden = s !== 'MAKEUP';
    f.score.disabled = s === 'ABSENT';
  }

  function fillPanel(td) {
    f.who.textContent = td.parentElement.dataset.name;
    f.what.textContent = labelOf(td);
    f.status.value = td.dataset.status;
    f.score.value = td.dataset.score;
    f.minutes.value = td.dataset.minutes;
    f.excused.checked = td.dataset.excused === 'true';
    f.msg.textContent = '';
    syncFields();
  }

  function closeCellPanel() {
    panel.classList.remove('open');
    var old = scroll.querySelector('td.cell.active');
    if (old) { old.classList.remove('active'); }
    current = null;
  }

  function closeStudentPanel() { sp.panel.classList.remove('open'); }

  function selectCell(td) {
    closeStudentPanel();
    var old = scroll.querySelector('td.cell.active');
    if (old) { old.classList.remove('active'); }
    td.classList.add('active');
    current = td;
    fillPanel(td);
    panel.classList.add('open');
  }

  // Синяя галочка: показываем или убираем после изменения оценок
  async function refreshBadge(row) {
    var nameTd = row.querySelector('.student-link');
    if (!nameTd) { return; }
    var data = await loadScholarship(nameTd.dataset.student);
    if (!data) { return; }
    var badge = nameTd.querySelector('.verified');
    if (data.status === 'YES' && !badge) {
      var span = document.createElement('span');
      span.className = 'verified';
      span.title = 'Достоин повышенной стипендии';
      span.innerHTML = VERIFIED;
      nameTd.appendChild(span);
    } else if (data.status !== 'YES' && badge) {
      badge.remove();
    }
  }

  function applyResult(td, j) {
    var active = td.classList.contains('active');
    td.textContent = j.text;
    td.className = 'cell ' + j.css + (active ? ' active' : '');
    td.title = j.title;
    td.dataset.status = j.status;
    td.dataset.score = j.score;
    td.dataset.minutes = j.lateMinutes;
    td.dataset.excused = String(j.excused);
    var row = td.parentElement;
    var avg = row.querySelector('.col-avg');
    if (avg) { avg.textContent = j.average; }
    var fin = row.querySelector('.col-final');
    if (fin) { fin.textContent = j.finalScore; }
    refreshBadge(row);
  }

  function csrfHeaders() {
    var h = { 'Content-Type': 'application/x-www-form-urlencoded' };
    var token = document.querySelector('meta[name="_csrf"]');
    var name = document.querySelector('meta[name="_csrf_header"]');
    if (token && name) { h[name.content] = token.content; }
    return h;
  }

  async function send(td, data) {
    var params = new URLSearchParams({ studentId: td.dataset.student, columnId: td.dataset.column });
    Object.keys(data).forEach(function (k) { params.append(k, data[k]); });
    var json;
    try {
      var res = await fetch(url, {
        method: 'POST',
        headers: csrfHeaders(),
        body: params
      });
      json = await res.json();
    } catch (e) {
      toast('Нет связи с сервером', true);
      return false;
    }
    if (!json.ok) {
      toast(json.message, true);
      f.msg.textContent = json.message;
      return false;
    }
    applyResult(td, json);
    return true;
  }

  // Правка оценки прямо в таблице (двойной клик)
  function editInline(td) {
    if (td.querySelector('input')) { return; }
    var oldText = td.textContent;
    var status = td.dataset.status === 'ABSENT' ? 'NORMAL' : td.dataset.status;
    var input = document.createElement('input');
    input.type = 'number';
    input.min = 0;
    input.max = 100;
    input.className = 'cell-input';
    input.value = td.dataset.score || '';
    td.textContent = '';
    td.appendChild(input);
    input.focus();
    input.select();

    var done = false;
    async function finish(save) {
      if (done) { return; }
      done = true;
      if (!save) { td.textContent = oldText; return; }
      var ok = await send(td, {
        status: status,
        score: input.value,
        lateMinutes: td.dataset.minutes || '',
        excused: 'false'
      });
      if (!ok) { td.textContent = oldText; }
      else if (td.classList.contains('active')) { fillPanel(td); }
    }
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') { finish(true); }
      if (e.key === 'Escape') { finish(false); }
    });
    input.addEventListener('blur', function () { finish(true); });
  }

  // ---- панель стипендии ----
  async function loadScholarship(studentId) {
    try {
      var res = await fetch(schUrl + studentId + '/scholarship');
      return await res.json();
    } catch (e) {
      return null;
    }
  }

  var STATUS_TEXT = {
    YES: 'Достоин повышенной стипендии: по всем предметам средний балл от 90.',
    NO: 'Пока не соответствует условию: средний балл по всем предметам должен быть от 90.',
    NO_DATA: 'Данных недостаточно: по некоторым предметам ещё нет оценок.'
  };

  async function openStudentPanel(nameTd) {
    var data = await loadScholarship(nameTd.dataset.student);
    if (!data) { toast('Не удалось загрузить данные', true); return; }
    closeCellPanel();
    sp.name.textContent = data.name;
    sp.status.textContent = STATUS_TEXT[data.status] || '';
    if (data.restricted) { sp.status.textContent += ' Подробности по предметам видит куратор группы.'; }
    sp.list.textContent = '';
    data.subjects.forEach(function (s) {
      var box = document.createElement('div');
      box.className = 'sp-line' + (s.ok ? ' ok' : '');
      var title = document.createElement('b');
      title.textContent = s.subject + ': ' + s.average;
      var note = document.createElement('span');
      if (s.count === 0) { note.textContent = 'Оценок пока нет'; }
      else if (s.ok) { note.textContent = 'Условие выполнено'; }
      else { note.textContent = 'До 90 нужно ещё ' + s.need + ' ' + plural(s.need) + ' по 100'; }
      box.appendChild(title);
      box.appendChild(note);
      sp.list.appendChild(box);
    });
    sp.panel.classList.add('open');
  }

  // ---- события ----
  scroll.addEventListener('click', function (e) {
    var name = e.target.closest('td.student-link');
    if (name) { openStudentPanel(name); return; }
    var td = e.target.closest('td.cell');
        if (!td || !editable || td.querySelector('input')) { return; }
    selectCell(td);
  });
  scroll.addEventListener('dblclick', function (e) {
    var td = e.target.closest('td.cell');
        if (td && editable) { editInline(td); }
  });

  f.status.addEventListener('change', syncFields);

  document.getElementById('btnSave').addEventListener('click', async function () {
    if (!current) { return; }
    var ok = await send(current, {
      status: f.status.value,
      score: f.score.value,
      lateMinutes: f.minutes.value,
      excused: f.excused.checked ? 'true' : 'false'
    });
    if (ok) { toast('Сохранено'); fillPanel(current); }
  });
  document.getElementById('btnClose').addEventListener('click', closeCellPanel);
  document.getElementById('spClose').addEventListener('click', closeStudentPanel);
})();