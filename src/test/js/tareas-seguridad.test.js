const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const codigo = readFileSync(join(__dirname, '../../main/resources/static/js/tareas.js'), 'utf8');
const plantilla = readFileSync(join(__dirname, '../../main/resources/templates/dashboard/tareas.html'), 'utf8');

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
        prioridad: 'Alta', fechaInicio: '2026-11-15', fechaFin: '2026-12-01' });
    await estado.guardarTarea();
    assert.equal(cuerpo.idProyecto, 10);
    assert.equal(cuerpo.idResponsable, 4);
    assert.equal(cuerpo.fechaInicio, '2026-11-15');
    assert.equal(cuerpo.proyecto, undefined);
    assert.equal(cuerpo.responsable, undefined);
});

test('editar fecha de inicio conserva su valor y muestra errores 400 sin alterar la tarea', async () => {
    const tarea = { id: 30, nombre: 'Tarea', descripcion: 'Descripción', idProyecto: 10,
        idResponsable: 4, proyecto: 'Propio', responsable: 'Miembro',
        prioridad: 'Alta', estado: 'Pendiente', fechaInicio: '2026-11-15', fechaFin: '2026-12-01' };
    let enviado;
    let fallo = true;
    const estado = crear(async (url, opciones = {}) => {
        if (url.endsWith('/auth/yo')) return respuesta({ id: 2, nombre: 'Responsable' });
        if (url.endsWith('/participaciones')) return respuesta([
            { idProyecto: 10, idUsuario: 4, nombre: 'Miembro', estado: 'Activo' }
        ]);
        if (url.endsWith('/proyectos')) return respuesta([{ id: 10, nombre: 'Propio', idResponsable: 2 }]);
        if (url.endsWith('/tareas/30') && opciones.method === 'PUT') {
            enviado = JSON.parse(opciones.body);
            if (fallo) return { ok: false, json: async () => ({
                mensaje: 'La fecha de fin no puede ser anterior a la fecha de inicio'
            }) };
            return respuesta({ ...tarea, ...enviado });
        }
        if (url.endsWith('/tareas')) return respuesta([tarea]);
        throw new Error(url);
    });
    await estado.cargarTareas();
    estado.editarTarea(estado.tareas[0]);
    assert.equal(estado.formulario.fechaInicio, '2026-11-15');
    estado.formulario.fechaInicio = '2026-12-02';
    await estado.guardarTarea();
    assert.match(estado.mensajeError, /fecha de fin/);
    assert.equal(estado.tareas[0].fechaInicio, '2026-11-15');
    assert.equal(estado.mostrarModal, true);

    fallo = false;
    estado.formulario.fechaInicio = '2026-11-20';
    await estado.guardarTarea();
    assert.equal(enviado.fechaInicio, '2026-11-20');
    assert.equal(estado.tareas[0].fechaInicio, '2026-11-20');
    assert.equal(estado.mostrarModal, false);
});

test('resumen de tareas no cambia con filtros, pero resultados y Kanban sí', async () => {
    const tareas = [
        { id: 1, nombre: 'Uno', descripcion: '', proyecto: 'A', responsable: 'Ana',
            prioridad: 'Alta', estado: 'Pendiente' },
        { id: 2, nombre: 'Dos', descripcion: '', proyecto: 'B', responsable: 'Luis',
            prioridad: 'Media', estado: 'En progreso' },
        { id: 3, nombre: 'Tres', descripcion: '', proyecto: 'A', responsable: 'Ana',
            prioridad: 'Baja', estado: 'Completada' }
    ];
    const estado = crear(async url => {
        if (url.endsWith('/auth/yo')) return respuesta({ id: 2, nombre: 'Responsable' });
        if (url.endsWith('/participaciones')) return respuesta([]);
        if (url.endsWith('/proyectos')) return respuesta([]);
        if (url.endsWith('/tareas')) return respuesta(tareas);
        throw new Error(url);
    });
    await estado.cargarTareas();
    assert.equal(estado.tareas.length, 3);
    estado.busqueda = 'Dos';
    estado.filtroProyecto = 'B';
    estado.filtroPrioridad = 'Media';
    estado.filtroEstado = 'En progreso';
    estado.filtroResponsable = 'Luis';
    assert.equal(estado.tareasFiltradas.length, 1);
    assert.equal(estado.tareasPorEstado('En progreso').length, 1);
    for (const estadoTarea of ['Pendiente', 'En progreso', 'Completada']) {
        assert.equal(estado.contarEstado(estadoTarea), 1);
    }
    estado.busqueda = 'No existe';
    assert.equal(estado.tareasFiltradas.length, 0);
    assert.equal(estado.tareasPorEstado('En progreso').length, 0);
    assert.equal(estado.tareas.length, 3);
    assert.equal(estado.contarEstado('En progreso'), 1);
    estado.tareas[0].estado = 'Completada';
    assert.equal(estado.contarEstado('Pendiente'), 0);
    assert.equal(estado.contarEstado('Completada'), 2);

    const resumen = plantilla.match(/<section class="task-stats">[\s\S]*?<\/section>/)?.[0];
    assert.ok(resumen);
    assert.match(resumen, /\{\{ tareas\.length \}\}/);
    assert.doesNotMatch(resumen, /tareasFiltradas\.length/);
    assert.match(plantilla, /\{\{ tareasFiltradas\.length \}\} resultado\(s\)/);
    assert.match(plantilla, /\{\{ tareasPorEstado\(estado\)\.length \}\}/);
});
