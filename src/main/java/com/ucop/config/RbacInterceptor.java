package com.ucop.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RbacInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);

        Long userId = (session != null) ? (Long) session.getAttribute("currentUserId") : null;
        boolean isAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"));
        boolean isStaff = session != null && Boolean.TRUE.equals(session.getAttribute("isStaff"));

        // Admin-only routes (SV1 RBAC)
        if (uri.startsWith("/admin")) {
            if (userId == null) {
                response.sendRedirect("/login");
                return false;
            }
            if (!isAdmin) {
                response.sendRedirect("/access-denied");
                return false;
            }
            return true;
        }

        // Staff or Admin routes
        if (uri.startsWith("/stock") || uri.startsWith("/promotions") || uri.startsWith("/reports")
                || uri.startsWith("/payment/refunds") || uri.startsWith("/fulfillment")
                || uri.startsWith("/items/add") || uri.startsWith("/items/edit") || uri.startsWith("/items/save")
                || uri.startsWith("/items/delete") || uri.startsWith("/items/import-csv")
                || uri.startsWith("/categories/save") || uri.startsWith("/categories/delete") || uri.startsWith("/categories/import-csv")) {
            if (userId == null) {
                response.sendRedirect("/login");
                return false;
            }
            if (!isAdmin && !isStaff) {
                response.sendRedirect("/access-denied");
                return false;
            }
            return true;
        }

        // Authenticated Customer/Staff/Admin routes
        if (uri.startsWith("/cart") || uri.startsWith("/orders") || uri.startsWith("/payment") || uri.startsWith("/profile")) {
            if (userId == null) {
                HttpSession loginSession = request.getSession(true);
                if ("/cart/add".equals(uri)) {
                    String itemIdParam = request.getParameter("itemId");
                    String qtyParam = request.getParameter("quantity");
                    if (itemIdParam != null && !itemIdParam.isBlank()) {
                        try {
                            loginSession.setAttribute("pendingCartItemId", Long.parseLong(itemIdParam.trim()));
                            int qty = (qtyParam != null && !qtyParam.isBlank()) ? Integer.parseInt(qtyParam.trim()) : 1;
                            loginSession.setAttribute("pendingCartQty", Math.max(1, qty));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    loginSession.setAttribute("loginPrompt", "Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng (sản phẩm bạn chọn sẽ được tự động thêm sau khi đăng nhập)!");
                } else {
                    loginSession.setAttribute("loginPrompt", "Vui lòng đăng nhập để tiếp tục sử dụng chức năng này!");
                }
                response.sendRedirect("/login");
                return false;
            }
        }

        return true;
    }
}
