// journalAccessForm.js — форма заявки старосты: предметы зависят от преподавателя и группы
(function () {
    var form = document.getElementById('jForm');
    if (!form) { return; }
    var teacher = document.getElementById('jTeacher');
    var target = document.getElementById('jTarget');
    var other = document.getElementById('jOther');
    var noSubjects = document.getElementById('jNoSubjects');
    var ownGroup = form.dataset.own;

    function scope() {
        return document.querySelector('input[name="scope"]:checked').value;
    }

    function groupId() {
        return scope() === 'other' ? target.value : ownGroup;
    }

    function sync() {
        var isOther = scope() === 'other';
        other.hidden = !isOther;
        target.disabled = !isOther;

        var any = false;
        form.querySelectorAll('.subject-block').forEach(function (block) {
            var show = block.dataset.teacher === String(teacher.value) && block.dataset.group === String(groupId());
            if (show) { any = true; }
            block.hidden = !show;
            block.querySelectorAll('input').forEach(function (i) {
                i.disabled = !show;
                if (!show) { i.checked = false; }
            });
        });
        noSubjects.hidden = any;
    }

    teacher.addEventListener('change', sync);
    target.addEventListener('change', sync);
    document.querySelectorAll('input[name="scope"]').forEach(function (r) {
        r.addEventListener('change', sync);
    });
    sync();
})();