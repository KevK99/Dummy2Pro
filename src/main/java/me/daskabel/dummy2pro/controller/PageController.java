package me.daskabel.dummy2pro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import me.daskabel.dummy2pro.repository.ThemeRepository;

@Controller
public class PageController
{
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
    public String room(@PathVariable int id, Model model)
    {
        if (id == 16)
        {
            model.addAttribute("roomId", 16);
            model.addAttribute("themeName", "Abschluss");
            return "room16";
        }

        var themes = themeRepository.findAllByOrderByThemeIdAsc();
        int roomCount = Math.min(themes.size(), 15);

        if (id < 1 || id > roomCount)
        {
            return "redirect:/dashboard";
        }

        var theme = themes.get(id - 1);

        model.addAttribute("roomId", id);
        model.addAttribute("themeName", theme.getName());

        return "room" + id;
    }

    @GetMapping("/endscreen")
    public String endscreen()
    {
        return "endscreen";
    }

    @GetMapping("/review")
    public String review(Model model)
    {
        model.addAttribute("avatarUrl", "/images/duck.jpg");
        return "review";
    }
}