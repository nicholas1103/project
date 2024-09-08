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

    public boolean addUserProjectWorkAttachmentResultResponse(
            String username, String password, String fullname,
            String projectName, String requirement, String projectDeadline,
            String role, String workDescription, String workDeadline, String workStatus,
            String fileName, String resultDescription, String resultTimestamp,
            String sender, String responseContent, String responseTimestamp
    ) {
        String projectCode = generateProjectCode("Project");
        String workCode = generateProjectCode("Work");
        String attachmentCode = generateProjectCode("Attachment");
        String resultCode = generateProjectCode("Result");
        String responseCode = generateProjectCode("Response");
        boolean isInserted = false;

        String insertUserSQL = "INSERT INTO User (username, password, fullname) VALUES (?, ?, ?)";
        String insertProjectSQL = "INSERT INTO Project (project_code, project_name, requirement, deadline) VALUES (?, ?, ?, ?)";
        String insertWorkSQL = "INSERT INTO Work (work_code, project_code, username, role, work, deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertAttachmentSQL = "INSERT INTO Attachment (attachment_code, project_code, file_name) VALUES (?, ?, ?)";
        String insertResultSQL = "INSERT INTO Result (result_code, work_code, result, time_stamp) VALUES (?, ?, ?, ?)";
        String insertResponseSQL = "INSERT INTO Response (response_code, work_code, sender, content, time_stamp) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement userStmt = connection.prepareStatement(insertUserSQL);
             PreparedStatement projectStmt = connection.prepareStatement(insertProjectSQL);
             PreparedStatement workStmt = connection.prepareStatement(insertWorkSQL);
             PreparedStatement attachmentStmt = connection.prepareStatement(insertAttachmentSQL);
             PreparedStatement resultStmt = connection.prepareStatement(insertResultSQL);
             PreparedStatement responseStmt = connection.prepareStatement(insertResponseSQL)) {

            userStmt.setString(1, username);
            userStmt.setString(2, password);
            userStmt.setString(3, fullname);
            userStmt.executeUpdate();

            projectStmt.setString(1, projectCode);
            projectStmt.setString(2, projectName);
            projectStmt.setString(3, requirement);
            projectStmt.setString(4, projectDeadline);
            projectStmt.executeUpdate();

            workStmt.setString(1, workCode);
            workStmt.setString(2, projectCode);
            workStmt.setString(3, username);
            workStmt.setString(4, role);
            workStmt.setString(5, workDescription);
            workStmt.setString(6, workDeadline);
            workStmt.setString(7, workStatus);
            workStmt.executeUpdate();

            attachmentStmt.setString(1, attachmentCode);
            attachmentStmt.setString(2, projectCode);
            attachmentStmt.setString(3, fileName);
            attachmentStmt.executeUpdate();

            resultStmt.setString(1, resultCode);
            resultStmt.setString(2, workCode);
            resultStmt.setString(3, resultDescription);
            resultStmt.setString(4, resultTimestamp);
            resultStmt.executeUpdate();

            responseStmt.setString(1, responseCode);
            responseStmt.setString(2, workCode);
            responseStmt.setString(3, sender);
            responseStmt.setString(4, responseContent);
            responseStmt.setString(5, responseTimestamp);
            responseStmt.executeUpdate();

            connection.commit();
            isInserted = true;

        } catch (SQLException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }

        return isInserted;
    }


}








