package org.example.models;

import java.io.Serializable;

public class Course implements Serializable {
    private int id;
    private String course_name;
    private String instructor;
    private String description;

    public Course(int id, String course_name, String instructor, String description) {
        this.id = id;
        this.course_name = course_name;
        this.instructor = instructor;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCourse_name() {
        return course_name;
    }

    public void setCourse_name(String course_name) {
        this.course_name = course_name;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
