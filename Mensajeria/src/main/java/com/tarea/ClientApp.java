package com.tarea;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Properties; // Importante

public class ClientApp extends Application {
    private Stage primaryStage;
    private int myUserId = -1;
    private String myUsername = "";
    private int currentChatId = -1;
    private PrintWriter serverOut;
    private VBox messageContainer;
    private ScrollPane scrollPane;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.setTitle("Sistema de Mensajería Segura");
        showWelcomeScene();
        this.primaryStage.show();
    }

    // --- NUEVO MÉTODO PARA LEER IP ---
    private String getServerIP() {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream("server.properties")) {
            prop.load(input);
            return prop.getProperty("server.ip", "localhost");
        } catch (IOException ex) {
            return "localhost";
        }
    }

    private void showWelcomeScene() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        Label title = new Label("Bienvenido a SecureChat");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        Button btnGoToRegister = new Button("1. Crear Usuario Nuevo");
        Button btnGoToLogin = new Button("2. Iniciar Sesión");
        btnGoToRegister.setPrefWidth(200);
        btnGoToLogin.setPrefWidth(200);
        btnGoToRegister.setOnAction(e -> showRegisterScene());
        btnGoToLogin.setOnAction(e -> showLoginScene());
        layout.getChildren().addAll(title, btnGoToRegister, btnGoToLogin);
        primaryStage.setScene(new Scene(layout, 400, 400));
    }

    private void showRegisterScene() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        Label title = new Label("Registro de Usuario");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        TextField userField = new TextField();
        userField.setPromptText("Elige tu nombre de usuario");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Crea una contraseña");
        Button btnRegister = new Button("Registrar");
        Button btnBack = new Button("Volver al Menú");
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: blue;");
        btnRegister.setOnAction(e -> {
            String u = userField.getText();
            String p = passField.getText();
            if(u.isEmpty() || p.isEmpty()){
                statusLabel.setText("Llena todos los campos.");
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }
            int id = DatabaseManager.registerUser(u, p);
            if (id > 0) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Usuario creado. ID: " + id);
                alert.showAndWait();
                showWelcomeScene();
            } else if (id == -2) {
                statusLabel.setText("El usuario ya existe.");
                statusLabel.setStyle("-fx-text-fill: red;");
            } else {
                statusLabel.setText("Error BD o Conexión.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        });
        btnBack.setOnAction(e -> showWelcomeScene());
        layout.getChildren().addAll(title, userField, passField, btnRegister, statusLabel, new Separator(), btnBack);
        primaryStage.setScene(new Scene(layout, 400, 400));
    }

    private void showLoginScene() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        Label title = new Label("Iniciar Sesión");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        TextField userField = new TextField();
        userField.setPromptText("Tu Usuario");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Tu Contraseña");
        Button btnLogin = new Button("Entrar");
        Button btnBack = new Button("Volver al Menú");
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: red;");
        btnLogin.setOnAction(e -> {
            String u = userField.getText();
            String p = passField.getText();
            int id = DatabaseManager.loginUser(u, p);
            if (id != -1) {
                myUserId = id;
                myUsername = u;
                showChatSelectionScene();
            } else {
                statusLabel.setText("Datos incorrectos o Error Conexión.");
            }
        });
        btnBack.setOnAction(e -> showWelcomeScene());
        layout.getChildren().addAll(title, userField, passField, btnLogin, statusLabel, new Separator(), btnBack);
        primaryStage.setScene(new Scene(layout, 400, 400));
    }

    private void showChatSelectionScene() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        Label welcome = new Label("Hola, " + myUsername);
        welcome.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        VBox boxCreate = new VBox(10);
        boxCreate.setStyle("-fx-border-color: #ccc; -fx-padding: 10; -fx-border-radius: 5;");
        Label lblCreate = new Label("1. Crear Nueva Conversación");
        TextField chatNameField = new TextField();
        chatNameField.setPromptText("Nombre de la Sala");
        PasswordField chatPassCreateField = new PasswordField();
        chatPassCreateField.setPromptText("Define una Contraseña");
        Button btnCreate = new Button("Crear Sala");
        VBox boxJoin = new VBox(10);
        boxJoin.setStyle("-fx-border-color: #ccc; -fx-padding: 10; -fx-border-radius: 5;");
        Label lblJoin = new Label("2. Unirse a Conversación");
        TextField chatIdentifierField = new TextField();
        chatIdentifierField.setPromptText("ID o Nombre de la Sala");
        PasswordField chatPassJoinField = new PasswordField();
        chatPassJoinField.setPromptText("Contraseña de la sala");
        Button btnJoin = new Button("Entrar");
        Label statusLabel = new Label();
        btnCreate.setOnAction(e -> {
            String name = chatNameField.getText();
            String pass = chatPassCreateField.getText();
            if(!name.isEmpty() && !pass.isEmpty()){
                int newId = DatabaseManager.createConversation(name, pass);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Sala Creada.\nID: " + newId + "\nPass: " + pass);
                alert.showAndWait();
                chatNameField.clear();
                chatPassCreateField.clear();
            } else {
                statusLabel.setText("Faltan datos.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        });
        btnJoin.setOnAction(e -> {
            String identifier = chatIdentifierField.getText();
            String pass = chatPassJoinField.getText();
            if (!identifier.isEmpty() && !pass.isEmpty()) {
                int validId = DatabaseManager.joinConversation(identifier, pass);
                if (validId != -1) {
                    currentChatId = validId;
                    showChatScene();
                } else {
                    statusLabel.setText("Sala no existe o pass incorrecta.");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }
            } else {
                statusLabel.setText("Faltan datos.");
            }
        });
        Button btnLogout = new Button("Cerrar Sesión");
        btnLogout.setOnAction(e -> {
            myUserId = -1;
            showWelcomeScene();
        });
        boxCreate.getChildren().addAll(lblCreate, chatNameField, chatPassCreateField, btnCreate);
        boxJoin.getChildren().addAll(lblJoin, chatIdentifierField, chatPassJoinField, btnJoin);
        layout.getChildren().addAll(welcome, boxCreate, boxJoin, statusLabel, new Separator(), btnLogout);
        primaryStage.setScene(new Scene(layout, 400, 600));
    }

    private void showChatScene() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #f0f2f5;");
        String chatName = DatabaseManager.getConversationName(currentChatId);
        Label lblName = new Label(chatName);
        lblName.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblName.setStyle("-fx-text-fill: #2c3e50;");
        Label lblId = new Label("ID Sala: " + currentChatId);
        lblId.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblId.setStyle("-fx-text-fill: #7f8c8d; -fx-background-color: #ecf0f1; -fx-padding: 5 10; -fx-background-radius: 10;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerBox = new HBox(10, lblName, spacer, lblId);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        messageContainer = new VBox(10);
        messageContainer.setPadding(new Insets(10));
        messageContainer.setStyle("-fx-background-color: transparent;");
        scrollPane = new ScrollPane(messageContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        List<DatabaseManager.ChatMessage> history = DatabaseManager.getMessages(currentChatId);
        for (DatabaseManager.ChatMessage msg : history) {
            boolean isMe = (msg.userId == myUserId);
            addMessageBubble(msg.username, msg.content, isMe);
        }
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
        connectToServer();
        TextField msgField = new TextField();
        msgField.setPromptText("Escribe un mensaje...");
        msgField.setStyle("-fx-background-radius: 20; -fx-padding: 10;");
        HBox.setHgrow(msgField, Priority.ALWAYS);
        Button btnSend = new Button("Enviar");
        btnSend.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 10 20; -fx-font-weight: bold;");
        Button btnBack = new Button("Salir");
        btnBack.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
        // Acción Enviar (CORREGIDA)
        Runnable sendAction = () -> {
            String text = msgField.getText();

            // 1. Validar longitud máxima
            if (text.length() > 50) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Máximo 50 caracteres.");
                alert.show();
            }
            // 2. Validar que no sean solo espacios (USANDO TRIM)
            // text.trim().isEmpty() elimina espacios y verifica si quedó algo
            else if (!text.trim().isEmpty()) {

                // Enviar al server
                if (serverOut != null) serverOut.println("MSG|" + myUserId + "|" + text);

                // Agregar visualmente mi mensaje (usando el texto original o el trim según prefieras)
                // Aquí usamos 'text' normal para respetar si el usuario quiso poner espacios entre palabras
                addMessageBubble("Yo", text, true);

                msgField.clear();
                Platform.runLater(() -> scrollPane.setVvalue(1.0));
            }
            // Si es solo espacios, no entra al IF y no hace nada (simplemente ignora el clic)
        };

        btnSend.setOnAction(e -> sendAction.run());
        msgField.setOnAction(e -> sendAction.run());
        btnBack.setOnAction(e -> {
            try { if(serverOut!=null) serverOut.close(); } catch(Exception ex){}
            showChatSelectionScene();
        });
        HBox inputBox = new HBox(10, msgField, btnSend);
        inputBox.setPadding(new Insets(10, 0, 0, 0));
        layout.getChildren().addAll(headerBox, new Separator(), scrollPane, inputBox, btnBack);
        primaryStage.setScene(new Scene(layout, 420, 600));
        primaryStage.setOnCloseRequest(e -> System.exit(0));
    }

    private void addMessageBubble(String senderName, String text, boolean isMe) {
        VBox bubble = new VBox(2);
        bubble.setMaxWidth(280);
        bubble.setPadding(new Insets(10));
        if (isMe) {
            bubble.setStyle("-fx-background-color: #dcf8c6; -fx-background-radius: 10 10 0 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);");
        } else {
            bubble.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10 10 10 0; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);");
        }
        if (!isMe) {
            Label lblUser = new Label(senderName);
            lblUser.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            lblUser.setStyle("-fx-text-fill: #e67e22;");
            bubble.getChildren().add(lblUser);
        }
        Label lblText = new Label(text);
        lblText.setWrapText(true);
        lblText.setStyle("-fx-text-fill: black; -fx-font-size: 13;");
        bubble.getChildren().add(lblText);
        HBox row = new HBox();
        if (isMe) {
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
        }
        row.getChildren().add(bubble); // Corrección en lineA

        Platform.runLater(() -> messageContainer.getChildren().add(row));
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                // USA LA IP DEL ARCHIVO
                String ip = getServerIP();
                Socket socket = new Socket(ip, 5000);
                serverOut = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                serverOut.println("JOIN|" + currentChatId);
                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split("\\|", 3);
                    if (parts[0].equals("MSG")) {
                        int senderId = Integer.parseInt(parts[1]);
                        String content = parts[2];
                        if (senderId != myUserId) {
                            // 1. Buscamos el nombre real en la BD antes de mostrarlo
                            String realName = DatabaseManager.getUsername(senderId);

                            // 2. Lo mostramos con el nombre bonito
                            addMessageBubble(realName, content, false);

                            Platform.runLater(() -> scrollPane.setVvalue(1.0));
                        }
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    Label err = new Label("[No conectado al Servidor]");
                    err.setStyle("-fx-text-fill: red; font-weight: bold;");
                    messageContainer.getChildren().add(err);
                });
            }
        }).start();
    }
}