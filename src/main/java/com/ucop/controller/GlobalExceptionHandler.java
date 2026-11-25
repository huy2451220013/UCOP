package com.ucop.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNotFound(NoResourceFoundException ex, HttpServletRequest request, Model model) {
        model.addAttribute("errorMessage", "Không tìm thấy trang hoặc tài nguyên: " + request.getRequestURI());
        model.addAttribute("requestUri", request.getRequestURI());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, HttpServletRequest request, Model model) {
        ex.printStackTrace();
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi không xác định.");
        model.addAttribute("requestUri", request.getRequestURI());
        return "error";
    }
}
