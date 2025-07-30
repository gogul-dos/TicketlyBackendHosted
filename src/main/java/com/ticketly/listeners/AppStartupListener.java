package com.ticketly.listeners;

import com.ticketly.tasks.DailyResetTask;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class AppStartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("AppStartupListener: Server started.");
        DailyResetTask.scheduleDailyReset();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("AppShutdownListener: Server stopped.");
    }
}
