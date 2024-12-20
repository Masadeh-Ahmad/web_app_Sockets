package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;


public class Main {
    public static void main(String[] args) throws IOException {
        Server server1 = new Server();
        server1.start();
    }
}