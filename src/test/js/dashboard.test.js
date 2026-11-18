const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const script = readFileSync(join(__dirname, '../../main/resources/static/js/dashboard.js'), 'utf8');

function crearDashboard(fetch) {
    let componente;
    vm.runInNewContext(script, {
        Vue: {
            createApp(opciones) {
                componente = opciones;
                return { mount() {} };
            }
        },
        fetch,
        Date
    });
    const estado = { ...componente.data(), ...componente.methods };
    return { componente, estado };
}

function respuesta(datos) {
    return { ok: true, json: async () => datos };
}

test('carga proyectos y tareas de los endpoints y calcula métricas reales', async () => {
    const rutas = [];
    const proyectos = [{ id: 1, nombre: 'Proyecto real', progreso: 40 }];
    const { componente, estado } = crearDashboard(async ruta => {
        rutas.push(ruta);
        return respuesta(ruta.endsWith('/proyectos') ? proyectos : [
            { id: 1, estado: 'Completada', fechaFin: '2026-01-01' }
        ]);
    });

    assert.equal(estado.proyectos.length, 0);
    assert.equal(estado.tareas.length, 0);
    await estado.cargarResumen();

    assert.deepEqual(rutas, ['/api/miembro3/proyectos', '/api/miembro3/tareas']);
    assert.equal(estado.proyectos[0].nombre, 'Proyecto real');
    assert.equal(estado.tareas.length, 1);
    assert.equal(componente.computed.tareasCompletadas.call(estado), 1);
    assert.equal(estado.cargando, false);
    assert.equal(estado.errorCarga, '');
});

test('próximas a vencer usa fechaFin, estado y la ventana de siete días', () => {
    const { componente, estado } = crearDashboard(async () => respuesta([]));
    const fecha = dias => {
        const valor = new Date();
        valor.setDate(valor.getDate() + dias);
        return estado.fechaISO(valor);
    };
    estado.tareas = [
        { id: 1, estado: 'Pendiente', fechaFin: fecha(8) },
        { id: 2, estado: 'En progreso', fechaFin: fecha(7) },
        { id: 3, estado: 'Completada', fechaFin: fecha(1) },
        { id: 4, estado: 'Pendiente', fechaFin: fecha(-1) },
        { id: 5, estado: 'Pendiente', fechaFin: fecha(0) },
        { id: 6, estado: 'Pendiente', fechaFin: null }
    ];

    const proximas = componente.computed.tareasProximas.call(estado);
    assert.deepEqual(Array.from(proximas, tarea => tarea.id), [5, 2]);
    assert.equal(estado.fechaLegible(fecha(0)), fecha(0).split('-').reverse().join('/'));
});

test('un error no presenta contadores engañosos y permite reintentar', async () => {
    let fallar = true;
    const { estado } = crearDashboard(async ruta => {
        if (fallar && ruta.endsWith('/tareas')) return { ok: false };
        return respuesta(ruta.endsWith('/proyectos') ? [{ id: 1 }] : []);
    });

    await estado.cargarResumen();
    assert.equal(estado.proyectos.length, 0);
    assert.equal(estado.tareas.length, 0);
    assert.ok(estado.errorCarga);
    assert.equal(estado.cargando, false);

    fallar = false;
    await estado.cargarResumen();
    assert.equal(estado.proyectos.length, 1);
    assert.equal(estado.errorCarga, '');
});
