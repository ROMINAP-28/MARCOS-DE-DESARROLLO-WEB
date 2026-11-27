const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const codigo = readFileSync(join(__dirname, '../../main/resources/static/js/configuracion.js'), 'utf8');

test('el formulario carga y guarda únicamente el perfil de la identidad autenticada', async () => {
    const peticiones = [];
    const elementos = new Map();
    const crear = () => ({ value: '', textContent: '', checked: false, disabled: false,
        classList: { add() {}, remove() {}, toggle() {} }, addEventListener(tipo, fn) { this[tipo] = fn; },
        querySelector() { return elementos.get('boton'); }, add() {} });
    for (const id of ['formularioPerfil', 'perfilError', 'perfilExito', 'nombreSidebar',
        'datosCuenta', 'perfilNombre', 'perfilCorreo', 'perfilTelefono',
        'perfilEspecialidad', 'boton', 'vistaCompacta', 'estadoPreferencia']) {
        elementos.set(id, crear());
    }
    const fetch = async (url, opciones) => {
        peticiones.push({ url, opciones });
        const datos = url === '/api/auth/yo' ? { id: 47, nombre: 'Persona', correo: 'a@ejemplo.test' }
            : url === '/api/miembro3/usuarios/47' && !opciones
                ? { id: 47, nombre: 'Persona', correo: 'a@ejemplo.test', telefono: '123', especialidad: null }
                : url === '/api/miembro3/especialidades' ? []
                    : { id: 47, nombre: 'Nombre nuevo', correo: 'a@ejemplo.test' };
        return { ok: true, json: async () => datos };
    };
    const document = { getElementById: id => elementos.get(id),
        querySelector: selector => selector === '.config-main' ? crear() : crear(),
        querySelectorAll: () => [] };
    const window = {};
    vm.runInNewContext(codigo, { document, window, fetch,
        localStorage: { getItem: () => null }, Option: class {} });
    await new Promise(setImmediate);
    assert.equal(elementos.get('perfilNombre').value, 'Persona');
    assert.equal(elementos.get('perfilTelefono').value, '123');
    elementos.get('perfilNombre').value = 'Nombre nuevo';
    await elementos.get('formularioPerfil').submit({ preventDefault() {} });
    assert.equal(peticiones.at(-1).url, '/api/miembro3/usuarios/47');
    assert.equal(peticiones.at(-1).opciones.method, 'PUT');
    const cuerpo = JSON.parse(peticiones.at(-1).opciones.body);
    assert.equal(cuerpo.nombre, 'Nombre nuevo');
    assert.equal(Object.hasOwn(cuerpo, 'idUsuario'), false);
    assert.match(elementos.get('perfilExito').textContent, /actualizado correctamente/);
});
