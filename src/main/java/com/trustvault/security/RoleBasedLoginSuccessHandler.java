package com.trustvault.security;
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedLoginSuccessHandler
        implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        boolean adminLoginRequested =
                "admin".equals(request.getParameter("loginType"));

        if (adminLoginRequested && !isAdmin) {
            SecurityContextHolder.clearContext();

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            response.sendRedirect(
                    request.getContextPath()
                            + "/admin-login.html?error=not-admin");
            return;
        }

        if (isAdmin) {
            response.sendRedirect(
                    request.getContextPath() + "/admin.html");
        } else {
            response.sendRedirect(
                    request.getContextPath() + "/dashboard.html");
        }
    }
}

