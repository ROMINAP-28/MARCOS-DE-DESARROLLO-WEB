
const { createApp } = Vue;

createApp({
    data() {
        return {
            usuario: "Brayan Quispe",

            tipoSeleccionado: "avance",
            busqueda: "",
            mensajeError: "",
            reporteActual: null,

            filtros: {
                proyecto: "",
                fechaDesde: "",
                fechaHasta: "",
                formato: "PDF"
            },

            // Datos temporales de demostración.
            // TODO: cargar proyectos desde la API de Spring Boot.
            proyectos: [
                "Travelink",
                "Sistema de Gestión",
                "Proyecto Académico"
            ],

            // TODO: obtener estos indicadores mediante consultas a PostgreSQL.
            proyectosActivos: 3,
            tareasCompletadas: 18,

            tiposReporte: [
                {
                    id: "avance",
                    nombre: "Avance de proyectos",
                    descripcion: "Consulta el progreso, cumplimiento y estado general de los proyectos.",
                    icono: "bi bi-bar-chart-line",
                    color: "icon-blue"
                },
                {
                    id: "tareas",
                    nombre: "Estado de tareas",
                    descripcion: "Revisa las tareas pendientes, en progreso y completadas.",
                    icono: "bi bi-list-check",
                    color: "icon-green"
                },
                {
                    id: "equipo",
                    nombre: "Actividad del equipo",
                    descripcion: "Consulta la participación, asignaciones y actividad de los integrantes.",
                    icono: "bi bi-people",
                    color: "icon-purple"
                },
                {
                    id: "recursos",
                    nombre: "Recursos utilizados",
                    descripcion: "Visualiza la distribución y utilización de recursos por proyecto.",
                    icono: "bi bi-box-seam",
                    color: "icon-orange"
                }
            ],

            // Datos ficticios para visualizar el historial.
            // TODO: reemplazar por reportes obtenidos desde la API.
            reportes: [
                {
                    id: 1,
                    nombre: "Avance general de proyectos",
                    tipo: "Avance de proyectos",
                    proyecto: "Travelink",
                    fecha: "2026-09-25",
                    formato: "PDF",
                    estado: "Generado",
                    resumen: [
                        { nombre: "Progreso del proyecto", valor: "75%" },
                        { nombre: "Tareas completadas", valor: "15" },
                        { nombre: "Tareas pendientes", valor: "5" }
                    ]
                },
                {
                    id: 2,
                    nombre: "Estado de tareas",
                    tipo: "Estado de tareas",
                    proyecto: "Sistema de Gestión",
                    fecha: "2026-09-24",
                    formato: "Excel",
                    estado: "Generado",
                    resumen: [
                        { nombre: "Total de tareas", valor: "24" },
                        { nombre: "En progreso", valor: "8" },
                        { nombre: "Completadas", valor: "12" }
                    ]
                },
                {
                    id: 3,
                    nombre: "Actividad del equipo",
                    tipo: "Actividad del equipo",
                    proyecto: "Travelink",
                    fecha: "2026-09-22",
                    formato: "PDF",
                    estado: "Generado",
                    resumen: [
                        { nombre: "Integrantes", valor: "6" },
                        { nombre: "Tareas asignadas", valor: "20" },
                        { nombre: "Actividades registradas", valor: "35" }
                    ]
                },
                {
                    id: 4,
                    nombre: "Recursos utilizados",
                    tipo: "Recursos utilizados",
                    proyecto: "Proyecto Académico",
                    fecha: "2026-09-20",
                    formato: "CSV",
                    estado: "Generado",
                    resumen: [
                        { nombre: "Recursos registrados", valor: "10" },
                        { nombre: "Recursos asignados", valor: "7" },
                        { nombre: "Disponibles", valor: "3" }
                    ]
                }
            ]
        };
    },

    computed: {
        inicialesUsuario() {
            return this.iniciales(this.usuario);
        },

        nombreTipoSeleccionado() {
            const tipo = this.tiposReporte.find(
                item => item.id === this.tipoSeleccionado
            );

            return tipo ? tipo.nombre : "Avance de proyectos";
        },

        reportesFiltrados() {
            const texto = this.busqueda.trim().toLowerCase();

            return this.reportes
                .filter(reporte => {
                    return (
                        reporte.nombre.toLowerCase().includes(texto) ||
                        reporte.tipo.toLowerCase().includes(texto) ||
                        reporte.proyecto.toLowerCase().includes(texto) ||
                        reporte.formato.toLowerCase().includes(texto)
                    );
                })
                .sort((a, b) => b.fecha.localeCompare(a.fecha));
        },

        reportesRecientes() {
            return [...this.reportes]
                .sort((a, b) => b.fecha.localeCompare(a.fecha))
                .slice(0, 5);
        }
    },

    methods: {
        iniciales(nombre) {
            if (!nombre || typeof nombre !== "string") {
                return "U";
            }

            return nombre
                .trim()
                .split(/\s+/)
                .filter(Boolean)
                .slice(0, 2)
                .map(parte => parte.charAt(0).toUpperCase())
                .join("");
        },

        seleccionarTipo(id) {
            const existe = this.tiposReporte.some(
                tipo => tipo.id === id
            );

            if (!existe) {
                this.mensajeError = "El tipo de reporte seleccionado no es válido.";
                return;
            }

            this.tipoSeleccionado = id;
            this.mensajeError = "";
            this.reporteActual = null;

            this.irAlGenerador();
        },

        abrirGenerador() {
            this.mensajeError = "";
            this.irAlGenerador();
        },

        irAlGenerador() {
            this.$nextTick(() => {
                document.getElementById("generadorReporte")
                    ?.scrollIntoView({
                        behavior: "smooth",
                        block: "start"
                    });
            });
        },

        limpiarFiltros() {
            this.filtros = {
                proyecto: "",
                fechaDesde: "",
                fechaHasta: "",
                formato: "PDF"
            };

            this.mensajeError = "";
        },

        generarReporte() {
            this.mensajeError = "";

            if (
                this.filtros.fechaDesde &&
                this.filtros.fechaHasta &&
                this.filtros.fechaDesde > this.filtros.fechaHasta
            ) {
                this.mensajeError =
                    "La fecha inicial no puede ser posterior a la fecha final.";
                return;
            }

            const tipo = this.tiposReporte.find(
                item => item.id === this.tipoSeleccionado
            );

            if (!tipo) {
                this.mensajeError =
                    "Selecciona un tipo de reporte válido.";
                return;
            }

            const fechaActual = this.fechaLocalISO();

            // Datos de demostración para la vista previa.
            // TODO: enviar tipo, proyecto y fechas a la API REST.
            // El backend consultará PostgreSQL y devolverá los datos reales.
            const nuevoReporte = {
                id: Date.now(),
                nombre: tipo.nombre,
                tipo: tipo.nombre,
                proyecto: this.filtros.proyecto || "Todos los proyectos",
                fecha: fechaActual,
                formato: this.filtros.formato,
                estado: "Generado",
                fechaDesde: this.filtros.fechaDesde,
                fechaHasta: this.filtros.fechaHasta,
                resumen: this.obtenerResumenDemo(tipo.id)
            };

            this.reportes.unshift(nuevoReporte);
            this.reporteActual = nuevoReporte;

            // TODO: guardar el historial del reporte mediante la API.
            // La generación real y el registro persistente corresponderán
            // al servicio de reportes de Spring Boot.
        },

        obtenerResumenDemo(tipo) {
            const resumenes = {
                avance: [
                    { nombre: "Progreso promedio", valor: "75%" },
                    { nombre: "Proyectos activos", valor: "3" },
                    { nombre: "Proyectos finalizados", valor: "1" }
                ],
                tareas: [
                    { nombre: "Total de tareas", valor: "40" },
                    { nombre: "En progreso", valor: "12" },
                    { nombre: "Completadas", valor: "18" }
                ],
                equipo: [
                    { nombre: "Integrantes", valor: "8" },
                    { nombre: "Tareas asignadas", valor: "32" },
                    { nombre: "Actividades registradas", valor: "45" }
                ],
                recursos: [
                    { nombre: "Recursos registrados", valor: "15" },
                    { nombre: "Recursos asignados", valor: "10" },
                    { nombre: "Recursos disponibles", valor: "5" }
                ]
            };

            return resumenes[tipo] || [];
        },

        verReporte(reporte) {
            if (!reporte) return;

            this.reporteActual = reporte;
            this.mensajeError = "";

            this.$nextTick(() => {
                document.getElementById("generadorReporte")
                    ?.scrollIntoView({
                        behavior: "smooth",
                        block: "start"
                    });
            });
        },

        descargarReporte(reporte) {
            if (!reporte) return;

            switch (reporte.formato) {
                case "CSV":
                    this.descargarCSV(reporte);
                    break;

                case "PDF":
                    this.prepararPDF(reporte);
                    break;

                case "Excel":
                    window.alert(
                        "La descarga de Excel estará disponible " +
                        "cuando se integre el generador del backend."
                    );
                    break;

                default:
                    window.alert("El formato seleccionado no es válido.");
            }
        },

        descargarCSV(reporte) {
            const filas = [
                ["Reporte", reporte.nombre],
                ["Tipo", reporte.tipo],
                ["Proyecto", reporte.proyecto],
                ["Fecha de generación", this.formatearFecha(reporte.fecha)],
                ["Formato", reporte.formato],
                ["Fecha desde", reporte.fechaDesde || "No especificada"],
                ["Fecha hasta", reporte.fechaHasta || "No especificada"],
                [],
                ["Indicador", "Valor"],
                ...reporte.resumen.map(dato => [
                    dato.nombre,
                    dato.valor
                ])
            ];

            const contenido = filas
                .map(fila =>
                    fila.map(valor =>
                        `"${String(valor ?? "").replace(/"/g, '""')}"`
                    ).join(",")
                )
                .join("\r\n");

            const blob = new Blob(
                ["\uFEFF" + contenido],
                { type: "text/csv;charset=utf-8;" }
            );

            const url = URL.createObjectURL(blob);
            const enlace = document.createElement("a");

            const nombreArchivo = reporte.nombre
                .normalize("NFD")
                .replace(/[\u0300-\u036f]/g, "")
                .toLowerCase()
                .replace(/[^a-z0-9]+/g, "-")
                .replace(/^-|-$/g, "");

            enlace.href = url;
            enlace.download =
                `${nombreArchivo || "reporte"}-${reporte.id}.csv`;

            document.body.appendChild(enlace);
            enlace.click();
            enlace.remove();

            URL.revokeObjectURL(url);
        },

        prepararPDF(reporte) {
            this.reporteActual = reporte;

            // La vista actual puede imprimirse o guardarse como PDF
            // desde el diálogo de impresión del navegador.
            this.$nextTick(() => {
                window.print();
            });
        },

        fechaLocalISO() {
            const ahora = new Date();
            const anio = ahora.getFullYear();
            const mes = String(ahora.getMonth() + 1).padStart(2, "0");
            const dia = String(ahora.getDate()).padStart(2, "0");

            return `${anio}-${mes}-${dia}`;
        },

        formatearFecha(fecha) {
            if (!fecha) return "Sin fecha";

            const partes = fecha.split("-");

            if (partes.length !== 3) {
                return fecha;
            }

            return `${partes[2]}/${partes[1]}/${partes[0]}`;
        }
    }
}).mount("#reportesApp");