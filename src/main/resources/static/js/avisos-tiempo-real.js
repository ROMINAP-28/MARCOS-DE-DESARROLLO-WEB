// El WebSocket transporta avisos, no datos de negocio: cada vista relee la API.
(() => {
    if (!('WebSocket' in window)) return;
    let conexion;
    let reconexion;
    let refresco;
    let espera = 1000;
    let conectoAntes = false;
    let saliendo = false;
    const recursos = new Set();

    const solicitarRefresco = recurso => {
        recursos.add(recurso);
        if (document.visibilityState === 'hidden' || refresco) return;
        refresco = window.setTimeout(() => {
            refresco = null;
            const objetivo = recursos.size === 1 ? [...recursos][0] : 'todos';
            recursos.clear();
            window.dispatchEvent(new CustomEvent('gestion:actualizado', { detail: { recurso: objetivo } }));
        }, 100);
    };

    const mostrarAviso = mensaje => {
        const contenedor = document.createElement('div');
        contenedor.className = 'position-fixed bottom-0 end-0 p-3';
        contenedor.style.zIndex = '1080';
        const aviso = document.createElement('div');
        aviso.className = 'alert alert-info shadow mb-0';
        aviso.setAttribute('role', 'status');
        aviso.textContent = mensaje;
        const enlace = document.createElement('a');
        enlace.href = '/tareas';
        enlace.className = 'ms-2 alert-link';
        enlace.textContent = 'Ver tareas';
        aviso.appendChild(enlace);
        contenedor.appendChild(aviso);
        document.body.appendChild(contenedor);
        window.setTimeout(() => contenedor.remove(), 8000);
    };

    const conectar = () => {
        if (saliendo || conexion?.readyState === WebSocket.OPEN ||
                conexion?.readyState === WebSocket.CONNECTING) return;
        const protocolo = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        conexion = new WebSocket(`${protocolo}//${window.location.host}/ws/avisos`);
        conexion.onopen = () => {
            espera = 1000;
            if (conectoAntes) solicitarRefresco('todos');
            conectoAntes = true;
        };
        conexion.onmessage = evento => {
            let aviso;
            try { aviso = JSON.parse(evento.data); } catch { return; }
            if (aviso.tipo === 'datos_actualizados' &&
                    ['proyectos', 'tareas', 'equipo'].includes(aviso.recurso)) {
                solicitarRefresco(aviso.recurso);
            } else if (aviso.tipo === 'tarea_asignada') {
                mostrarAviso('Se te asignó una tarea.');
                solicitarRefresco('tareas');
            }
        };
        conexion.onclose = () => {
            if (saliendo) return;
            reconexion = window.setTimeout(conectar, espera);
            espera = Math.min(espera * 2, 30000);
        };
    };

    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState !== 'visible') return;
        // Recupera cambios que ocurrieron mientras la pestaña estaba suspendida.
        solicitarRefresco('todos');
        conectar();
    });
    window.addEventListener('pagehide', () => {
        saliendo = true;
        window.clearTimeout(reconexion);
        window.clearTimeout(refresco);
        conexion?.close();
    });
    conectar();
})();
