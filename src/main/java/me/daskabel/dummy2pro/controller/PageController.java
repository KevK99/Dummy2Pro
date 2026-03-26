package me.daskabel.dummy2pro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;

import me.daskabel.dummy2pro.repository.ThemeRepository;

@Controller
public class PageController {

    private final ThemeRepository themeRepository;

    public PageController(ThemeRepository themeRepository)
    {
        this.themeRepository = themeRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model)
    {
        model.addAttribute("username", "NutzerName");
        model.addAttribute("answeredQuestions", 0);
        model.addAttribute("totalQuestions", 0);
        model.addAttribute("avatarUrl", "/images/duck.jpg");

        return "dashboard";
    }

    @GetMapping("/room/{id}")
    public String room(@PathVariable int id) {
        int roomCount =  Math.min(themeRepository.findAllByOrderByThemeIdAsc().size(), 7);
        if (id < 1 || id > roomCount) return "redirect:/dashboard";
        return "room" + id; // http://localhost:8080/room/1 .. http://localhost:8080/room/7
    }
}