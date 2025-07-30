package com.ticketly.servlets;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ticketly.utils.DBUtils;
import com.ticketly.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BookSeatServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
        setCorsHeaders(res);
        res.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) {
        setCorsHeaders(res);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        try (PrintWriter out = res.getWriter()) {
            String authHeader = req.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.write("{\"error\": \"Missing or invalid token\"}");
                return;
            }

            String token = authHeader.replace("Bearer ", "");
            Claims claims = JwtUtil.verifyToken(token);
            if (claims == null) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.write("{\"error\": \"Invalid token\"}");
                return;
            }

            int userId = (int) claims.get("user_id");

            StringBuilder sb = new StringBuilder();
            BufferedReader reader = req.getReader();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            JSONObject body = new JSONObject(sb.toString());
            JSONArray seatIds = body.getJSONArray("seat_ids");
            int showId = body.getInt("show_id");
            double totalAmount = body.getDouble("total_amount");

            int movieId = -1;
            int theatreId = -1;
            int screenId = -1;

            try (
                Connection conn = DBUtils.getConnection();
                PreparedStatement showInfoStmt = conn.prepareStatement(
                    "SELECT movie_id, theatre_id, screen_id FROM shows WHERE show_id = ?"
                )
            ) {
                showInfoStmt.setInt(1, showId);
                ResultSet rs = showInfoStmt.executeQuery();
                if (rs.next()) {
                    movieId = rs.getInt("movie_id");
                    theatreId = rs.getInt("theatre_id");
                    screenId = rs.getInt("screen_id");
                } else {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.write("{\"error\": \"Invalid show_id\"}");
                    return;
                }
            }

            try (
                Connection conn = DBUtils.getConnection()
            ) {
                conn.setAutoCommit(false);

                PreparedStatement ticketStmt = conn.prepareStatement(
                    "INSERT INTO tickets (user_id, show_id, movie_id, theatre_id, screen_id, total_amount) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                ticketStmt.setInt(1, userId);
                ticketStmt.setInt(2, showId);
                ticketStmt.setInt(3, movieId);
                ticketStmt.setInt(4, theatreId);
                ticketStmt.setInt(5, screenId);
                ticketStmt.setDouble(6, totalAmount);
                ticketStmt.executeUpdate();

                ResultSet ticketRs = ticketStmt.getGeneratedKeys();
                int ticketId = -1;
                if (ticketRs.next()) {
                    ticketId = ticketRs.getInt(1);
                }

                
                PreparedStatement seatStmt = conn.prepareStatement(
                    "UPDATE seats SET is_booked = 1, booked_at = datetime('now') WHERE seat_id = ? AND is_booked = 0"
                );

                for (int i = 0; i < seatIds.length(); i++) {
                    seatStmt.setInt(1, seatIds.getInt(i));
                    seatStmt.addBatch();
                }

                seatStmt.executeBatch();

                conn.commit();

                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("ticket_id", ticketId);
                response.put("seats_booked", seatIds.length());

                out.write(response.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void setCorsHeaders(HttpServletResponse res) {
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
