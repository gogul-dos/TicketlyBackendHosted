package com.ticketly.servlets;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ticketly.utils.DBUtils;
import com.ticketly.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ShowDetailsServlet extends HttpServlet {

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

            String theatreIdParam = req.getParameter("theatre_id");
            String movieIdParam = req.getParameter("movie_id");

            if (theatreIdParam == null || movieIdParam == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write("{\"error\":\"Missing theatre_id or movie_id parameter\"}");
                return;
            }

            int theatreId = Integer.parseInt(theatreIdParam);
            int movieId = Integer.parseInt(movieIdParam);

            String query = """
                SELECT 
                    s.show_id, s.movie_id, s.theatre_id, s.screen_id, s.show_datetime, 
                    s.ticket_price, sc.screen_number, sc.screen_type
                FROM shows s
                JOIN screens sc ON s.screen_id = sc.screen_id
                WHERE s.theatre_id = ? AND s.movie_id = ?
                ORDER BY sc.screen_number, s.show_datetime
            """;

            try (
                Connection connection = DBUtils.getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)
            ) {
                stmt.setInt(1, theatreId);
                stmt.setInt(2, movieId);
                ResultSet rs = stmt.executeQuery();

                Map<Integer, JSONObject> screenMap = new LinkedHashMap<>();

                while (rs.next()) {
                    int screenId = rs.getInt("screen_id");
                    int showId = rs.getInt("show_id");

                    // Count available seats (SQLite syntax works identically here)
                    int availableSeats = 0;
                    try (PreparedStatement seatStmt = connection.prepareStatement(
                            "SELECT COUNT(*) FROM seats WHERE show_id = ? AND is_booked = 0"
                    )) {
                        seatStmt.setInt(1, showId);
                        ResultSet seatRs = seatStmt.executeQuery();
                        if (seatRs.next()) {
                            availableSeats = seatRs.getInt(1);
                        }
                    }

                    // Create screen object if not already present
                    if (!screenMap.containsKey(screenId)) {
                        JSONObject screenObj = new JSONObject();
                        screenObj.put("screen_id", screenId);
                        screenObj.put("screen_number", rs.getInt("screen_number"));
                        screenObj.put("screen_type", rs.getString("screen_type"));
                        screenObj.put("shows", new JSONArray());
                        screenMap.put(screenId, screenObj);
                    }

                    JSONObject show = new JSONObject();
                    show.put("show_id", showId);
                    show.put("movie_id", rs.getInt("movie_id"));
                    show.put("show_datetime", rs.getString("show_datetime"));
                    show.put("ticket_price", rs.getDouble("ticket_price"));
                    show.put("available_seats", availableSeats);

                    screenMap.get(screenId).getJSONArray("shows").put(show);
                }

                JSONArray screensArray = new JSONArray(screenMap.values());

                PrintWriter out = res.getWriter();
                out.print(screensArray.toString());
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
