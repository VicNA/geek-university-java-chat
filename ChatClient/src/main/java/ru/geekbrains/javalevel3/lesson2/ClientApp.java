package ru.geekbrains.javalevel3.lesson2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat.fxml"));
        Parent root = loader.load();
        ClientController controller = loader.getController();

        stage.setScene(new Scene(root));
        stage.setTitle("My Chat client");
        stage.setOnCloseRequest(event -> {
            controller.exitApplication();
            Platform.exit();
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
