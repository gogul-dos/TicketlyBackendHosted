package com.ticketly.utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;

public class DBUtils {
    private static final String DB_PATH;

    static {
    	File dbFile = new File("src/main/webapp/ticketly.db");
        DB_PATH = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_PATH);
    }
}
