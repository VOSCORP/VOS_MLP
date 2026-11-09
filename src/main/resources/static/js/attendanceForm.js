// attendanceForm.js — выбор пропусков по диапазону дат
(function () {
    var from = document.getElementById('rangeFrom');
    var to = document.getElementById('rangeTo');
    var rows = document.querySelectorAll('tr[data-date]');
    if (!from || !rows.length) { return; }

    function setAll(value) {
        rows.forEach(function (r) { r.querySelector('input[type="checkbox"]').checked = value; });
    }

    document.getElementById('rangeApply').addEventListener('click', function () {
        if (!from.value || !to.value) { return; }
        rows.forEach(function (r) {
            var d = r.dataset.date; // формат 2026-09-04, строки сравниваются правильно
            if (d >= from.value && d <= to.value) {
                r.querySelector('input[type="checkbox"]').checked = true;
            }
        });
    });
    document.getElementById('selectAll').addEventListener('click', function () { setAll(true); });
    document.getElementById('selectNone').addEventListener('click', function () { setAll(false); });
})();