
const { createApp } = Vue;

createApp({
    data() {
        return {
            usuario: "Usuario",
            vista: "tabla",
            busqueda: "",
            filtroProyecto: "",
            filtroPrioridad: "",
            filtroEstado: "",
            filtroResponsable: "",
            mostrarModal: false,
            modoEdicion: false,
            tareaArrastrada: null,
            mensajeError: "",
            siguienteId: 6,

            estados: ["Pendiente", "En progreso", "Completada"],
            prioridades: ["Baja", "Media", "Alta", "Urgente"],
            proyectos: [
                "Travelink",
                "Sistema de Gestión",
                "Proyecto Académico"
            ],
            responsables: [
                "Ana Torres",
                "Carlos Ruiz",
                "María López",
                "Juan Pérez"
            ],

            // Datos temporales de demostración.
            // TODO: reemplazar por tareas obtenidas desde la API de Spring Boot.
            tareas: [
                {
                    id: 1,
                    nombre: "Diseñar la interfaz",
                    descripcion: "Preparar las vistas principales del sistema.",
                    proyecto: "Travelink",
                    prioridad: "Alta",
                    estado: "Pendiente",
                    fechaFin: "2026-09-30",
                    responsable: "Ana Torres"
                },
                {
                    id: 2,
                    nombre: "Conectar base de datos",
                    descripcion: "Configurar la conexión con PostgreSQL.",
                    proyecto: "Travelink",
                    prioridad: "Urgente",
                    estado: "En progreso",
                    fechaFin: "2026-10-02",
                    responsable: "Carlos Ruiz"
                },
                {
                    id: 3,
                    nombre: "Revisar requisitos",
                    descripcion: "Validar los requisitos funcionales.",
                    proyecto: "Sistema de Gestión",
                    prioridad: "Media",
                    estado: "Pendiente",
                    fechaFin: "2026-10-05",
                    responsable: "María López"
                },
                {
                    id: 4,
                    nombre: "Crear estructura HTML",
                    descripcion: "Implementar la estructura inicial de las páginas.",
                    proyecto: "Travelink",
                    prioridad: "Baja",
                    estado: "Completada",
                    fechaFin: "2026-09-25",
                    responsable: "Juan Pérez"
                },
                {
                    id: 5,
                    nombre: "Implementar autenticación",
                    descripcion: "Preparar el inicio de sesión de usuarios.",
                    proyecto: "Proyecto Académico",
                    prioridad: "Alta",
                    estado: "En progreso",
                    fechaFin: "2026-10-08",
                    responsable: "Ana Torres"
                }
            ],

            formulario: this.formularioVacio()
        };
    },

    computed: {
        tareasFiltradas() {
            const texto = this.busqueda.toLowerCase();

            return this.tareas.filter(tarea => {
                const coincideTexto =
                    tarea.nombre.toLowerCase().includes(texto) ||
                    tarea.descripcion.toLowerCase().includes(texto) ||
                    tarea.proyecto.toLowerCase().includes(texto) ||
                    tarea.responsable.toLowerCase().includes(texto);

                const coincideProyecto =
                    !this.filtroProyecto ||
                    tarea.proyecto === this.filtroProyecto;

                const coincidePrioridad =
                    !this.filtroPrioridad ||
                    tarea.prioridad === this.filtroPrioridad;

                const coincideEstado =
                    !this.filtroEstado ||
                    tarea.estado === this.filtroEstado;

                const coincideResponsable =
                    !this.filtroResponsable ||
                    tarea.responsable === this.filtroResponsable;

                return coincideTexto &&
                    coincideProyecto &&
                    coincidePrioridad &&
                    coincideEstado &&
                    coincideResponsable;
            });
        }
    },

    methods: {
        formularioVacio() {
            return {
                id: null,
                nombre: "",
                descripcion: "",
                proyecto: "",
                prioridad: "",
                estado: "Pendiente",
                fechaFin: "",
                responsable: ""
            };
        },

        contarEstado(estado) {
            return this.tareasFiltradas.filter(
                tarea => tarea.estado === estado
            ).length;
        },

        tareasPorEstado(estado) {
            return this.tareasFiltradas.filter(
                tarea => tarea.estado === estado
            );
        },

        abrirNuevaTarea() {
            this.modoEdicion = false;
            this.formulario = this.formularioVacio();
            this.mensajeError = "";
            this.mostrarModal = true;
        },

        editarTarea(tarea) {
            this.modoEdicion = true;
            this.formulario = { ...tarea };
            this.mensajeError = "";
            this.mostrarModal = true;
        },

        cerrarModal() {
            this.mostrarModal = false;
            this.mensajeError = "";
        },

        guardarTarea() {
            this.mensajeError = "";

            if (!this.formulario.nombre ||
                !this.formulario.proyecto ||
                !this.formulario.prioridad ||
                !this.formulario.fechaFin ||
                !this.formulario.responsable) {
                this.mensajeError = "Completa todos los campos obligatorios.";
                return;
            }

            if (this.modoEdicion) {
                const indice = this.tareas.findIndex(
                    tarea => tarea.id === this.formulario.id
                );

                if (indice !== -1) {
                    this.tareas[indice] = { ...this.formulario };
                }

                // TODO: enviar los cambios al backend para actualizar la tarea en PostgreSQL.
            } else {
                const nuevaTarea = {
                    ...this.formulario,
                    id: this.siguienteId++
                };

                this.tareas.unshift(nuevaTarea);

                // TODO: enviar la nueva tarea al backend para guardarla en PostgreSQL.
            }

            this.cerrarModal();
        },

        eliminarTarea(id) {
            const confirmar = window.confirm(
                "¿Estás seguro de que deseas eliminar esta tarea?"
            );

            if (!confirmar) return;

            this.tareas = this.tareas.filter(tarea => tarea.id !== id);

            // TODO: solicitar al backend la eliminación de la tarea en PostgreSQL.
        },

        arrastrarTarea(evento, tarea) {
            this.tareaArrastrada = tarea.id;
            evento.dataTransfer.effectAllowed = "move";
            evento.dataTransfer.setData("text/plain", String(tarea.id));
        },

        soltarTarea(evento, nuevoEstado) {
            evento.preventDefault();

            const id = Number(
                evento.dataTransfer.getData("text/plain") ||
                this.tareaArrastrada
            );

            const tarea = this.tareas.find(item => item.id === id);

            if (!tarea || tarea.estado === nuevoEstado) {
                this.tareaArrastrada = null;
                return;
            }

            tarea.estado = nuevoEstado;
            this.tareaArrastrada = null;

            // TODO: persistir el nuevo estado mediante la API de Spring Boot.
        },

        clasePrioridad(prioridad) {
            const clases = {
                "Baja": "priority-low",
                "Media": "priority-medium",
                "Alta": "priority-high",
                "Urgente": "priority-urgent"
            };

            return clases[prioridad] || "";
        },

        claseEstado(estado) {
            const clases = {
                "Pendiente": "status-pending",
                "En progreso": "status-progress",
                "Completada": "status-done"
            };

            return clases[estado] || "";
        },

        clasePunto(estado) {
            const clases = {
                "Pendiente": "dot-pending",
                "En progreso": "dot-progress",
                "Completada": "dot-done"
            };

            return clases[estado] || "";
        },

        iniciales(nombre) {
            return nombre

                .split(" ")
                .filter(Boolean)
                .slice(0, 2)
                .map(parte => parte[0].toUpperCase())
                .join("");
        },

        formatearFecha(fecha) {
            if (!fecha) return "Sin fecha";

            const partes = fecha.split("-");
            if (partes.length !== 3) return fecha;

            return `${partes[2]}/${partes[1]}/${partes[0]}`;
        }
    }
}).mount("#app");