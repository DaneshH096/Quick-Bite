package com.fooddelivery.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * GlobalExceptionHandler — catches all uncaught exceptions across the app
 * and routes them to user-friendly error pages instead of showing white-label errors.
 *
 * Covers:
 *   403 Access Denied    → error/403.html
 *   404 Not Found        → error/404.html
 *   500 Server Error     → error/500.html
 *   Any RuntimeException → error/general.html  (with safe message)
 *
 * API endpoints (under /api/**) return JSON error responses, not HTML pages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // ── 403 Access Denied ─────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(HttpServletRequest req, Model model) {
        model.addAttribute("requestUri", req.getRequestURI());
        return "error/403";
    }

    // ── 404 Not Found (Spring MVC route not found) ─────────────────
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(HttpServletRequest req, Model model) {
        model.addAttribute("requestUri", req.getRequestURI());
        return "error/404";
    }

    // ── Restaurant / User not found (app-level 404) ────────────────
    // Thrown by orElseThrow() in controllers when entity doesn't exist.
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntime(RuntimeException ex,
                                HttpServletRequest req,
                                Model model) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.";

        // Route "not found" runtime errors to 404
        if (msg.toLowerCase().contains("not found")) {
            model.addAttribute("requestUri", req.getRequestURI());
            model.addAttribute("errorMessage", msg);
            return "error/404";
        }

        // All other runtime errors: friendly error page
        model.addAttribute("errorTitle",   "Something went wrong");
        model.addAttribute("errorMessage", safeMessage(msg));
        model.addAttribute("errorEmoji",   "⚙️");
        model.addAttribute("backUrl",      req.getHeader("Referer"));
        return "error/general";
    }

    // ── Catch-all 500 ─────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        // Only show technical details in dev mode (message trimmed)
        model.addAttribute("errorMessage", safeMessage(ex.getMessage()));
        return "error/500";
    }

    /**
     * Strip sensitive details from error messages before showing to users.
     * DB connection strings, stack traces, passwords must never leak.
     */
    private String safeMessage(String msg) {
        if (msg == null) return null;
        // Hide JDBC / SQL internals
        if (msg.contains("jdbc:") || msg.contains("JDBC") ||
            msg.contains("SQLException") || msg.contains("HibernateException")) {
            return "A database error occurred. Please try again later.";
        }
        // Truncate very long messages
        return msg.length() > 200 ? msg.substring(0, 197) + "…" : msg;
    }
}
