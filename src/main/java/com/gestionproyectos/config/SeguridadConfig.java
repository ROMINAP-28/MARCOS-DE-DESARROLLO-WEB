package com.gestionproyectos.config;

import com.gestionproyectos.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableMethodSecurity
public class SeguridadConfig {
    @Bean
    UserDetailsService usuariosAutenticados(UsuarioRepository repositorio) {
        return correo -> new UsuarioPrincipal(repositorio.findByEmailIgnoreCase(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas")));
    }

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(reglas -> reglas
                .requestMatchers("/", "/login", "/registro", "/recuperar-password",
                        "/css/**", "/js/**", "/images/**", "/img/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/registro").permitAll()
                .anyRequest().authenticated())
            .formLogin(login -> login.loginPage("/login").usernameParameter("correo")
                    .passwordParameter("contrasena").defaultSuccessUrl("/dashboard", true)
                    .failureUrl("/login?error").permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?salida").permitAll())
            .exceptionHandling(errores -> errores
                    .authenticationEntryPoint((request, response, exception) -> {
                        if (request.getRequestURI().startsWith("/api/")) {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        } else {
                            new LoginUrlAuthenticationEntryPoint("/login")
                                    .commence(request, response, exception);
                        }
                    })
                    .accessDeniedHandler((request, response, exception) -> {
                        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
                        if (request.getRequestURI().startsWith("/api/miembro3/")
                                && (autenticacion == null
                                    || autenticacion instanceof AnonymousAuthenticationToken)) {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        } else {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        }
                    }));
        return http.build();
    }
}
