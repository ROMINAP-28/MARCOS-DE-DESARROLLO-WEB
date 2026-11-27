(function () {
    var CLAVE = 'gestion-proyectos:tema';
    var VALIDOS = ['claro', 'oscuro', 'sistema'];
    var consulta = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

    function leer() {
        try {
            var guardado = localStorage.getItem(CLAVE);
            return VALIDOS.indexOf(guardado) !== -1 ? guardado : 'claro';
        } catch (error) {
            return 'claro';
        }
    }

    function efectivo(preferencia) {
        if (preferencia === 'sistema') {
            return consulta && consulta.matches ? 'dark' : 'light';
        }
        return preferencia === 'oscuro' ? 'dark' : 'light';
    }

    function aplicar(preferencia) {
        var raiz = document.documentElement;
        raiz.setAttribute('data-bs-theme', efectivo(preferencia));
        raiz.setAttribute('data-tema', preferencia);
    }

    function guardar(preferencia) {
        if (VALIDOS.indexOf(preferencia) === -1) return false;
        try {
            localStorage.setItem(CLAVE, preferencia);
            return true;
        } catch (error) {
            return false;
        }
    }

    aplicar(leer());

    if (consulta && consulta.addEventListener) {
        consulta.addEventListener('change', function () {
            if (leer() === 'sistema') aplicar('sistema');
        });
    }

    window.addEventListener('storage', function (evento) {
        if (evento.key === CLAVE) aplicar(leer());
    });

    window.GestionTema = { leer: leer, aplicar: aplicar, guardar: guardar };
})();
