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


    @PostMapping("/addProject")
    public String addProject(
            @RequestParam("projectData") String jsonRequest,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("creator") String creatorUsername) {

        Gson gson = new Gson();
        JsonObject projectData = gson.fromJson(jsonRequest, JsonObject.class);

        String projectName = projectData.get("project_name").getAsString();
        String requirement = projectData.get("requirement").getAsString();
        String deadline = projectData.get("deadline").getAsString();
        JsonArray projectAttachments = projectData.getAsJsonArray("ProjectAttachments");
        JsonArray members = projectData.getAsJsonArray("members");

        String projectCode = generateProjectCode("Project");

        String insertProjectSQL = "INSERT INTO Project (project_code, project_name, requirement, deadline) VALUES (?, ?, ?, ?)";
        String insertWorkSQL = "INSERT INTO Work (work_code, project_code, username, role, work, deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertAttachmentSQL = "INSERT INTO Attachment (attachment_code, project_code, file_name) VALUES (?, ?, ?)";
        String insertAttachmentMemberSQL = "INSERT INTO Attachment_Members (attachmentMembers_Code, username, file_name) VALUES (?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement projectStmt = connection.prepareStatement(insertProjectSQL);
             PreparedStatement workStmt = connection.prepareStatement(insertWorkSQL);
             PreparedStatement attachmentStmt = connection.prepareStatement(insertAttachmentSQL);
             PreparedStatement attachmentMemberStmt = connection.prepareStatement(insertAttachmentMemberSQL)) {

            projectStmt.setString(1, projectCode);
            projectStmt.setString(2, projectName);
            projectStmt.setString(3, requirement);
            projectStmt.setString(4, deadline);
            projectStmt.executeUpdate();

            String managerWorkCode = generateProjectCode("Work");
            workStmt.setString(1, managerWorkCode);
            workStmt.setString(2, projectCode);
            workStmt.setString(3, creatorUsername); // Người tạo project
            workStmt.setString(4, "Manager"); // Vai trò là Manager
            workStmt.setString(5, "Quản lý dự án");
            workStmt.setString(6, deadline);
            workStmt.setString(7, "unfinished");
            workStmt.executeUpdate();

            for (JsonElement projectAttachment : projectAttachments) {
                String fileName = projectAttachment.getAsString();
                MultipartFile file = findFileByName(files, fileName);
                if (file != null) {
                    String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
                    saveFileContent("C:/Users/Tan Phong/IdeaProjects/PROJECT_PROJECT/Attachment_Project/", fileName, fileContent);

                    String attachmentCode = generateProjectCode("Attachment");
                    attachmentStmt.setString(1, attachmentCode);
                    attachmentStmt.setString(2, projectCode);
                    attachmentStmt.setString(3, fileName);
                    attachmentStmt.executeUpdate();
                }
            }

            for (JsonElement memberElement : members) {
                JsonObject member = memberElement.getAsJsonObject();
                String username = member.get("name").getAsString();
                String role = member.get("role").getAsString();
                String work = member.get("work").getAsString();
                String memberDeadline = member.get("deadline").getAsString();

                String workCode = generateProjectCode("Work");
                workStmt.setString(1, workCode);
                workStmt.setString(2, projectCode);
                workStmt.setString(3, username);
                workStmt.setString(4, role);
                workStmt.setString(5, work);
                workStmt.setString(6, memberDeadline);
                workStmt.setString(7, "unfinished");
                workStmt.executeUpdate();

                JsonArray memberAttachments = member.getAsJsonArray("MemberAttachments");
                for (JsonElement memberAttachment : memberAttachments) {
                    String fileName = memberAttachment.getAsString();
                    MultipartFile file = findFileByName(files, fileName);
                    if (file != null) {
                        String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
                        saveFileContent("C:/Users/Tan Phong/IdeaProjects/PROJECT_PROJECT/Attachment_Member/", fileName, fileContent);

                        String attachmentMemberCode = generateProjectCode("Attachment_Members");
                        attachmentMemberStmt.setString(1, attachmentMemberCode);
                        attachmentMemberStmt.setString(2, username);
                        attachmentMemberStmt.setString(3, fileName);
                        attachmentMemberStmt.executeUpdate();
                    }
                }
            }

            return "Project added successfully";

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private MultipartFile findFileByName(List<MultipartFile> files, String fileName) {
        for (MultipartFile file : files) {
            if (file.getOriginalFilename().equals(fileName)) {
                return file;
            }
        }
        return null;
    }

    private void saveFileContent(String folder, String fileName, String content) throws IOException {
        File file = new File(folder + fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            fos.write(contentBytes);
        }
    }

    public MultipartFile createMockMultipartFile(String fileName, String content) {
        return new MockMultipartFile(fileName, fileName, "text/plain", content.getBytes(StandardCharsets.UTF_8));
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
