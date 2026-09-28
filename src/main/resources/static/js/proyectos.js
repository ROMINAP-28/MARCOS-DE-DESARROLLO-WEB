
/* Vue.js + API REST de Spring Boot. */
const { createApp } = Vue;

createApp({
    data() {
        return {
            usuario: 'Usuario',
            busqueda: '',
            filtroEstado: '',
            filtroPrioridad: '',
            mostrarModal: false,
            mostrarDetalle: false,
            detalleSeleccionado: null,
            modoEdicion: false,
            idEdicion: null,
            errorFormulario: '',
            mensajeExito: '',
            cargando: true,
            equipoDisponible: ['María López', 'Carlos Torres', 'Ana García', 'José Ramírez', 'Brayan Quispe'],
            proyectos: [],
            formulario: this.formularioVacio()
        };
    },

    computed: {
        proyectosFiltrados() {
            const termino = this.busqueda.toLowerCase();
            return this.proyectos.filter(proyecto => {
                const texto = `${proyecto.nombre} ${proyecto.descripcion}`.toLowerCase();
                return texto.includes(termino) &&
                    (!this.filtroEstado || proyecto.estado === this.filtroEstado) &&
                    (!this.filtroPrioridad || proyecto.prioridad === this.filtroPrioridad);
            });
        },

        iniciales() {
            return this.usuario.trim().split(/\s+/).slice(0, 2)
                .map(p => p.charAt(0).toUpperCase()).join('');
        }
    },

    mounted() {
        this.cargarProyectos();
    },

    methods: {
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

        async cargarProyectos() {
            this.cargando = true;

            try {
                const respuesta = await fetch('/api/miembro3/proyectos');

                if (!respuesta.ok) {
                    throw new Error('No se pudieron cargar los proyectos.');
                }

                this.proyectos = await respuesta.json();

            } catch (error) {
                this.errorFormulario = error.message;

            } finally {
                this.cargando = false;
            }
        },

        abrirModal() {
            this.modoEdicion = false;
            this.idEdicion = null;
            this.formulario = this.formularioVacio();
            this.errorFormulario = '';
            this.mostrarModal = true;
        },

        cerrarModal() {
            this.mostrarModal = false;
            this.errorFormulario = '';
        },

        async guardarProyecto() {
            this.errorFormulario = '';

            if (!this.formulario.nombre ||
                !this.formulario.descripcion ||
                !this.formulario.fechaInicio ||
                !this.formulario.fechaFin ||
                !this.formulario.prioridad ||
                !this.formulario.responsable) {

                this.errorFormulario = 'Completa todos los campos obligatorios.';
                return;
            }

            if (this.formulario.fechaFin < this.formulario.fechaInicio) {
                this.errorFormulario =
                    'La fecha de finalización debe ser posterior a la fecha de inicio.';
                return;
            }

            try {
                const respuesta = await fetch('/api/miembro3/proyectos', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(this.formulario)
                });

                if (!respuesta.ok) {
                    throw new Error('No se pudo registrar el proyecto.');
                }

                const nuevo = await respuesta.json();

                this.proyectos.unshift(nuevo);

                this.cerrarModal();

                this.mensajeExito = 'Proyecto creado correctamente.';

            } catch (error) {
                this.errorFormulario = error.message;
            }
        },

        editarProyecto(proyecto) {
            this.modoEdicion = true;
            this.idEdicion = proyecto.id;

            this.formulario = {
                ...proyecto,
                integrantes: [...(proyecto.integrantes || [])]
            };

            this.errorFormulario =
                'La edición de proyectos queda preparada para conectarse al CRUD de la base de datos del grupo.';

            this.mostrarModal = true;
        },

        async verProyecto(proyecto) {
            try {
                const respuesta =
                    await fetch('/api/miembro3/proyectos/' + proyecto.id);

                if (!respuesta.ok) {
                    throw new Error('No se pudo obtener el detalle.');
                }

                this.detalleSeleccionado = await respuesta.json();
                this.mostrarDetalle = true;

            } catch (error) {
                this.errorFormulario = error.message;
            }
        },

        cerrarDetalle() {
            this.mostrarDetalle = false;
            this.detalleSeleccionado = null;
        },

        contarEstado(estado) {
            return this.proyectosFiltrados
                .filter(p => p.estado === estado)
                .length;
        },

        claseEstado(estado) {
            return ({
                'Planificado': 'text-bg-secondary',
                'En progreso': 'text-bg-primary',
                'Completado': 'text-bg-success',
                'Atrasado': 'text-bg-danger'
            })[estado] || 'text-bg-secondary';
        },

        clasePrioridad(prioridad) {
            return ({
                'Alta': 'text-bg-danger',
                'Media': 'text-bg-warning',
                'Baja': 'text-bg-success'
            })[prioridad] || 'text-bg-secondary';
        },

        fechaLegible(fecha) {
            if (!fecha) return 'Sin fecha';

            const [anio, mes, dia] = fecha.split('-');

            return `${dia}/${mes}/${anio}`;
        }
    }
}).mount('#proyectosApp');
