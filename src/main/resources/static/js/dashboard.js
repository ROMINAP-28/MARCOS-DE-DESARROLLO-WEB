
// Vue.js: crea la aplicación reactiva del dashboard.
const { createApp } = Vue;

createApp({
    data() {
        return {
            usuario: 'Usuario',
            proyectos: [],
            tareas: [],
            cargando: true,
            errorCarga: ''
        };
    },

    mounted() {
        this.cargarResumen();
        this.alActualizar = evento => {
            if (['todos', 'proyectos', 'tareas'].includes(evento.detail?.recurso)) {
                this.cargarResumen();
            }
        };
        window.addEventListener('gestion:actualizado', this.alActualizar);
    },

    beforeUnmount() {
        window.removeEventListener('gestion:actualizado', this.alActualizar);
    },

    computed: {
        // Vue.js: calcula el total de tareas completadas.
        tareasCompletadas() {
            return this.tareas.filter(
                tarea => tarea.estado === 'Completada'
            ).length;
        },

        // Tareas no completadas que vencen desde hoy hasta dentro de 7 días.
        tareasProximas() {
            const hoy = new Date();
            const finVentana = new Date(hoy);
            finVentana.setDate(finVentana.getDate() + 7);
            const desde = this.fechaISO(hoy);
            const hasta = this.fechaISO(finVentana);

            return this.tareas.filter(
                tarea => tarea.estado !== 'Completada' &&
                    tarea.fechaFin && tarea.fechaFin >= desde && tarea.fechaFin <= hasta
            ).sort((a, b) => a.fechaFin.localeCompare(b.fechaFin));
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
        async cargarResumen() {
            this.cargando = true;
            this.errorCarga = '';

            try {
                const [respuestaProyectos, respuestaTareas] = await Promise.all([
                    fetch('/api/miembro3/proyectos'),
                    fetch('/api/miembro3/tareas')
                ]);
                if (!respuestaProyectos.ok || !respuestaTareas.ok) {
                    throw new Error('No se pudo cargar el resumen. Inténtalo de nuevo.');
                }

                const [proyectos, tareas] = await Promise.all([
                    respuestaProyectos.json(), respuestaTareas.json()
                ]);
                if (!Array.isArray(proyectos) || !Array.isArray(tareas)) {
                    throw new Error('La respuesta del servidor no es válida.');
                }

                this.proyectos = proyectos;
                this.tareas = tareas;
            } catch (error) {
                this.proyectos = [];
                this.tareas = [];
                this.errorCarga = error.message || 'No se pudo cargar el resumen.';
            } finally {
                this.cargando = false;
            }
        },

        fechaISO(fecha) {
            const anio = fecha.getFullYear();
            const mes = String(fecha.getMonth() + 1).padStart(2, '0');
            const dia = String(fecha.getDate()).padStart(2, '0');
            return `${anio}-${mes}-${dia}`;
        },

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
