/* index.js — Búsqueda, filtros y orden cronológico de la página principal. */
(function () {
  'use strict';

  var timeline = document.getElementById('timeline');
  if (!timeline) return;

  var search = document.getElementById('runSearch');
  var envFilter = document.getElementById('envFilter');
  var sortBtn = document.getElementById('sortToggle');
  var noResults = document.getElementById('noResults');
  var filterButtons = document.querySelectorAll('.seg-btn[data-filter]');
  var status = 'all';

  function apply() {
    var q = search ? search.value.trim().toLowerCase() : '';
    var env = envFilter ? envFilter.value : '';
    var visible = 0;

    timeline.querySelectorAll('.day').forEach(function (day) {
      var dayVisible = 0;
      day.querySelectorAll('.run').forEach(function (run) {
        var ok =
          (status === 'all' || run.getAttribute('data-status') === status) &&
          (!env || run.getAttribute('data-env') === env) &&
          (!q || run.getAttribute('data-text').indexOf(q) !== -1 || run.getAttribute('data-ts').indexOf(q) !== -1);
        run.style.display = ok ? '' : 'none';
        if (ok) dayVisible++;
      });
      day.style.display = dayVisible ? '' : 'none';
      visible += dayVisible;
    });

    if (noResults) noResults.hidden = visible > 0 || !timeline.querySelector('.run');
  }

  filterButtons.forEach(function (btn) {
    btn.addEventListener('click', function () {
      filterButtons.forEach(function (b) { b.classList.remove('is-active'); });
      btn.classList.add('is-active');
      status = btn.getAttribute('data-filter');
      apply();
    });
  });
  if (search) search.addEventListener('input', apply);
  if (envFilter) envFilter.addEventListener('change', apply);

  // Invertir el orden cronológico (días y ejecuciones dentro de cada día).
  if (sortBtn) {
    sortBtn.addEventListener('click', function () {
      var desc = sortBtn.getAttribute('data-order') === 'desc';
      var days = Array.prototype.slice.call(timeline.querySelectorAll('.day')).reverse();
      days.forEach(function (day) {
        var list = day.querySelector('.day-runs');
        Array.prototype.slice.call(list.children).reverse().forEach(function (r) { list.appendChild(r); });
        timeline.appendChild(day);
      });
      sortBtn.setAttribute('data-order', desc ? 'asc' : 'desc');
      sortBtn.querySelector('span').textContent = desc ? 'Más antiguas primero' : 'Más recientes primero';
      try { localStorage.setItem('reportOrder', desc ? 'asc' : 'desc'); } catch (e) { /* sin almacenamiento */ }
    });
    try { if (localStorage.getItem('reportOrder') === 'asc') sortBtn.click(); } catch (e) { /* sin almacenamiento */ }
  }
})();
