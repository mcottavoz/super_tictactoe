package com.supermorpion;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/supermorpion/menu-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 780, 920);
        stage.setTitle("Super Morpion — Ultimate Tic-Tac-Toe");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
