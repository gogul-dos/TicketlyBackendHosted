package com.ticketly.servlets;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ticketly.utils.DBUtils;
import com.ticketly.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HomeServlet extends HttpServlet {

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

            System.out.println("Authenticated user: " + claim.getSubject());

            String query = "SELECT movie_id, title, rating, poster_url, genre, trailer_url FROM movies";

            JSONArray moviesArray = new JSONArray();

            try (
                Connection conn = DBUtils.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)
            ) {
                while (rs.next()) {
                    JSONObject movie = new JSONObject();
                    movie.put("movie_id", rs.getInt("movie_id"));
                    movie.put("title", rs.getString("title"));
                    movie.put("rating", rs.getDouble("rating"));
                    movie.put("poster_url", rs.getString("poster_url"));
                    movie.put("trailer_url", rs.getString("trailer_url"));
                    movie.put("genre", rs.getString("genre"));
                    moviesArray.put(movie);
                }

                try (PrintWriter out = res.getWriter()) {
                    out.print(moviesArray.toString());
                    out.flush();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in HomeServlet");
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
