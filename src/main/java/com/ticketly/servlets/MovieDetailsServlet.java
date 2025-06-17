package com.ticketly.servlets;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.ticketly.utils.DBUtils;
import com.ticketly.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class MovieDetailsServlet extends HttpServlet {

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


            String pathInfo = req.getPathInfo(); // returns "/5"
            if (pathInfo == null || pathInfo.length() <= 1) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            int movieId = Integer.parseInt(pathInfo.substring(1));
            System.out.println(movieId);
            String query = "SELECT * FROM movies WHERE movie_id = ?";

            try (
                Connection connection = DBUtils.getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)
            ) {
                stmt.setInt(1, movieId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    JSONObject movie = new JSONObject();
                    movie.put("movie_id", rs.getInt("movie_id"));
                    movie.put("title", rs.getString("title"));
                    movie.put("description", rs.getString("description"));
                    movie.put("duration_min", rs.getInt("duration_min"));
                    movie.put("language", rs.getString("language"));
                    movie.put("genre", rs.getString("genre"));
                    movie.put("rating", rs.getDouble("rating"));
                    movie.put("release_date", rs.getString("release_date"));
                    movie.put("poster_url", rs.getString("poster_url"));
                    movie.put("trailer_url", rs.getString("trailer_url"));
                    movie.put("created_at", rs.getString("created_at"));
                    movie.put("updated_at", rs.getString("updated_at"));

                    PrintWriter out = res.getWriter();
                    out.print(movie.toString());
                    out.flush();
                } else {
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
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
