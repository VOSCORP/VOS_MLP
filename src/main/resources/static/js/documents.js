// documents.js — окно документа
(function () {
  var modal = document.getElementById('docModal');
  if (!modal) { return; }
  var syncForm = document.getElementById('mSyncForm');
  var download = document.getElementById('mDownload');

  function setRow(id, value) {
    var row = document.getElementById(id);
    row.querySelector('dd').textContent = value || '';
    row.hidden = !value;
  }

  function open(btn) {
    var d = btn.dataset;
    document.getElementById('mTitle').textContent = d.title;
    setRow('rType', d.type);
    setRow('rNumber', d.number);
    setRow('rIssuer', d.issuer);
    setRow('rIssued', d.issued);
    setRow('rExpiry', d.expiry);
    setRow('rStatus', d.status);
    setRow('rSource', d.source);
    setRow('rNote', d.note);
    download.href = d.download;
    if (d.sync) {
      syncForm.action = d.sync;
      syncForm.hidden = false;
    } else {
      syncForm.hidden = true;
    }
    modal.hidden = false;
  }

  function close() { modal.hidden = true; }

  document.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-doc-open]');
    if (btn) { open(btn); return; }
    if (e.target === modal || e.target.closest('#mClose')) { close(); }
  });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') { close(); }
  });
})();