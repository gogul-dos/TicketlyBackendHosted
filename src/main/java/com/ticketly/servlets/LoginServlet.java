package com.ticketly.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.ticketly.utils.DBUtils;
import com.ticketly.utils.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;

import java.util.Date;

//@WebServlet("/login") // Add this annotation if it's not already present in your actual file
public class LoginServlet extends HttpServlet {

    private String jsonResponse(boolean success, String message) {
        return String.format("{\"success\": %b, \"message\": \"%s\"}", success, escapeJson(message));
    }

    private String escapeJson(String str) {
        return str.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private boolean emailExists(String email) {
        try (Connection connection = DBUtils.getConnection()) {
            String sql = "SELECT email FROM users WHERE email = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            System.out.println("Error checking email existence.");
            e.printStackTrace();
            return false;
        }
    }

    private boolean validCredentials(String email, String password) {
        try (Connection connection = DBUtils.getConnection()) {
            String sql = "SELECT password FROM users WHERE email = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return PasswordUtil.verifyPassword(password, storedPassword);
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error validating credentials.");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type");
        res.setHeader("Access-Control-Allow-Credentials", "true");

        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String email = req.getParameter("email");
        String password = req.getParameter("password");

        System.out.println("Login attempt with email: " + email);

        if (email == null || password == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().println(jsonResponse(false, "Email or password is missing."));
            return;
        }

        if (!emailExists(email)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().println(jsonResponse(false, "Email not registered."));
            return;
        }

        if (!validCredentials(email, password)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().println(jsonResponse(false, "Incorrect email or password."));
            return;
        }

        String jwtToken = Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(SignatureAlgorithm.HS256, "Ticketly")
                .compact();

        Cookie cookie = new Cookie("token", jwtToken);
        cookie.setPath("/");
        cookie.setMaxAge(48 * 60 * 60); // 2 days
        cookie.setSecure(false); // Set to true for HTTPS in production

        res.addCookie(cookie);

        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().println(jsonResponse(true, "Login successful"));
    }
}
