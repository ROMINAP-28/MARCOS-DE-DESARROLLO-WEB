const { createApp } = Vue;

createApp({
    data() {
        return {
            usuario: 'Usuario',
            idUsuarioActual: null,
            esAdministrador: false,
            vista: 'integrantes',
            busqueda: '',
            filtroProyecto: '',
            filtroRol: '',
            filtroEspecialidad: '',
            filtroEstado: '',
            mostrarModalIntegrante: false,
            modoEdicion: false,
            integranteSeleccionado: null,
            tareaAsignada: null,
            formularioTarea: { nombre: '', descripcion: '', prioridad: 'Media', fechaInicio: '', fechaFin: '' },
            mensajeError: '',
            mensajeExito: '',
            errorCarga: false,
            cargando: true,
            guardando: false,
            proyectos: [],
            usuarios: [],
            especialidades: [],
            roles: ['Responsable', 'Integrante'],
            estados: ['Activo', 'Inactivo', 'Pendiente'],
            integrantes: [],
            formulario: { idProyecto: '', idUsuario: '', estado: 'Activo' }
        };
    },

    computed: {
        integrantesFiltrados() {
            const texto = this.busqueda.trim().toLowerCase();
            return this.integrantes.filter(integrante =>
                [integrante.nombre, integrante.correo, integrante.especialidad, integrante.rol]
                    .some(valor => (valor || '').toLowerCase().includes(texto)) &&
                (!this.filtroProyecto || integrante.idProyecto === this.filtroProyecto) &&
                (!this.filtroRol || integrante.rol === this.filtroRol) &&
                (!this.filtroEspecialidad || integrante.especialidad === this.filtroEspecialidad) &&
                (!this.filtroEstado || integrante.estado === this.filtroEstado)
            );
        },
        usuariosDisponibles() {
            return this.usuarios.filter(usuario => !this.integrantes.some(integrante =>
                integrante.idProyecto === this.formulario.idProyecto &&
                integrante.idUsuario === usuario.id));
        },
        totalTareasAsignadas() {
            return this.integrantesFiltrados.reduce(
                (total, integrante) => total + Number(integrante.tareasAsignadas || 0), 0);
        },
        resumenEspecialidades() {
            return this.especialidades.map(especialidad => ({
                nombre: especialidad.nombre,
                total: this.integrantes.filter(integrante => integrante.especialidad === especialidad.nombre).length
            }));
        }
    },

    mounted() {
        this.cargarDatos();
        this.alActualizar = evento => {
            if (['todos', 'proyectos', 'tareas', 'equipo'].includes(evento.detail?.recurso)) {
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
                throw new Error(error.mensaje || 'No se pudo completar la operación.');
            }
            return respuesta.status === 204 ? null : respuesta.json();
        },

        async cargarDatos() {
            this.cargando = true;
            try {
                const [proyectos, usuarios, especialidades, participaciones, cuenta] = await Promise.all([
                    this.solicitar('/api/miembro3/proyectos'),
                    this.solicitar('/api/miembro3/usuarios'),
                    this.solicitar('/api/miembro3/especialidades'),
                    this.solicitar('/api/miembro3/participaciones'),
                    this.solicitar('/api/auth/yo')
                ]);
                this.proyectos = proyectos;
                this.idUsuarioActual = cuenta.id;
                this.esAdministrador = cuenta.rol === 'ADMIN';
                this.usuario = cuenta.nombre;
                this.usuarios = usuarios;
                this.especialidades = especialidades;
                this.integrantes = participaciones.map(item => ({
                    ...item,
                    especialidad: item.especialidad || 'Sin especialidad',
                    fechaIngreso: item.fechaAsignacion?.slice(0, 10) || ''
                }));
                this.mensajeError = '';
                this.errorCarga = false;
                return true;
            } catch (error) {
                this.proyectos = [];
                this.usuarios = [];
                this.especialidades = [];
                this.integrantes = [];
                this.idUsuarioActual = null;
                this.esAdministrador = false;
                this.mensajeError = error.message;
                this.errorCarga = true;
                return false;
            } finally {
                this.cargando = false;
            }
        },

        contarEstado(estado) {
            return this.integrantesFiltrados.filter(integrante => integrante.estado === estado).length;
        },
        puedeGestionar(integrante) {
            return this.proyectos.some(proyecto => proyecto.id === integrante.idProyecto
                && proyecto.idResponsable === this.idUsuarioActual);
        },
        iniciales(nombre) {
            return (nombre || '').trim().split(/\s+/).filter(Boolean).slice(0, 2)
                .map(parte => parte[0].toUpperCase()).join('');
        },
        claseEstado(estado) {
            return { Activo: 'text-bg-success', Inactivo: 'text-bg-secondary',
                Pendiente: 'text-bg-warning' }[estado] || 'text-bg-light';
        },
        abrirNuevoIntegrante() {
            this.modoEdicion = false;
            this.formulario = { idProyecto: '', idUsuario: '', estado: 'Activo' };
            this.mensajeError = '';
            this.mensajeExito = '';
            this.mostrarModalIntegrante = true;
        },
        editarIntegrante(integrante) {
            this.modoEdicion = true;
            this.formulario = { idProyecto: integrante.idProyecto, idUsuario: integrante.idUsuario,
                estado: integrante.estado, nombre: integrante.nombre, proyecto: integrante.proyecto };
            this.integranteSeleccionado = null;
            this.mensajeError = '';
            this.mensajeExito = '';
            this.mostrarModalIntegrante = true;
        },
        cerrarModal() {
            this.mostrarModalIntegrante = false;
            this.mensajeError = '';
        },

        async guardarIntegrante() {
            this.mensajeError = '';
            if (!this.formulario.idProyecto || !this.formulario.idUsuario || !this.formulario.estado) {
                this.mensajeError = 'Selecciona un usuario, un proyecto y un estado.';
                return;
            }
            this.guardando = true;
            try {
                const edicion = this.modoEdicion;
                const url = edicion
                    ? `/api/miembro3/participaciones/${this.formulario.idProyecto}/${this.formulario.idUsuario}`
                    : '/api/miembro3/participaciones';
                await this.solicitar(url, {
                    method: edicion ? 'PUT' : 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(edicion ? { estado: this.formulario.estado } : this.formulario)
                });
                const recargado = await this.cargarDatos();
                this.mostrarModalIntegrante = false;
                this.mensajeExito = edicion ? 'Participación actualizada.' : 'Integrante incorporado.';
                if (!recargado) this.mensajeError = 'Guardado, pero no se pudo recargar el equipo.';
            } catch (error) {
                this.mensajeError = error.message;
            } finally {
                this.guardando = false;
            }
        },
        verPerfil(integrante) { this.integranteSeleccionado = integrante; },

        async retirarIntegrante(integrante) {
            if (!window.confirm(`¿Deseas retirar a ${integrante.nombre} de ${integrante.proyecto}?`)) return;
            this.mensajeError = '';
            this.mensajeExito = '';
            try {
                await this.solicitar(
                    `/api/miembro3/participaciones/${integrante.idProyecto}/${integrante.idUsuario}`,
                    { method: 'DELETE' });
                const recargado = await this.cargarDatos();
                this.mensajeExito = 'Integrante retirado del proyecto.';
                if (!recargado) this.mensajeError = 'Retirado, pero no se pudo recargar el equipo.';
            } catch (error) {
                this.mensajeError = error.message;
            }
        },

        asignarTarea(integrante) {
            this.tareaAsignada = integrante;
            this.formularioTarea = { nombre: '', descripcion: '', prioridad: 'Media', fechaInicio: '', fechaFin: '' };
            this.mensajeError = '';
            this.mensajeExito = '';
        },
        async guardarTareaAsignada() {
            if (!this.tareaAsignada || !this.formularioTarea.nombre.trim() || !this.formularioTarea.fechaFin) {
                this.mensajeError = 'Ingresa el nombre y la fecha de fin de la tarea.';
                return;
            }
            this.guardando = true;
            try {
                await this.solicitar('/api/miembro3/tareas', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ ...this.formularioTarea,
                        fechaInicio: this.formularioTarea.fechaInicio || null,
                        idProyecto: this.tareaAsignada.idProyecto,
                        idResponsable: this.tareaAsignada.idUsuario, estado: 'Pendiente' })
                });
                const recargado = await this.cargarDatos();
                this.tareaAsignada = null;
                this.mensajeExito = 'Tarea registrada en PostgreSQL.';
                if (!recargado) this.mensajeError = 'Guardada, pero no se pudo recargar el equipo.';
            } catch (error) {
                this.mensajeError = error.message;
            } finally {
                this.guardando = false;
            }
        },

        async abrirEspecialidad() {
            const nombre = window.prompt('Nombre de la nueva especialidad:');
            if (nombre === null) return;
            this.mensajeError = '';
            this.mensajeExito = '';
            if (!nombre.trim()) { this.mensajeError = 'El nombre no puede estar vacío.'; return; }
            try {
                await this.solicitar('/api/miembro3/especialidades', {
                    method: 'POST', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ nombre: nombre.trim() })
                });
                const recargado = await this.cargarDatos();
                this.mensajeExito = 'Especialidad registrada en PostgreSQL.';
                if (!recargado) this.mensajeError = 'Guardada, pero no se pudo recargar el equipo.';
            } catch (error) {
                this.mensajeError = error.message;
            }
        },
        filtrarPorEspecialidad(especialidad) {
            this.filtroEspecialidad = especialidad;
            this.vista = 'integrantes';
        },
        formatearFecha(fecha) {
            if (!fecha) return 'Sin fecha';
            const partes = fecha.split('-');
            return partes.length === 3 ? `${partes[2]}/${partes[1]}/${partes[0]}` : fecha;
        }
    }
}).mount('#equipoApp');
