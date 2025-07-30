package com.ticketly.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import com.ticketly.utils.DBUtils;
import com.ticketly.utils.PasswordUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginServlet extends HttpServlet {

    private String jsonResponse(boolean success, String message) {
        return String.format("{\"success\": %b, \"message\": \"%s\"}", success, escapeJson(message));
    }

    private String escapeJson(String str) {
        return str.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private boolean emailExists(String email) {
        try (Connection conn = DBUtils.getConnection()) {
            String sql = "SELECT email FROM users WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
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
        try (Connection conn = DBUtils.getConnection()) {
            String sql = "SELECT password FROM users WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
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
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        setCorsHeaders(res);

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

        int userId = -1;
        try (Connection conn = DBUtils.getConnection()) {
            String query = "SELECT id FROM users WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("id");
            } else {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().println(jsonResponse(false, "User not found after login."));
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().println(jsonResponse(false, "Error retrieving user_id."));
            return;
        }

        String jwtToken = Jwts.builder()
                .claim("user_id", userId)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day expiry
                .signWith(SignatureAlgorithm.HS256, "Ticketly")
                .compact();

        Cookie cookie = new Cookie("token", jwtToken);
        cookie.setPath("/");
        cookie.setMaxAge(48 * 60 * 60);
        cookie.setSecure(false);

        res.addCookie(cookie);
        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().println(jsonResponse(true, "Login successful"));
    }

    private void setCorsHeaders(HttpServletResponse res) {
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type");
        res.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
