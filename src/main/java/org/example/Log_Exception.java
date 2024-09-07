package org.example;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log_Exception {
    protected String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    protected void logException(Exception e) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        //String logFileName = "error_log.txt";
        String logFileName = "/tomcat11/webapps/testcases/errorLog.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFileName, true))) {
            writer.println("Time: " + timeStamp);
            writer.println("Exception:");
            writer.println(getStackTraceAsString(e));
            writer.println("-------------------------------------------");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
