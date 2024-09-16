package org.example;

import com.google.gson.*;
import jakarta.annotation.Resource;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


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
    public ResponseEntity<String> getUserDetails(@RequestParam("userName") String username,
                                                 @RequestParam("passWord") String password) {
        JsonObject jsonResponse = new JsonObject();

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
                    return ResponseEntity.status(404).body("Invalid username or password");
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
        }

        return ResponseEntity.ok(jsonResponse.toString());
    }

    @GetMapping("/getInformationProject")

    public ResponseEntity<String> getProjectDetailsByUsername(@RequestParam("userName") String username) {
        JsonArray projectsArray = new JsonArray();

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
            @RequestPart("jsonRequest") String jsonRequest,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam("creator") String creatorUsername) {
        System.out.println(jsonRequest);
        System.out.println(creatorUsername);
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
        String insertAttachmentMemberSQL = "INSERT INTO Attachment_Members (attachmentMembers_Code, project_code, username, file_path) VALUES (?, ?, ?, ?)";

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
            workStmt.setString(3, creatorUsername);
            workStmt.setString(4, "Manager");
            workStmt.setString(5, "Quản lý dự án");
            workStmt.setString(6, deadline);
            workStmt.setString(7, "unfinished");
            workStmt.executeUpdate();

            File projectBaseFolder = new File(File_Path.file_path + projectCode);
            File projectFolder = new File(projectBaseFolder, "Attachment_Project");
            File memberBaseFolder = new File(projectBaseFolder, "Attachment_Member");

            if (!projectFolder.exists()) {
                projectFolder.mkdirs();
            }

            if (!memberBaseFolder.exists()) {
                memberBaseFolder.mkdirs();
            }

            for (JsonElement projectAttachment : projectAttachments) {
                String fileName = projectAttachment.getAsString();
                MultipartFile file = findFileByName(files, fileName);
                if (file != null) {
                    String fullFilePath = projectFolder.getPath() + "/" + fileName;
                    saveFileContent(fullFilePath,file);
                    String attachmentCode = generateProjectCode("Attachment");
                    attachmentStmt.setString(1, attachmentCode);
                    attachmentStmt.setString(2, projectCode);
                    attachmentStmt.setString(3, fullFilePath);  // Store full file path
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

                File memberFolder = new File(memberBaseFolder, username);
                if (!memberFolder.exists()) {
                    memberFolder.mkdirs();
                }

                JsonArray memberAttachments = member.getAsJsonArray("MemberAttachments");
                for (JsonElement memberAttachment : memberAttachments) {
                    String fileName = memberAttachment.getAsString();
                    MultipartFile file = findFileByName(files, fileName);
                    if (file != null) {
                        String fullFilePath = memberFolder.getPath() + "/" + fileName;
                        saveFileContent(fullFilePath,file);

                        String attachmentMemberCode = generateProjectCode("Attachment_Members");
                        attachmentMemberStmt.setString(1, attachmentMemberCode);
                        attachmentMemberStmt.setString(2, projectCode);
                        attachmentMemberStmt.setString(3, username);
                        attachmentMemberStmt.setString(4, fullFilePath);  // Store full file path
                        attachmentMemberStmt.executeUpdate();
                    }
                }
            }
            System.out.println("successfully");
            return "Project added successfully";

        } catch (SQLException | IOException e) {
            return e.getMessage();
        }
    }



    private void saveFileContent(String fullFilePath, MultipartFile file) throws IOException {
        // Ghi nội dung tệp vào đường dẫn chỉ định
        try (FileOutputStream fos = new FileOutputStream(fullFilePath)) {
            fos.write(file.getBytes());
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


    public MultipartFile createMockMultipartFile(String fileName, String content) {
        return new MockMultipartFile(fileName, fileName, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/updateProjectName")
    public boolean updateProjectName(@RequestParam("projectCode") String projectCode,
                                     @RequestParam("newProjectName") String newProjectName) {
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

    @GetMapping("/getProjectDetails")
    public String getProjectDetails(@RequestParam("projectCode") String projectCode,
                                    @RequestParam("userName") String username) {
        JsonObject result = new JsonObject();

        String projectName = "";
        String requirement = "";
        String deadline = "";
        boolean isManager = false;
        JsonArray attachmentsArray = new JsonArray();
        JsonArray membersArray = new JsonArray();

        try (Connection connection = DriverManager.getConnection(Connect_SQL.jdbcURL, Connect_SQL.USERNAME, Connect_SQL.PASSWORD)) {

            String checkManagerSQL = "SELECT role FROM Work WHERE project_code = ? AND username = ?";
            try (PreparedStatement checkManagerStmt = connection.prepareStatement(checkManagerSQL)) {
                checkManagerStmt.setString(1, projectCode);
                checkManagerStmt.setString(2, username);
                ResultSet rs = checkManagerStmt.executeQuery();
                if (rs.next() && "Manager".equals(rs.getString("role"))) {
                    isManager = true;
                }
            }

            String projectSQL = "SELECT project_name, requirement, deadline FROM Project WHERE project_code = ?";
            try (PreparedStatement projectStmt = connection.prepareStatement(projectSQL)) {
                projectStmt.setString(1, projectCode);
                ResultSet rs = projectStmt.executeQuery();
                if (rs.next()) {
                    projectName = rs.getString("project_name");
                    requirement = rs.getString("requirement");
                    deadline = rs.getString("deadline");
                }
            }

            String attachmentSQL = "SELECT file_name FROM Attachment WHERE project_code = ?";
            try (PreparedStatement attachmentStmt = connection.prepareStatement(attachmentSQL)) {
                attachmentStmt.setString(1, projectCode);
                ResultSet rs = attachmentStmt.executeQuery();
                while (rs.next()) {
                    String filePath = rs.getString("file_name");
                    String fileName = new File(filePath).getName();
                    attachmentsArray.add(fileName);
                }
            }

            String memberSQL = "SELECT username, role, work, status, deadline FROM Work WHERE project_code = ?";
            try (PreparedStatement memberStmt = connection.prepareStatement(memberSQL)) {
                memberStmt.setString(1, projectCode);
                ResultSet rs = memberStmt.executeQuery();
                while (rs.next()) {
                    JsonObject memberObj = new JsonObject();
                    String memberUsername = rs.getString("username");
                    memberObj.addProperty("name", memberUsername);
                    memberObj.addProperty("role", rs.getString("role"));
                    memberObj.addProperty("work", rs.getString("work"));
                    memberObj.addProperty("work_status", rs.getString("status"));
                    memberObj.addProperty("deadline", rs.getString("deadline"));

                    JsonArray memberAttachmentsArray = new JsonArray();
                    String memberAttachmentSQL = "SELECT file_path FROM Attachment_Members WHERE project_code = ? AND username = ?";
                    try (PreparedStatement memberAttachmentStmt = connection.prepareStatement(memberAttachmentSQL)) {
                        memberAttachmentStmt.setString(1, projectCode);
                        memberAttachmentStmt.setString(2, memberUsername);
                        ResultSet memberRs = memberAttachmentStmt.executeQuery();
                        while (memberRs.next()) {
                            String filePath = memberRs.getString("file_path");
                            String fileName = new File(filePath).getName();
                            memberAttachmentsArray.add(fileName);
                        }
                    }
                    memberObj.add("work_attachments", memberAttachmentsArray);

                    JsonArray workSubmitArray = new JsonArray();
                    memberObj.add("work_submit", workSubmitArray);

                    membersArray.add(memberObj);
                }
            }

            result.addProperty("manager_status", isManager ? "true" : "false");
            result.addProperty("project_name", projectName);
            result.addProperty("requirement", requirement);
            result.add("attachments", attachmentsArray);
            result.addProperty("deadline", deadline);
            result.add("members", membersArray);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage()).toString();
        }

        return result.toString();
    }


    public List<MultipartFile> getAttachments(String projectCode) {
        JsonObject result = new JsonObject();
        JsonObject projectAttachments = new JsonObject();
        JsonArray membersArray = new JsonArray();

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD)) {
            String projectSQL = "SELECT file_name FROM Attachment WHERE project_code = ?";
            try (PreparedStatement projectStmt = connection.prepareStatement(projectSQL)) {
                projectStmt.setString(1, projectCode);
                ResultSet projectRs = projectStmt.executeQuery();
                while (projectRs.next()) {
                    String filePath = projectRs.getString("file_name");
                    File file = new File(filePath);
                    if (file.exists()) {
                        String fileName = file.getName();
                        String fileContent = readFileToString(file);
                        projectAttachments.addProperty(fileName, fileContent);
                    }
                }
            }
            result.add("attachmentProject", projectAttachments);

            String memberSQL = "SELECT username FROM Work WHERE project_code = ?";
            try (PreparedStatement memberStmt = connection.prepareStatement(memberSQL)) {
                memberStmt.setString(1, projectCode);
                ResultSet memberRs = memberStmt.executeQuery();
                while (memberRs.next()) {
                    String username = memberRs.getString("username");

                    JsonObject memberAttachments = new JsonObject();
                    String memberAttachmentSQL = "SELECT file_path FROM Attachment_Members WHERE project_code = ? AND username = ?";
                    try (PreparedStatement memberAttachmentStmt = connection.prepareStatement(memberAttachmentSQL)) {
                        memberAttachmentStmt.setString(1, projectCode);
                        memberAttachmentStmt.setString(2, username);
                        ResultSet memberAttachmentRs = memberAttachmentStmt.executeQuery();
                        while (memberAttachmentRs.next()) {
                            String filePath = memberAttachmentRs.getString("file_path");
                            File file = new File(filePath);
                            if (file.exists()) {
                                String fileName = file.getName();
                                String fileContent = readFileToString(file);
                                memberAttachments.addProperty(fileName, fileContent);
                            }
                        }
                    }

                    JsonObject memberJson = new JsonObject();
                    memberJson.addProperty("username", username);
                    memberJson.add("MemberAttachments", memberAttachments);
                    membersArray.add(memberJson);
                }
            }
            result.add("members", membersArray);

        } catch (Exception e) {
            e.printStackTrace();
            result.addProperty("error", "An error occurred while accessing the database or files.");
        }

        return convertJsonToMultipartFiles(result.toString());
    }

    private static String readFileToString(File file) throws IOException {
        try (FileReader fr = new FileReader(file, StandardCharsets.UTF_8);
             java.io.BufferedReader br = new java.io.BufferedReader(fr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString().trim();
        }
    }


    public static List<MultipartFile> convertJsonToMultipartFiles(String jsonString) {
        List<MultipartFile> multipartFiles = new ArrayList<>();

        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            JsonObject projectAttachments = jsonObject.getAsJsonObject("attachmentProject");
            for (String fileName : projectAttachments.keySet()) {
                String fileContent = projectAttachments.get(fileName).getAsString();
                multipartFiles.add(createMultipartFile(fileName, fileContent));
            }

            JsonArray members = jsonObject.getAsJsonArray("members");
            for (JsonElement memberElement : members) {
                JsonObject member = memberElement.getAsJsonObject();
                JsonObject memberAttachments = member.getAsJsonObject("MemberAttachments");
                for (String fileName : memberAttachments.keySet()) {
                    String fileContent = memberAttachments.get(fileName).getAsString();
                    multipartFiles.add(createMultipartFile(fileName, fileContent));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return multipartFiles;
    }

    private static MultipartFile createMultipartFile(String fileName, String fileContent) throws IOException {
        return new MockMultipartFile(fileName, fileName, "application/octet-stream", fileContent.getBytes());
    }
}
