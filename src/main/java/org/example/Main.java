package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class Main extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}

//        Insert_Data_SQL insertDataSql = new Insert_Data_SQL();
////        insertDataSql.insertData();
//        Controller controller = new Controller();
////        System.out.println(controller.getUserDetails("phongnt", "securePassword"));
////
////        System.out.println(controller.getProjectDetailsByUsername("phongnt"));
////
////        System.out.println(controller.addProject("Project Vui vui", "Hahahaaa", "2222-02-22", "user3"));
//
////        System.out.println(insertDataSql.generateProjectCode("Work"));
//
////        System.out.println(controller.checkManager("Project-0001", "phucnh"));
//
////        System.out.println(controller.updateProjectName("Project-0001", "Project Manager"));
//
//        System.out.println(controller.updateDeadlineProject("Project-0001", "03-11-2003 3:00 AM"));
//
////        insertDataSql.addUserProjectWorkAttachmentResultResponse(
////                "phongnt", "securePassword", "Nguyen Tan Phong",
////                "New Project", "This is a new project", "2024-12-31",
////                "Manager", "Manage project", "2024-12-31", "unfinished",
////                "doc1.doc", "Project completed", "2024-09-07 10:00:00",
////                "phongnt", "This is a response to the work", "2024-09-07 11:00:00"
////        );
//
//    }

//    public static void main(String[] args) {
//        Controller controller = new Controller();
//
//        JsonObject projectData = new JsonObject();
//        projectData.addProperty("project_name", "Me Project");
//        projectData.addProperty("requirement", "Big size project");
//        projectData.addProperty("deadline", "2024-12-12");
//
//        JsonArray projectAttachments = new JsonArray();
//        projectAttachments.add("yccccccccc.doc");
//        projectAttachments.add("yccccccccccc.doc");
//        projectData.add("ProjectAttachments", projectAttachments);
//
//        JsonArray members = new JsonArray();
//
//        JsonObject member1 = new JsonObject();
//        member1.addProperty("name", "phongnt");
//        member1.addProperty("role", "Developer");
//        member1.addProperty("work", "Back-End 1");
//        member1.addProperty("deadline", "2024-03-11");
//
//        JsonArray memberAttachments1 = new JsonArray();
//        memberAttachments1.add("rqqqqqqqq1.doc");
//        memberAttachments1.add("rqqqqqqqqqqqqqqqqq1.doc");
//        member1.add("MemberAttachments", memberAttachments1);
//
//        members.add(member1);
//
//        JsonObject member2 = new JsonObject();
//        member2.addProperty("name", "phucnh");
//        member2.addProperty("role", "Developer");
//        member2.addProperty("work", "Back-End 2");
//        member2.addProperty("deadline", "2024-02-14");
//
//        JsonArray memberAttachments2 = new JsonArray();
//        memberAttachments2.add("rqqqqqqqq2.doc");
//        memberAttachments2.add("rqqqqqqqqqqqqq2.doc");
//        member2.add("MemberAttachments", memberAttachments2);
//
//        members.add(member2);
//
//        projectData.add("members", members);
//
//        Gson gson = new Gson();
//        String jsonRequest = gson.toJson(projectData);
//
//        List<MultipartFile> files = new ArrayList<>();
//        files.add(controller.createMockMultipartFile("yccccccccc.doc", "yc1"));
//        files.add(controller.createMockMultipartFile("yccccccccccc.doc", "yc2"));
//
//        files.add(controller.createMockMultipartFile("rqqqqqqqq1.doc", "req1"));
//        files.add(controller.createMockMultipartFile("rqqqqqqqqqqqqqqqqq1.doc", "req2"));
//
//        files.add(controller.createMockMultipartFile("rqqqqqqqq2.doc", "reqr1"));
//        files.add(controller.createMockMultipartFile("rqqqqqqqqqqqqq2.doc", "reqr2"));
//
//        System.out.println(jsonRequest);
//
////        String result = controller.addProject(jsonRequest, files, "phucbm");
//
////        System.out.println(result);
//    }
//
//
//}
