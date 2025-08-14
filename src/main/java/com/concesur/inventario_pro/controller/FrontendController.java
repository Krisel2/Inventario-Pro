package com.concesur.inventario_pro.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class FrontendController {

    @GetMapping("/inventario")
    public RedirectView redirectToIndex() {
        return new RedirectView("/index.html");
    }

    @GetMapping(value = "/{path:[^\\.]*}")
    public String catchAll() {
        return "forward:/index.html";
    }
}
