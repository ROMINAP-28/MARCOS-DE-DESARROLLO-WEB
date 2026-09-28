
/* Vue.js: inicializa la lógica interactiva de Proyectos. */
const { createApp } = Vue;

createApp({
    data() {
        return {
            usuario: 'Usuario',
            busqueda: '',
            filtroEstado: '',
            filtroPrioridad: '',
            mostrarModal: false,
            modoEdicion: false,
            idEdicion: null,
            errorFormulario: '',
            mensajeExito: '',

            // Datos de demostración hasta integrar la API REST.
            equipoDisponible: [
                'María López',
                'Carlos Torres',
                'Ana García',
                'José Ramírez',
                'Brayan Quispe'
            ],

            proyectos: [
                {
                    id: 1,
                    nombre: 'Plataforma web colaborativa',
                    descripcion: 'Sistema para organizar equipos, tareas y proyectos.',
                    fechaInicio: '2026-09-10',
                    fechaFin: '2026-10-30',
                    prioridad: 'Alta',
                    responsable: 'María López',
                    integrantes: ['María López', 'Carlos Torres', 'Ana García'],
                    estado: 'En progreso',
                    progreso: 65
                },
                {
                    id: 2,
                    nombre: 'Sistema de inventario',
                    descripcion: 'Aplicación para administrar productos y existencias.',
                    fechaInicio: '2026-09-15',
                    fechaFin: '2026-11-15',
                    prioridad: 'Media',
                    responsable: 'Carlos Torres',
                    integrantes: ['Carlos Torres', 'José Ramírez'],
                    estado: 'Planificado',
                    progreso: 0
                },
                {
                    id: 3,
                    nombre: 'Portal de reportes',
                    descripcion: 'Panel de consulta y generación de informes.',
                    fechaInicio: '2026-08-01',
                    fechaFin: '2026-09-20',
                    prioridad: 'Baja',
                    responsable: 'Ana García',
                    integrantes: ['Ana García', 'Brayan Quispe'],
                    estado: 'Completado',
                    progreso: 100
                }
            ],

            // Vue.js: modelo reactivo del formulario.
            formulario: thisFormularioVacio()
        };
    },

    computed: {
        // Vue.js: filtra proyectos por búsqueda, estado y prioridad.
        proyectosFiltrados() {
            const termino = this.busqueda.toLowerCase();

            return this.proyectos.filter(proyecto => {
                const coincideTexto =
                    proyecto.nombre.toLowerCase().includes(termino) ||
                    proyecto.descripcion.toLowerCase().includes(termino);

                const coincideEstado =
                    !this.filtroEstado ||
                    proyecto.estado === this.filtroEstado;

                const coincidePrioridad =
                    !this.filtroPrioridad ||
                    proyecto.prioridad === this.filtroPrioridad;

                return coincideTexto && coincideEstado && coincidePrioridad;
            });
        },

        // Vue.js: calcula las iniciales para el avatar del usuario.
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
        // Vue.js: abre el modal en modo de creación.
        abrirModal() {
            this.modoEdicion = false;
            this.idEdicion = null;
            this.formulario = this.formularioVacio();
            this.errorFormulario = '';
            this.mostrarModal = true;
        },

        // Vue.js: devuelve un formulario limpio.
        formularioVacio() {
            return {
                nombre: '',
                descripcion: '',
                fechaInicio: '',
                fechaFin: '',
                prioridad: '',
                responsable: '',
                integrantes: []
            };
        },

        // Vue.js: cierra el modal y limpia el mensaje de validación.
        cerrarModal() {
            this.mostrarModal = false;
            this.errorFormulario = '';
        },

        // Vue.js: registra un proyecto o actualiza uno existente.
        guardarProyecto() {
            this.errorFormulario = '';

            if (this.formulario.fechaFin < this.formulario.fechaInicio) {
                this.errorFormulario =
                    'La fecha de finalización debe ser posterior a la fecha de inicio.';
                return;
            }

            if (!this.formulario.nombre || !this.formulario.descripcion) {
                this.errorFormulario =
                    'Completa el nombre y la descripción del proyecto.';
                return;
            }

            if (!this.formulario.responsable || !this.formulario.prioridad) {
                this.errorFormulario =
                    'Selecciona una prioridad y un responsable.';
                return;
            }

            if (this.modoEdicion) {
                const indice = this.proyectos.findIndex(
                    proyecto => proyecto.id === this.idEdicion
                );

                if (indice !== -1) {
                    const proyectoAnterior = this.proyectos[indice];

                    this.proyectos[indice] = {
                        ...proyectoAnterior,
                        ...this.formulario,
                        integrantes: [...this.formulario.integrantes]
                    };
                }

                this.mensajeExito = 'Proyecto actualizado correctamente.';
            } else {
                const nuevoProyecto = {
                    id: Date.now(),
                    ...this.formulario,
                    integrantes: [...this.formulario.integrantes],
                    estado: 'Planificado',
                    progreso: 0
                };

                this.proyectos.unshift(nuevoProyecto);
                this.mensajeExito = 'Proyecto creado correctamente.';
            }

            this.cerrarModal();
            this.limpiarMensajeLuego();
        },

        // Vue.js: prepara los datos de un proyecto para edición.
        editarProyecto(proyecto) {
            this.modoEdicion = true;
            this.idEdicion = proyecto.id;
            this.errorFormulario = '';

            this.formulario = {
                nombre: proyecto.nombre,
                descripcion: proyecto.descripcion,
                fechaInicio: proyecto.fechaInicio,
                fechaFin: proyecto.fechaFin,
                prioridad: proyecto.prioridad,
                responsable: proyecto.responsable,
                integrantes: [...proyecto.integrantes]
            };

            this.mostrarModal = true;
        },

        // Vue.js: muestra una vista informativa básica del proyecto.
        verProyecto(proyecto) {
            alert(
                'Proyecto: ' + proyecto.nombre +
                '\nEstado: ' + proyecto.estado +
                '\nResponsable: ' + proyecto.responsable +
                '\nProgreso: ' + proyecto.progreso + '%'
            );
        },

        // Vue.js: cuenta los proyectos según su estado.
        contarEstado(estado) {
            return this.proyectosFiltrados.filter(
                proyecto => proyecto.estado === estado
            ).length;
        },

        // Bootstrap: aplica un color según el estado del proyecto.
        claseEstado(estado) {
            const clases = {
                'Planificado': 'text-bg-secondary',
                'En progreso': 'text-bg-primary',
                'Completado': 'text-bg-success',
                'Atrasado': 'text-bg-danger'
            };

            return clases[estado] || 'text-bg-secondary';
        },

        // Bootstrap: aplica un color según la prioridad.
        clasePrioridad(prioridad) {
            const clases = {
                'Alta': 'text-bg-danger',
                'Media': 'text-bg-warning',
                'Baja': 'text-bg-success'
            };

            return clases[prioridad] || 'text-bg-secondary';
        },

        // Vue.js: convierte una fecha ISO en formato día/mes/año.
        fechaLegible(fecha) {
            if (!fecha) return 'Sin fecha';

            const [anio, mes, dia] = fecha.split('-');
            return `${dia}/${mes}/${anio}`;
        },

        // Vue.js: limpia el mensaje de éxito al siguiente cambio de vista.
        limpiarMensajeLuego() {
            // El mensaje se mantiene visible hasta que se cierre manualmente
            // o se implemente una notificación temporal en la siguiente etapa.
        }
    }
}).mount('#proyectosApp');

// JavaScript: crea el modelo vacío inicial fuera de Vue.
function thisFormularioVacio() {
    return {
        nombre: '',
        descripcion: '',
        fechaInicio: '',
        fechaFin: '',
        prioridad: '',
        responsable: '',
        integrantes: []
    };
}