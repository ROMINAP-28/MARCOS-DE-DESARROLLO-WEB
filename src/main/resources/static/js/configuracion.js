(() => {
    const formulario = document.getElementById('formularioPerfil');
    const errorPerfil = document.getElementById('perfilError');
    const exitoPerfil = document.getElementById('perfilExito');
    let idActual;
    const mensaje = (elemento, texto) => {
        elemento.textContent = texto;
        elemento.classList.toggle('d-none', !texto);
    };
    const mostrarCuenta = cuenta => {
        document.getElementById('nombreSidebar').textContent = cuenta.nombre;
        const avatar = document.querySelector('.user-avatar');
        if (avatar) {
            avatar.textContent = cuenta.nombre.trim().split(/\s+/).slice(0, 2)
                .map(parte => parte.charAt(0).toUpperCase()).join('');
        }
        document.getElementById('datosCuenta').textContent = `${cuenta.nombre} · ${cuenta.correo}`;
    };
    const obtener = async url => {
        const respuesta = await fetch(url);
        if (!respuesta.ok) throw new Error('No se pudieron cargar los datos del perfil.');
        return respuesta.json();
    };
    (async () => {
        try {
            const cuenta = await obtener('/api/auth/yo');
            idActual = cuenta.id; // Nunca se toma de un campo editable del formulario.
            const [perfil, especialidades] = await Promise.all([
                obtener(`/api/miembro3/usuarios/${idActual}`),
                obtener('/api/miembro3/especialidades')
            ]);
            mostrarCuenta(perfil);
            document.getElementById('perfilNombre').value = perfil.nombre;
            document.getElementById('perfilCorreo').value = perfil.correo;
            document.getElementById('perfilTelefono').value = perfil.telefono || '';
            const selector = document.getElementById('perfilEspecialidad');
            especialidades.forEach(item => selector.add(new Option(item.nombre, String(item.id))));
            selector.value = String(especialidades.find(item => item.nombre === perfil.especialidad)?.id || '');
            formulario.classList.remove('d-none');
        } catch (error) {
            mensaje(errorPerfil, error.message);
            document.getElementById('datosCuenta').textContent = 'No se pudieron cargar los datos de la cuenta.';
        }
    })();

    formulario.addEventListener('submit', async evento => {
        evento.preventDefault();
        if (idActual == null) return;
        mensaje(errorPerfil, '');
        mensaje(exitoPerfil, '');
        const boton = formulario.querySelector('button[type="submit"]');
        boton.disabled = true;
        try {
            const respuesta = await fetch(`/api/miembro3/usuarios/${idActual}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    nombre: document.getElementById('perfilNombre').value.trim(),
                    correo: document.getElementById('perfilCorreo').value.trim(),
                    telefono: document.getElementById('perfilTelefono').value.trim() || null,
                    idEspecialidad: Number(document.getElementById('perfilEspecialidad').value) || null
                })
            });
            if (!respuesta.ok) {
                const detalle = await respuesta.json().catch(() => ({}));
                throw new Error(detalle.mensaje || 'No se pudo actualizar el perfil.');
            }
            const actualizado = await respuesta.json();
            mostrarCuenta(actualizado);
            mensaje(exitoPerfil, 'Perfil actualizado correctamente. Si cambiaste tu correo, úsalo en el próximo inicio de sesión.');
        } catch (error) {
            mensaje(errorPerfil, error.message);
        } finally {
            boton.disabled = false;
        }
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
