
/* Vue.js + API REST de Spring Boot. */
const { createApp } = Vue;

createApp({
    data() {
        return {
            usuario: 'Usuario',
            idUsuarioActual: null,
            busqueda: '',
            filtroEstado: '',
            filtroPrioridad: '',
            mostrarModal: false,
            mostrarDetalle: false,
            detalleSeleccionado: null,
            modoEdicion: false,
            idEdicion: null,
            errorFormulario: '',
            errorCarga: '',
            errorOperacion: '',
            mensajeExito: '',
            cargando: true,
            guardando: false,
            equipoDisponible: [],
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
        this.alActualizar = evento => {
            if (['todos', 'proyectos', 'equipo'].includes(evento.detail?.recurso)) {
                this.cargarProyectos();
            }
        };
        window.addEventListener('gestion:actualizado', this.alActualizar);
    },

    beforeUnmount() {
        window.removeEventListener('gestion:actualizado', this.alActualizar);
    },

    methods: {
        formularioVacio() {
            return {
                nombre: '',
                descripcion: '',
                fechaInicio: '',
                fechaFin: '',
                prioridad: '',
                idResponsable: '',
                idsIntegrantes: []
            };
        },

        async cargarProyectos() {
            this.cargando = true;
            this.errorCarga = '';

            try {
                const [proyectos, usuarios, cuenta] = await Promise.all([
                    this.solicitar('/api/miembro3/proyectos'),
                    this.solicitar('/api/miembro3/usuarios'),
                    this.solicitar('/api/auth/yo')
                ]);
                this.proyectos = proyectos;
                this.equipoDisponible = usuarios;
                this.idUsuarioActual = cuenta.id;
                this.usuario = cuenta.nombre;
                this.errorCarga = '';
                this.errorOperacion = '';

            } catch (error) {
                this.errorCarga = error.message;
                this.proyectos = [];
                this.equipoDisponible = [];
                this.idUsuarioActual = null;

            } finally {
                this.cargando = false;
            }
        },

        async solicitar(url, opciones) {
            const respuesta = await fetch(url, opciones);
            if (!respuesta.ok) {
                const error = await respuesta.json().catch(() => ({}));
                throw new Error(error.mensaje || 'No se pudo completar la operación.');
            }
            return respuesta.status === 204 ? null : respuesta.json();
        },

        abrirModal() {
            this.modoEdicion = false;
            this.idEdicion = null;
            this.formulario = this.formularioVacio();
            this.formulario.idResponsable = this.idUsuarioActual;
            this.errorFormulario = '';
            this.errorOperacion = '';
            this.mensajeExito = '';
            this.mostrarModal = true;
        },

        cerrarModal() {
            this.mostrarModal = false;
            this.errorFormulario = '';
        },

        async guardarProyecto() {
            this.errorFormulario = '';
            if (this.guardando) return;

            const responsable = this.equipoDisponible.find(
                usuario => usuario.id === this.formulario.idResponsable);
            const integrantes = this.formulario.idsIntegrantes.map(id =>
                this.equipoDisponible.find(usuario => usuario.id === id));

            if (!this.formulario.nombre ||
                !this.formulario.descripcion ||
                !this.formulario.fechaInicio ||
                !this.formulario.fechaFin ||
                !this.formulario.prioridad || !responsable ||
                integrantes.some(usuario => !usuario)) {

                this.errorFormulario = 'Completa todos los campos obligatorios.';
                return;
            }

            if (this.formulario.fechaFin < this.formulario.fechaInicio) {
                this.errorFormulario =
                    'La fecha de finalización debe ser posterior a la fecha de inicio.';
                return;
            }

            this.guardando = true;
            try {
                const edicion = this.modoEdicion;
                const proyecto = await this.solicitar(edicion
                    ? `/api/miembro3/proyectos/${this.idEdicion}` : '/api/miembro3/proyectos', {
                    method: edicion ? 'PUT' : 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(this.formulario)
                });
                if (edicion) {
                    const indice = this.proyectos.findIndex(item => item.id === this.idEdicion);
                    if (indice !== -1) this.proyectos.splice(indice, 1, proyecto);
                    else this.proyectos.unshift(proyecto);
                } else {
                    this.proyectos.unshift(proyecto);
                }

                this.cerrarModal();
                this.mensajeExito = edicion ? 'Proyecto actualizado correctamente.'
                    : 'Proyecto creado correctamente.';

            } catch (error) {
                this.errorFormulario = error.message;
            } finally {
                this.guardando = false;
            }
        },

        editarProyecto(proyecto) {
            if (!this.puedeGestionar(proyecto)) return;
            this.modoEdicion = true;
            this.idEdicion = proyecto.id;

            this.formulario = { nombre: proyecto.nombre, descripcion: proyecto.descripcion,
                fechaInicio: proyecto.fechaInicio, fechaFin: proyecto.fechaFin,
                prioridad: proyecto.prioridad, idResponsable: proyecto.idResponsable,
                idsIntegrantes: [...(proyecto.idsIntegrantes || [])],
                estado: proyecto.estado, progreso: proyecto.progreso };

            this.errorFormulario = '';
            this.mensajeExito = '';

            this.mostrarModal = true;
        },

        async eliminarProyecto(proyecto) {
            if (!this.puedeGestionar(proyecto)) return;
            if (!window.confirm(`¿Eliminar definitivamente "${proyecto.nombre}"? También se eliminarán `
                + 'sus tareas, participaciones, recursos y reportes asociados. Esta acción no se puede deshacer.')) {
                return;
            }
            this.errorOperacion = '';
            this.mensajeExito = '';
            try {
                await this.solicitar(`/api/miembro3/proyectos/${proyecto.id}`, { method: 'DELETE' });
                this.proyectos = this.proyectos.filter(item => item.id !== proyecto.id);
                if (this.detalleSeleccionado?.id === proyecto.id) this.cerrarDetalle();
                this.mensajeExito = 'Proyecto eliminado correctamente.';
            } catch (error) {
                this.errorOperacion = error.message;
            }
        },

        puedeGestionar(proyecto) {
            return this.idUsuarioActual != null && proyecto.idResponsable === this.idUsuarioActual;
        },

        async verProyecto(proyecto) {
            this.errorOperacion = '';
            try {
                const respuesta =
                    await fetch('/api/miembro3/proyectos/' + proyecto.id);

                if (!respuesta.ok) {
                    throw new Error('No se pudo obtener el detalle.');
                }

                this.detalleSeleccionado = await respuesta.json();
                this.mostrarDetalle = true;

            } catch (error) {
                this.errorOperacion = error.message;
            }
        },

        cerrarDetalle() {
            this.mostrarDetalle = false;
            this.detalleSeleccionado = null;
        },

        contarEstado(estado) {
            return this.proyectos
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
