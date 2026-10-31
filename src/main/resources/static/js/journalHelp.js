// journalHelp.js — панель «Помощь с журналом»
(function () {
    var panel = document.getElementById('helpPanel');
    var btn = document.getElementById('btnHelp');
    if (!panel || !btn) { return; }

    var starosta = document.getElementById('hStarosta');
    var other = document.getElementById('hOther');
    var target = document.getElementById('hTarget');
    var expires = document.getElementById('hExpires');
    var noSubjects = document.getElementById('hNoSubjects');

    function scope() {
        return document.querySelector('input[name="scope"]:checked').value;
    }

    function targetGroupId() {
        if (scope() === 'other') { return target.value; }
        var opt = starosta.options[starosta.selectedIndex];
        return opt ? opt.dataset.group : '';
    }

    function sync() {
        var isOther = scope() === 'other';
        other.hidden = !isOther;
        other.querySelectorAll('input, select').forEach(function (el) { el.disabled = !isOther; });

        var gid = String(targetGroupId());
        var anyBlock = false;
        panel.querySelectorAll('.subject-block').forEach(function (block) {
            var show = block.dataset.group === gid;
            if (show) { anyBlock = true; }
            block.hidden = !show;
            block.querySelectorAll('input').forEach(function (i) {
                i.disabled = !show;
                if (!show) { i.checked = false; }
            });
        });
        noSubjects.hidden = anyBlock;
    }

    function pad(n) { return String(n).padStart(2, '0'); }

    function defaultExpiry() {
        var d = new Date(Date.now() + 2 * 3600 * 1000);
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
            'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function open() {
        ['btnClose', 'spClose'].forEach(function (id) {
            var el = document.getElementById(id);
            if (el) { el.click(); }
        });
        panel.classList.add('open');
    }

    btn.addEventListener('click', open);
    document.getElementById('hClose').addEventListener('click', function () {
        panel.classList.remove('open');
    });
    starosta.addEventListener('change', sync);
    target.addEventListener('change', sync);
    document.querySelectorAll('input[name="scope"]').forEach(function (r) {
        r.addEventListener('change', sync);
    });

    // клик по таблице закрывает панель помощи
    var table = document.querySelector('.journal-scroll');
    if (table) {
        table.addEventListener('click', function () { panel.classList.remove('open'); });
    }

    if (!expires.value) { expires.value = defaultExpiry(); }
    if (panel.dataset.open === 'true') { panel.classList.add('open'); }
    sync();
})();