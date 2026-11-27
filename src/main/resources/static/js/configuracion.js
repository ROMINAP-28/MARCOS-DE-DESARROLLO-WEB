(() => {
    fetch('/api/auth/yo').then(respuesta => {
        if (!respuesta.ok) throw new Error('No se pudieron consultar los datos de la cuenta.');
        return respuesta.json();
    }).then(cuenta => {
        document.getElementById('nombreSidebar').textContent = cuenta.nombre;
        const avatar = document.querySelector('.user-avatar');
        if (avatar) {
            avatar.textContent = cuenta.nombre.trim().split(/\s+/).slice(0, 2)
                .map(parte => parte.charAt(0).toUpperCase()).join('');
        }
        document.getElementById('datosCuenta').textContent = `${cuenta.nombre} · ${cuenta.correo}`;
    }).catch(() => {
        document.getElementById('datosCuenta').textContent = 'No se pudieron cargar los datos de la cuenta.';
    });
    // Tema claro / oscuro / automático (aplicado en todas las pantallas por tema.js).
    const opcionesTema = document.querySelectorAll('input[name="tema"]');
    const ayudaTema = document.getElementById('temaAyuda');
    const mensajesTema = {
        claro: 'Tema claro activado.',
        oscuro: 'Tema oscuro activado.',
        sistema: 'La plataforma seguirá el tema de tu dispositivo.'
    };

    if (window.GestionTema) {
        const temaActual = window.GestionTema.leer();
        opcionesTema.forEach(opcion => {
            opcion.checked = opcion.value === temaActual;
            opcion.addEventListener('change', () => {
                if (!opcion.checked) return;
                window.GestionTema.aplicar(opcion.value);
                const guardado = window.GestionTema.guardar(opcion.value);
                ayudaTema.textContent = guardado
                    ? mensajesTema[opcion.value]
                    : 'El tema se aplicó, pero no se pudo guardar en este navegador.';
            });
        });
    }

    const interruptor = document.getElementById('vistaCompacta');
    const contenido = document.querySelector('.config-main');
    const estado = document.getElementById('estadoPreferencia');
    const clave = 'gestion-proyectos:configuracion:compacta';

    try {
        interruptor.checked = localStorage.getItem(clave) === 'true';
    } catch (error) {
    }

    const aplicarPreferencia = () => {
        contenido.classList.toggle('config-compact', interruptor.checked);
    };

    aplicarPreferencia();

    interruptor.addEventListener('change', () => {
        aplicarPreferencia();
        try {
            localStorage.setItem(clave, String(interruptor.checked));
            estado.textContent = 'Preferencia de visualización guardada en este navegador.';
        } catch (error) {
            estado.textContent = 'La preferencia se aplicó, pero no se pudo guardar en este navegador.';
        }
    });
})();
