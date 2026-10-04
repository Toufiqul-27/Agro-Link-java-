package com.agrolinkbd.agrolinkbd;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class AgroLinkApp extends Application {
    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        sceneChange("Login.fxml", "AgroLink BD - Login", 600, 500);
    }

    public static void sceneChange(String fxml, String title, double width, double height) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(AgroLinkApp.class.getResource(fxml));
            Scene scene = new Scene(fxmlLoader.load(), width, height);
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) { launch(); }
}