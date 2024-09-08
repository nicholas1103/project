package org.example;

import com.google.gson.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

@RestController
public class Controller {
    private static final String jdbcURL = Connect_SQL.jdbcURL;
    private static final String USERNAME = Connect_SQL.USERNAME;
    private static final String PASSWORD = Connect_SQL.PASSWORD;
    Log_Exception logException = new Log_Exception();
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
    @GetMapping("/login")
    public String getUserDetails(@RequestParam("userName") String username, @RequestParam("passWord") String password) {
        String httpStatusCode = "200";
        JsonObject jsonResponse = new JsonObject();
        Gson gson = new Gson();

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD)) {
            String sql = "SELECT username, fullname FROM User WHERE username = ? AND password = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password);

                ResultSet rs = stmt.executeQuery();
                boolean dataFound = false;
                if (rs.next()) {
                    dataFound = true;
                    jsonResponse.addProperty("username", rs.getString("username"));
                    jsonResponse.addProperty("fullname", rs.getString("fullname"));
                }

                if (!dataFound) {
                    httpStatusCode = "404";
                    jsonResponse.addProperty("error", "Invalid username or password");
                    return gson.toJson(jsonResponse) + " HTTP Status Code: " + httpStatusCode;
                }
            }
        } catch (Exception e) {
            jsonResponse.addProperty("error", "An error occurred: " + e.getMessage());
        }

        return gson.toJson(jsonResponse) + " HTTP Status Code: " + httpStatusCode;
    }

    @GetMapping("/getInformationProject")
    public ResponseEntity<String> getProjectDetailsByUsername(@RequestParam("userName") String username) {
        JsonArray projectsArray = new JsonArray();
        Gson gson = new Gson();
        String httpStatusCode = "200";

        String sql = "SELECT Project.project_code, Project.project_name, Work.role, Work.status, Project.deadline, Work.deadline " +
                "FROM Project, Work " +
                "WHERE Project.project_code = Work.project_code AND Work.username = ?";

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            boolean dataFound = false;
            while (rs.next()) {
                dataFound = true;
                JsonObject project = new JsonObject();
                project.addProperty("project_code", rs.getString("project_code"));
                project.addProperty("project_name", rs.getString("project_name"));
                project.addProperty("role", rs.getString("role"));
                project.addProperty("status", rs.getString("status"));
                project.addProperty("project_deadline", rs.getString("deadline"));
                project.addProperty("personal_deadline", rs.getString("deadline"));

                projectsArray.add(project);
            }

            if (!dataFound)
                return ResponseEntity.status(404).body("No projects found for the given username");


        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
        }
        JsonObject responseJson = new JsonObject();
        responseJson.add("projects", projectsArray);
        return ResponseEntity.ok(responseJson.toString());
    }
    private void saveFileContent(String fileName, String content) throws IOException {
        Path filePath = Paths.get("/path/to/save/files/" + fileName);
        Files.write(filePath, content.getBytes());
    }

    @PostMapping("/addProject")
    public String addProject(@RequestBody String jsonRequest) {
        String httpStatusCode = "400";
        JsonObject jsonResponse = new JsonObject();

        Gson gson = new Gson();

        JsonObject projectData = gson.fromJson(jsonRequest, JsonObject.class);
        String managerUsername = projectData.get("manager_username").getAsString();
        String projectName = projectData.get("project_name").getAsString();
        String requirement = projectData.get("requirement").getAsString();
        String deadline = projectData.get("deadline").getAsString();
        JsonArray members = projectData.getAsJsonArray("members");
        JsonObject attachments = projectData.getAsJsonObject("attachments");

        String projectCode = generateProjectCode("Project");

        String insertProjectSQL = "INSERT INTO Project (project_code, project_name, requirement, deadline) VALUES (?, ?, ?, ?)";
        String insertWorkSQL = "INSERT INTO Work (work_code, project_code, username, role, work, deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertAttachmentSQL = "INSERT INTO Attachment (attachment_code, project_code, file_name) VALUES (?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement projectStmt = connection.prepareStatement(insertProjectSQL);
             PreparedStatement workStmt = connection.prepareStatement(insertWorkSQL);
             PreparedStatement attachmentStmt = connection.prepareStatement(insertAttachmentSQL)) {
            
            projectStmt.setString(1, projectCode);
            projectStmt.setString(2, projectName);
            projectStmt.setString(3, requirement);
            projectStmt.setString(4, deadline);
            int rowsAffected = projectStmt.executeUpdate();

            if (rowsAffected > 0) {
                workStmt.setString(1, generateProjectCode("Work"));
                workStmt.setString(2, projectCode);
                workStmt.setString(3, managerUsername);
                workStmt.setString(4, "Manager");
                workStmt.setString(5, "Manage Project");
                workStmt.setString(6, deadline);
                workStmt.setString(7, "unfinished");
                workStmt.executeUpdate();

                for (JsonElement memberElement : members) {
                    JsonObject member = memberElement.getAsJsonObject();
                    String memberName = member.get("name").getAsString();
                    String memberRole = member.get("role").getAsString();
                    String memberWork = member.get("work").getAsString();
                    String memberDeadline = member.get("deadline").getAsString();

                    workStmt.setString(1, generateProjectCode("Work"));
                    workStmt.setString(2, projectCode);
                    workStmt.setString(3, memberName);
                    workStmt.setString(4, memberRole);
                    workStmt.setString(5, memberWork);
                    workStmt.setString(6, memberDeadline);
                    workStmt.setString(7, "unfinished");
                    workStmt.executeUpdate();
                }

                for (String fileName : attachments.keySet()) {
                    String fileContent = attachments.get(fileName).getAsString();
                    String attachmentCode = generateProjectCode("Attachment");

                    saveFileContent(fileName, fileContent);

                    attachmentStmt.setString(1, attachmentCode);
                    attachmentStmt.setString(2, projectCode);
                    attachmentStmt.setString(3, fileName);
                    attachmentStmt.executeUpdate();
                }

                httpStatusCode = "200";
            }

        } catch (SQLException e) {
            jsonResponse.addProperty("status", "error");
            jsonResponse.addProperty("error", "An error occurred: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "HTTP Status Code: " + httpStatusCode;
    }

    @GetMapping("/checkManager")
    public boolean checkManager(@RequestParam("projectCode") String projectCode, @RequestParam("userName") String username) {
        boolean isManager = false;

        String checkManagerSQL = "SELECT role FROM Work WHERE project_code = ? AND username = ? AND role = 'Manager'";

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(checkManagerSQL)) {

            stmt.setString(1, projectCode);
            stmt.setString(2, username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                isManager = true;
            }

        } catch (SQLException e) {
            System.out.println("An error occurred while checking manager: " + e.getMessage());
        }

        return isManager;
    }

    public boolean updateProjectName(String projectCode, String newProjectName) {
        boolean isUpdated = false;

        String updateProjectSQL = "UPDATE Project SET project_name = ? WHERE project_code = ?";

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(updateProjectSQL)) {

            stmt.setString(1, newProjectName);
            stmt.setString(2, projectCode);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                isUpdated = true;
            }

        } catch (SQLException e) {
            System.out.println("An error occurred while updating project name: " + e.getMessage());
        }

        return isUpdated;
    }


}