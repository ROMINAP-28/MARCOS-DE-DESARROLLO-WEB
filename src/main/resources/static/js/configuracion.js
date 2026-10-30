(() => {
    const interruptor = document.getElementById('vistaCompacta');
    const contenido = document.querySelector('.config-main');
    const estado = document.getElementById('estadoPreferencia');
    const clave = 'gestion-proyectos:configuracion:compacta';

    try {
        interruptor.checked = localStorage.getItem(clave) === 'true';
    } catch (error) {
        // La página sigue funcionando cuando el almacenamiento está bloqueado.
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
