package com.sanjay.workspace;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainApp extends Application {

    private VBox chatBox;
    private VBox libraryBox;
    private TextField inputField;
    private ScrollPane scrollPane;

    private final File historyFile = new File("history.txt");

    private final String DARK_BG = "#0d0d0d";

    private final String PANEL_STYLE = """
        -fx-background-color: rgba(40, 40, 40, 0.6);
        -fx-background-radius: 25;
        -fx-border-color: rgba(255,255,255,0.08);
        -fx-border-radius: 25;
        -fx-effect: dropshadow(gaussian, rgba(0,150,255,0.25), 25, 0.3, 0, 0);
    """;

    @Override
    public void start(Stage stage) {

        VBox root = new VBox();
        root.setStyle("-fx-background-color: " + DARK_BG + ";");

        // ===== HEADER =====
        HBox header = new HBox();
        header.setPadding(new Insets(15, 40, 10, 40));
        header.setAlignment(Pos.CENTER_LEFT);

        Label appTitle = new Label("SANJAY-SAN'S AI");
        appTitle.setStyle("""
            -fx-font-size: 22px;
            -fx-font-weight: bold;
            -fx-text-fill: linear-gradient(to right, #00aaff, #0066ff);
        """);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button installBtn = new Button("Installation Guide");
        installBtn.setStyle("""
            -fx-background-color: #222;
            -fx-text-fill: white;
            -fx-background-radius: 20;
            -fx-border-color: #444;
            -fx-border-radius: 20;
            -fx-cursor: hand;
        """);
        installBtn.setOnAction(e -> showInstallationGuide());

        header.getChildren().addAll(appTitle, spacer, installBtn);

        // ===== LEFT SIDEBAR =====
        libraryBox = new VBox(10);
        libraryBox.setPadding(new Insets(20));
        libraryBox.setPrefWidth(220);
        libraryBox.setStyle(PANEL_STYLE);

        Label libraryTitle = new Label("Library");
        libraryTitle.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:white;");
        libraryBox.getChildren().add(libraryTitle);

        loadHistory();

        // ===== CHAT AREA =====
        chatBox = new VBox(15);
        chatBox.setPadding(new Insets(15));

        scrollPane = new ScrollPane(chatBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox conversationBox = new VBox(10);
        conversationBox.setPadding(new Insets(20));
        conversationBox.setStyle(PANEL_STYLE);

        Label convoTitle = new Label("Conversation");
        convoTitle.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:white;");

        conversationBox.getChildren().addAll(convoTitle, scrollPane);

        VBox.setVgrow(conversationBox, Priority.ALWAYS);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox mainArea = new HBox(20, libraryBox, conversationBox);
        mainArea.setPadding(new Insets(20));
        HBox.setHgrow(conversationBox, Priority.ALWAYS);
        VBox.setVgrow(mainArea, Priority.ALWAYS);

        // ===== INPUT BAR =====
        HBox inputBar = createFloatingInput();

        root.getChildren().addAll(header, mainArea, inputBar);
        VBox.setVgrow(mainArea, Priority.ALWAYS);

        Scene scene = new Scene(root, 1100, 750);

        stage.setTitle("SANJAY-SAN'S AI");
        stage.setScene(scene);
        stage.show();
    }

    // ===== INSTALLATION POPUP =====

    private void showInstallationGuide() {

        Stage popup = new Stage();
        popup.setTitle("Installation Guide");

        TextArea guide = new TextArea();
        guide.setEditable(false);
        guide.setWrapText(true);
        guide.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: white;");

        guide.setText("""
INSTALLATION GUIDE

1. Install Java (JDK 17 or above)
   https://adoptium.net/

2. Install Ollama
   https://ollama.com

3. Install DeepSeek model:
   Open CMD and run:
   ollama pull deepseek-coder:1.3b

4. Make sure Ollama is running:
   ollama serve

5. Launch SANJAY-SAN'S AI

Enjoy your local AI assistant 🚀
""");

        Scene scene = new Scene(new VBox(guide), 500, 400);
        popup.setScene(scene);
        popup.show();
    }

    // ===== INPUT BAR =====

    private HBox createFloatingInput() {

        HBox container = new HBox();
        container.setPadding(new Insets(0, 40, 30, 40));
        container.setAlignment(Pos.CENTER);

        HBox wrapper = new HBox(10);
        wrapper.setPadding(new Insets(10, 20, 10, 20));
        wrapper.setStyle("""
            -fx-background-color: #1a1a1a;
            -fx-background-radius: 30;
            -fx-border-color: #333;
            -fx-border-radius: 30;
        """);
        HBox.setHgrow(wrapper, Priority.ALWAYS);

        inputField = new TextField();
        inputField.setPromptText("Ask something...");
        inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");
        inputField.setOnAction(e -> sendMessage());
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button send = new Button("Ask AI");
        send.setStyle("""
            -fx-background-color: linear-gradient(to right, #0066ff, #00aaff);
            -fx-text-fill: white;
            -fx-background-radius: 20;
        """);
        send.setOnAction(e -> sendMessage());

        wrapper.getChildren().addAll(inputField, send);
        container.getChildren().add(wrapper);

        return container;
    }

    // ===== CHAT =====

    private void sendMessage() {

        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        addMessage(text, true);
        storeHistory(text);
        inputField.clear();

        Label thinking = addMessage("Thinking...", false);

        new Thread(() -> {
            try {
                streamFromOllama(text, thinking);
            } catch (Exception ex) {
                Platform.runLater(() ->
                        thinking.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    private Label addMessage(String text, boolean isUser) {

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(450);
        msg.setPadding(new Insets(12));
        msg.setTextFill(Color.WHITE);

        if (isUser) {
            msg.setStyle("""
                -fx-background-color: linear-gradient(to right, #0066ff, #00aaff);
                -fx-background-radius: 18 18 4 18;
            """);
        } else {
            msg.setStyle("""
                -fx-background-color: #2b2b2b;
                -fx-background-radius: 18 18 18 4;
            """);
        }

        HBox wrapper = new HBox(msg);
        wrapper.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        FadeTransition fade = new FadeTransition(Duration.millis(200), wrapper);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        Platform.runLater(() -> {
            chatBox.getChildren().add(wrapper);
            scrollPane.setVvalue(1.0);
        });

        return msg;
    }

    // ===== HISTORY =====

    private void storeHistory(String message) {
        try (FileWriter fw = new FileWriter(historyFile, true)) {
            fw.write(message + "\n");
        } catch (IOException ignored) {}

        Label item = new Label("• " + message);
        item.setStyle("-fx-text-fill: #888;");
        libraryBox.getChildren().add(item);
    }

    private void loadHistory() {
        if (!historyFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(historyFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                Label item = new Label("• " + line);
                item.setStyle("-fx-text-fill: #888;");
                libraryBox.getChildren().add(item);
            }
        } catch (IOException ignored) {}
    }

    // ===== STREAM =====

    private void streamFromOllama(String prompt, Label label) throws Exception {

        URL url = new URL("http://localhost:11434/api/generate");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String json = """
            {
              "model": "deepseek-coder:1.3b",
              "prompt": "%s",
              "stream": true
            }
        """.formatted(prompt.replace("\"", "\\\""));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));

        StringBuilder full = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.contains("\"response\"")) {
                String part = line.split("\"response\":\"")[1]
                        .split("\"")[0]
                        .replace("\\n", "\n");

                full.append(part);
                Platform.runLater(() -> label.setText(full.toString()));
            }
        }

        reader.close();
    }

    public static void main(String[] args) {
        launch();
    }
}