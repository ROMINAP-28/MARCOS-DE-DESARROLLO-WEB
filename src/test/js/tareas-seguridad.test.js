const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const codigo = readFileSync(join(__dirname, '../../main/resources/static/js/tareas.js'), 'utf8');

function crear(fetch) {
    let componente;
    vm.runInNewContext(codigo, { Vue: { createApp(opciones) {
        componente = opciones;
        return { mount() {} };
    } }, fetch });
    const estado = { ...componente.methods, ...componente.data.call(componente.methods) };
    Object.defineProperties(estado, Object.fromEntries(Object.entries(componente.computed)
        .map(([nombre, get]) => [nombre, { get: () => get.call(estado) }])));
    return estado;
}

const respuesta = datos => ({ ok: true, json: async () => datos });

test('tareas usa proyecto y miembro activo por IDs y oculta gestión ajena', async () => {
    let cuerpo;
    const estado = crear(async (url, opciones = {}) => {
        if (url.endsWith('/tareas') && opciones.method === 'POST') {
            cuerpo = JSON.parse(opciones.body);
            return respuesta({ ...cuerpo, id: 30 });
        }
        if (url.endsWith('/tareas')) return respuesta([]);
        if (url.endsWith('/proyectos')) return respuesta([
            { id: 10, nombre: 'Propio', idResponsable: 2 },
            { id: 11, nombre: 'Ajeno', idResponsable: 3 }
        ]);
        if (url.endsWith('/participaciones')) return respuesta([
            { idProyecto: 10, idUsuario: 4, nombre: 'Miembro', estado: 'Activo' },
            { idProyecto: 10, idUsuario: 5, nombre: 'Inactivo', estado: 'Inactivo' }
        ]);
        if (url.endsWith('/auth/yo')) return respuesta({ id: 2, nombre: 'Responsable' });
        throw new Error(url);
    });
    await estado.cargarTareas();
    assert.equal(estado.proyectosGestionados.length, 1);
    assert.equal(estado.puedeGestionarTarea({ idProyecto: 11 }), false);
    estado.abrirNuevaTarea();
    estado.formulario.idProyecto = 10;
    assert.equal(estado.responsablesDisponibles.length, 1);
    Object.assign(estado.formulario, { idResponsable: 4, nombre: 'Verificar',
        prioridad: 'Alta', fechaFin: '2026-12-01' });
    await estado.guardarTarea();
    assert.equal(cuerpo.idProyecto, 10);
    assert.equal(cuerpo.idResponsable, 4);
    assert.equal(cuerpo.proyecto, undefined);
    assert.equal(cuerpo.responsable, undefined);
});
