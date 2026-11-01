// attendance.js — отметка посещаемости
(function () {
    var scroll = document.querySelector('.journal-scroll');
    if (!scroll) { return; }
    var url = scroll.dataset.url;
    var editable = scroll.dataset.editable === 'true';
    var panel = document.getElementById('attPanel');
    var f = {
        who: document.getElementById('attWho'),
        what: document.getElementById('attWhat'),
        state: document.getElementById('aState'),
        minutes: document.getElementById('aMinutes'),
        excused: document.getElementById('aExcused'),
        minutesBox: document.getElementById('boxMinutes'),
        excusedBox: document.getElementById('boxExcused'),
        msg: document.getElementById('attMsg')
    };
    var current = null;

    function csrfHeaders() {
        var h = { 'Content-Type': 'application/x-www-form-urlencoded' };
        var token = document.querySelector('meta[name="_csrf"]');
        var name = document.querySelector('meta[name="_csrf_header"]');
        if (token && name) { h[name.content] = token.content; }
        return h;
    }

    function toast(text, bad) {
        var el = document.createElement('div');
        el.className = 'toast' + (bad ? ' bad' : '');
        el.textContent = text;
        document.body.appendChild(el);
        setTimeout(function () { el.remove(); }, 2800);
    }

    function syncFields() {
        var s = f.state.value;
        f.minutesBox.hidden = s !== 'LATE';
        f.excusedBox.hidden = s !== 'ABSENT';
    }

    function fillPanel(td) {
        var head = document.querySelectorAll('.journal thead tr:first-child th')[td.cellIndex];
        f.who.textContent = td.parentElement.dataset.name;
        f.what.textContent = head ? head.textContent : '';
        f.state.value = td.dataset.state;
        f.minutes.value = td.dataset.minutes;
        f.excused.checked = td.dataset.excused === 'true';
        f.msg.textContent = '';
        syncFields();
    }

    function closePanel() {
        panel.classList.remove('open');
        var old = scroll.querySelector('td.cell.active');
        if (old) { old.classList.remove('active'); }
        current = null;
    }

    function selectCell(td) {
        var old = scroll.querySelector('td.cell.active');
        if (old) { old.classList.remove('active'); }
        td.classList.add('active');
        current = td;
        fillPanel(td);
        panel.classList.add('open');
    }

    function apply(td, j) {
        td.textContent = j.text;
        td.className = 'cell ' + j.css + ' active';
        td.title = j.title;
        td.dataset.state = j.state;
        td.dataset.minutes = j.minutes;
        td.dataset.excused = String(j.excused);
        var row = td.parentElement;
        row.querySelector('.col-abs').textContent = j.absences;
        row.querySelector('.col-lates').textContent = j.lates;
        row.querySelector('.col-percent').textContent = j.percent + '%';
    }

    async function save() {
        if (!current) { return; }
        var params = new URLSearchParams({
            studentId: current.dataset.student,
            columnId: current.dataset.column,
            state: f.state.value,
            minutes: f.minutes.value,
            excused: f.excused.checked ? 'true' : 'false'
        });
        var json;
        try {
            var res = await fetch(url, { method: 'POST', headers: csrfHeaders(), body: params });
            json = await res.json();
        } catch (e) {
            toast('Нет связи с сервером', true);
            return;
        }
        if (!json.ok) {
            toast(json.message, true);
            f.msg.textContent = json.message;
            return;
        }
        apply(current, json);
        fillPanel(current);
        toast('Сохранено');
    }

    scroll.addEventListener('click', function (e) {
        if (!editable) { return; }
        var td = e.target.closest('td.cell');
        if (td) { selectCell(td); }
    });
    f.state.addEventListener('change', syncFields);
    document.getElementById('attSave').addEventListener('click', save);
    document.getElementById('attClose').addEventListener('click', closePanel);
})();