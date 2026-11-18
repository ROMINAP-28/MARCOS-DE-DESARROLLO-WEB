const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const codigo = readFileSync(join(__dirname, '../../main/resources/static/js/reportes.js'), 'utf8');

function crear(fetch, extras = {}) {
    let componente;
    const contexto = { fetch, Vue: { createApp(opciones) {
        componente = opciones;
        return { mount() {} };
    } }, ...extras };
    vm.runInNewContext(codigo, contexto);
    const estado = { ...componente.data(), ...componente.methods,
        $nextTick: callback => callback() };
    Object.defineProperties(estado, Object.fromEntries(Object.entries(componente.computed)
        .map(([nombre, get]) => [nombre, { get: () => get.call(estado) }])));
    return estado;
}

const respuesta = datos => ({ ok: true, json: async () => datos });

test('carga indicadores reales y consulta por proyecto, fechas y tipo', async () => {
    const llamadas = [];
    const estado = crear(async (url, opciones = {}) => {
        llamadas.push({ url, opciones });
        if (url.endsWith('/proyectos')) return respuesta([{ id: 12, nombre: 'Proyecto BD' }]);
        if (url.endsWith('/indicadores')) return respuesta({ proyectosActivos: 2,
            proyectosTotal: 5, tareasCompletadas: 7, tareasTotal: 10 });
        return respuesta({ tipo: 'tareas', proyecto: 'Proyecto BD', fechaConsulta: '2026-11-17T12:00:00',
            resumen: [{ nombre: 'Completadas', valor: '7' }] });
    });
    assert.equal(estado.indicadores, null);
    await estado.cargarDatos();
    assert.equal(estado.indicadores.tareasCompletadas, 7);
    assert.equal(estado.proyectos[0].id, 12);
    estado.tipoSeleccionado = 'tareas';
    estado.filtros = { idProyecto: 12, fechaDesde: '2026-11-01', fechaHasta: '2026-11-30' };
    await estado.generarReporte();
    assert.equal(estado.reporteActual.resumen[0].valor, '7');
    assert.deepEqual(JSON.parse(llamadas.at(-1).opciones.body), {
        tipo: 'tareas', idProyecto: 12, fechaDesde: '2026-11-01', fechaHasta: '2026-11-30'
    });
    assert.equal(llamadas.at(-1).opciones.method, 'POST');
});

test('errores de carga y consulta no muestran cifras ni vista previa ficticias', async () => {
    let fallar = false;
    const estado = crear(async (url, opciones = {}) => {
        if (fallar) return { ok: false, json: async () => ({ mensaje: 'API no disponible' }) };
        if (url.endsWith('/proyectos')) return respuesta([{ id: 1, nombre: 'Real' }]);
        if (url.endsWith('/indicadores')) return respuesta({ proyectosTotal: 1 });
        if (opciones.method === 'POST') return respuesta({ tipo: 'avance', resumen: [] });
    });
    await estado.cargarDatos();
    await estado.generarReporte();
    assert.ok(estado.reporteActual);
    fallar = true;
    await estado.generarReporte();
    assert.equal(estado.reporteActual, null);
    assert.equal(estado.mensajeError, 'API no disponible');
    await estado.cargarDatos();
    assert.equal(estado.indicadores, null);
    assert.equal(estado.proyectos.length, 0);
    assert.equal(estado.reporteActual, null);
});

test('CSV exporta resumen recibido del servidor y neutraliza fórmulas', async () => {
    let archivo;
    let click = false;
    const enlace = { click() { click = true; }, remove() {} };
    const estado = crear(async () => respuesta([]), {
        Blob: class { constructor(partes) { archivo = partes.join(''); } },
        URL: { createObjectURL: () => 'blob:csv', revokeObjectURL() {} },
        document: { createElement: () => enlace, body: { appendChild() {} } }
    });
    estado.descargarCSV({ tipo: 'tareas', proyecto: '=DATO', fechaConsulta: '2026-11-17T12:00:00',
        resumen: [{ nombre: 'Completadas', valor: '+SUM(1)' }] });
    assert.ok(click);
    assert.match(archivo, /"'=DATO"/);
    assert.match(archivo, /"'\+SUM\(1\)"/);
    assert.match(archivo, /"Completadas"/);
    assert.match(enlace.download, /^consulta-tareas-.*\.csv$/);
});
