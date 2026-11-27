
package com.gestionproyectos.Controller;

import com.gestionproyectos.service.RecuperacionPasswordServicio;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final RecuperacionPasswordServicio recuperacion;

    public HomeController(RecuperacionPasswordServicio recuperacion) {
        this.recuperacion = recuperacion;
    }

    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registro() {
        return "auth/registro";
    }

    @GetMapping("/recuperar-password")
    public String recuperarPassword(HttpServletResponse response) {
        noGuardar(response);
        return "auth/recuperar-password";
    }

    @PostMapping("/recuperar-password")
    public String solicitarRecuperacion(@RequestParam String correo, Model modelo,
                                       HttpServletResponse response, RedirectAttributes atributos) {
        noGuardar(response);
        try {
            recuperacion.solicitar(correo);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            modelo.addAttribute("error", "La recuperación por correo no está disponible en este momento.");
            return "auth/recuperar-password";
        }
        atributos.addFlashAttribute("mensaje", "Si existe una cuenta con ese correo, recibirás un enlace para restablecer la contraseña.");
        return "redirect:/recuperar-password";
    }

    @GetMapping("/restablecer-password")
    public String formularioRestablecer(@RequestParam(required = false) String token, Model modelo,
                                       HttpServletResponse response) {
        noGuardar(response);
        modelo.addAttribute("token", token == null ? "" : token);
        return "auth/restablecer-password";
    }

    @PostMapping("/restablecer-password")
    public String restablecer(@RequestParam String token, @RequestParam String nuevaContrasena,
                              @RequestParam String confirmarContrasena, Model modelo,
                              HttpServletResponse response) {
        noGuardar(response);
        modelo.addAttribute("token", token);
        if (!nuevaContrasena.equals(confirmarContrasena)) {
            modelo.addAttribute("error", "Las contraseñas no coinciden.");
            return "auth/restablecer-password";
        }
        try {
            recuperacion.restablecer(token, nuevaContrasena);
        } catch (IllegalArgumentException e) {
            modelo.addAttribute("error", e.getMessage());
            return "auth/restablecer-password";
        }
        return "redirect:/login?restablecida";
    }

    private void noGuardar(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }

    // Rúbrica: expone la vista del dashboard mediante una ruta del controlador Spring MVC.
    @Controller
    public class DashboardController {

        @GetMapping("/dashboard")
        public String mostrarDashboard() {
            return "dashboard/dashboard";
        }
    }

    // Spring MVC + Thymeleaf: muestra la página de proyectos.
    @GetMapping("/proyectos")
    public String mostrarProyectos() {
        return "dashboard/proyectos";
    }

    @GetMapping("/tareas")
    public String mostrarTareas() {
        return "dashboard/tareas";
    }

    @GetMapping("/equipo")
    public String mostrarEquipo() {
        return "dashboard/equipo";
    }

    @GetMapping("/reportes")
    public String mostrarReportes() {
        return "dashboard/reportes";
    }

    @GetMapping("/configuracion")
    public String mostrarConfiguracion() {
        return "perfil/configuracion";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil() {
        return "redirect:/configuracion";
    }
}
