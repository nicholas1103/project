package org.example;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class Main extends SpringBootServletInitializer {
    public static void main(String[] args) {
//        SpringApplication.run(Main.class, args);
//        Insert_Data_SQL insertDataSql = new Insert_Data_SQL();
//        insertDataSql.insertData();
        Controller controller = new Controller();
//        System.out.println(controller.getUserDetails("user12", "password1"));
//
        System.out.println(controller.getProjectDetailsByUsername("user1"));
//
//        System.out.println(controller.addProject("Project Vui vui", "Hahahaaa", "2222-02-22", "user3"));

//        System.out.println(insertDataSql.generateProjectCode("Work"));

    }
}