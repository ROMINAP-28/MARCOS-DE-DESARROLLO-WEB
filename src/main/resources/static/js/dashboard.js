
// Vue.js: crea la aplicación reactiva del dashboard.
const { createApp } = Vue;

createApp({
    data() {
        return {
            // Datos demostrativos que luego se obtendrán desde la API REST.
            usuario: 'Usuario',

            proyectos: [
                { id: 1, nombre: 'Proyecto 1', progreso: 25 },
                { id: 2, nombre: 'Proyecto 2', progreso: 30 },
                { id: 3, nombre: 'Proyecto 3', progreso: 20 }
            ],

            tareas: [
                {
                    id: 1,
                    nombre: 'Diseñar interfaz',
                    fecha: '2026-09-29',
                    prioridad: 'Alta',
                    estado: 'Pendiente'
                },
                {
                    id: 2,
                    nombre: 'Revisar documentación',
                    fecha: '2026-09-30',
                    prioridad: 'Media',
                    estado: 'Pendiente'
                },
                {
                    id: 3,
                    nombre: 'Preparar presentación',
                    fecha: '2026-10-01',
                    prioridad: 'Baja',
                    estado: 'Completada'
                },
                {
                    id: 4,
                    nombre: 'Validar requisitos',
                    fecha: '2026-10-02',
                    prioridad: 'Alta',
                    estado: 'En progreso'
                }
            ]
        };
    },

    computed: {
        // Vue.js: calcula el total de tareas completadas.
        tareasCompletadas() {
            return this.tareas.filter(
                tarea => tarea.estado === 'Completada'
            ).length;
        },

        // Vue.js: filtra las tareas que aún no están completadas.
        tareasProximas() {
            return this.tareas.filter(
                tarea => tarea.estado !== 'Completada'
            );
        },

        // Vue.js: obtiene el saludo según la hora local.
        saludo() {
            const hora = new Date().getHours();

            if (hora < 12) return 'Buenos días';
            if (hora < 19) return 'Buenas tardes';
            return 'Buenas noches';
        },

        // Vue.js: genera las iniciales del usuario para su avatar.
        iniciales() {
            return this.usuario
                .trim()
                .split(/\s+/)
                .slice(0, 2)
                .map(palabra => palabra.charAt(0).toUpperCase())
                .join('');
        }
    },

    methods: {
        // Vue.js + Bootstrap: asigna el color según la prioridad.
        clasePrioridad(prioridad) {
            const clases = {
                Alta: 'text-bg-danger',
                Media: 'text-bg-warning',
                Baja: 'text-bg-success'
            };

            return clases[prioridad] || 'text-bg-secondary';
        },

        // Vue.js: presenta las fechas en formato día/mes/año.
        fechaLegible(fecha) {
            if (!fecha) return 'Sin fecha';

            const [anio, mes, dia] = fecha.split('-');
            return `${dia}/${mes}/${anio}`;
        }
    }
}).mount('#dashboardApp');