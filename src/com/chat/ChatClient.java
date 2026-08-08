package com.chat;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            if ("SYSTEM_ENTER_USERNAME".equals(in.readLine())) {
                while (true) {
                    System.out.print("Enter your username: ");
                    String username = scanner.nextLine().trim();
                    out.println(username);

                    String response = in.readLine();
                    if ("SYSTEM_AUTH_SUCCESS".equals(response)) {
                        System.out.println("\n=================================================");
                        System.out.println("        CONNECTED TO ENTERPRISE CHAT HUB         ");
                        System.out.println("=================================================");
                        System.out.println("Available Commands:");
                        System.out.println("  /msg <user> <msg>  : Send a private direct message");
                        System.out.println("  /join <channel>    : Switch channel (e.g. /join datascience)");
                        System.out.println("  /users             : List online users");
                        System.out.println("  /exit              : Leave the chat");
                        System.out.println("-------------------------------------------------\n");
                        break;
                    } else if ("SYSTEM_USERNAME_TAKEN".equals(response)) {
                        System.out.println("Error: Username taken or invalid. Please try another.");
                    }
                }
            }

            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("\n[Disconnected from server]");
                }
            }).start();

            while (true) {
                String userInput = scanner.nextLine();
                if (userInput.trim().equalsIgnoreCase("/exit")) {
                    out.println("/exit");
                    break;
                }
                out.println(userInput);
            }

            socket.close();
            scanner.close();
            System.exit(0);

        } catch (IOException e) {
            System.err.println("Could not connect to Chat Server: " + e.getMessage());
        }
    }
}