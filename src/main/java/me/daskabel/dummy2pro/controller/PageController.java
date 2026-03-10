package me.daskabel.dummy2pro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/room/{id}")
    public String room(@PathVariable int id) {
        if (id < 1 || id > 7) return "redirect:/dashboard";
        return "room" + id; // http://localhost:8080/room/1 .. http://localhost:8080/room/7
    }
}