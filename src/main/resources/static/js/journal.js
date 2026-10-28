// journal.js — правка оценок в таблице и боковая панель
(function () {
    var scroll = document.querySelector('.journal-scroll');
    if (!scroll) { return; }
    var url = scroll.dataset.url;
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
    var current = null;

    function toast(text, bad) {
        var el = document.createElement('div');
        el.className = 'toast' + (bad ? ' bad' : '');
        el.textContent = text;
        document.body.appendChild(el);
        setTimeout(function () { el.remove(); }, 2800);
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

    function selectCell(td) {
        var old = scroll.querySelector('td.cell.active');
        if (old) { old.classList.remove('active'); }
        td.classList.add('active');
        current = td;
        fillPanel(td);
        panel.classList.add('open');
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
        var avg = td.parentElement.querySelector('.col-avg');
        if (avg) { avg.textContent = j.average; }
    }

    async function send(td, data) {
        var params = new URLSearchParams({ studentId: td.dataset.student, columnId: td.dataset.column });
        Object.keys(data).forEach(function (k) { params.append(k, data[k]); });
        var json;
        try {
            var res = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
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

    scroll.addEventListener('click', function (e) {
        var td = e.target.closest('td.cell');
        if (!td || td.querySelector('input')) { return; }
        selectCell(td);
    });
    scroll.addEventListener('dblclick', function (e) {
        var td = e.target.closest('td.cell');
        if (td) { editInline(td); }
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

    document.getElementById('btnClose').addEventListener('click', function () {
        panel.classList.remove('open');
        var old = scroll.querySelector('td.cell.active');
        if (old) { old.classList.remove('active'); }
        current = null;
    });
})();