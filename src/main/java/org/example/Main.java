package org.example;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class Main extends SpringBootServletInitializer {
    public static void main(String[] args) {
//        SpringApplication.run(Main.class, args);
        Insert_Data_SQL insertDataSql = new Insert_Data_SQL();
//        insertDataSql.insertData();
        Controller controller = new Controller();
//        System.out.println(controller.getUserDetails("phongnt", "securePassword"));
//
        System.out.println(controller.getProjectDetailsByUsername("phongnt"));
//
//        System.out.println(controller.addProject("Project Vui vui", "Hahahaaa", "2222-02-22", "user3"));

//        System.out.println(insertDataSql.generateProjectCode("Work"));

//        insertDataSql.addUserProjectWorkAttachmentResultResponse(
//                "phongnt", "securePassword", "Nguyen Tan Phong",
//                "New Project", "This is a new project", "2024-12-31",
//                "Manager", "Manage project", "2024-12-31", "unfinished",
//                "doc1.doc", "Project completed", "2024-09-07 10:00:00",
//                "phongnt", "This is a response to the work", "2024-09-07 11:00:00"
//        );

    }
}