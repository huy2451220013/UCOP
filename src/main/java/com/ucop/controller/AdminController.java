package com.ucop.controller;

import com.ucop.enums.UserRole;
import com.ucop.service.AccountService;
import com.ucop.service.AuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AccountService accountService;
    private final AuditService auditService;

    public AdminController(AccountService accountService, AuditService auditService) {
        this.accountService = accountService;
        this.auditService = auditService;
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "role", required = false) UserRole role,
                            Model model) {
        model.addAttribute("accounts", accountService.searchAccounts(keyword, role));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("allRoles", UserRole.values());
        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam("email") String email,
                             @RequestParam("fullName") String fullName,
                             @RequestParam(value = "phone", required = false) String phone,
                             @RequestParam("role") UserRole role,
                             HttpSession session,
                             RedirectAttributes ra) {
        try {
            String admin = (String) session.getAttribute("username");
            accountService.createAccountByAdmin(username, password, email, fullName, phone, role, admin);
            ra.addFlashAttribute("success", "Đã tạo tài khoản '" + username + "' với quyền " + role + "!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable("id") Long id,
                             @RequestParam("role") UserRole role,
                             HttpSession session,
                             RedirectAttributes ra) {
        try {
            String admin = (String) session.getAttribute("username");
            accountService.updateRole(id, role, admin);
            ra.addFlashAttribute("success", "Đã cập nhật vai trò thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-lock")
    public String toggleLock(@PathVariable("id") Long id,
                             @RequestParam("lock") boolean lock,
                             HttpSession session,
                             RedirectAttributes ra) {
        Long currentId = (Long) session.getAttribute("currentUserId");
        if (currentId != null && currentId.equals(id) && lock) {
            ra.addFlashAttribute("error", "Không thể tự khóa tài khoản đang đăng nhập!");
            return "redirect:/admin/users";
        }
        try {
            String admin = (String) session.getAttribute("username");
            accountService.toggleLock(id, lock, admin);
            ra.addFlashAttribute("success", lock ? "Đã khóa tài khoản!" : "Đã mở khóa tài khoản!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/audit")
    public String viewAuditLogs(@RequestParam(value = "entityType", required = false) String entityType,
                                @RequestParam(value = "action", required = false) String action,
                                Model model) {
        model.addAttribute("logs", auditService.getRecentLogs(entityType, action));
        model.addAttribute("selectedEntityType", entityType);
        model.addAttribute("selectedAction", action);
        return "admin/audit";
    }
}
