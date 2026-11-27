/* Tareas + Kanban + drag-and-drop + API REST. */
const { createApp } = Vue;

createApp({
    data() {
        return {
            usuario: 'Usuario',
            idUsuarioActual: null,
            vista: 'tabla',
            busqueda: '',
            filtroProyecto: '',
            filtroPrioridad: '',
            filtroEstado: '',
            filtroResponsable: '',
            mostrarModal: false,
            modoEdicion: false,
            tareaArrastrada: null,
            mensajeError: '',
            siguienteId: 6,

            estados: ['Pendiente', 'En progreso', 'Completada'],
            prioridades: ['Baja', 'Media', 'Alta', 'Urgente'],

            proyectos: [],
            proyectosCatalogo: [],
            participaciones: [],

            responsables: [],

            tareas: [],

            formulario: this.formularioVacio(),

            cargando: true
        };
    },

    computed: {
        proyectosGestionados() {
            return this.proyectosCatalogo.filter(p => p.idResponsable === this.idUsuarioActual);
        },
        responsablesDisponibles() {
            return this.participaciones.filter(p => p.idProyecto === this.formulario.idProyecto
                && p.estado === 'Activo');
        },
        tareasFiltradas() {
            const texto = this.busqueda.toLowerCase();

            return this.tareas.filter(tarea => {
                const contenido =
                    `${tarea.nombre} ${tarea.descripcion} ${tarea.proyecto} ${tarea.responsable}`
                        .toLowerCase();

                return contenido.includes(texto) &&
                    (!this.filtroProyecto ||
                        tarea.proyecto === this.filtroProyecto) &&
                    (!this.filtroPrioridad ||
                        tarea.prioridad === this.filtroPrioridad) &&
                    (!this.filtroEstado ||
                        tarea.estado === this.filtroEstado) &&
                    (!this.filtroResponsable ||
                        tarea.responsable === this.filtroResponsable);
            });
        }
    },

    mounted() {
        this.cargarTareas();
        this.alActualizar = evento => {
            if (['todos', 'proyectos', 'tareas', 'equipo'].includes(evento.detail?.recurso)) {
                this.cargarTareas();
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
                id: null,
                nombre: '',
                descripcion: '',
                proyecto: '',
                idProyecto: '',
                prioridad: '',
                estado: 'Pendiente',
                fechaInicio: '',
                fechaFin: '',
                responsable: '',
                idResponsable: ''
            };
        },

        async cargarTareas() {
            this.cargando = true;

            try {
                const respuestas = await Promise.all([
                    fetch('/api/miembro3/tareas'), fetch('/api/miembro3/proyectos'),
                    fetch('/api/miembro3/participaciones'), fetch('/api/auth/yo')
                ]);
                if (respuestas.some(respuesta => !respuesta.ok)) {
                    throw new Error('No se pudieron cargar los datos de tareas.');
                }
                const [tareas, proyectos, participaciones, cuenta] = await Promise.all(
                    respuestas.map(respuesta => respuesta.json()));
                this.tareas = tareas;
                this.proyectosCatalogo = proyectos;
                this.participaciones = participaciones;
                this.idUsuarioActual = cuenta.id;
                this.usuario = cuenta.nombre;

                this.proyectos = [
                    ...new Set(this.tareas.map(t => t.proyecto))
                ];
                this.responsables = [...new Set(this.tareas.map(t => t.responsable))];

                const maxId = this.tareas.reduce(
                    (max, t) => Math.max(max, Number(t.id)),
                    0
                );

                this.siguienteId = maxId + 1;

            } catch (error) {
                this.mensajeError = error.message;

            } finally {
                this.cargando = false;
            }
        },

        contarEstado(estado) {
            return this.tareas
                .filter(t => t.estado === estado)
                .length;
        },

        tareasPorEstado(estado) {
            return this.tareasFiltradas
                .filter(t => t.estado === estado);
        },

        abrirNuevaTarea() {
            this.modoEdicion = false;
            this.formulario = this.formularioVacio();
            this.mensajeError = '';
            this.mostrarModal = true;
        },

        editarTarea(tarea) {
            if (!this.puedeGestionarTarea(tarea)) return;
            this.modoEdicion = true;
            this.formulario = { ...tarea, fechaInicio: tarea.fechaInicio || '' };
            this.mensajeError = '';
            this.mostrarModal = true;
        },

        puedeGestionarTarea(tarea) {
            return this.proyectosGestionados.some(p => p.id === tarea.idProyecto);
        },

        puedeCambiarEstado(tarea) {
            return this.puedeGestionarTarea(tarea) || tarea.idResponsable === this.idUsuarioActual;
        },

        cerrarModal() {
            this.mostrarModal = false;
            this.mensajeError = '';
        },

        async guardarTarea() {
            this.mensajeError = '';

            const proyecto = this.proyectosGestionados.find(p => p.id === this.formulario.idProyecto);
            const responsable = this.responsablesDisponibles.find(p => p.idUsuario === this.formulario.idResponsable);

            if (!this.formulario.nombre ||
                !proyecto ||
                !this.formulario.prioridad ||
                !this.formulario.fechaFin ||
                !responsable) {

                this.mensajeError =
                    'Completa todos los campos obligatorios.';

                return;
            }

            try {
                const esEdicion = this.modoEdicion;

                const url = esEdicion
                    ? `/api/miembro3/tareas/${this.formulario.id}`
                    : '/api/miembro3/tareas';

                const respuesta = await fetch(url, {
                    method: esEdicion ? 'PUT' : 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        nombre: this.formulario.nombre,
                        descripcion: this.formulario.descripcion,
                        idProyecto: this.formulario.idProyecto,
                        prioridad: this.formulario.prioridad,
                        estado: this.formulario.estado,
                        fechaInicio: this.formulario.fechaInicio || null,
                        fechaFin: this.formulario.fechaFin,
                        idResponsable: this.formulario.idResponsable
                    })
                });

                if (!respuesta.ok) {
                    const error = await respuesta.json().catch(() => ({}));
                    throw new Error(error.mensaje || 'No se pudo guardar la tarea.');
                }

                const guardada = await respuesta.json();

                if (esEdicion) {
                    const indice = this.tareas.findIndex(
                        t => t.id === guardada.id
                    );

                    if (indice !== -1) {
                        this.tareas[indice] = guardada;
                    }

                } else {
                    this.tareas.unshift(guardada);
                }

                this.proyectos = [...new Set(this.tareas.map(t => t.proyecto))];
                this.responsables = [...new Set(this.tareas.map(t => t.responsable))];

                this.cerrarModal();

            } catch (error) {
                this.mensajeError = error.message;
            }
        },

        async eliminarTarea(id) {
            const tarea = this.tareas.find(item => item.id === id);
            if (!tarea || !this.puedeGestionarTarea(tarea)) return;
            if (!window.confirm(
                '¿Estás seguro de que deseas eliminar esta tarea?'
            )) {
                return;
            }

            try {
                const respuesta = await fetch(
                    `/api/miembro3/tareas/${id}`,
                    {
                        method: 'DELETE'
                    }
                );

                if (!respuesta.ok) {
                    throw new Error('No se pudo eliminar la tarea.');
                }

                this.tareas = this.tareas.filter(
                    t => t.id !== id
                );

            } catch (error) {
                this.mensajeError = error.message;
            }
        },

        arrastrarTarea(evento, tarea) {
            if (!this.puedeCambiarEstado(tarea)) return;
            this.tareaArrastrada = tarea.id;

            evento.dataTransfer.effectAllowed = 'move';

            evento.dataTransfer.setData(
                'text/plain',
                String(tarea.id)
            );
        },

        async soltarTarea(evento, nuevoEstado) {
            evento.preventDefault();

            const id = Number(
                evento.dataTransfer.getData('text/plain') ||
                this.tareaArrastrada
            );

            const tarea = this.tareas.find(
                item => Number(item.id) === id
            );

            if (!tarea || tarea.estado === nuevoEstado) {
                this.tareaArrastrada = null;
                return;
            }

            if (!this.puedeCambiarEstado(tarea)) return;

            const estadoAnterior = tarea.estado;

            tarea.estado = nuevoEstado;
            this.tareaArrastrada = null;

            try {
                const respuesta = await fetch(
                    `/api/miembro3/tareas/${id}/estado`,
                    {
                        method: 'PATCH',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            estado: nuevoEstado
                        })
                    }
                );

                if (!respuesta.ok) {
                    throw new Error(
                        'No se pudo guardar el cambio de estado.'
                    );
                }

                const actualizada = await respuesta.json();

                const indice = this.tareas.findIndex(
                    t => Number(t.id) === id
                );

                if (indice !== -1) {
                    this.tareas[indice] = actualizada;
                }

            } catch (error) {
                tarea.estado = estadoAnterior;
                this.mensajeError = error.message;
            }
        },

        clasePrioridad(prioridad) {
            return ({
                Baja: 'priority-low',
                Media: 'priority-medium',
                Alta: 'priority-high',
                Urgente: 'priority-urgent'
            })[prioridad] || '';
        },

        claseEstado(estado) {
            return ({
                Pendiente: 'status-pending',
                'En progreso': 'status-progress',
                Completada: 'status-done'
            })[estado] || '';
        },

        clasePunto(estado) {
            return ({
                Pendiente: 'dot-pending',
                'En progreso': 'dot-progress',
                Completada: 'dot-done'
            })[estado] || '';
        },

        iniciales(nombre) {
            return (nombre || '')
                .split(' ')
                .filter(Boolean)
                .slice(0, 2)
                .map(p => p[0].toUpperCase())
                .join('');
        },

        formatearFecha(fecha) {
            if (!fecha) return 'Sin fecha';

            const partes = fecha.split('-');

            return partes.length === 3
                ? `${partes[2]}/${partes[1]}/${partes[0]}`
                : fecha;
        }
    }
}).mount('#app');