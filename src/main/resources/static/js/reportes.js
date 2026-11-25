const { createApp } = Vue;

createApp({
    data() {
        return {
            usuario: 'Usuario',
            tipoSeleccionado: 'avance',
            mensajeError: '',
            cargando: true,
            consultando: false,
            reporteActual: null,
            proyectos: [],
            indicadores: null,
            filtros: { idProyecto: '', fechaDesde: '', fechaHasta: '' },
            tiposReporte: [
                { id: 'avance', nombre: 'Avance de proyectos',
                    descripcion: 'Progreso y estado de proyectos según su fecha de inicio.',
                    icono: 'bi bi-bar-chart-line', color: 'icon-blue' },
                { id: 'tareas', nombre: 'Estado de tareas',
                    descripcion: 'Tareas agrupadas por estado y fecha de fin.',
                    icono: 'bi bi-list-check', color: 'icon-green' },
                { id: 'equipo', nombre: 'Participación del equipo',
                    descripcion: 'Participaciones según su fecha de incorporación.',
                    icono: 'bi bi-people', color: 'icon-purple' },
                { id: 'recursos', nombre: 'Recursos registrados',
                    descripcion: 'Cantidad y tipos de recursos por proyecto (sin filtro de fechas).',
                    icono: 'bi bi-box-seam', color: 'icon-orange' }
            ]
        };
    },
    computed: {
        inicialesUsuario() { return this.iniciales(this.usuario); },
        nombreTipoSeleccionado() {
            return this.tiposReporte.find(tipo => tipo.id === this.tipoSeleccionado)?.nombre || '';
        }
    },
    mounted() {
        this.cargarDatos();
        this.alActualizar = evento => {
            if (['todos', 'proyectos', 'tareas', 'equipo'].includes(evento.detail?.recurso)) {
                this.reporteActual = null;
                this.cargarDatos();
            }
        };
        window.addEventListener('gestion:actualizado', this.alActualizar);
    },
    beforeUnmount() {
        window.removeEventListener('gestion:actualizado', this.alActualizar);
    },
    methods: {
        async solicitar(url, opciones) {
            const respuesta = await fetch(url, opciones);
            if (!respuesta.ok) {
                const error = await respuesta.json().catch(() => ({}));
                throw new Error(error.mensaje || 'No se pudo consultar el servidor.');
            }
            return respuesta.json();
        },
        async cargarDatos() {
            this.cargando = true;
            this.mensajeError = '';
            try {
                const [proyectos, indicadores] = await Promise.all([
                    this.solicitar('/api/miembro3/proyectos'),
                    this.solicitar('/api/miembro3/reportes/indicadores')
                ]);
                this.proyectos = proyectos;
                this.indicadores = indicadores;
            } catch (error) {
                this.proyectos = [];
                this.indicadores = null;
                this.reporteActual = null;
                this.mensajeError = error.message;
            } finally {
                this.cargando = false;
            }
        },
        iniciales(nombre) {
            return (nombre || 'Usuario').trim().split(/\s+/).slice(0, 2)
                .map(parte => parte.charAt(0).toUpperCase()).join('');
        },
        seleccionarTipo(id) {
            if (!this.tiposReporte.some(tipo => tipo.id === id)) return;
            this.tipoSeleccionado = id;
            this.reporteActual = null;
            this.mensajeError = '';
            if (id === 'recursos') {
                this.filtros.fechaDesde = '';
                this.filtros.fechaHasta = '';
            }
            this.irAlGenerador();
        },
        abrirGenerador() { this.irAlGenerador(); },
        irAlGenerador() {
            this.$nextTick(() => document.getElementById('generadorReporte')
                ?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
        },
        limpiarFiltros() {
            this.filtros = { idProyecto: '', fechaDesde: '', fechaHasta: '' };
            this.mensajeError = '';
        },
        async generarReporte() {
            this.mensajeError = '';
            this.reporteActual = null;
            if (!this.indicadores) {
                this.mensajeError = 'Carga los datos antes de consultar un reporte.';
                return;
            }
            if (this.filtros.fechaDesde && this.filtros.fechaHasta
                && this.filtros.fechaDesde > this.filtros.fechaHasta) {
                this.mensajeError = 'La fecha inicial no puede ser posterior a la final.';
                return;
            }
            this.consultando = true;
            try {
                this.reporteActual = await this.solicitar('/api/miembro3/reportes/consulta', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ tipo: this.tipoSeleccionado,
                        idProyecto: this.filtros.idProyecto || null,
                        fechaDesde: this.filtros.fechaDesde || null,
                        fechaHasta: this.filtros.fechaHasta || null })
                });
            } catch (error) {
                this.mensajeError = error.message;
            } finally {
                this.consultando = false;
            }
        },
        descargarCSV(reporte) {
            if (!reporte) return;
            const tipo = this.tiposReporte.find(item => item.id === reporte.tipo)?.nombre || reporte.tipo;
            const filas = [
                ['Reporte', tipo], ['Proyecto', reporte.proyecto],
                ['Fecha de consulta', reporte.fechaConsulta],
                ['Fecha desde', reporte.fechaDesde || 'Sin filtro'],
                ['Fecha hasta', reporte.fechaHasta || 'Sin filtro'],
                [], ['Indicador', 'Valor'],
                ...reporte.resumen.map(dato => [dato.nombre, dato.valor])
            ];
            // Evita que hojas de cálculo interpreten valores de la BD como fórmulas.
            const celda = valor => {
                let texto = String(valor ?? '');
                if (/^[\s\u0000-\u001f]*[=+@-]/.test(texto)) texto = "'" + texto;
                return `"${texto.replace(/"/g, '""')}"`;
            };
            const contenido = '\uFEFF' + filas.map(fila => fila.map(celda).join(',')).join('\r\n');
            const url = URL.createObjectURL(new Blob([contenido], { type: 'text/csv;charset=utf-8' }));
            const enlace = document.createElement('a');
            enlace.href = url;
            enlace.download = `consulta-${reporte.tipo}-${this.fechaLocalISO()}.csv`;
            document.body.appendChild(enlace);
            enlace.click();
            enlace.remove();
            URL.revokeObjectURL(url);
        },
        fechaLocalISO() {
            const ahora = new Date();
            return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}`
                + `-${String(ahora.getDate()).padStart(2, '0')}`;
        },
        formatearFecha(fecha) {
            if (!fecha) return 'Sin fecha';
            const partes = fecha.slice(0, 10).split('-');
            return partes.length === 3 ? `${partes[2]}/${partes[1]}/${partes[0]}` : fecha;
        }
    }
}).mount('#reportesApp');
