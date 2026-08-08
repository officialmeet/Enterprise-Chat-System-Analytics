package com.chat;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 5000;
    private static final String CHAT_LOG_CSV = "chat_logs.csv";
    
    private static final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  ENTERPRISE MULTI-THREADED CHAT SERVER (v2.0)  ");
        System.out.println("=================================================");
        System.out.println("Status: ONLINE | Listening on Port: " + PORT);
        System.out.println("Logs: Persisting real-time transcripts to " + CHAT_LOG_CSV);
        System.out.println("-------------------------------------------------");

        initializeCsvHeader();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Server Exception: " + e.getMessage());
        }
    }

    private static void initializeCsvHeader() {
        File file = new File(CHAT_LOG_CSV);
        if (!file.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(CHAT_LOG_CSV, true))) {
                pw.println("timestamp,channel,message_type,sender,recipient,message");
            } catch (IOException e) {
                System.err.println("Failed to initialize CSV header: " + e.getMessage());
            }
        }
    }

    public static synchronized void logToCsv(String channel, String type, String sender, String recipient, String message) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CHAT_LOG_CSV, true))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String escapedMsg = message.replace("\"", "\"\"");
            pw.printf("%s,%s,%s,%s,%s,\"%s\"%n", timestamp, channel, type, sender, recipient, escapedMsg);
        } catch (IOException e) {
            System.err.println("CSV Logging Error: " + e.getMessage());
        }
    }

    public static void broadcast(String message, String currentChannel, ClientHandler sender) {
        for (ClientHandler client : activeClients.values()) {
            if (client != sender && client.getChannel().equalsIgnoreCase(currentChannel)) {
                client.sendMessage(message);
            }
        }
    }

    public static boolean sendPrivateMessage(String senderUsername, String targetUsername, String message) {
        ClientHandler recipient = activeClients.get(targetUsername.toLowerCase());
        if (recipient != null) {
            recipient.sendMessage(String.format("[DM from %s]: %s", senderUsername, message));
            logToCsv("DIRECT_MESSAGE", "DM", senderUsername, targetUsername, message);
            return true;
        }
        return false;
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private String currentChannel = "general";

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getChannel() { return currentChannel; }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("SYSTEM_ENTER_USERNAME");
                while (true) {
                    username = in.readLine();
                    if (username == null) return;
                    
                    String key = username.toLowerCase().trim();
                    if (key.isEmpty() || activeClients.containsKey(key)) {
                        out.println("SYSTEM_USERNAME_TAKEN");
                    } else {
                        activeClients.put(key, this);
                        out.println("SYSTEM_AUTH_SUCCESS");
                        break;
                    }
                }

                System.out.printf("[%s] Connected from %s%n", username, socket.getRemoteSocketAddress());
                broadcast(String.format(">>> %s joined #%s", username, currentChannel), currentChannel, this);
                logToCsv(currentChannel, "SYSTEM", "SERVER", "ALL", username + " joined");

                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.equalsIgnoreCase("/exit")) {
                        break;
                    } else if (line.equalsIgnoreCase("/users")) {
                        out.println("Active Users: " + String.join(", ", activeClients.keySet()));
                    } else if (line.startsWith("/join ")) {
                        String newChannel = line.substring(6).replace("#", "").trim();
                        if (!newChannel.isEmpty()) {
                            broadcast(String.format("<<< %s left #%s", username, currentChannel), currentChannel, this);
                            this.currentChannel = newChannel;
                            out.println("System: Switched to channel #" + currentChannel);
                            broadcast(String.format(">>> %s joined #%s", username, currentChannel), currentChannel, this);
                        }
                    } else if (line.startsWith("/msg ")) {
                        String[] parts = line.split(" ", 3);
                        if (parts.length >= 3) {
                            String targetUser = parts[1];
                            String privateMsg = parts[2];
                            boolean sent = sendPrivateMessage(username, targetUser, privateMsg);
                            if (sent) {
                                out.println(String.format("[DM to %s]: %s", targetUser, privateMsg));
                            } else {
                                out.println("System Error: User '" + targetUser + "' is offline or not found.");
                            }
                        } else {
                            out.println("System: Usage: /msg <username> <message>");
                        }
                    } else {
                        String formattedMessage = String.format("[%s @ #%s]: %s", username, currentChannel, line);
                        broadcast(formattedMessage, currentChannel, this);
                        logToCsv(currentChannel, "GLOBAL", username, "ALL", line);
                    }
                }
            } catch (IOException e) {
                System.err.println("Handler Error (" + username + "): " + e.getMessage());
            } finally {
                if (username != null) {
                    activeClients.remove(username.toLowerCase());
                    broadcast(String.format("<<< %s disconnected", username), currentChannel, this);
                    logToCsv(currentChannel, "SYSTEM", "SERVER", "ALL", username + " disconnected");
                    System.out.printf("[%s] Disconnected.%n", username);
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        public void sendMessage(String msg) {
            out.println(msg);
        }
    }
}