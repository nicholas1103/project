package org.example;

import java.sql.*;
import java.util.UUID;

public class Insert_Data_SQL {
    private static final String jdbcURL = Connect_SQL.jdbcURL;
    private static final String USERNAME = Connect_SQL.USERNAME;
    private static final String PASSWORD = Connect_SQL.PASSWORD;
    private static Log_Exception logException = new Log_Exception();


    public String generateProjectCode(String prefix) {
        int nextId = getNextProjectId(prefix);
        return prefix + "-" + String.format("%04d", nextId);
    }

    public int getNextProjectId(String prefix) {
        int nextId = 1;
        String sql = "SELECT COUNT(*) FROM " + prefix;

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                nextId = rs.getInt(1) + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return nextId;
    }


    private static String generateCode(String start) {
        return start + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    public void insertData() {
        try {
            insertUser("user1", "password1", "Nguyen Tan Phong");
            insertUser("user2", "password2", "Nguyen Hoang Phuc");
            insertUser("user3", "password3", "Vo Hoang Nam");
            insertUser("user4", "password4", "Nguyen Tan Kiet");
            insertUser("user5", "password5", "Bach Minh Phuc");

            insertProject("Coffee Project", "Requirement 1", "2024-12-31");
            insertProject("Compiler Project", "Requirement 2", "2025-03-31");
            insertProject("Buffer Project", "Requirement 3", "2025-12-04");

            insertWork("proj2", "user1", "Developer", "Write API", "2024-10-15", "unfinished");
            insertWork("proj2", "user2", "Developer", "Query Data", "2024-11-20", "unfinished");
            insertWork("proj2", "user3", "Project Manager", "Control Project", "2025-03-31", "unfinished");
            insertWork("proj2", "user4", "Developer", "Design UI", "2024-09-15", "finished");
            insertWork("proj2", "user5", "Developer", "Security", "2025-10-15", "unfinished");

            insertResult("work1", "Feature X developed", "2024-10-16 16:10:00");
            insertResult("work2", "Testing not started", "2024-11-01 09:20:00");
            insertResult("work3", "Feature X developed", "2024-10-11 11:30:00");
            insertResult("work4", "Testing not started", "2024-11-02 22:40:00");
            insertResult("work5", "Feature X developed", "2024-10-03 05:45:00");

            insertResponse("work1", "user1", "Work completed", "2024-10-16 11:00:00");
            insertResponse("work2", "user2", "Please update status", "2024-11-01 09:30:00");
            insertResponse("work3", "user3", "Work completed", "2024-10-16 11:00:00");
            insertResponse("work4", "user4", "Please update status", "2024-11-01 09:30:00");
            insertResponse("work5", "user5", "Work completed", "2024-10-16 11:00:00");
        } catch (Exception e){
//            logException.logException(e);
        }
    }
    private static void insertUser(String username, String password, String fullname){
        try {
            System.out.println("- Inserting User: ");
            Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
            String sql = "INSERT INTO User (username, password, fullname) VALUES (?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, fullname);
            stmt.executeUpdate();
            connection.close();
        } catch (Exception e){
//            logException.logException(e);
        }
    }

    private void insertProject(String projectName, String requirement, String deadline) {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            String projectCode = generateCode("PROJ-"); // Tạo mã dự án mới

            System.out.println("- Inserting Project: ");
            connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);

            String sql = "INSERT INTO Project (project_code, project_name, requirement, deadline) VALUES (?, ?, ?, ?)";
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, projectCode);
            stmt.setString(2, projectName);
            stmt.setString(3, requirement);
            stmt.setString(4, deadline);
            stmt.executeUpdate();

            System.out.println("Project inserted with code: " + projectCode);
        } catch (SQLException e) {
            e.printStackTrace(); // Thay vì logException, in lỗi ra console
        } finally {
            // Đóng tài nguyên
            try {
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void insertWork(String projectCode, String username, String role, String work, String deadline, String status) {
        try {
            System.out.println("- Inserting Work: ");
            Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
            String sql = "INSERT INTO Work (work_code, project_code, username, role, work, deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, generateCode("WORK-"));
            stmt.setString(2, projectCode);
            stmt.setString(3, username);
            stmt.setString(4, role);
            stmt.setString(5, work);
            stmt.setString(6, deadline);
            stmt.setString(7, status);
            stmt.executeUpdate();
            connection.close();
        } catch (Exception e){
//            logException.logException(e);
        }
    }

    private static void insertResult(String workCode, String result, String timeStamp) {

        try {
            System.out.println("- Inserting Result: ");
            Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
            String sql = "INSERT INTO Result (result_code, work_code, result, time_stamp) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, generateCode("RESULT-"));
            stmt.setString(2, workCode);
            stmt.setString(3, result);
            stmt.setString(4, timeStamp);
            stmt.executeUpdate();
            connection.close();
        } catch (Exception e){
//            logException.logException(e);
        }
    }

    private static void insertResponse(String workCode, String sender, String content, String timeStamp) {
        try {
            System.out.println("- Inserting Response: ");
            Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
            String sql = "INSERT INTO Response (response_code, work_code, sender, content, time_stamp) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, generateCode("RESPOND-"));
            stmt.setString(2, workCode);
            stmt.setString(3, sender);
            stmt.setString(4, content);
            stmt.setString(5, timeStamp);
            stmt.executeUpdate();
            connection.close();
        } catch (Exception e){
//            logException.logException(e);
        }
    }
}
