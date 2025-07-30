package com.ticketly.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import com.ticketly.utils.DBUtils;
import com.ticketly.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class TheatreDetailsServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");
        res.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");
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

            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write("{\"error\": \"Missing movie ID\"}");
                return;
            }

            int movieId;
            try {
                movieId = Integer.parseInt(pathInfo.substring(1));
            } catch (NumberFormatException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write("{\"error\": \"Invalid movie ID format\"}");
                return;
            }

            String city = req.getParameter("city");
            if (city == null || city.trim().isEmpty()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write("{\"error\": \"Missing city parameter\"}");
                return;
            }

            getMovieTheatresByCity(movieId, city, res);

        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("{\"error\": \"Internal server error\"}");
        }
    }

    private void getMovieTheatresByCity(int movieId, String city, HttpServletResponse res) {
        // SQLite uses 0 (false) instead of FALSE keyword
        String cityShowsSql = """
            SELECT t.theatre_id, t.name AS theatre_name, t.city, t.address,
                   s.show_id, s.show_datetime,
                   COUNT(seat.seat_id) AS total_seats,
                   SUM(CASE WHEN seat.is_booked = 0 THEN 1 ELSE 0 END) AS available_seats
            FROM Theatres t
            JOIN Shows s ON t.theatre_id = s.theatre_id
            JOIN Seats seat ON seat.show_id = s.show_id
            WHERE s.movie_id = ? AND LOWER(TRIM(t.city)) = LOWER(TRIM(?))
            GROUP BY t.theatre_id, t.name, t.city, t.address, s.show_id, s.show_datetime
            ORDER BY t.name, s.show_datetime
        """;

        String totalTheatresSql = """
            SELECT COUNT(DISTINCT t.theatre_id) AS total
            FROM Theatres t
            JOIN Shows s ON t.theatre_id = s.theatre_id
            WHERE s.movie_id = ?
        """;

        try (
            Connection connection = DBUtils.getConnection();
            PreparedStatement cityStmt = connection.prepareStatement(cityShowsSql);
            PreparedStatement totalStmt = connection.prepareStatement(totalTheatresSql)
        ) {
            cityStmt.setInt(1, movieId);
            cityStmt.setString(2, city);
            ResultSet rs = cityStmt.executeQuery();

            Map<Integer, JSONObject> theatreMap = new LinkedHashMap<>();
            int cityCount = 0;

            while (rs.next()) {
                int theatreId = rs.getInt("theatre_id");
                JSONObject theatre = theatreMap.get(theatreId);
                if (theatre == null) {
                    theatre = new JSONObject();
                    theatre.put("theatreId", theatreId);
                    theatre.put("name", rs.getString("theatre_name"));
                    theatre.put("city", rs.getString("city"));
                    theatre.put("address", rs.getString("address"));
                    theatre.put("shows", new JSONArray());
                    theatreMap.put(theatreId, theatre);
                    cityCount++;
                }

                JSONObject show = new JSONObject();
                show.put("showId", rs.getInt("show_id"));
                show.put("time", rs.getString("show_datetime"));
                int available = rs.getInt("available_seats");
                int total = rs.getInt("total_seats");
                int percent = total > 0 ? (available * 100 / total) : 0;
                show.put("availablePercentage", percent);

                theatre.getJSONArray("shows").put(show);
            }

            totalStmt.setInt(1, movieId);
            ResultSet totalRs = totalStmt.executeQuery();
            int totalTheatres = 0;
            if (totalRs.next()) {
                totalTheatres = totalRs.getInt("total");
            }

            JSONObject responseJson = new JSONObject();
            responseJson.put("totalTheatres", totalTheatres);
            responseJson.put("cityTheatres", cityCount);
            responseJson.put("theatres", new JSONArray(theatreMap.values()));

            res.getWriter().write(responseJson.toString());

        } catch (Exception e) {
            e.printStackTrace();
            try {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                res.getWriter().write("{\"error\": \"Failed to fetch theatres\"}");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
