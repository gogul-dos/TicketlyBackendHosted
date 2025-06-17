package com.ticketly.servlets;

import com.ticketly.utils.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BookTicket extends HttpServlet{
	@Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
        setCorsHeaders(res);
        res.setStatus(HttpServletResponse.SC_OK);
    }
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) {
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
            
		}catch(Exception e) {
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
