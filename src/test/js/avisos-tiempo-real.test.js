const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const codigo = readFileSync(join(__dirname, '../../main/resources/static/js/avisos-tiempo-real.js'), 'utf8');

test('WebSocket refresca recursos y recupera datos al reconectar o reactivar pestaña', () => {
    const conexiones = [];
    const eventos = [];
    const escucha = {};
    const timers = new Map();
    let contador = 0;
    class WebSocket {
        static OPEN = 1;
        static CONNECTING = 0;
        constructor(url) {
            this.url = url;
            this.readyState = WebSocket.CONNECTING;
            conexiones.push(this);
        }
        close() { this.readyState = 3; }
    }
    const window = {
        WebSocket,
        location: { protocol: 'https:', host: 'ejemplo.test' },
        setTimeout(fn) { const id = ++contador; timers.set(id, fn); return id; },
        clearTimeout(id) { timers.delete(id); },
        dispatchEvent(evento) { eventos.push(evento.detail.recurso); },
        addEventListener(tipo, fn) { escucha[tipo] = fn; }
    };
    const document = {
        visibilityState: 'visible',
        addEventListener(tipo, fn) { escucha[tipo] = fn; },
        createElement() { return { style: {}, setAttribute() {}, appendChild() {}, remove() {} }; },
        body: { appendChild() {} }
    };
    const avanzar = () => {
        const pendientes = [...timers.values()];
        timers.clear();
        pendientes.forEach(fn => fn());
    };
    vm.runInNewContext(codigo, { window, document, WebSocket,
        CustomEvent: class { constructor(tipo, opciones) { this.detail = opciones.detail; } } });
    assert.equal(conexiones[0].url, 'wss://ejemplo.test/ws/avisos');
    conexiones[0].readyState = WebSocket.OPEN;
    conexiones[0].onopen();
    conexiones[0].onmessage({ data: '{"tipo":"datos_actualizados","recurso":"tareas"}' });
    avanzar();
    assert.deepEqual(eventos, ['tareas']);
    conexiones[0].readyState = 3;
    conexiones[0].onclose();
    avanzar();
    assert.equal(conexiones.length, 2);
    conexiones[1].readyState = WebSocket.OPEN;
    conexiones[1].onopen();
    avanzar();
    assert.deepEqual(eventos, ['tareas', 'todos']);
    document.visibilityState = 'hidden';
    conexiones[1].onmessage({ data: '{"tipo":"datos_actualizados","recurso":"proyectos"}' });
    assert.equal(timers.size, 0);
    document.visibilityState = 'visible';
    escucha.visibilitychange();
    avanzar();
    assert.deepEqual(eventos, ['tareas', 'todos', 'todos']);
});
