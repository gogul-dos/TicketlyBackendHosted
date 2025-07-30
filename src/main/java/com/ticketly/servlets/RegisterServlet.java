package com.ticketly.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import com.ticketly.utils.DBUtils;
import com.ticketly.utils.PasswordUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RegisterServlet extends HttpServlet {

    private boolean emailAlreadyExists(String email) {
        String query = "SELECT 1 FROM users WHERE email = ?";
        try (Connection connection = DBUtils.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
            return true; // Assume taken if error occurs
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

        var out = res.getWriter();

        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");
        String firstName = req.getParameter("firstName");
        String lastName = req.getParameter("lastName");
        String email = req.getParameter("email");
        String mobileStr = req.getParameter("mobile");
        String dobStr = req.getParameter("dob");

        if (isEmpty(password) || isEmpty(confirmPassword) || isEmpty(firstName) ||
            isEmpty(lastName) || isEmpty(email) || isEmpty(mobileStr) || isEmpty(dobStr)) {
            out.println(jsonResponse(false, "All fields are required."));
            return;
        }

        if (!password.equals(confirmPassword)) {
            out.println(jsonResponse(false, "Passwords do not match."));
            return;
        }

        if (password.length() < 8 || password.length() > 18) {
            out.println(jsonResponse(false, "Password must be 8 to 18 characters."));
            return;
        }

        if (!isValidEmail(email)) {
            out.println(jsonResponse(false, "Invalid email address."));
            return;
        }

        if (emailAlreadyExists(email)) {
            out.println(jsonResponse(false, "Email already registered."));
            return;
        }

        if (!isValidMobile(mobileStr)) {
            out.println(jsonResponse(false, "Invalid mobile number."));
            return;
        }

        Date dob;
        try {
            dob = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
        } catch (ParseException e) {
            out.println(jsonResponse(false, "Invalid date format."));
            return;
        }

        long mobile;
        try {
            mobile = Long.parseLong(mobileStr);
        } catch (NumberFormatException e) {
            out.println(jsonResponse(false, "Invalid mobile number format."));
            return;
        }

        java.sql.Date sqlDob = new java.sql.Date(dob.getTime());
        if (insertIntoDb(password, firstName, lastName, email, mobile, sqlDob)) {
            out.println(jsonResponse(true, "Registration successful."));
        } else {
            out.println(jsonResponse(false, "Unknown error."));
        }
    }

    private boolean insertIntoDb(String password, String firstName, String lastName,
                                 String email, Long mobile, java.sql.Date dob) {
        try (Connection connection = DBUtils.getConnection()) {
            String sql = "INSERT INTO users (first_name, last_name, password, email, mobile_number, dob) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            String hashedPassword = PasswordUtil.hashPassword(password);
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, hashedPassword);
            stmt.setString(4, email);
            stmt.setLong(5, mobile);
            stmt.setDate(6, dob); // Works because SQLite JDBC maps it to TEXT or INTEGER
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[\\w.-]+@[\\w.-]+\\.\\w{2,}$";
        return Pattern.matches(emailRegex, email);
    }

    private boolean isValidMobile(String mobile) {
        return Pattern.matches("\\d{10}", mobile);
    }

    private String jsonResponse(boolean success, String message) {
        return String.format("{\"success\": %b, \"message\": \"%s\"}", success, escapeJson(message));
    }

    private String escapeJson(String str) {
        return str.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
