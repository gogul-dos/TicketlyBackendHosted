package com.ticketly.servlets;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ticketly.utils.DBUtils;
import com.ticketly.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SeatLayoutServlet extends HttpServlet {

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
            String authHeader = req.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String token = authHeader.replace("Bearer ", "");
            Claims claim = JwtUtil.verifyToken(token);
            if (claim == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String showIdParam = req.getParameter("show_id");
            if (showIdParam == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write("{\"error\":\"Missing show_id parameter\"}");
                return;
            }

            int showId = Integer.parseInt(showIdParam);

            String query = "SELECT seat_id, seat_number, row_label, column_number, is_booked FROM seats WHERE show_id = ? ORDER BY row_label, column_number";

            try (
                Connection connection = DBUtils.getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)
            ) {
                stmt.setInt(1, showId);
                ResultSet rs = stmt.executeQuery();
                System.out.println("Fetching seats for show_id: " + showId);

                JSONArray seats = new JSONArray();
                while (rs.next()) {
                    JSONObject seat = new JSONObject();
                    seat.put("seat_id", rs.getInt("seat_id"));
                    seat.put("seat_number", rs.getString("seat_number"));
                    seat.put("row_label", rs.getString("row_label"));
                    seat.put("column_number", rs.getInt("column_number"));
                    seat.put("is_booked", rs.getInt("is_booked")); // works fine in SQLite (0/1)
                    seats.put(seat);
                }

                PrintWriter out = res.getWriter();
                out.print(seats.toString());
                out.flush();
            }

        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
    }

    private void setCorsHeaders(HttpServletResponse res) {
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
