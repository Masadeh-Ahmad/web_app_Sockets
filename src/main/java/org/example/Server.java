package org.example;

import org.example.models.Course;
import org.example.models.Enrollment;
import org.example.models.Student;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class Server {
    public void start() throws IOException {
        ServerSocket serverSocket;
        Connection connection;
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream("application.properties");
        prop.load(stream);
        try {
            serverSocket = new ServerSocket(1234);
            System.out.println("Server started");

            // Connect to the MySQL database
            String url = prop.getProperty("url");
            String user = prop.getProperty("user");
            String password = prop.getProperty("password");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to database");
        }
        catch (Exception e) {
            System.out.println("Connection FAILURE");;
            return;
        }
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected");

                    // Create input and output streams for the client
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                    // Read the username and password from the client
                    String username = in.readLine();
                    String userPassword = in.readLine();

                    Student student = verifyCredentials(username, userPassword, connection);
                    if (student != null) {
                        // Send a success message to the client
                        out.println("SUCCESS");

                        boolean done = false;
                        while (!done) {
                            int choice = Integer.parseInt(in.readLine());
                            if (choice == 1) {
                                outputStream.writeObject(viewMarks(student.getId(), connection));
                                outputStream.flush();
                            } else if (choice == 2) {
                                // Handle request to view class stats
                                int courseId = Integer.parseInt(in.readLine());
                                if(isAccessible(student.getId(),courseId,connection)){
                                    Course course = getCourseById(courseId, connection);
                                    out.println(course.getCourse_name());
                                    outputStream.writeObject(classStats(course.getId(),connection));
                                    outputStream.flush();
                                }
                                else
                                    out.println("null");
                            } else if (choice == 3) {
                                // Handle request to exit
                                done = true;
                            }
                        }
                    } else {
                        // Send a failure message to the client
                        out.println("FAILURE");
                    }

                    // Close the client socket
                    outputStream.close();
                    clientSocket.close();

                }catch (SocketException e){
                    System.out.println("Client disconnected");
                }catch (IOException |SQLException e) {
                    e.getMessage();
                }
            }
        }

        private List<String> classStats(int courseId, Connection conn) throws SQLException {
            List<String> data = new ArrayList<>();
            data.add("Average: " + getClassAvg(courseId, conn));
            data.add("Median: " + getClassMedian(courseId, conn));
            data.add("Highest: " + getClassHighest(courseId, conn));
            data.add("Lowest: " + getClassLowest(courseId, conn));
            return data;
        }
        private boolean isAccessible(int studentId,int courseId,Connection connection) throws SQLException {
            List<Course> courses = getCoursesForStudent(studentId,connection);
            for (Course course : courses){
                if(course.getId() == courseId)
                    return true;
            }
            return false;
        }
    private List<String> viewMarks(int id, Connection conn) throws SQLException {
        List<Enrollment> enrollments = getEnrollmentsForStudent(id, conn);
        List<String> result = new ArrayList<>();
        for (Enrollment enrollment : enrollments){
            StringBuilder sb = new StringBuilder();
            String courseName = getCourseById(enrollment.getCourse_id(),conn).getCourse_name();
            sb.append("ID:").append(enrollment.getCourse_id()).append(" ").append(courseName).append(": ").append(enrollment.getMark());
            result.add(sb.toString());
        }
        return result;
    }
    private Student verifyCredentials(String username, String password,Connection conn) {
        try {
            String query = "SELECT * FROM users WHERE username='" + username + "' AND password='" + password + "'";
            Statement statement = conn.createStatement();
            ResultSet result = statement.executeQuery(query);
            if(result.next())
                return new Student(result.getInt(1),result.getString(2),result.getString(3));
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    private List<Student> getStudentsById(int studentId, Connection connection) throws SQLException {
        List<Student> result = new ArrayList<>();
        String query = "SELECT * FROM users WHERE id=" + studentId;
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next())
            result.add(new Student(resultSet.getInt(1),resultSet.getString(2),resultSet.getString(3)));
        return result;
    }
    private Enrollment getEnrollmentById(int id,Connection conn) throws SQLException {
        String query = "SELECT * FROM enrollment WHERE id=" + id;
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        if(resultSet.next())
            return new Enrollment(id,resultSet.getInt(2),resultSet.getInt(3),resultSet.getDouble(4));
        return null;
    }
    private Course getCourseById(int id,Connection conn) throws SQLException {
        String query = "SELECT * FROM courses WHERE id=" + id;
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        if(resultSet.next())
            return new Course(id,resultSet.getString(2),resultSet.getString(3),resultSet.getString(4));
        return null;
    }
    private List<Enrollment> getEnrollmentsForStudent(int studentId, Connection connection) throws SQLException {
        List<Enrollment> result = new ArrayList<>();
        String query = "SELECT * FROM enrollment WHERE user_id=" + studentId;
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next())
            result.add(new Enrollment(resultSet.getInt(1),resultSet.getInt(2),resultSet.getInt(3),resultSet.getDouble(4)));
        return result;
    }

    private List<Course> getCoursesForStudent(int studentId, Connection connection) throws SQLException {
        List<Enrollment> enrollments = getEnrollmentsForStudent(studentId,connection);
        List<Course> result = new ArrayList<>();
        for(Enrollment enrollment : enrollments){
            String query = "SELECT * FROM courses WHERE id=" + enrollment.getCourse_id();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next())
                result.add(new Course(resultSet.getInt(1),resultSet.getString(2),resultSet.getString(3),resultSet.getString(4)));
        }
        return result;
    }

    public float getClassAvg(int courseId, Connection connection) throws SQLException {
        String query = "SELECT AVG(mark) FROM enrollment WHERE course_id=" + courseId;
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        if (resultSet.next()) {
            return resultSet.getFloat(1);
        } else {
            return -1;
        }
    }

    public float getClassMedian(int courseId, Connection connection) throws SQLException {
        try {
            String query = "SELECT COUNT(*) FROM enrollment WHERE course_id=" + courseId;
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count % 2 == 0) {
                    // Even number of marks, need to average middle two
                    query = "SELECT mark FROM enrollment WHERE course_id=" + courseId + " ORDER BY mark LIMIT " + (count/2 - 1) + ", 2";
                    resultSet = statement.executeQuery(query);
                    if (resultSet.next()) {
                        float mark1 = resultSet.getFloat(1);
                        if (resultSet.next()) {
                            float mark2 = resultSet.getFloat(1);
                            return (mark1 + mark2) / 2;
                        } else {
                            return -1;
                        }
                    } else {
                        return -1;
                    }
                } else {
                    // Odd number of marks, need to find middle one
                    query = "SELECT mark FROM enrollment WHERE course_id=" + courseId + " ORDER BY mark LIMIT " + (count/2) + ", 1";
                    resultSet = statement.executeQuery(query);
                    if (resultSet.next()) {
                        return resultSet.getFloat(1);
                    } else {
                        return -1;
                    }
                }
            } else {
                return -1;
            }
        }catch (SQLException e){
            return 0;
        }

    }

    public float getClassHighest(int courseId, Connection connection) throws SQLException {
        String query = "SELECT MAX(mark) FROM enrollment WHERE course_id=" + courseId;
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        if (resultSet.next()) {
            return resultSet.getFloat(1);
        } else {
            return -1;
        }
    }

    public float getClassLowest(int courseId, Connection connection) throws SQLException {
        String query = "SELECT MIN(mark) FROM enrollment WHERE course_id=" + courseId;
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        if (resultSet.next()) {
            return resultSet.getFloat(1);
        } else {
            return -1;
        }
    }

}

