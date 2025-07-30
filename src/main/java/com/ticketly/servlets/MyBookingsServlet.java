package com.ticketly.servlets;

import com.ticketly.utils.DBUtils;
import com.ticketly.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MyBookingsServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
        setCorsHeaders(res);
        res.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        setCorsHeaders(res);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try {
            System.out.println("My Bookings triggered");

            String authHeader = req.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String token = authHeader.replace("Bearer ", "");
            Claims claims = JwtUtil.verifyToken(token);
            if (claims == null || claims.get("user_id") == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            int userId = (int) claims.get("user_id");

            String sql = """
                SELECT t.ticket_id, t.total_amount, t.created_at,
                       s.show_datetime,
                       m.title AS movie_name,
                       th.name AS name,
                       sc.screen_number
                FROM tickets t
                JOIN shows s ON t.show_id = s.show_id
                JOIN movies m ON t.movie_id = m.movie_id
                JOIN theatres th ON t.theatre_id = th.theatre_id
                JOIN screens sc ON t.screen_id = sc.screen_id
                WHERE t.user_id = ?
                ORDER BY t.created_at DESC
            """;

            try (
                Connection conn = DBUtils.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
            ) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                JSONArray bookings = new JSONArray();
                while (rs.next()) {
                    JSONObject obj = new JSONObject();
                    obj.put("ticket_id", rs.getInt("ticket_id"));
                    obj.put("total_amount", rs.getDouble("total_amount"));
                    obj.put("created_at", rs.getString("created_at"));
                    obj.put("show_datetime", rs.getString("show_datetime"));
                    obj.put("movie_name", rs.getString("movie_name"));
                    obj.put("theatre_name", rs.getString("name"));
                    obj.put("screen_number", rs.getInt("screen_number"));

                    bookings.put(obj);
                }

                try (PrintWriter out = res.getWriter()) {
                    out.print(bookings.toString());
                    out.flush();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void setCorsHeaders(HttpServletResponse res) {
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
