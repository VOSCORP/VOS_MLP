// notifications.js — всплывающие уведомления при открытии страницы
(function () {
    function csrfHeaders() {
        var h = {};
        var token = document.querySelector('meta[name="_csrf"]');
        var name = document.querySelector('meta[name="_csrf_header"]');
        if (token && name) { h[name.content] = token.content; }
        return h;
    }

    function show(stack, item) {
        var el = document.createElement('div');
        el.className = 'notice-toast';
        var text = document.createElement('span');
        text.textContent = item.text;
        el.appendChild(text);
        if (item.link) {
            var a = document.createElement('a');
            a.href = item.link;
            a.textContent = 'Открыть';
            el.appendChild(a);
        }
        stack.appendChild(el);
        setTimeout(function () { el.remove(); }, 7000);
    }

    async function load() {
        var list;
        try {
            var res = await fetch('/notifications/toasts', { method: 'POST', headers: csrfHeaders() });
            if (!res.ok) { return; }
            list = await res.json();
        } catch (e) {
            return;
        }
        if (!list.length) { return; }
        var stack = document.createElement('div');
        stack.className = 'notice-stack';
        document.body.appendChild(stack);
        list.slice(0, 4).forEach(function (item, i) {
            setTimeout(function () { show(stack, item); }, i * 400);
        });
    }

    load();
})();