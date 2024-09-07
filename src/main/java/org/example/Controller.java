package org.example;

import com.google.gson.*;
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
    public String getUserDetails(@RequestParam("username") String username, @RequestParam("password") String password) {
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

    @GetMapping("getInformationProject")
    public String getProjectDetailsByUsername(String username) {
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

            if (!dataFound) {
                httpStatusCode = "404";
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "No projects found for the given username");
                return gson.toJson(errorResponse) + " HTTP Status Code: " + httpStatusCode;
            }

        } catch (Exception e) {
            httpStatusCode = "500";
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("error", "An error occurred: " + e.getMessage());
            return gson.toJson(errorResponse) + " HTTP Status Code: " + httpStatusCode;
        }

        JsonObject responseJson = new JsonObject();
        responseJson.add("projects", projectsArray);
        return gson.toJson(responseJson) + " HTTP Status Code: " + httpStatusCode;
    }
    private void saveFileContent(String fileName, String content) throws IOException {
        Path filePath = Paths.get("/path/to/save/files/" + fileName);
        Files.write(filePath, content.getBytes());
    }

    @PostMapping("addProject")
    public String addProject(@RequestBody String jsonRequest) {
        String httpStatusCode = "400";
        JsonObject jsonResponse = new JsonObject();
        Gson gson = new Gson();

        try {
            JsonObject projectData = gson.fromJson(jsonRequest, JsonObject.class);
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
                projectStmt.executeUpdate();

                for (String fileName : attachments.keySet()) {
                    String fileContent = attachments.get(fileName).getAsString();
                    String attachmentCode = generateProjectCode("Attachment");

                    attachmentStmt.setString(1, attachmentCode);
                    attachmentStmt.setString(2, projectCode);
                    attachmentStmt.setString(3, fileName);
                    attachmentStmt.executeUpdate();

                    saveFileContent(fileName, fileContent);
                }

                for (JsonElement memberElement : members) {
                    JsonObject member = memberElement.getAsJsonObject();
                    String memberName = member.get("name").getAsString();
                    String role = member.get("role").getAsString();
                    String work = member.get("work").getAsString();
                    String memberDeadline = member.get("deadline").getAsString();
                    JsonObject memberAttachments = member.getAsJsonObject("attachments");

                    String workCode = generateProjectCode("Work");

                    workStmt.setString(1, workCode);
                    workStmt.setString(2, projectCode);
                    workStmt.setString(3, memberName);
                    workStmt.setString(4, role);
                    workStmt.setString(5, work);
                    workStmt.setString(6, memberDeadline);
                    workStmt.setString(7, "unfinished");
                    workStmt.executeUpdate();

                    for (String fileName : memberAttachments.keySet()) {
                        String fileContent = memberAttachments.get(fileName).getAsString();
                        String attachmentCode = generateProjectCode("Attachment");

                        attachmentStmt.setString(1, attachmentCode);
                        attachmentStmt.setString(2, projectCode);
                        attachmentStmt.setString(3, fileName);
                        attachmentStmt.executeUpdate();

                        saveFileContent(fileName, fileContent);
                    }
                }

                httpStatusCode = "200";

            } catch (SQLException e) {
                jsonResponse.addProperty("status", "error");
                jsonResponse.addProperty("error", "An error occurred: " + e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (JsonSyntaxException e) {
            jsonResponse.addProperty("status", "error");
            jsonResponse.addProperty("error", "Invalid JSON format: " + e.getMessage());
        }

        return " HTTP Status Code: " + httpStatusCode;
    }
}











//    public String getProjectDetailsByUsername(@RequestParam("username") String username) {
//        JsonArray projectsArray = new JsonArray();
//        Gson gson = new Gson();
//        String httpStatusCode = "200"; // Default to OK (200)
//        String sql = "SELECT p.project_code, p.project_name, w.role, w.status, p.deadline AS project_deadline, w.deadline AS personal_deadline " +
//                "FROM Project p " +
//                "JOIN Work w ON p.project_code = w.project_code " +
//                "WHERE w.username = ?";
//
//        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
//             PreparedStatement stmt = connection.prepareStatement(sql)) {
//
//            stmt.setString(1, username);
//            ResultSet rs = stmt.executeQuery();
//
//            boolean dataFound = false;
//            while (rs.next()) {
//                dataFound = true;
//                JsonObject project = new JsonObject();
//                project.addProperty("project_code", rs.getString("project_code"));
//                project.addProperty("project_name", rs.getString("project_name"));
//                project.addProperty("role", rs.getString("role"));
//                project.addProperty("status", rs.getString("status"));
//                project.addProperty("project_deadline", rs.getString("project_deadline"));
//                project.addProperty("personal_deadline", rs.getString("personal_deadline"));
//
//                projectsArray.add(project);
//            }
//
//            if (!dataFound) {
//                httpStatusCode = "404";
//                JsonObject errorResponse = new JsonObject();
//                errorResponse.addProperty("error", "No projects found for the given username");
//                return gson.toJson(errorResponse) + " HTTP Status Code: " + httpStatusCode;
//            }
//
//        } catch (Exception e) {
//            httpStatusCode = "500"; // Set HTTP 500 status code
//            JsonObject errorResponse = new JsonObject();
//            errorResponse.addProperty("error", "An error occurred: " + e.getMessage());
//            return gson.toJson(errorResponse) + " HTTP Status Code: " + httpStatusCode;
//        }
//
//        JsonObject responseJson = new JsonObject();
//        responseJson.add("projects", projectsArray);
//        return gson.toJson(responseJson) + " HTTP Status Code: " + httpStatusCode;
//    }




//    public String addProject(@RequestParam("projectName") String projectName, @RequestParam("requirement") String requirement, @RequestParam("projectDeadline") String deadline, @RequestParam("username") String username) {
//        String httpStatusCode = "400";
//        JsonObject jsonResponse = new JsonObject();
//
//        String projectCode = generateProjectCode("Project");
//
//        String insertProjectSQL = "INSERT INTO Project (project_code, project_name, requirement, deadline) VALUES (?, ?, ?, ?)";
//        String insertWorkSQL = "INSERT INTO Work (work_code, project_code, username, role, work, deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
//
//        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
//             PreparedStatement projectStmt = connection.prepareStatement(insertProjectSQL);
//             PreparedStatement workStmt = connection.prepareStatement(insertWorkSQL)) {
//
//            projectStmt.setString(1, projectCode);
//            projectStmt.setString(2, projectName);
//            projectStmt.setString(3, requirement);
//            projectStmt.setString(4, deadline);
//
//            int rowsAffected = projectStmt.executeUpdate();
//            if (rowsAffected > 0) {
//                workStmt.setString(1, generateProjectCode("Work"));
//                workStmt.setString(2, projectCode);
//                workStmt.setString(3, username);
//                workStmt.setString(4, "Manager");
//                workStmt.setString(5, "Manage Project");
//                workStmt.setString(6, deadline);
//                workStmt.setString(7, "unfinished");
//
//                workStmt.executeUpdate();
//
//                httpStatusCode = "200";
//            }
//        } catch (SQLException e) {
//            jsonResponse.addProperty("status", "error");
//            jsonResponse.addProperty("error", "An error occurred: " + e.getMessage());
//        }
//
//        return " HTTP Status Code: " + httpStatusCode;
//    }
