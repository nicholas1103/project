package org.example;

import com.google.gson.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
        String sql = "";
        if (prefix.equals("Project")){
            sql = "SELECT MAX(CAST(SUBSTRING_INDEX(project_code, '-', -1) AS UNSIGNED)) FROM " + prefix;
        } else if (prefix.equals("Work")){
            sql = "SELECT MAX(CAST(SUBSTRING_INDEX(work_code, '-', -1) AS UNSIGNED)) FROM " + prefix;
        } else if (prefix.equals("Attachment")) {
            sql = "SELECT MAX(CAST(SUBSTRING_INDEX(attachment_code, '-', -1) AS UNSIGNED)) FROM " + prefix;
        } else if (prefix.equals("Result")) {
            sql = "SELECT MAX(CAST(SUBSTRING_INDEX(result_code, '-', -1) AS UNSIGNED)) FROM " + prefix;
        } else if (prefix.equals("Response")) {
            sql = "SELECT MAX(CAST(SUBSTRING_INDEX(response_code, '-', -1) AS UNSIGNED)) FROM " + prefix;
        } else if (prefix.equals("Attachment_Members")) {
            sql = "SELECT MAX(CAST(SUBSTRING_INDEX(attachmentMembers_Code, '-', -1) AS UNSIGNED)) FROM " + prefix;
        } else if (prefix.equals("WorkSubmit")) {
            sql = "SELECT MAX(CAST(SUBSTRING_INDEX(worksubmitcode, '-', -1) AS UNSIGNED)) FROM " + prefix;
        }

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int currentMaxId = rs.getInt(1);
                if (currentMaxId > 0) {
                    nextId = currentMaxId + 1;
                }
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

    @PostMapping("/createProject")
    public ResponseEntity<String> createProject(
            @RequestPart("jsonData") String jsonData,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        try {
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

            String projectName = jsonObject.get("project_name").getAsString();
            String requirement = jsonObject.get("requirement").getAsString();
            String deadline = jsonObject.get("deadline").getAsString();
            String creatorUsername = jsonObject.get("creator").getAsString();

            String projectCode = generateProjectCode("Project");

            String insertProjectSQL = "INSERT INTO Project (project_code, project_name, requirement, deadline) VALUES (?, ?, ?, ?)";
            String insertAttachmentSQL = "INSERT INTO Attachment (attachment_code, project_code, file_name) VALUES (?, ?, ?)";
            String insertWorkSQL = "INSERT INTO Work (work_code, project_code, username, role, work, deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
                 PreparedStatement projectStmt = connection.prepareStatement(insertProjectSQL);
                 PreparedStatement workStmt = connection.prepareStatement(insertWorkSQL);
                 PreparedStatement attachmentStmt = connection.prepareStatement(insertAttachmentSQL)) {

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

                if (!projectFolder.exists()) {
                    projectFolder.mkdirs();
                }

                if (files != null) {
                    for (MultipartFile file : files) {
                        String fileName = file.getOriginalFilename();
                        String fullFilePath = projectFolder.getPath() + "/" + fileName;
                        saveFileContent(fullFilePath, file);
                        
                        String attachmentCode = generateProjectCode("Attachment");
                        attachmentStmt.setString(1, attachmentCode);
                        attachmentStmt.setString(2, projectCode);
                        attachmentStmt.setString(3, fullFilePath);
                        attachmentStmt.executeUpdate();
                    }
                }

                return ResponseEntity.ok().body(projectCode);

            } catch (SQLException | IOException e) {
                return ResponseEntity.status(500).body(e.getMessage());
            }
        } catch (JsonSyntaxException e) {
            return ResponseEntity.status(400).body("Invalid JSON format");
        }
    }

    @PostMapping("/addMember")
    public ResponseEntity<String> addMember(
            @RequestPart("jsonData") String jsonData,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        try {
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

            String username = jsonObject.get("username").getAsString();
            String role = jsonObject.get("role").getAsString();
            String work = jsonObject.get("work").getAsString();
            String deadline = jsonObject.get("deadline").getAsString();
            String projectCode = jsonObject.get("project_code").getAsString();

            String workCode = generateProjectCode("Work");
            String insertWorkSQL = "INSERT INTO Work (work_code, project_code, username, role, work, deadline, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
            String insertAttachmentMemberSQL = "INSERT INTO Attachment_Members (attachmentMembers_Code, project_code, username, file_path) VALUES (?, ?, ?, ?)";

            try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
                 PreparedStatement workStmt = connection.prepareStatement(insertWorkSQL);
                 PreparedStatement attachmentMemberStmt = connection.prepareStatement(insertAttachmentMemberSQL)) {

                workStmt.setString(1, workCode);
                workStmt.setString(2, projectCode);
                workStmt.setString(3, username);
                workStmt.setString(4, role);
                workStmt.setString(5, work);
                workStmt.setString(6, deadline);
                workStmt.setString(7, "unfinished");
                workStmt.executeUpdate();
                
                File memberBaseFolder = new File(File_Path.file_path + projectCode + "/Attachment_Member/" + username);
                if (!memberBaseFolder.exists()) {
                    memberBaseFolder.mkdirs();
                }

                if (files != null) {
                    for (MultipartFile file : files) {
                        String fileName = file.getOriginalFilename();
                        String fullFilePath = memberBaseFolder.getPath() + "/" + fileName;
                        saveFileContent(fullFilePath, file);

                        String attachmentMemberCode = generateProjectCode("Attachment_Members");
                        attachmentMemberStmt.setString(1, attachmentMemberCode);
                        attachmentMemberStmt.setString(2, projectCode);
                        attachmentMemberStmt.setString(3, username);
                        attachmentMemberStmt.setString(4, fullFilePath);
                        attachmentMemberStmt.executeUpdate();
                    }
                }

                return ResponseEntity.ok().body("Member has been added successfully");

            } catch (SQLException | IOException e) {
                return ResponseEntity.status(500).body(e.getMessage());
            }
        } catch (JsonSyntaxException e) {
            return ResponseEntity.status(400).body("Invalid JSON format");
        }
    }

    private void saveFileContent(String fullFilePath, MultipartFile file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fullFilePath)) {
            fos.write(file.getBytes());
        }
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
    public ResponseEntity<String> getProjectDetails(@RequestParam("projectCode") String projectCode,
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
                    String workSubmitSQL = "SELECT file_path, submit_time FROM WorkSubmit WHERE project_code = ? AND username = ?";
                    try (PreparedStatement workSubmitStmt = connection.prepareStatement(workSubmitSQL)) {
                        workSubmitStmt.setString(1, projectCode);
                        workSubmitStmt.setString(2, memberUsername);
                        ResultSet submitRs = workSubmitStmt.executeQuery();
                        while (submitRs.next()) {
                            JsonObject submitObj = new JsonObject();
                            String filePath = submitRs.getString("file_path");
                            String fileName = new File(filePath).getName();
                            submitObj.addProperty("file_name", fileName);
                            submitObj.addProperty("submit_time", submitRs.getString("submit_time"));
                            workSubmitArray.add(submitObj);
                        }
                    }
                    memberObj.add("work_submit", workSubmitArray);
                    memberObj.addProperty("work_status", rs.getString("status")); 
                    memberObj.addProperty("deadline", rs.getString("deadline"));

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
            return ResponseEntity.status(500).body(e.getMessage());
        }

        return ResponseEntity.ok().body(result.toString());
    }

    @GetMapping("/getAttachment")
    public ResponseEntity<byte[]> getAttachment(@RequestParam("projectCode") String projectCode,
                                                @RequestParam("typeString") String typeString,
                                                @RequestParam("username") String username,
                                                @RequestParam("fileName") String fileName) {
        byte[] attachmentData = null;
        String contentType = "application/octet-stream"; // Mặc định
        String filePath = null;

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD)) {
            // Xác định truy vấn SQL dựa trên typeString
            String sql = "SELECT file_path FROM " +
                    (typeString.equals("Attachment_Member") ? "Attachment_Members" : "WorkSubmit") +
                    " WHERE project_code = ?" +
                    (typeString.equals("Attachment_Member") ? " AND username = ?" : "");

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, projectCode);
                if (typeString.equals("Attachment_Member")) {
                    stmt.setString(2, username);
                }

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    filePath = rs.getString("file_path");
                    if (new File(filePath).getName().equals(fileName)) {
                        File file = new File(filePath);
                        if (file.exists()) {
                            attachmentData = Files.readAllBytes(file.toPath());
                            contentType = determineContentType(fileName); // Xác định Content-Type
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        if (attachmentData == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);

        return ResponseEntity.ok()
                .headers(headers)
                .body(attachmentData);
    }


    @GetMapping("/getAttachmentProject")
    public ResponseEntity<ByteArrayResource> getAttachmentProject(@RequestParam("projectCode") String projectCode,
                                                                  @RequestParam("fileName") String fileName) {
        byte[] attachmentData = null;
        String contentType = "application/octet-stream"; // Mặc định

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD)) {
            String projectSQL = "SELECT file_name FROM Attachment WHERE project_code = ?";
            try (PreparedStatement projectStmt = connection.prepareStatement(projectSQL)) {
                projectStmt.setString(1, projectCode);
                ResultSet projectRs = projectStmt.executeQuery();

                while (projectRs.next()) {
                    String filePath = projectRs.getString("file_name");
                    File file = new File(filePath);
                    if (file.exists()) {
                        String actualFileName = file.getName();

                        if (actualFileName.equals(fileName)) {
                            attachmentData = Files.readAllBytes(file.toPath());
                            contentType = determineContentType(actualFileName); // Xác định Content-Type
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        if (attachmentData == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        System.out.println(headers);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(attachmentData));
    }

    // Phương thức xác định Content-Type dựa trên phần mở rộng tệp
    private String determineContentType(String fileName) {
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("zip", "application/zip");
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("csv", "text/csv");
        mimeTypes.put("doc", "application/msword");
        mimeTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mimeTypes.put("ppt", "application/vnd.ms-powerpoint");
        mimeTypes.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        mimeTypes.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        String extension = getFileExtension(fileName).toLowerCase();
        return mimeTypes.getOrDefault(extension, "application/octet-stream"); // Mặc định
    }

    // Phương thức để lấy phần mở rộng tệp
    private String getFileExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');
        return (lastIndexOfDot == -1) ? "" : fileName.substring(lastIndexOfDot + 1);
    }

    @PostMapping("/saveWorkSubmit")
    public boolean saveWorkSubmit(@RequestPart("projectCode") String projectCode,
                                  @RequestPart("username") String username, 
                                  @RequestPart("files") List<MultipartFile> files,
                                  @RequestPart("submitTime") String submitTime) {
        boolean isUpdated = false;
        String workSubmitPath = File_Path.file_path + projectCode + "/Attachment_Submit/" + username + "/";

        File directory = new File(workSubmitPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD)) {
            String insertSQL = "INSERT INTO WorkSubmit (worksubmitcode, project_code, username, file_path, submit_time) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(insertSQL);

            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                String filePath = workSubmitPath + fileName;
                File savedFile = new File(filePath);
                try (FileOutputStream fos = new FileOutputStream(savedFile)) {
                    fos.write(file.getBytes());
                }

                pstmt.setString(1, generateProjectCode("WorkSubmit"));
                pstmt.setString(2, projectCode);
                pstmt.setString(3, username);
                pstmt.setString(4, filePath);
                pstmt.setString(5, submitTime);

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    isUpdated = true;
                }
            }

            if (isUpdated) {
                String updateSQL = "UPDATE Work SET status = 'finished' WHERE project_code = ? AND username = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                    updateStmt.setString(1, projectCode);
                    updateStmt.setString(2, username);

                    updateStmt.executeUpdate();
                }
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        return isUpdated;
    }

    // --------------------------------------------- Ham dung de luu cac attachment submit cua cac thanh vien

//    @GetMapping("/getWorkSubmitFiles")
//    public List<MultipartFile> getWorkSubmitFiles(String projectCode, String username) {
//        JsonObject result = new JsonObject();
//
//        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD)) {
//
//            String sql = "SELECT file_path FROM WorkSubmit WHERE project_code = ? AND username = ?";
//            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
//                pstmt.setString(1, projectCode);
//                pstmt.setString(2, username);
//
//                ResultSet rs = pstmt.executeQuery();
//
//                while (rs.next()) {
//                    String filePath = rs.getString("file_path");
//                    String fileName = new File(filePath).getName();
//                    String fileContent = readFileContent(filePath);
//                    result.addProperty(fileName, fileContent);
//                }
//            }
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return convertJsonToMultipartFilesSubmit(result);
//    }

    private String readFileContent(String filePath) {
        StringBuilder content = new StringBuilder();
        try {
            Files.lines(Paths.get(filePath)).forEach(content::append);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }

    // --------------------------------------------- Ham dung de tra ve attachment submit

    @PostMapping("/addResponse")
    public void addResponse(@RequestParam("projectCode") String projectCode,
                            @RequestParam("receiver") String receiver,
                            @RequestParam("sender") String sender,
                            @RequestParam("response") String response,
                            @RequestParam("timestamp") String timestamp) {
        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD)) {

            String workCode = null;
            String selectWorkCodeSQL = "SELECT work_code FROM Work WHERE project_code = ? AND username = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectWorkCodeSQL)) {
                preparedStatement.setString(1, projectCode);
                preparedStatement.setString(2, receiver);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        workCode = resultSet.getString("work_code");
                    } else {
                        throw new SQLException("No matching work code found for the provided projectCode and receiver.");
                    }
                }
            }

            String responseCode = generateProjectCode("Response");

            String insertResponseSQL = "INSERT INTO Response (response_code, work_code, sender, content, time_stamp) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertResponseSQL)) {
                preparedStatement.setString(1, responseCode);
                preparedStatement.setString(2, workCode);
                preparedStatement.setString(3, sender);
                preparedStatement.setString(4, response);
                preparedStatement.setString(5, timestamp);

                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ------------------------------------- Ham de add Response cua 1 nguoi ve phan cua 1 nguoi bat ki nao do

    @GetMapping("/getResponseData")
    public String getResponseData(@RequestParam("projectCode") String projectCode, @RequestParam("userName") String username) {
        JsonObject responseData = new JsonObject();
        Gson gson = new Gson();
        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD)) {

            String workCode = null;
            String selectWorkCodeSQL = "SELECT work_code FROM Work WHERE project_code = ? AND username = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectWorkCodeSQL)) {
                preparedStatement.setString(1, projectCode);
                preparedStatement.setString(2, username);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        workCode = resultSet.getString("work_code");
                    } else {
                        throw new SQLException("No matching work code found for the provided projectCode and username.");
                    }
                }
            }

            String selectResponseSQL = "SELECT sender, content, time_stamp FROM Response WHERE work_code = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectResponseSQL)) {
                preparedStatement.setString(1, workCode);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    JsonArray responseArray = new JsonArray();

                    while (resultSet.next()) {
                        JsonObject responseEntry = new JsonObject();
                        responseEntry.addProperty("sender", resultSet.getString("sender"));
                        responseEntry.addProperty("content", resultSet.getString("content"));
                        responseEntry.addProperty("timestamp", resultSet.getString("time_stamp"));
                        responseArray.add(responseEntry);
                    }

                    responseData.add("responses", responseArray);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return gson.toJson(responseData);
    }

    // ----------------------------- Ham dung de lay cac reponse cua nguoi khac ve work cua minh

    @PostMapping("/updateProjectRequirement")
    public ResponseEntity<String> updateProjectRequirement(@RequestParam("projectCode") String projectCode, @RequestParam("newRequirement") String newRequirement) {
        String updateSQL = "UPDATE Project SET requirement = ? WHERE project_code = ?";

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {

            pstmt.setString(1, newRequirement);
            pstmt.setString(2, projectCode);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Project requirement updated successfully.");
                return ResponseEntity.ok().body("Project requirement updated successfully.");
            } else {
                System.out.println("No project found with the provided project_code.");
                return ResponseEntity.status(404).body("No project found with the provided project_code.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(404).body("The project request to be updated failed.");
    }

    //-------------------------------------------- Ham dung de update requirement cua Project

    @PostMapping("/updateProjectDeadline")
    public ResponseEntity<String> updateProjectDeadline(@RequestParam("projectCode") String projectCode, @RequestParam("newDeadline") String newDeadline) {
        String updateSQL = "UPDATE Project SET deadline = ? WHERE project_code = ?";

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {

            pstmt.setString(1, newDeadline);
            pstmt.setString(2, projectCode);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                return ResponseEntity.ok().body("Project deadline updated successfully.");
            } else {
                return ResponseEntity.status(404).body("No project found with the provided project_code.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(404).body("The project request to be updated failed.");
    }

    //------------------------------------------------ Ham dung de update deadline cua Project

    @PostMapping("/deleteProject")
    public ResponseEntity<String> deleteProject(@RequestParam("projectCode") String projectCode) {
        String responseMessage = "Project and related data deleted successfully";

        String deleteAttachmentMembersSQL = "DELETE FROM Attachment_Members WHERE project_code = ?";
        String deleteAttachmentSQL = "DELETE FROM Attachment WHERE project_code = ?";
        String deleteResponseSQL = "DELETE FROM Response WHERE work_code IN (SELECT work_code FROM Work WHERE project_code = ?)";
        String deleteResultSQL = "DELETE FROM Result WHERE work_code IN (SELECT work_code FROM Work WHERE project_code = ?)";
        String deleteSubmitSQL = "DELETE FROM WorkSubmit WHERE project_code = ?";
        String deleteWorkSQL = "DELETE FROM Work WHERE project_code = ?";
        String deleteProjectSQL = "DELETE FROM Project WHERE project_code = ?";

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD)) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement stmt = connection.prepareStatement(deleteAttachmentMembersSQL)) {
                    stmt.setString(1, projectCode);
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = connection.prepareStatement(deleteAttachmentSQL)) {
                    stmt.setString(1, projectCode);
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = connection.prepareStatement(deleteResponseSQL)) {
                    stmt.setString(1, projectCode);
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = connection.prepareStatement(deleteResultSQL)) {
                    stmt.setString(1, projectCode);
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = connection.prepareStatement(deleteSubmitSQL)) {
                    stmt.setString(1, projectCode);
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = connection.prepareStatement(deleteWorkSQL)) {
                    stmt.setString(1, projectCode);
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = connection.prepareStatement(deleteProjectSQL)) {
                    stmt.setString(1, projectCode);
                    stmt.executeUpdate();
                }

                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                e.printStackTrace();
                return ResponseEntity.status(500).body("Error occurred while deleting project: " + e.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Database connection failed: " + e.getMessage());
        }

        return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/deleteUser")
    public ResponseEntity<String> deleteUserFromProject(@RequestParam("projectCode") String projectCode, @RequestParam("username") String username) {
        String deleteWorkSQL = "DELETE FROM Work WHERE project_code = ? AND username = ?";
        String deleteAttachmentMembersSQL = "DELETE FROM Attachment_Members WHERE project_code = ? AND username = ?";
        String deleteResponseSQL = "DELETE FROM Response WHERE work_code IN (SELECT work_code FROM Work WHERE project_code = ? AND username = ?)";

        Connection connection = null;

        try {
            connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);

            connection.setAutoCommit(false);

            try (PreparedStatement workStmt = connection.prepareStatement(deleteWorkSQL)) {
                workStmt.setString(1, projectCode);
                workStmt.setString(2, username);
                int workRows = workStmt.executeUpdate();
                System.out.println("Deleted " + workRows + " rows from Work.");
            }

            try (PreparedStatement attachmentMembersStmt = connection.prepareStatement(deleteAttachmentMembersSQL)) {
                attachmentMembersStmt.setString(1, projectCode);
                attachmentMembersStmt.setString(2, username);
                int attachmentMemberRows = attachmentMembersStmt.executeUpdate();
                System.out.println("Deleted " + attachmentMemberRows + " rows from Attachment_Members.");
            }

            try (PreparedStatement responseStmt = connection.prepareStatement(deleteResponseSQL)) {
                responseStmt.setString(1, projectCode);
                responseStmt.setString(2, username);
                int responseRows = responseStmt.executeUpdate();
                System.out.println("Deleted " + responseRows + " rows from Response.");
            }

            connection.commit();
            return ResponseEntity.ok("All related data for user '" + username + "' in project '" + projectCode + "' has been deleted.");

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            return ResponseEntity.status(500).body("An error occurred while trying to delete the user's data from the project.");
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }
}
