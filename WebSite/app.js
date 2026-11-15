// Lógica mínima del sitio demo (solo para practicar; no es seguridad real).
(function () {
  var USERS = { admin: 'Admin123', qa: 'Qa2026!' };

  var form = document.getElementById('login-form');
  if (form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var user = document.getElementById('username').value.trim();
      var pass = document.getElementById('password').value;
      var error = document.getElementById('error');

      if (!user || !pass) {
        error.textContent = 'Usuario y contraseña son obligatorios';
        error.hidden = false;
        return;
      }
      if (USERS[user] !== pass) {
        error.textContent = 'Usuario o contraseña incorrectos';
        error.hidden = false;
        return;
      }
      sessionStorage.setItem('user', user);
      window.location.href = 'home.html';
    });
  }

  var welcome = document.getElementById('welcome');
  if (welcome) {
    var current = sessionStorage.getItem('user');
    if (!current) { window.location.href = 'index.html'; return; }
    welcome.textContent = 'Bienvenido, ' + current;
    document.getElementById('logout').addEventListener('click', function () {
      sessionStorage.removeItem('user');
      window.location.href = '/';
    });
  }
})();
