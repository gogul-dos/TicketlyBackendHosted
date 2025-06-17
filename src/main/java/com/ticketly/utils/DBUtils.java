package com.ticketly.utils;
import java.sql.*;

public class DBUtils {
	private static final String URL = "jdbc:mysql://shinkansen.proxy.rlwy.net:44499/railway";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "yrmVxcyBXiztuNJBvFkLWrvMLTufaXkM";
	
	public static Connection getConnection() throws SQLException, ClassNotFoundException {
	    Class.forName("com.mysql.cj.jdbc.Driver");
	    return DriverManager.getConnection(
	        URL,
	        USERNAME,
	        PASSWORD
	    );
	}

}
