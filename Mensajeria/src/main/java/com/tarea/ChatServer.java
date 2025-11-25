package com.tarea;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 5000;
    // Lista de clientes conectados: Map<ConversationID, List<ClientWriter>>
    private static Map<Integer, List<PrintWriter>> activeChats = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Servidor iniciado en puerto " + PORT);
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                pool.execute(new ClientHandler(serverSocket.accept()));
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private int currentChatId = -1;

        public ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String line;
                while ((line = in.readLine()) != null) {
                    // Protocolo simple: TIPO|DATA1|DATA2
                    String[] parts = line.split("\\|", 3);
                    String type = parts[0];

                    if (type.equals("JOIN")) {
                        int chatId = Integer.parseInt(parts[1]);
                        currentChatId = chatId;
                        activeChats.computeIfAbsent(chatId, k -> new ArrayList<>()).add(out);
                        System.out.println("Usuario unido al chat: " + chatId);
                    } else if (type.equals("MSG")) {
                        // MSG|USER_ID|CONTENT
                        int userId = Integer.parseInt(parts[1]);
                        String content = parts[2];

                        if (currentChatId != -1) {
                            // 1. Guardar en BD (cifrado)
                            DatabaseManager.saveMessage(currentChatId, userId, content);
                            // 2. Retransmitir a clientes conectados (en texto plano, ya que la conexión socket es "local" para la tarea)
                            broadcast(currentChatId, "MSG|" + userId + "|" + content);
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
            finally {
                if (currentChatId != -1 && activeChats.containsKey(currentChatId)) {
                    activeChats.get(currentChatId).remove(out);
                }
            }
        }

        private void broadcast(int chatId, String msg) {
            List<PrintWriter> clients = activeChats.get(chatId);
            if (clients != null) {
                for (PrintWriter writer : clients) {
                    writer.println(msg);
                }
            }
        }
    }
}