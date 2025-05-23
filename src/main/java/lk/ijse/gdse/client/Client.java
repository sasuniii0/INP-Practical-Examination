package lk.ijse.gdse.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

import java.io.*;
import java.net.Socket;

public class Client {
    @FXML
    private ListView<String> messageView;

    @FXML
    private Button btnSend;

    @FXML
    private TextField txtInputField;

    @FXML
    private AnchorPane rootClient;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientName;
    private boolean nameAccepted = false;

    public void initialize() {
        messageView.setCellFactory(ListView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
            }
        });

        try {
            Socket socket = new Socket("localhost", 5000);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Thread thread = new Thread(() -> listenForMessages());
            thread.start();
        } catch (IOException e) {
            messageView.getItems().add("Error connecting to server: " + e.getMessage());
        }
    }

    private void namePrompt() {
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enter your Name");
            dialog.setHeaderText("Please enter your name");
            dialog.setContentText("Name:");

            dialog.showAndWait().ifPresent(name -> {
                clientName = name.trim();
                if (clientName.isEmpty()) {
                    messageView.getItems().add("Name cannot be empty...");
                    namePrompt();
                } else {
                    try {
                        out.writeObject(clientName);
                        out.flush();
                    } catch (Exception e) {
                        messageView.getItems().add("Error sending name...");
                    }
                }
            });
        });
    }

    @FXML
    public void btnSendOnAction(ActionEvent actionEvent) {
        String message = txtInputField.getText().trim();
        if (message.isEmpty()) return;

        try {
            if (message.equalsIgnoreCase("TIME") || message.equalsIgnoreCase("DATE") ||
                    message.equalsIgnoreCase("UPTIME") || message.equalsIgnoreCase("BYE")) {
                out.writeObject(message);
            } else {
                out.writeObject(message);
                //messageView.getItems().add("You: " + message);
            }
            out.flush();
            txtInputField.clear();
        } catch (IOException e) {
            messageView.getItems().add("Error sending message: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        try {
            while (true) {
                Object message = in.readObject();
                if (message == null) break;

                if (message instanceof String) {
                    String text = (String) message;
                    if (text.startsWith("SUBMITNAME")) {
                        if (!nameAccepted) {
                            namePrompt();
                        }
                    } else if (text.startsWith("NAMEACCEPTED")) {
                        nameAccepted = true;
                        Platform.runLater(() -> messageView.getItems().add("Connected as " + clientName));
                    } else if (text.startsWith("TIME ")) {
                        Platform.runLater(() -> messageView.getItems().add("Server time: " + text.substring(5)));
                    } else if (text.startsWith("DATE ")) {
                        Platform.runLater(() -> messageView.getItems().add("Today's date: " + text.substring(5)));
                    } else if (text.startsWith("UPTIME ")) {
                        Platform.runLater(() -> messageView.getItems().add("Server uptime: " + text.substring(7)));
                    } else if (text.startsWith("BYE ")) {
                        Platform.runLater(() -> {
                            messageView.getItems().add(text.substring(4));
                            closeConnection();
                        });
                    } else if (text.startsWith("TEXT ")) {
                        Platform.runLater(() -> messageView.getItems().add(text.substring(5)));
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(() -> messageView.getItems().add("Disconnected: " + e.getMessage()));
        } finally {
            closeConnection();
        }
    }

    public void closeConnection() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            Platform.runLater(() -> messageView.getItems().add("Error closing connection"));
        }
    }
}