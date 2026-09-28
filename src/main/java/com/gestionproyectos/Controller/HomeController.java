
package com.gestionproyectos.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

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
    public String recuperarPassword() {
        return "auth/recuperar-password";
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
}