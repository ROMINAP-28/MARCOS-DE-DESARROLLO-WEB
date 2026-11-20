const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const codigo = readFileSync(join(__dirname, '../../main/resources/static/js/csrf.js'), 'utf8');

test('solo añade CSRF a escrituras del mismo origen y falla cerrado sin token', async () => {
    const llamadas = [];
    let token = 'secreto';
    const ventana = { location: { href: 'http://localhost:8080/proyectos',
        origin: 'http://localhost:8080', assign() {} },
        fetch: async (url, opciones) => { llamadas.push({ url, opciones }); return { status: 200 }; } };
    vm.runInNewContext(codigo, { window: ventana, URL, Headers, Request, Promise,
        document: { querySelector: selector => ({ content: selector.includes('_header')
            ? 'X-CSRF-TOKEN' : token }) } });
    await ventana.fetch('/api/miembro3/proyectos', { method: 'PUT', headers: { 'Content-Type': 'application/json' } });
    assert.equal(llamadas[0].opciones.headers.get('X-CSRF-TOKEN'), 'secreto');
    assert.equal(llamadas[0].opciones.headers.get('Content-Type'), 'application/json');
    await ventana.fetch('https://otro.test/externo', { method: 'POST' });
    assert.equal(llamadas[1].opciones.headers, undefined);
    await ventana.fetch('/api/miembro3/proyectos');
    assert.equal(llamadas[2].opciones.headers, undefined);
    token = '';
    await assert.rejects(ventana.fetch('/api/miembro3/proyectos', { method: 'DELETE' }),
        /Token de seguridad/);
    assert.equal(llamadas.length, 3);
});
