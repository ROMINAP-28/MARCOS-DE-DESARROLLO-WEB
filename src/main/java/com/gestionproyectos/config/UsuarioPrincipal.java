package com.gestionproyectos.config;

import com.gestionproyectos.model.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class UsuarioPrincipal extends User {
    private final Integer id;
    private final String nombre;
    private final String rol;

    public UsuarioPrincipal(Usuario usuario) {
        super(usuario.getEmail(), usuario.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol())));
        this.id = usuario.getIdUsuario();
        this.nombre = usuario.getNombre();
        this.rol = usuario.getRol();
    }

    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
}
