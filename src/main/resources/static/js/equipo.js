
const { createApp } = Vue;

createApp({
    data() {
        return {
            usuario: "Usuario",
            vista: "integrantes",

            busqueda: "",
            filtroProyecto: "",
            filtroRol: "",
            filtroEspecialidad: "",
            filtroEstado: "",

            mostrarModalIntegrante: false,
            modoEdicion: false,
            integranteSeleccionado: null,
            mensajeError: "",
            siguienteId: 5,

            proyectos: [
                "Travelink",
                "Sistema de Gestión",
                "Proyecto Académico"
            ],

            roles: [
                "Administrador",
                "Líder de proyecto",
                "Desarrollador",
                "Diseñador",
                "Analista",
                "Colaborador"
            ],

            especialidades: [
                "Desarrollo Frontend",
                "Desarrollo Backend",
                "Base de datos",
                "Diseño UX/UI",
                "Análisis de sistemas",
                "Pruebas de software"
            ],

            estados: ["Activo", "Inactivo", "Pendiente"],

            // Datos temporales de demostración.
            // TODO: reemplazar por integrantes obtenidos desde la API de Spring Boot.
            integrantes: [
                {
                    id: 1,
                    nombre: "Ana Torres",
                    correo: "ana.torres@ejemplo.com",
                    proyecto: "Travelink",
                    rol: "Líder de proyecto",
                    especialidad: "Análisis de sistemas",
                    estado: "Activo",
                    fechaIngreso: "2026-09-01",
                    tareasAsignadas: 4
                },
                {
                    id: 2,
                    nombre: "Carlos Ruiz",
                    correo: "carlos.ruiz@ejemplo.com",
                    proyecto: "Travelink",
                    rol: "Desarrollador",
                    especialidad: "Desarrollo Backend",
                    estado: "Activo",
                    fechaIngreso: "2026-09-03",
                    tareasAsignadas: 3
                },
                {
                    id: 3,
                    nombre: "María López",
                    correo: "maria.lopez@ejemplo.com",
                    proyecto: "Sistema de Gestión",
                    rol: "Diseñador",
                    especialidad: "Diseño UX/UI",
                    estado: "Activo",
                    fechaIngreso: "2026-09-05",
                    tareasAsignadas: 2
                },
                {
                    id: 4,
                    nombre: "Juan Pérez",
                    correo: "juan.perez@ejemplo.com",
                    proyecto: "Proyecto Académico",
                    rol: "Desarrollador",
                    especialidad: "Desarrollo Frontend",
                    estado: "Pendiente",
                    fechaIngreso: "2026-09-10",
                    tareasAsignadas: 1
                }
            ],

            actividades: [
                {
                    id: 1,
                    descripcion: "Ana Torres fue incorporada como líder de proyecto.",
                    fecha: "01/09/2026",
                    icono: "bi-person-plus"
                },
                {
                    id: 2,
                    descripcion: "Carlos Ruiz fue asignado al proyecto Travelink.",
                    fecha: "03/09/2026",
                    icono: "bi-person-check"
                },
                {
                    id: 3,
                    descripcion: "María López se incorporó al área de Diseño UX/UI.",
                    fecha: "05/09/2026",
                    icono: "bi-palette"
                }
            ],

            formulario: {
                id: null,
                nombre: "",
                correo: "",
                proyecto: "",
                rol: "",
                especialidad: "",
                estado: "Activo",
                fechaIngreso: ""
            }
        };
    },

    computed: {
        inicialesUsuario() {
            return this.usuario
                .trim()
                .split(/\s+/)
                .filter(Boolean)
                .slice(0, 2)
                .map(parte => parte.charAt(0).toUpperCase())
                .join("");
        },

        integrantesFiltrados() {
            const texto = this.busqueda.trim().toLowerCase();

            return this.integrantes.filter(integrante => {
                const coincideTexto =
                    integrante.nombre.toLowerCase().includes(texto) ||
                    integrante.correo.toLowerCase().includes(texto) ||
                    integrante.especialidad.toLowerCase().includes(texto) ||
                    integrante.rol.toLowerCase().includes(texto);

                const coincideProyecto =
                    !this.filtroProyecto ||
                    integrante.proyecto === this.filtroProyecto;

                const coincideRol =
                    !this.filtroRol ||
                    integrante.rol === this.filtroRol;

                const coincideEspecialidad =
                    !this.filtroEspecialidad ||
                    integrante.especialidad === this.filtroEspecialidad;

                const coincideEstado =
                    !this.filtroEstado ||
                    integrante.estado === this.filtroEstado;

                return coincideTexto &&
                    coincideProyecto &&
                    coincideRol &&
                    coincideEspecialidad &&
                    coincideEstado;
            });
        },

        totalTareasAsignadas() {
            return this.integrantesFiltrados.reduce(
                (total, integrante) => total + Number(integrante.tareasAsignadas || 0),
                0
            );
        },

        resumenEspecialidades() {
            return this.especialidades.map(nombre => ({
                nombre,
                total: this.integrantes.filter(
                    integrante => integrante.especialidad === nombre
                ).length
            }));
        }
    },

    methods: {
        formularioVacio() {
            return {
                id: null,
                nombre: "",
                correo: "",
                proyecto: "",
                rol: "",
                especialidad: "",
                estado: "Activo",
                fechaIngreso: this.fechaActual()
            };
        },

        fechaActual() {
            const hoy = new Date();
            const anio = hoy.getFullYear();
            const mes = String(hoy.getMonth() + 1).padStart(2, "0");
            const dia = String(hoy.getDate()).padStart(2, "0");

            return `${anio}-${mes}-${dia}`;
        },

        contarEstado(estado) {
            return this.integrantesFiltrados.filter(
                integrante => integrante.estado === estado
            ).length;
        },

        iniciales(nombre) {
            if (!nombre) return "";

            return nombre
                .trim()
                .split(/\s+/)
                .filter(Boolean)
                .slice(0, 2)
                .map(parte => parte.charAt(0).toUpperCase())
                .join("");
        },

        claseEstado(estado) {
            const clases = {
                Activo: "text-bg-success",
                Inactivo: "text-bg-secondary",
                Pendiente: "text-bg-warning"
            };

            return clases[estado] || "text-bg-light";
        },

        abrirNuevoIntegrante() {
            this.modoEdicion = false;
            this.formulario = this.formularioVacio();
            this.mensajeError = "";
            this.mostrarModalIntegrante = true;
        },

        editarIntegrante(integrante) {
            this.modoEdicion = true;
            this.formulario = { ...integrante };
            this.integranteSeleccionado = null;
            this.mensajeError = "";
            this.mostrarModalIntegrante = true;
        },

        cerrarModal() {
            this.mostrarModalIntegrante = false;
            this.mensajeError = "";
        },

        guardarIntegrante() {
            this.mensajeError = "";

            const camposObligatorios = [
                "nombre",
                "correo",
                "proyecto",
                "rol",
                "especialidad",
                "fechaIngreso"
            ];

            const faltaCampo = camposObligatorios.some(campo =>
                !String(this.formulario[campo] || "").trim()
            );

            if (faltaCampo) {
                this.mensajeError = "Completa todos los campos obligatorios.";
                return;
            }

            const correoValido = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(
                this.formulario.correo
            );

            if (!correoValido) {
                this.mensajeError = "Ingresa un correo electrónico válido.";
                return;
            }

            const correoDuplicado = this.integrantes.some(integrante =>
                integrante.correo.toLowerCase() ===
                this.formulario.correo.toLowerCase() &&
                integrante.id !== this.formulario.id
            );

            if (correoDuplicado) {
                this.mensajeError = "Ya existe un integrante con ese correo.";
                return;
            }

            if (this.modoEdicion) {
                const indice = this.integrantes.findIndex(
                    integrante => integrante.id === this.formulario.id
                );

                if (indice !== -1) {
                    this.integrantes[indice] = { ...this.formulario };

                    this.registrarActividad(
                        `${this.formulario.nombre} actualizó sus datos de participación.`,
                        "bi-pencil-square"
                    );
                }

                // TODO: enviar los cambios al backend para actualizar el integrante en PostgreSQL.
            } else {
                const nuevoIntegrante = {
                    ...this.formulario,
                    id: this.siguienteId++,
                    tareasAsignadas: 0
                };

                this.integrantes.unshift(nuevoIntegrante);

                this.registrarActividad(
                    `${nuevoIntegrante.nombre} fue incorporado al proyecto ${nuevoIntegrante.proyecto}.`,
                    "bi-person-plus"
                );

                // TODO: enviar el nuevo integrante al backend para guardarlo en PostgreSQL.
            }

            this.cerrarModal();
        },

        verPerfil(integrante) {
            this.integranteSeleccionado = integrante;
        },

        retirarIntegrante(id) {
            const integrante = this.integrantes.find(
                item => item.id === id
            );

            if (!integrante) return;

            const confirmar = window.confirm(
                `¿Deseas retirar a ${integrante.nombre} del equipo?`
            );

            if (!confirmar) return;

            this.integrantes = this.integrantes.filter(
                item => item.id !== id
            );

            this.registrarActividad(
                `${integrante.nombre} fue retirado del equipo.`,
                "bi-person-dash"
            );

            // TODO: solicitar al backend el cambio de estado o retiro del integrante.
        },

        asignarTarea(integrante) {
            const nombreTarea = window.prompt(
                `Escribe el nombre de la tarea que deseas asignar a ${integrante.nombre}:`
            );

            if (nombreTarea === null) return;

            if (!nombreTarea.trim()) {
                window.alert("Debes escribir un nombre para la tarea.");
                return;
            }

            integrante.tareasAsignadas++;

            this.registrarActividad(
                `Se asignó "${nombreTarea.trim()}" a ${integrante.nombre}.`,
                "bi-clipboard-check"
            );

            window.alert(
                `Tarea registrada en la demostración para ${integrante.nombre}.`
            );

            // TODO: crear la tarea y vincularla al integrante mediante la API.
        },

        abrirEspecialidad() {
            const nombre = window.prompt(
                "Escribe el nombre de la nueva especialidad:"
            );

            if (nombre === null) return;

            const nombreLimpio = nombre.trim();

            if (!nombreLimpio) {
                window.alert("El nombre de la especialidad no puede estar vacío.");
                return;
            }

            const existe = this.especialidades.some(
                especialidad =>
                    especialidad.toLowerCase() === nombreLimpio.toLowerCase()
            );

            if (existe) {
                window.alert("Esa especialidad ya está registrada.");
                return;
            }

            this.especialidades.push(nombreLimpio);

            this.registrarActividad(
                `Se agregó la especialidad "${nombreLimpio}".`,
                "bi-diagram-3"
            );

            // TODO: registrar la especialidad en el catálogo de PostgreSQL mediante la API.
        },

        filtrarPorEspecialidad(especialidad) {
            this.filtroEspecialidad = especialidad;
            this.vista = "integrantes";
        },

        registrarActividad(descripcion, icono) {
            this.actividades.unshift({
                id: Date.now(),
                descripcion,
                fecha: this.formatearFecha(this.fechaActual()),
                icono
            });
        },

        formatearFecha(fecha) {
            if (!fecha) return "Sin fecha";

            const partes = fecha.split("-");

            if (partes.length !== 3) return fecha;

            return `${partes[2]}/${partes[1]}/${partes[0]}`;
        }
    }
}).mount("#equipoApp");