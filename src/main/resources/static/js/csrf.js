// Protege únicamente las peticiones de escritura al mismo origen.
// El token se obtiene de la página Thymeleaf, nunca de localStorage.
(() => {
    const original = window.fetch.bind(window);
    window.fetch = (url, opciones = {}) => {
        const metodo = (opciones.method || 'GET').toUpperCase();
        const destino = new URL(url instanceof Request ? url.url : url, window.location.href);
        if (destino.origin !== window.location.origin) {
            return original(url, opciones);
        }
        const verificarSesion = promesa => promesa.then(respuesta => {
            if (respuesta.status === 401 && destino.pathname.startsWith('/api/')) {
                window.location.assign('/login');
            }
            return respuesta;
        });
        if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(metodo)) {
            return verificarSesion(original(url, opciones));
        }
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const cabecera = document.querySelector('meta[name="_csrf_header"]')?.content;
        if (!token || !cabecera) return Promise.reject(new Error('Token de seguridad no disponible. Recarga la página.'));
        const headers = new Headers(opciones.headers);
        headers.set(cabecera, token);
        return verificarSesion(original(url, { ...opciones, headers }));
    };
})();
