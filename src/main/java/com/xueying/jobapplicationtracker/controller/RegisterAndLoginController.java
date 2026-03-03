package com.xueying.jobapplicationtracker.controller;

import com.xueying.jobapplicationtracker.service.UserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles account registration and session login/logout entry points.
 */
@Controller
public class RegisterAndLoginController {
    private final UserService userService;

    public RegisterAndLoginController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        Subject subject = SecurityUtils.getSubject();
        if (subject != null && subject.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("activeNav", "");
        return "login";
    }

    @PostMapping("/login")
    /**
     * Authenticates by email and password, then redirects to dashboard.
     */
    public String login(@RequestParam("email") String email,
                        @RequestParam("password") String password,
                        RedirectAttributes redirectAttributes) {
        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(email, password);
        try {
            subject.login(token);
            return "redirect:/dashboard";
        } catch (AuthenticationException e) {
            redirectAttributes.addFlashAttribute("message", "Invalid email or password.");
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        Subject subject = SecurityUtils.getSubject();
        if (subject != null && subject.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        model.addAttribute("activeNav", "");
        return "register";
    }

    @PostMapping("/register")
    /**
     * Creates a user account after basic validation in UserService.
     */
    public String register(@RequestParam("email") String email,
                           @RequestParam("password") String password,
                           RedirectAttributes redirectAttributes) {
        boolean created = userService.register(email, password);
        if (!created) {
            redirectAttributes.addFlashAttribute("message", "Registration failed. Check email format, password length, or duplicate account.");
            return "redirect:/register";
        }
        redirectAttributes.addFlashAttribute("message", "Registration successful. Please login.");
        return "redirect:/login";
    }
}

