package com.ticketly.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.ticketly.utils.DBUtils;

public class DailyResetTask {

    public static void scheduleDailyReset() {
        Timer timer = new Timer(true);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 5);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        
        if (calendar.getTime().before(new Date())) {
            calendar.add(Calendar.DATE, 1);
        }

        Date firstRun = calendar.getTime();
        long repeatInterval = 24 * 60 * 60 * 1000;

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try (Connection conn = DBUtils.getConnection()) {
                    System.out.println("Running daily reset...");

                    PreparedStatement seatStmt = conn.prepareStatement(
                        "UPDATE seats SET is_booked = 0, booked_at = NULL WHERE is_booked = 1"
                    );
                    seatStmt.executeUpdate();

                    PreparedStatement showStmt = conn.prepareStatement(
                        "UPDATE shows SET show_datetime = CONCAT(CURDATE(), ' ', TIME(show_datetime))"
                    );
                    showStmt.executeUpdate();

                    System.out.println("Daily reset completed at " + new Date());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, firstRun, repeatInterval);
    }
}
