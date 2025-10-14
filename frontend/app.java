package com.smilereward;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.smilereward.service.WebcamService;
import com.smilereward.service.ApiService;
import com.smilereward.model.DetectionResult;
import java.io.File;
import java.util.Optional;

/**
 * Main JavaFX Application for Smile Recognition System
 * Provides webcam capture, image upload, and real-time smile detection
 */
public class SmileRecognitionApp extends Application {

    private WebcamService webcamService;
    private ApiService apiService;
    private ImageView imageView;
    private Label statusLabel;
    private Label scoreLabel;
    private Label streakLabel;
    private ProgressBar confidenceBar;
    private Button startWebcamBtn;
    private Button stopWebcamBtn;
    private Button captureBtn;
    private Button uploadBtn;
    private VBox rewardPanel;
    
    private String currentUser = "guest";
    private int totalScore = 0;
    private int currentStreak = 0;

    @Override
    public void start(Stage primaryStage) {
        // Initialize services
        apiService = new ApiService("http://localhost:8000");
        webcamService = new WebcamService(apiService, this::onDetectionResult);

        // Create main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Top: Header
        root.setTop(createHeader());

        // Center: Camera view
        root.setCenter(createCameraView());

        // Right: Stats and rewards panel
        root.setRight(createStatsPanel());

        // Bottom: Controls
        root.setBottom(createControlPanel());

        // Create scene
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        primaryStage.setTitle("Smile Recognition & Reward System");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> cleanup());
        primaryStage.show();

        // Show login dialog
        showLoginDialog();
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 15, 0));

        Label title = new Label("🎯 Smile Recognition & Reward System");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label subtitle = new Label("Smile to earn rewards and track your happiness!");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        statusLabel = new Label("Status: Ready");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");

        header.getChildren().addAll(title, subtitle, statusLabel);
        return header;
    }

    private VBox createCameraView() {
        VBox cameraBox = new VBox(10);
        cameraBox.setAlignment(Pos.CENTER);
        cameraBox.setPadding(new Insets(10));
        cameraBox.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        // Image view for camera/uploaded images
        imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");

        // Placeholder
        Label placeholder = new Label("📷 Camera feed will appear here");
        placeholder.setStyle("-fx-font-size: 18px; -fx-text-fill: #95a5a6;");
        
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(640, 480);
        imageContainer.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 10;");
        imageContainer.getChildren().addAll(placeholder, imageView);

        // Confidence bar
        confidenceBar = new ProgressBar(0);
        confidenceBar.setPrefWidth(640);
        confidenceBar.setStyle("-fx-accent: #2ecc71;");

        Label confidenceLabel = new Label("Smile Confidence: 0%");
        confidenceLabel.setStyle("-fx-font-size: 12px;");

        cameraBox.getChildren().addAll(imageContainer, confidenceBar, confidenceLabel);
        return cameraBox;
    }

    private VBox createStatsPanel() {
        VBox statsPanel = new VBox(15);
        statsPanel.setPadding(new Insets(10, 0, 10, 15));
        statsPanel.setPrefWidth(300);
        statsPanel.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        // User info
        Label userLabel = new Label("👤 User: " + currentUser);
        userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Score
        scoreLabel = new Label("🏆 Total Score: 0");
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #f39c12;");

        // Streak
        streakLabel = new Label("🔥 Current Streak: 0");
        streakLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #e74c3c;");

        Separator sep1 = new Separator();

        // Rewards section
        Label rewardsTitle = new Label("🎁 Recent Rewards");
        rewardsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        rewardPanel = new VBox(5);
        rewardPanel.setPadding(new Insets(10));
        rewardPanel.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 5;");

        ScrollPane rewardScroll = new ScrollPane(rewardPanel);
        rewardScroll.setPrefHeight(200);
        rewardScroll.setFitToWidth(true);

        Separator sep2 = new Separator();

        // Analytics button
        Button analyticsBtn = new Button("📊 View Analytics");
        analyticsBtn.setMaxWidth(Double.MAX_VALUE);
        analyticsBtn.setOnAction(e -> showAnalytics());

        Button exportBtn = new Button("💾 Export Data");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> exportData());

        statsPanel.getChildren().addAll(
            userLabel, scoreLabel, streakLabel, sep1,
            rewardsTitle, rewardScroll, sep2,
            analyticsBtn, exportBtn
        );

        VBox.setMargin(statsPanel, new Insets(0, 10, 0, 0));
        return statsPanel;
    }

    private HBox createControlPanel() {
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(15, 0, 0, 0));

        startWebcamBtn = new Button("▶ Start Webcam");
        startWebcamBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #27ae60; -fx-text-fill: white;");
        startWebcamBtn.setOnAction(e -> startWebcam());

        stopWebcamBtn = new Button("⏹ Stop Webcam");
        stopWebcamBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #e74c3c; -fx-text-fill: white;");
        stopWebcamBtn.setDisable(true);
        stopWebcamBtn.setOnAction(e -> stopWebcam());

        captureBtn = new Button("📸 Capture Smile");
        captureBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #3498db; -fx-text-fill: white;");
        captureBtn.setDisable(true);
        captureBtn.setOnAction(e -> captureSmile());

        uploadBtn = new Button("📁 Upload Image");
        uploadBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #9b59b6; -fx-text-fill: white;");
        uploadBtn.setOnAction(e -> uploadImage());

        Button settingsBtn = new Button("⚙ Settings");
        settingsBtn.setOnAction(e -> showSettings());

        controls.getChildren().addAll(
            startWebcamBtn, stopWebcamBtn, captureBtn, uploadBtn, settingsBtn
        );

        return controls;
    }

    private void showLoginDialog() {
        TextInputDialog dialog = new TextInputDialog("guest");
        dialog.setTitle("User Login");
        dialog.setHeaderText("Welcome to Smile Recognition System!");
        dialog.setContentText("Enter your username:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            currentUser = username;
            updateUI();
        });
    }

    private void startWebcam() {
        if (webcamService.startCapture()) {
            startWebcamBtn.setDisable(true);
            stopWebcamBtn.setDisable(false);
            captureBtn.setDisable(false);
            updateStatus("Webcam started - Smile detection active", "#27ae60");
        } else {
            showError("Failed to start webcam. Please check your camera.");
        }
    }

    private void stopWebcam() {
        webcamService.stopCapture();
        startWebcamBtn.setDisable(false);
        stopWebcamBtn.setDisable(true);
        captureBtn.setDisable(true);
        updateStatus("Webcam stopped", "#e67e22");
    }

    private void captureSmile() {
        webcamService.captureFrame(currentUser);
    }

    private void uploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            apiService.detectSmileFromFile(file, currentUser, this::onDetectionResult);
        }
    }

    private void onDetectionResult(DetectionResult result) {
        javafx.application.Platform.runLater(() -> {
            if (result.isSmileDetected()) {
                totalScore += result.getPointsAwarded();
                currentStreak = result.getCurrentStreak();
                
                updateScores();
                showReward(result);
                playSuccessAnimation();
                
                updateStatus("😊 Smile detected! +" + result.getPointsAwarded() + " points", "#27ae60");
            } else {
                updateStatus("No smile detected. Keep trying!", "#e67e22");
            }

            confidenceBar.setProgress(result.getConfidence());
        });
    }

    private void updateScores() {
        scoreLabel.setText("🏆 Total Score: " + totalScore);
        streakLabel.setText("🔥 Current Streak: " + currentStreak);
    }

    private void showReward(DetectionResult result) {
        Label rewardLabel = new Label(String.format(
            "✨ +%d points | Confidence: %.1f%% | Streak: %d",
            result.getPointsAwarded(),
            result.getConfidence() * 100,
            result.getCurrentStreak()
        ));
        rewardLabel.setStyle("-fx-font-size: 11px; -fx-padding: 5;");
        
        rewardPanel.getChildren().add(0, rewardLabel);
        
        // Keep only last 10 rewards
        if (rewardPanel.getChildren().size() > 10) {
            rewardPanel.getChildren().remove(10);
        }
    }

    private void playSuccessAnimation() {
        // Add confetti or flash animation here
        imageView.setOpacity(0.7);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(300), imageView
        );
        ft.setFromValue(0.7);
        ft.setToValue(1.0);
        ft.play();
    }

    private void updateStatus(String message, String color) {
        statusLabel.setText("Status: " + message);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + ";");
    }

    private void updateUI() {
        scoreLabel.setText("🏆 Total Score: " + totalScore);
        streakLabel.setText("🔥 Current Streak: " + currentStreak);
    }

    private void showAnalytics() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Analytics");
        alert.setHeaderText("User Statistics for " + currentUser);
        alert.setContentText(
            "Total Score: " + totalScore + "\n" +
            "Current Streak: " + currentStreak + "\n" +
            "Session Time: " + webcamService.getSessionTime()
        );
        alert.showAndWait();
    }

    private void exportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Data");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("smile_data_" + currentUser + ".csv");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            apiService.exportData(currentUser, file);
            showInfo("Data exported successfully to " + file.getName());
        }
    }

    private void showSettings() {
        // Settings dialog implementation
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText("Application Settings");
        alert.setContentText("Settings panel coming soon...");
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void cleanup() {
        if (webcamService != null) {
            webcamService.stopCapture();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}