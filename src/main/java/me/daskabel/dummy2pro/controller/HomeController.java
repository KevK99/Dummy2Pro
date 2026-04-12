package me.daskabel.dummy2pro.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Leitet den Aufruf der Start-URL auf die eigentliche Einstiegsseite weiter.
 */
@Controller
public class HomeController
{
    @GetMapping("/")
    public String home()
    {
        return "redirect:/index.html";
    }
}
