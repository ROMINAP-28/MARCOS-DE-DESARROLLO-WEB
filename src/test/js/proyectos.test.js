const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const codigo = readFileSync(join(__dirname, '../../main/resources/static/js/proyectos.js'), 'utf8');
const plantilla = readFileSync(join(__dirname, '../../main/resources/templates/dashboard/proyectos.html'), 'utf8');

function crear(fetch, confirmar = () => true) {
    let componente;
    vm.runInNewContext(codigo, {
        fetch, window: { confirm: confirmar },
        Vue: { createApp(opciones) { componente = opciones; return { mount() {} }; } }
    });
    const estado = { ...componente.methods, ...componente.data.call(componente.methods) };
    Object.defineProperties(estado, Object.fromEntries(Object.entries(componente.computed)
        .map(([nombre, get]) => [nombre, { get: () => get.call(estado) }])));
    return estado;
}

const usuario = { id: 20, nombre: 'Ana', correo: 'ana@ejemplo.test' };
const proyecto = { id: 10, nombre: 'Inicial', descripcion: 'Descripción',
    fechaInicio: '2026-11-01', fechaFin: '2026-12-01', prioridad: 'Alta',
    responsable: 'Ana', idResponsable: 20, integrantes: ['Ana'], idsIntegrantes: [20],
    estado: 'Planificado', progreso: 0 };
const respuesta = (datos, status = 200) => ({ ok: true, status, json: async () => datos });

test('crear POST, editar PUT sin duplicado y eliminar DELETE confirmado', async () => {
    const llamadas = [];
    const estado = crear(async (url, opciones = {}) => {
        llamadas.push({ url, opciones });
        if (url.endsWith('/auth/yo')) return respuesta({ id: 20, nombre: 'Ana', rol: 'USUARIO' });
        if (url.endsWith('/usuarios')) return respuesta([usuario]);
        if (opciones.method === 'PUT') return respuesta({ ...proyecto, ...JSON.parse(opciones.body),
            id: 10, nombre: 'Editado' });
        if (opciones.method === 'POST') return respuesta({ ...proyecto, id: 11 });
        if (opciones.method === 'DELETE') return respuesta(null, 204);
        return respuesta([proyecto]);
    });
    await estado.cargarProyectos();
    assert.equal(estado.equipoDisponible[0].correo, usuario.correo);
    estado.editarProyecto(estado.proyectos[0]);
    estado.formulario.nombre = 'Editado';
    await estado.guardarProyecto();
    assert.equal(estado.proyectos.length, 1);
    assert.equal(estado.proyectos[0].nombre, 'Editado');
    const edicion = llamadas.find(llamada => llamada.opciones.method === 'PUT');
    assert.equal(edicion.url, '/api/miembro3/proyectos/10');
    assert.equal(JSON.parse(edicion.opciones.body).idResponsable, 20);
    assert.equal(JSON.parse(edicion.opciones.body).responsable, undefined);
    assert.deepEqual(JSON.parse(edicion.opciones.body).idsIntegrantes, [20]);

    estado.abrirModal();
    Object.assign(estado.formulario, { ...proyecto, idResponsable: 20,
        idsIntegrantes: [20], nombre: 'Nuevo' });
    await estado.guardarProyecto();
    assert.equal(estado.proyectos.length, 2);
    assert.ok(llamadas.some(llamada => llamada.opciones.method === 'POST'
        && llamada.url === '/api/miembro3/proyectos'));

    await estado.eliminarProyecto(estado.proyectos[0]);
    assert.equal(estado.proyectos.length, 1);
    assert.ok(llamadas.some(llamada => llamada.opciones.method === 'DELETE'
        && llamada.url === '/api/miembro3/proyectos/11'));
});

test('un error PUT o DELETE no modifica la lista, cancelar no envía DELETE', async () => {
    const llamadas = [];
    const estado = crear(async (url, opciones = {}) => {
        llamadas.push(opciones.method || 'GET');
        if (url.endsWith('/auth/yo')) return respuesta({ id: 20, nombre: 'Ana', rol: 'USUARIO' });
        if (url.endsWith('/usuarios')) return respuesta([usuario]);
        if (opciones.method) return { ok: false, json: async () => ({ mensaje: 'Error de prueba' }) };
        return respuesta([proyecto]);
    }, () => false);
    await estado.cargarProyectos();
    estado.editarProyecto(estado.proyectos[0]);
    await estado.guardarProyecto();
    assert.equal(estado.errorFormulario, 'Error de prueba');
    assert.equal(estado.proyectos.length, 1);
    assert.equal(estado.mostrarModal, true);
    await estado.eliminarProyecto(proyecto);
    assert.equal(llamadas.includes('DELETE'), false);
});

test('el formulario presenta el mensaje 400 por descripción excesiva sin anunciar guardado', async () => {
    const estado = crear(async (url, opciones = {}) => {
        if (url.endsWith('/auth/yo')) return respuesta({ id: 20, nombre: 'Ana' });
        if (url.endsWith('/usuarios')) return respuesta([usuario]);
        if (opciones.method === 'PUT') return {
            ok: false, status: 400,
            json: async () => ({ mensaje: 'descripcion: el tamaño debe estar entre 0 y 255' })
        };
        return respuesta([proyecto]);
    });
    await estado.cargarProyectos();
    estado.editarProyecto(estado.proyectos[0]);
    estado.formulario.descripcion = 'x'.repeat(256);
    await estado.guardarProyecto();
    assert.match(estado.errorFormulario, /descripcion.*255/);
    assert.equal(estado.mostrarModal, true);
    assert.equal(estado.proyectos[0].descripcion, 'Descripción');
    assert.equal(estado.mensajeExito, '');
});

test('los indicadores de proyectos son globales y el listado conserva sus filtros', async () => {
    const estado = crear(async url => {
        if (url.endsWith('/auth/yo')) return respuesta({ id: 20, nombre: 'Ana' });
        if (url.endsWith('/usuarios')) return respuesta([usuario]);
        return respuesta([proyecto,
            { ...proyecto, id: 11, nombre: 'Segundo', estado: 'En progreso', prioridad: 'Media' },
            { ...proyecto, id: 12, nombre: 'Tercero', estado: 'Completado', prioridad: 'Baja' }]);
    });
    await estado.cargarProyectos();
    assert.equal(estado.proyectos.length, 3);
    estado.busqueda = 'Segundo';
    estado.filtroEstado = 'En progreso';
    estado.filtroPrioridad = 'Media';
    assert.equal(estado.proyectosFiltrados.length, 1);
    assert.equal(estado.contarEstado('Planificado'), 1);
    assert.equal(estado.contarEstado('En progreso'), 1);
    assert.equal(estado.contarEstado('Completado'), 1);
    estado.busqueda = 'No existe';
    assert.equal(estado.proyectosFiltrados.length, 0);
    assert.equal(estado.proyectos.length, 3);
    assert.equal(estado.contarEstado('En progreso'), 1);
    estado.proyectos.push({ ...proyecto, id: 13, estado: 'Planificado' });
    assert.equal(estado.proyectos.length, 4);
    assert.equal(estado.contarEstado('Planificado'), 2);

    const resumen = plantilla.match(/class="row g-3 projects-summary"[\s\S]*?<\/section>/)?.[0];
    assert.ok(resumen);
    assert.match(resumen, /\{\{ proyectos\.length \}\}/);
    assert.doesNotMatch(resumen, /proyectosFiltrados\.length/);
    assert.match(plantilla, /\{\{ proyectosFiltrados\.length \}\} resultado\(s\)/);
});
