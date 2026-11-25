package com.ucop.controller;

import com.ucop.dto.RegisterRequest;
import com.ucop.entity.Account;
import com.ucop.enums.UserRole;
import com.ucop.service.AccountService;
import com.ucop.service.AuditService;
import com.ucop.service.CartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final AccountService accountService;
    private final AuditService auditService;
    private final CartService cartService;

    public AuthController(AccountService accountService, AuditService auditService, CartService cartService) {
        this.accountService = accountService;
        this.auditService = auditService;
        this.cartService = cartService;
    }

    @GetMapping("/login")
    public String loginForm(HttpSession session, Model model) {
        if (session.getAttribute("currentUserId") != null) {
            return "redirect:/";
        }
        Object prompt = session.getAttribute("loginPrompt");
        if (prompt != null) {
            session.removeAttribute("loginPrompt");
            if (!model.containsAttribute("error") && !model.containsAttribute("success")) {
                model.addAttribute("success", prompt.toString());
            }
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          HttpSession session,
                          RedirectAttributes ra) {
        Optional<Account> opt = accountService.authenticate(username, password);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Sai tên đăng nhập hoặc mật khẩu!");
            return "redirect:/login";
        }

        Account acc = opt.get();
        if ("LOCKED".equalsIgnoreCase(acc.getStatus())) {
            ra.addFlashAttribute("error", "Tài khoản của bạn đang bị KHÓA! Vui lòng liên hệ Quản trị viên.");
            return "redirect:/login";
        }

        boolean isAdmin = acc.hasRole(UserRole.ADMIN);
        boolean isStaff = acc.hasRole(UserRole.STAFF);

        session.setAttribute("currentUserId", acc.getId());
        session.setAttribute("username", acc.getUsername());
        session.setAttribute("userRole", acc.getPrimaryRoleName());
        session.setAttribute("isAdmin", isAdmin);
        session.setAttribute("isStaff", isStaff);

        auditService.log("LOGIN", "Account", acc.getId(), acc.getUsername(), null, acc.getPrimaryRoleName(),
                "Đăng nhập hệ thống thành công");

        Long pendingItemId = (Long) session.getAttribute("pendingCartItemId");
        Integer pendingQty = (Integer) session.getAttribute("pendingCartQty");
        session.removeAttribute("pendingCartItemId");
        session.removeAttribute("pendingCartQty");
        session.removeAttribute("loginPrompt");

        if (pendingItemId != null) {
            try {
                cartService.addItem(acc.getId(), pendingItemId, pendingQty != null ? pendingQty : 1);
                ra.addFlashAttribute("success", "Xin chào " + acc.getUsername() + "! Đã thêm sản phẩm vào giỏ hàng của bạn.");
            } catch (Exception e) {
                ra.addFlashAttribute("error", e.getMessage());
            }
            return "redirect:/cart";
        }

        ra.addFlashAttribute("success", "Xin chào " + acc.getUsername() + " (" + acc.getPrimaryRoleName() + ")!");
        if (isAdmin) return "redirect:/reports/dashboard";
        if (isStaff) return "redirect:/orders";
        return "redirect:/items";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest req,
                             BindingResult br,
                             Model model,
                             RedirectAttributes ra) {
        if (br.hasErrors()) {
            return "auth/register";
        }
        try {
            accountService.registerCustomer(req);
            ra.addFlashAttribute("success", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        Account acc = accountService.findById(accountId).orElseThrow();
        model.addAttribute("account", acc);
        return "auth/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "address", required = false) String address,
                                HttpSession session,
                                RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        try {
            accountService.updateProfile(accountId, fullName, phone, address);
            ra.addFlashAttribute("success", "Cập nhật thông tin hồ sơ thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Long accountId = (Long) session.getAttribute("currentUserId");
        try {
            accountService.changePassword(accountId, oldPassword, newPassword);
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }
}
