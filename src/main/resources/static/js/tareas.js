
/* Tareas + Kanban + calendario + drag-and-drop + API REST. */
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

            cargando: true,

            // Datos del calendario
            fechaCalendario: new Date(),
            diasSemana: [
                'Lun', 'Mar', 'Mié', 'Jue',
                'Vie', 'Sáb', 'Dom'
            ]
        };
    },

    computed: {
        proyectosGestionados() {
            return this.proyectosCatalogo.filter(
                p => p.idResponsable === this.idUsuarioActual
            );
        },

        responsablesDisponibles() {
            return this.participaciones.filter(
                p => p.idProyecto === this.formulario.idProyecto &&
                    p.estado === 'Activo'
            );
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
        },

        // Nombre del mes que se muestra en el calendario
        nombreMes() {
            return this.fechaCalendario.toLocaleDateString('es-PE', {
                month: 'long',
                year: 'numeric'
            });
        },

        // Genera los días del calendario y asigna las tareas
        calendarioDias() {
            const anio = this.fechaCalendario.getFullYear();
            const mes = this.fechaCalendario.getMonth();

            const primerDia = new Date(anio, mes, 1);
            const ultimoDia = new Date(anio, mes + 1, 0);

            // Ajustar para que la semana empiece el lunes
            const desplazamiento = (primerDia.getDay() + 6) % 7;

            // Calcular las semanas necesarias para el mes
            const totalDias = Math.ceil(
                (desplazamiento + ultimoDia.getDate()) / 7
            ) * 7;

            const hoy = new Date();

            const fechaHoy = [
                hoy.getFullYear(),
                String(hoy.getMonth() + 1).padStart(2, '0'),
                String(hoy.getDate()).padStart(2, '0')
            ].join('-');

            return Array.from({ length: totalDias }, (_, i) => {
                const fecha = new Date(
                    anio,
                    mes,
                    i - desplazamiento + 1
                );

                const fechaTexto = [
                    fecha.getFullYear(),
                    String(fecha.getMonth() + 1).padStart(2, '0'),
                    String(fecha.getDate()).padStart(2, '0')
                ].join('-');

                return {
                    fecha: fechaTexto,
                    numero: fecha.getDate(),
                    delMes: fecha.getMonth() === mes,
                    esHoy: fechaTexto === fechaHoy,

                    // Mostrar las tareas que vencen este día
                    tareas: this.tareasFiltradas.filter(tarea =>
                        (tarea.fechaFin || '').slice(0, 10) === fechaTexto
                    )
                };
            });
        }
    },

    mounted() {
        this.cargarTareas();

        this.alActualizar = evento => {
            if (
                ['todos', 'proyectos', 'tareas', 'equipo']
                    .includes(evento.detail?.recurso)
            ) {
                this.cargarTareas();
            }
        };

        window.addEventListener(
            'gestion:actualizado',
            this.alActualizar
        );
    },

    beforeUnmount() {
        window.removeEventListener(
            'gestion:actualizado',
            this.alActualizar
        );
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

        // Cargar las tareas desde la API
        async cargarTareas() {
            this.cargando = true;

            try {
                const respuestas = await Promise.all([
                    fetch('/api/miembro3/tareas'),
                    fetch('/api/miembro3/proyectos'),
                    fetch('/api/miembro3/participaciones'),
                    fetch('/api/auth/yo')
                ]);

                if (respuestas.some(respuesta => !respuesta.ok)) {
                    throw new Error(
                        'No se pudieron cargar los datos de tareas.'
                    );
                }

                const [
                    tareas,
                    proyectos,
                    participaciones,
                    cuenta
                ] = await Promise.all(
                    respuestas.map(respuesta => respuesta.json())
                );

                this.tareas = tareas;
                this.proyectosCatalogo = proyectos;
                this.participaciones = participaciones;
                this.idUsuarioActual = cuenta.id;
                this.usuario = cuenta.nombre;

                this.proyectos = [
                    ...new Set(this.tareas.map(t => t.proyecto))
                ];

                this.responsables = [
                    ...new Set(this.tareas.map(t => t.responsable))
                ];

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
            this.formulario = {
                ...tarea,
                fechaInicio: tarea.fechaInicio || ''
            };

            this.mensajeError = '';
            this.mostrarModal = true;
        },

        puedeGestionarTarea(tarea) {
            return this.proyectosGestionados.some(
                p => p.id === tarea.idProyecto
            );
        },

        puedeCambiarEstado(tarea) {
            return this.puedeGestionarTarea(tarea) ||
                tarea.idResponsable === this.idUsuarioActual;
        },

        cerrarModal() {
            this.mostrarModal = false;
            this.mensajeError = '';
        },

        // Guardar o actualizar una tarea
        async guardarTarea() {
            this.mensajeError = '';

            const proyecto = this.proyectosGestionados.find(
                p => p.id === this.formulario.idProyecto
            );

            const responsable = this.responsablesDisponibles.find(
                p => p.idUsuario === this.formulario.idResponsable
            );

            if (
                !this.formulario.nombre ||
                !proyecto ||
                !this.formulario.prioridad ||
                !this.formulario.fechaFin ||
                !responsable
            ) {
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

                    throw new Error(
                        error.mensaje || 'No se pudo guardar la tarea.'
                    );
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

                this.proyectos = [
                    ...new Set(this.tareas.map(t => t.proyecto))
                ];

                this.responsables = [
                    ...new Set(this.tareas.map(t => t.responsable))
                ];

                this.cerrarModal();

            } catch (error) {
                this.mensajeError = error.message;
            }
        },

        // Eliminar una tarea
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

        // Arrastrar una tarea en el Kanban
        arrastrarTarea(evento, tarea) {
            if (!this.puedeCambiarEstado(tarea)) return;

            this.tareaArrastrada = tarea.id;

            evento.dataTransfer.effectAllowed = 'move';

            evento.dataTransfer.setData(
                'text/plain',
                String(tarea.id)
            );
        },

        // Cambiar el estado al soltar una tarea
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

        // Cambiar el mes del calendario
        cambiarMes(cantidad) {
            this.fechaCalendario = new Date(
                this.fechaCalendario.getFullYear(),
                this.fechaCalendario.getMonth() + cantidad,
                1
            );
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
