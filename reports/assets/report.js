/* report.js — Consolidado (filtros de tarjetas) y caso (visor de pasos). Sin dependencias. */
(function () {
  'use strict';

  // ── Filtros por estado + búsqueda (tarjetas de casos o pasos) ──
  var buttons = document.querySelectorAll('.seg-btn[data-filter]');
  var search = document.getElementById('search');
  var current = 'all';

  function applyFilter() {
    var q = search ? search.value.trim().toLowerCase() : '';
    document.querySelectorAll('[data-item]').forEach(function (el) {
      var okStatus = current === 'all' || el.getAttribute('data-status') === current;
      var text = el.getAttribute('data-text') || el.textContent.toLowerCase();
      el.hidden = !(okStatus && (!q || text.indexOf(q) !== -1));
    });
  }
  buttons.forEach(function (btn) {
    btn.addEventListener('click', function () {
      buttons.forEach(function (b) { b.classList.remove('is-active'); });
      btn.classList.add('is-active');
      current = btn.getAttribute('data-filter');
      applyFilter();
    });
  });
  if (search) search.addEventListener('input', applyFilter);

  // ── Visor de capturas en pantalla completa ─────────────────────
  var box = document.getElementById('lightbox');
  function openBox(src, caption) {
    if (!box || !src) return;
    box.querySelector('img').src = src;
    box.querySelector('.lightbox-caption').textContent = caption || '';
    box.hidden = false;
  }
  if (box) {
    box.addEventListener('click', function (e) { if (e.target === box || e.target.closest('.lightbox-close')) box.hidden = true; });
  }

  // ── Visor de pasos (reporte de caso) ───────────────────────────
  var list = document.getElementById('stepList');
  if (!list) return;
  var items = Array.prototype.slice.call(list.querySelectorAll('.step-item'));
  var v = {
    num: document.getElementById('vNum'), title: document.getElementById('vTitle'), time: document.getElementById('vTime'),
    label: document.getElementById('vLabel'), error: document.getElementById('vError'), img: document.getElementById('vImg'),
    empty: document.getElementById('vEmpty'), stage: document.getElementById('vStage'), viewer: document.getElementById('viewer')
  };
  var selected = null;

  function select(item) {
    if (!item) return;
    items.forEach(function (i) { i.classList.remove('is-selected'); });
    item.classList.add('is-selected');
    selected = item;
    var status = item.getAttribute('data-status');
    v.viewer.className = 'viewer viewer-' + status;
    v.num.textContent = item.getAttribute('data-index');
    v.title.textContent = item.getAttribute('data-title');
    v.time.textContent = item.getAttribute('data-time');
    v.label.textContent = item.getAttribute('data-label');
    var err = item.getAttribute('data-error');
    v.error.hidden = !err;
    v.error.textContent = err;
    var img = item.getAttribute('data-img');
    v.img.hidden = !img;
    v.empty.hidden = !!img;
    if (img) { v.img.src = img; v.img.alt = 'Captura del paso ' + item.getAttribute('data-index'); }
    item.scrollIntoView({ block: 'nearest' });
  }

  function move(delta) {
    var visible = items.filter(function (i) { return !i.hidden; });
    var idx = visible.indexOf(selected);
    select(visible[Math.max(0, Math.min(visible.length - 1, idx + delta))]);
  }

  items.forEach(function (item) {
    item.addEventListener('click', function () { select(item); });
    item.addEventListener('keydown', function (e) { if (e.key === 'Enter') select(item); });
  });
  document.getElementById('vPrev').addEventListener('click', function () { move(-1); });
  document.getElementById('vNext').addEventListener('click', function () { move(1); });
  v.stage.addEventListener('click', function () {
    if (selected && selected.getAttribute('data-img')) {
      openBox(selected.getAttribute('data-img'), 'Paso ' + selected.getAttribute('data-index') + ': ' + selected.getAttribute('data-title'));
    }
  });
  document.addEventListener('keydown', function (e) {
    if (box && !box.hidden) { if (e.key === 'Escape') box.hidden = true; return; }
    if (e.key === 'ArrowDown' || e.key === 'ArrowRight') { e.preventDefault(); move(1); }
    if (e.key === 'ArrowUp' || e.key === 'ArrowLeft') { e.preventDefault(); move(-1); }
  });

  select(list.querySelector('.is-selected') || items[0]);
})();
