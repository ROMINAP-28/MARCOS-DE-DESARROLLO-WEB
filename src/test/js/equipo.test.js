const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const codigo = readFileSync(join(__dirname, '../../main/resources/static/js/equipo.js'), 'utf8');

function crear(fetch, window = { confirm: () => true, prompt: () => 'Nueva especialidad' }) {
    let componente;
    vm.runInNewContext(codigo, {
        Vue: { createApp(opciones) { componente = opciones; return { mount() {} }; } },
        fetch, window
    });
    const estado = { ...componente.data(), ...componente.methods };
    Object.defineProperties(estado, Object.fromEntries(Object.entries(componente.computed)
        .map(([nombre, get]) => [nombre, { get: () => get.call(estado) }])));
    return estado;
}

test('Equipo carga datos reales y solo presenta cambios confirmados por la API', async () => {
    const peticiones = [];
    const participaciones = [];
    const especialidades = [];
    let tareas = 0;
    const estado = crear(async (url, opciones = {}) => {
        peticiones.push(`${opciones.method || 'GET'} ${url}`);
        if (url.endsWith('/auth/yo')) return respuesta({ id: 20, nombre: 'Ana', rol: 'ADMIN' });
        if (url.endsWith('/proyectos')) return respuesta([{ id: 10, nombre: 'Proyecto real' }]);
        if (url.endsWith('/usuarios')) return respuesta([{ id: 20, nombre: 'Ana', correo: 'ana@ejemplo.com' }]);
        if (url.endsWith('/especialidades')) {
            if (opciones.method === 'POST') especialidades.push({ id: 1, nombre: JSON.parse(opciones.body).nombre });
            return respuesta(opciones.method === 'POST' ? especialidades.at(-1) : especialidades);
        }
        if (url.endsWith('/tareas')) {
            const guardada = JSON.parse(opciones.body);
            assert.equal(guardada.proyecto, undefined);
            assert.equal(guardada.responsable, undefined);
            assert.equal(guardada.idProyecto, 10);
            assert.equal(guardada.idResponsable, 20);
            assert.equal(guardada.fechaInicio, '2026-11-20');
            tareas++;
            participaciones[0].tareasAsignadas = tareas;
            return respuesta({ id: tareas });
        }
        if (url.endsWith('/participaciones')) {
            if (opciones.method === 'POST') {
                const datos = JSON.parse(opciones.body);
                participaciones.push({ ...datos, proyecto: 'Proyecto real', nombre: 'Ana',
                    correo: 'ana@ejemplo.com', rol: 'Integrante', especialidad: null,
                    fechaAsignacion: '2026-11-17T12:00:00', tareasAsignadas: 0 });
            }
            return respuesta(opciones.method === 'POST' ? participaciones.at(-1) : participaciones);
        }
        if (url.endsWith('/10/20') && opciones.method === 'PUT') {
            participaciones[0].estado = JSON.parse(opciones.body).estado;
            return respuesta(participaciones[0]);
        }
        if (url.endsWith('/10/20') && opciones.method === 'DELETE') {
            participaciones.length = 0;
            return { ok: true, status: 204 };
        }
        throw new Error(`Ruta inesperada: ${url}`);
    });

    assert.equal(estado.integrantes.length, 0);
    await estado.cargarDatos();
    assert.equal(estado.proyectos[0].nombre, 'Proyecto real');
    assert.equal(estado.integrantes.length, 0);

    estado.abrirNuevoIntegrante();
    estado.formulario = { idProyecto: 10, idUsuario: 20, estado: 'Activo' };
    await estado.guardarIntegrante();
    assert.equal(estado.integrantes.length, 1);
    assert.equal(estado.integrantes[0].especialidad, 'Sin especialidad');

    estado.editarIntegrante(estado.integrantes[0]);
    estado.formulario.estado = 'Inactivo';
    await estado.guardarIntegrante();
    assert.equal(estado.integrantes[0].estado, 'Inactivo');

    await estado.retirarIntegrante(estado.integrantes[0]);
    assert.equal(estado.integrantes.length, 0);
    estado.abrirNuevoIntegrante();
    estado.formulario = { idProyecto: 10, idUsuario: 20, estado: 'Activo' };
    await estado.guardarIntegrante();

    estado.asignarTarea(estado.integrantes[0]);
    estado.formularioTarea = { nombre: 'Verificar', descripcion: '', prioridad: 'Alta',
        fechaInicio: '2026-11-20', fechaFin: '2026-12-01' };
    await estado.guardarTareaAsignada();
    assert.equal(estado.integrantes[0].tareasAsignadas, 1);

    await estado.abrirEspecialidad();
    assert.equal(estado.especialidades[0].nombre, 'Nueva especialidad');

    for (const metodo of ['POST /api/miembro3/participaciones',
        'PUT /api/miembro3/participaciones/10/20', 'POST /api/miembro3/tareas',
        'POST /api/miembro3/especialidades', 'DELETE /api/miembro3/participaciones/10/20']) {
        assert.ok(peticiones.includes(metodo), metodo);
    }
});

test('un fallo de guardado no agrega integrantes locales', async () => {
    const estado = crear(async (url, opciones = {}) => {
        if (url.endsWith('/auth/yo')) return respuesta({ id: 20, nombre: 'Ana', rol: 'USUARIO' });
        if (opciones.method === 'POST') return { ok: false, json: async () => ({ mensaje: 'Ya pertenece al proyecto' }) };
        if (url.endsWith('/proyectos')) return respuesta([{ id: 10, nombre: 'Proyecto real' }]);
        if (url.endsWith('/usuarios')) return respuesta([{ id: 20, nombre: 'Ana' }]);
        return respuesta([]);
    });
    await estado.cargarDatos();
    estado.abrirNuevoIntegrante();
    estado.formulario = { idProyecto: 10, idUsuario: 20, estado: 'Activo' };
    await estado.guardarIntegrante();
    assert.equal(estado.integrantes.length, 0);
    assert.equal(estado.mensajeError, 'Ya pertenece al proyecto');
});

test('un fallo al recargar oculta datos anteriores y permite reintentar', async () => {
    let fallar = false;
    const estado = crear(async (url) => {
        if (fallar) throw new Error('Sin conexión');
        if (url.endsWith('/auth/yo')) return respuesta({ id: 20, nombre: 'Ana', rol: 'USUARIO' });
        if (url.endsWith('/participaciones')) return respuesta([{ idProyecto: 10, idUsuario: 20,
            nombre: 'Ana', proyecto: 'Proyecto real', rol: 'Integrante', estado: 'Activo',
            tareasAsignadas: 2 }]);
        return respuesta([]);
    });
    assert.equal(await estado.cargarDatos(), true);
    assert.equal(estado.totalTareasAsignadas, 2);
    fallar = true;
    assert.equal(await estado.cargarDatos(), false);
    assert.equal(estado.errorCarga, true);
    assert.equal(estado.integrantes.length, 0);
    assert.equal(estado.totalTareasAsignadas, 0);
    fallar = false;
    assert.equal(await estado.cargarDatos(), true);
    assert.equal(estado.errorCarga, false);
    assert.equal(estado.totalTareasAsignadas, 2);
});

function respuesta(datos) {
    return { ok: true, status: 200, json: async () => datos };
}
