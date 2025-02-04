package org.example;

import com.google.gson.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;
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

    // --------------------------------------------- Ham tren la dung de random cac code PK o cac cot 1 cua cac bang
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

    // --------------------------------------------- Ham dung de tra ve username va fullname (DASHBOARD)

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

            if (!dataFound) {
                return ResponseEntity.status(404).body("No projects found for the given username");
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
        }
        JsonObject responseJson = new JsonObject();
        responseJson.add("projects", projectsArray);
        return ResponseEntity.ok(responseJson.toString());
    }

    // --------------------------------------------- Ham tra tra ve thong cac project ma username do co lien quan (DASHBOARD)


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

            String checkUserExistsSQL = "SELECT COUNT(*) FROM Work WHERE project_code = ? AND username = ?";
            try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
                 PreparedStatement checkUserStmt = connection.prepareStatement(checkUserExistsSQL)) {

                checkUserStmt.setString(1, projectCode);
                checkUserStmt.setString(2, username);
                ResultSet rs = checkUserStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return ResponseEntity.status(400).body("Username already exists for this project");
                }
            } catch (Exception e){
                return ResponseEntity.status(500).body(e.getMessage());
            }

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


    // --------------------------------------------- Ham de dung de them project moi (DASHBOARD)

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

    // --------------------------------------------- Ham dung de update project name (Project's Information for Manager)

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

    @GetMapping("/updateProjectDeadline")
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

                if (rs.next()) {
                    String role = rs.getString("role");
                    if (role != null && role.toLowerCase().contains("manager")) {
                        isManager = true;
                    }
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



// --------------------------------------------- Ham tra ve thong tin cua cu the 1 project nao do (Project's Information for Member)

    @GetMapping("/getAttachment")
    public ResponseEntity<byte[]> getAttachment(@RequestParam("projectCode") String projectCode,
                                @RequestParam("typeString") String typeString,
                                @RequestParam("username") String username,
                                @RequestParam("fileName") String fileName) {
        byte[] attachmentData = null;

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD)) {
            String filePath = null;

            if (typeString.equals("Attachment_Member")) {
                String memberAttachmentSQL = "SELECT file_path FROM Attachment_Members WHERE project_code = ? AND username = ?";
                try (PreparedStatement memberAttachmentStmt = connection.prepareStatement(memberAttachmentSQL)) {
                    memberAttachmentStmt.setString(1, projectCode);
                    memberAttachmentStmt.setString(2, username);
                    ResultSet memberAttachmentRs = memberAttachmentStmt.executeQuery();
                    while (memberAttachmentRs.next()) {
                        filePath = memberAttachmentRs.getString("file_path");
                        if (new File(filePath).getName().equals(fileName)) {
                            File file = new File(filePath);
                            if (file.exists()) {
                                attachmentData = Files.readAllBytes(file.toPath());
                            }
                            break;
                        }
                    }
                }
            } else if (typeString.equals("Attachment_Submit")) {
                String submitSQL = "SELECT file_path FROM WorkSubmit WHERE project_code = ?";
                try (PreparedStatement submitStmt = connection.prepareStatement(submitSQL)) {
                    submitStmt.setString(1, projectCode);
                    ResultSet submitRs = submitStmt.executeQuery();
                    while (submitRs.next()) {
                        filePath = submitRs.getString("file_path");
                        if (new File(filePath).getName().equals(fileName)) {
                            File file = new File(filePath);
                            if (file.exists()) {
                                attachmentData = Files.readAllBytes(file.toPath());
                            }
                            break;
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("Invalid typeString: " + typeString);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok().body(attachmentData);
    }

    @GetMapping("/getAttachmentProject")
    public ResponseEntity<byte[]> getAttachmentProject(@RequestParam("projectCode") String projectCode,
                                                       @RequestParam("fileName") String fileName) {
        byte[] attachmentData = null;

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
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok().body(attachmentData);
    }



    // --------------------------------------------- Ham tra ve cac attachment cua project va cac attachment cua username

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

    @PostMapping("/addResponse")
    public ResponseEntity<String> addResponse(@RequestParam("projectCode") String projectCode,
                            @RequestParam("receiver") String receiver,
                            @RequestParam("sender") String sender,
                            @RequestParam("response") String response,
                            @RequestParam("timestamp") String timestamp) {
        boolean status = false;

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

                status = true;

                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (status){
            return ResponseEntity.ok().body("Add data successful.");
        } else {
            return ResponseEntity.status(404).body("Add data failed.");
        }
    }

    // ------------------------------------- Ham de add Response cua 1 nguoi ve phan cua 1 nguoi bat ki nao do

    @GetMapping("/getResponseData")
    public ResponseEntity<String> getResponseData(@RequestParam("projectCode") String projectCode, @RequestParam("userName") String username) {
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

        return ResponseEntity.ok().body(gson.toJson(responseData));
    }

    // ----------------------------- Ham dung de lay cac reponse cua nguoi khac ve work cua minh

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

    //-------------------------------------------------------bat dau tu day---------------------------------

    @PostMapping("/updateWorkField")
    public ResponseEntity<String> updateWorkField(@RequestBody String jsonInput) {
        JsonObject jsonObject = JsonParser.parseString(jsonInput).getAsJsonObject();

        String projectCode = jsonObject.get("project_code").getAsString();
        String username = jsonObject.get("username").getAsString();
        String typeString = jsonObject.get("typeString").getAsString();
        String newChange = jsonObject.get("newChange").getAsString();

        String sql = "";
        switch (typeString.toLowerCase()) {
            case "role":
                sql = "UPDATE Work SET role = ? WHERE project_code = ? AND username = ?";
                break;
            case "work":
                sql = "UPDATE Work SET work = ? WHERE project_code = ? AND username = ?";
                break;
            case "deadline":
                sql = "UPDATE Work SET deadline = ? WHERE project_code = ? AND username = ?";
                break;
            default:
                return ResponseEntity.status(500).body("Invalid typeString. Valid options are: role, work, deadline.");
        }

        try (Connection connection = DriverManager.getConnection(Connect_SQL.jdbcURL, Connect_SQL.USERNAME, Connect_SQL.PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, newChange);
            preparedStatement.setString(2, projectCode);
            preparedStatement.setString(3, username);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                return ResponseEntity.ok().body("Update successful.");
            } else {
                return ResponseEntity.status(500).body("No records updated. Check if the project_code and username exist.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/deleteAttachment")
    public ResponseEntity<String> deleteAttachment(@RequestPart("projectCode") String projectCode,
                                                   @RequestPart("fileName") String fileName) {
        String selectSQL = "SELECT attachment_code, file_name FROM Attachment WHERE project_code = ?";
        String deleteSQL = "DELETE FROM Attachment WHERE attachment_code = ?";

        boolean status = false;

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement selectStmt = connection.prepareStatement(selectSQL)) {

            selectStmt.setString(1, projectCode);
            ResultSet resultSet = selectStmt.executeQuery();

            while (resultSet.next()) {
                String attachmentCode = resultSet.getString("attachment_code");
                String fullFilePath = resultSet.getString("file_name");

                String extractedFileName = fullFilePath.substring(fullFilePath.lastIndexOf("/") + 1);

                if (extractedFileName.equals(fileName)) {
                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSQL)) {
                        deleteStmt.setString(1, attachmentCode);
                        deleteStmt.executeUpdate();
                        System.out.println("Attachment " + fileName + " has been deleted from the database successfully.");
                        status = true;
                    }

                    File fileToDelete = new File(fullFilePath);
                    if (fileToDelete.exists()) {
                        if (fileToDelete.delete()) {
                            System.out.println("File " + fullFilePath + " has been deleted successfully from the system.");
                        } else {
                            System.out.println("Failed to delete the file " + fullFilePath);
                        }
                    } else {
                        System.out.println("File " + fullFilePath + " does not exist.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (status){
            return ResponseEntity.ok().body("Attachment " + fileName + " has been deleted from the database successfully.");
        } else {
            return ResponseEntity.status(404).body("File does not exist.");
        }
    }

    @PostMapping("/deleteAttachmentMember")
    public ResponseEntity<String> deleteAttachmentMember(@RequestPart("projectCode") String projectCode,
                                       @RequestPart("username") String username,
                                       @RequestPart("fileName") String fileName) {
        String selectSQL = "SELECT attachmentMembers_Code, file_path FROM Attachment_Members WHERE project_code = ? AND username = ?";
        String deleteSQL = "DELETE FROM Attachment_Members WHERE attachmentMembers_Code = ?";

        boolean status = false;

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement selectStmt = connection.prepareStatement(selectSQL)) {

            selectStmt.setString(1, projectCode);
            selectStmt.setString(2, username);
            ResultSet resultSet = selectStmt.executeQuery();

            while (resultSet.next()) {
                String attachmentMembersCode = resultSet.getString("attachmentMembers_Code");
                String fullFilePath = resultSet.getString("file_path");

                String extractedFileName = fullFilePath.substring(fullFilePath.lastIndexOf("/") + 1);

                if (extractedFileName.equals(fileName)) {
                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSQL)) {
                        deleteStmt.setString(1, attachmentMembersCode);
                        deleteStmt.executeUpdate();
                        System.out.println("Attachment for member " + fileName + " has been deleted from the database successfully.");
                        status = true;
                    }

                    File fileToDelete = new File(fullFilePath);
                    if (fileToDelete.exists()) {
                        if (fileToDelete.delete()) {
                            System.out.println("File " + fullFilePath + " has been deleted successfully from the system.");
                        } else {
                            System.out.println("Failed to delete the file " + fullFilePath);
                        }
                    } else {
                        System.out.println("File " + fullFilePath + " does not exist.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (status){
            return ResponseEntity.ok().body("Attachment for member " + fileName + " has been deleted from the database successfully.");
        } else {
            return ResponseEntity.status(404).body("File does not exist.");
        }
    }

    @PostMapping("/deleteWorkSubmit")
    public ResponseEntity<String> deleteWorkSubmit(@RequestPart("/projectCode") String projectCode,
                                 @RequestPart("username") String username,
                                 @RequestPart("fileName") String fileName) {
        String selectSQL = "SELECT worksubmitcode, file_path FROM WorkSubmit WHERE project_code = ? AND username = ?";
        String deleteSQL = "DELETE FROM WorkSubmit WHERE worksubmitcode = ?";

        boolean status = false;

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement selectStmt = connection.prepareStatement(selectSQL)) {

            selectStmt.setString(1, projectCode);
            selectStmt.setString(2, username);
            ResultSet resultSet = selectStmt.executeQuery();

            while (resultSet.next()) {
                String worksubmitCode = resultSet.getString("worksubmitcode");
                String fullFilePath = resultSet.getString("file_path");

                String extractedFileName = fullFilePath.substring(fullFilePath.lastIndexOf("/") + 1);

                if (extractedFileName.equals(fileName)) {
                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSQL)) {
                        deleteStmt.setString(1, worksubmitCode);
                        deleteStmt.executeUpdate();
                        System.out.println("WorkSubmit file " + fileName + " has been deleted from the database successfully.");
                        status = true;
                    }

                    File fileToDelete = new File(fullFilePath);
                    if (fileToDelete.exists()) {
                        if (fileToDelete.delete()) {
                            System.out.println("File " + fullFilePath + " has been deleted successfully from the system.");
                        } else {
                            System.out.println("Failed to delete the file " + fullFilePath);
                        }
                    } else {
                        System.out.println("File " + fullFilePath + " does not exist.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (status){
            return ResponseEntity.ok().body("WorkSubmit file " + fileName + " has been deleted from the database successfully.");
        } else {
            return ResponseEntity.status(404).body("File does not exist.");
        }
    }


    @PostMapping("/addAttachment")
    public ResponseEntity<String> addAttachment(@RequestPart("projectCode") String projectCode,
                                                @RequestPart("file") MultipartFile file) {
        String insertSQL = "INSERT INTO Attachment (attachment_code, project_code, file_name) VALUES (?, ?, ?)";
        String filePath = File_Path.file_path + projectCode + "/Attachment_Project/" + file.getOriginalFilename();

        boolean status = false;

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {

            String attachmentCode = generateProjectCode("Attachment");

            insertStmt.setString(1, attachmentCode);
            insertStmt.setString(2, projectCode);
            insertStmt.setString(3, filePath);
            insertStmt.executeUpdate();

            status = true;

            File targetFile = new File(filePath);
            if (!targetFile.getParentFile().exists()) {
                boolean dirsCreated = targetFile.getParentFile().mkdirs();
                if (dirsCreated) {
                    System.out.println("Directories created: " + targetFile.getParent());
                } else {
                    System.out.println("Failed to create directories: " + targetFile.getParent());
                }
            }

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(file.getBytes());
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        if (status){
            return ResponseEntity.ok("Save data successfully.");
        } else {
            return ResponseEntity.status(500).body("Save data failed.");
        }
    }

    @PostMapping("/addAttachmentMember")
    public ResponseEntity<String> addAttachmentMember(@RequestPart("projectCode") String projectCode,
                                    @RequestPart("username") String username,
                                    @RequestPart("file") MultipartFile file) {
        String insertSQL = "INSERT INTO Attachment_Members (attachmentMembers_Code, project_code, username, file_path) VALUES (?, ?, ?, ?)";
        String filePath = File_Path.file_path + projectCode + "/Attachment_Member/" + username + "/" + file.getOriginalFilename();

        boolean status = false;

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {

            String attachmentMembersCode = generateProjectCode("Attachment_Members");

            insertStmt.setString(1, attachmentMembersCode);
            insertStmt.setString(2, projectCode);
            insertStmt.setString(3, username);
            insertStmt.setString(4, filePath);
            insertStmt.executeUpdate();

            status = true;

            File targetFile = new File(filePath);
            if (!targetFile.getParentFile().exists()) {
                boolean dirsCreated = targetFile.getParentFile().mkdirs();
                if (dirsCreated) {
                    System.out.println("Directories created: " + targetFile.getParent());
                } else {
                    System.out.println("Failed to create directories: " + targetFile.getParent());
                }
            }

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(file.getBytes());
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        if (status){
            return ResponseEntity.ok().body("Save data successfully.");
        } else {
            return ResponseEntity.status(500).body("Save data failed.");
        }
    }

    @PostMapping("/addWorkSubmit")
    public ResponseEntity<String> addWorkSubmit(@RequestPart("projectCode") String projectCode,
                                                @RequestPart("username") String username,
                                                @RequestPart("file") MultipartFile file,
                                                @RequestPart("submitTime") String submitTime) {
        String insertSQL = "INSERT INTO WorkSubmit (worksubmitcode, project_code, username, file_path, submit_time) VALUES (?, ?, ?, ?, ?)";
        String filePath = File_Path.file_path + projectCode + "/Attachment_Submit/" + username + "/" + file.getOriginalFilename();

        boolean status = false;

        try (Connection connection = DriverManager.getConnection(jdbcURL, USERNAME, PASSWORD);
             PreparedStatement insertStmt = connection.prepareStatement(insertSQL)) {

            String worksubmitCode = generateProjectCode("WorkSubmit");

            insertStmt.setString(1, worksubmitCode);
            insertStmt.setString(2, projectCode);
            insertStmt.setString(3, username);
            insertStmt.setString(4, filePath);
            insertStmt.setString(5, submitTime);  // Thêm submitTime từ tham số đầu vào
            insertStmt.executeUpdate();

            status = true;

            File targetFile = new File(filePath);
            if (!targetFile.getParentFile().exists()) {
                boolean dirsCreated = targetFile.getParentFile().mkdirs();
                if (dirsCreated) {
                    System.out.println("Directories created: " + targetFile.getParent());
                } else {
                    System.out.println("Failed to create directories: " + targetFile.getParent());
                }
            }

            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(file.getBytes());
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        if (status) {
            return ResponseEntity.ok().body("Save data successfully.");
        } else {
            return ResponseEntity.status(500).body("Save data failed.");
        }
    }
}


