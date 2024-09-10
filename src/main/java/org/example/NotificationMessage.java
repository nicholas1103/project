package org.example;

public class NotificationMessage {
    private String memberName;
    private String projectName;

    public NotificationMessage() {
    }

    public NotificationMessage(String memberName, String projectName) {
        this.memberName = memberName;
        this.projectName = projectName;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
