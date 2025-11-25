package com.tarea;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseManager {
    private static final String USER = "administrador";
    private static final String PASS = "administrador";

    // Método para leer IP (Igual que antes)
    private static String getServerIP() {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream("server.properties")) {
            prop.load(input);
            return prop.getProperty("server.ip", "localhost");
        } catch (IOException ex) {
            return "localhost";
        }
    }

    public static Connection getConnection() throws SQLException {
        String ip = getServerIP();
        String url = "jdbc:mariadb://" + ip + ":3306/chat_secure_db";
        return DriverManager.getConnection(url, USER, PASS);
    }

    // --- USUARIOS (MODIFICADO: Cifrar Contraseña) ---
    public static int registerUser(String username, String plainPassword) {
        if (userExists(username)) return -2;

        try {
            // 1. Ciframos la contraseña antes de guardarla
            String[] cryptoData = AESUtil.encrypt(plainPassword);
            String encryptedPass = cryptoData[0];
            String iv = cryptoData[1];

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO users (username, password, password_iv) VALUES (?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, username);
                ps.setString(2, encryptedPass); // Guardamos lo cifrado
                ps.setString(3, iv);            // Guardamos el IV

                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    private static boolean userExists(String username) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    // --- LOGIN (MODIFICADO: Descifrar y Comparar) ---
    public static int loginUser(String username, String inputPassword) {
        // 1. Buscamos al usuario SOLO por nombre
        String sql = "SELECT id, password, password_iv FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String storedEncryptedPass = rs.getString("password");
                String storedIv = rs.getString("password_iv");

                try {
                    // 2. Desciframos la contraseña de la BD
                    String decryptedStorePass = AESUtil.decrypt(storedEncryptedPass, storedIv);

                    // 3. Comparamos con la que escribió el usuario
                    if (decryptedStorePass.equals(inputPassword)) {
                        return id; // ¡Éxito!
                    }
                } catch (Exception e) {
                    System.out.println("Error al descifrar password: " + e.getMessage());
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1; // Falló
    }

    // --- CONVERSACIONES (MODIFICADO: Cifrar Contraseña de Sala) ---

    public static int createConversation(String name, String plainChatPassword) {
        try {
            // Ciframos la pass de la sala
            String[] cryptoData = AESUtil.encrypt(plainChatPassword);
            String encryptedPass = cryptoData[0];
            String iv = cryptoData[1];

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO conversations (name, password, password_iv) VALUES (?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, name);
                ps.setString(2, encryptedPass);
                ps.setString(3, iv);

                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public static int joinConversation(String identifier, String inputChatPassword) {
        // Buscamos la sala por ID o Nombre
        String sql = "SELECT id, password, password_iv FROM conversations WHERE name = ? OR id = ?";
        int idSearch = -1;
        try { idSearch = Integer.parseInt(identifier); } catch (NumberFormatException e) {}

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, String.valueOf(idSearch));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int dbId = rs.getInt("id");
                String storedEncryptedPass = rs.getString("password");
                String storedIv = rs.getString("password_iv");

                try {
                    // Desciframos y comparamos
                    String decryptedPass = AESUtil.decrypt(storedEncryptedPass, storedIv);
                    if (decryptedPass.equals(inputChatPassword)) {
                        return dbId;
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public static String getConversationName(int chatId) {
        String sql = "SELECT name FROM conversations WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, chatId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Desconocido";
    }

    // --- MENSAJES (Sigue igual, ya usaba cifrado) ---

    public static void saveMessage(int chatId, int userId, String content) {
        try {
            String[] encryptedData = AESUtil.encrypt(content);
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO messages (conversation_id, user_id, encrypted_content, iv) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, chatId);
                ps.setInt(2, userId);
                ps.setString(3, encryptedData[0]);
                ps.setString(4, encryptedData[1]);
                ps.executeUpdate();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static List<ChatMessage> getMessages(int chatId) {
        List<ChatMessage> messages = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT u.username, m.user_id, m.encrypted_content, m.iv FROM messages m JOIN users u ON m.user_id = u.id WHERE m.conversation_id = ? ORDER BY m.timestamp ASC")) {
            ps.setInt(1, chatId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String user = rs.getString("username");
                int userId = rs.getInt("user_id");
                String enc = rs.getString("encrypted_content");
                String iv = rs.getString("iv");
                String decryptedContent;
                try {
                    decryptedContent = AESUtil.decrypt(enc, iv);
                } catch (Exception ex) {
                    decryptedContent = "[Error Descifrando]";
                }
                messages.add(new ChatMessage(user, decryptedContent, userId));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return messages;
    }

    public static class ChatMessage {
        public String username;
        public String content;
        public int userId;
        public ChatMessage(String username, String content, int userId) {
            this.username = username;
            this.content = content;
            this.userId = userId;
        }
    }

    public static String getUsername(int userId) {
        String sql = "SELECT username FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "User " + userId;
    }
}