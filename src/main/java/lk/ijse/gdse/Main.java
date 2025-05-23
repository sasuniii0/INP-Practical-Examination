package lk.ijse.gdse;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/view/server.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Server");
        stage.setScene(scene);
        stage.show();
    }
}
/*

else if (text.startsWith("TIME")) {

        }else if (text.startsWith("DATE")) {

        }else if (text.startsWith("UPTIME")) {

        }else if (text.startsWith("BYE")) {
        Platform.runLater(()-> messageView.getItems().add(""));


        out.writeObject("DATE");
                    Date today = new Date();
                    date = String.valueOf(today.getDate());
                    if (!date.trim().isEmpty() && date!= null){
                        break;
                    }
                    appendMessage("Cannot update the Date");

        }*/
