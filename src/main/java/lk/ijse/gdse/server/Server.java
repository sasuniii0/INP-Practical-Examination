package lk.ijse.gdse.server;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lk.ijse.gdse.client.Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

public class Server {
    private static final int PORT = 5000;
    private static HashSet<ObjectOutputStream> writers = new HashSet<>();
    private boolean isRunning = false;
    private ServerSocket serverSocket;
    private long serverStartTime;

    @FXML
    private Button btnAddClient;

    @FXML
    private TextArea txtServer;

    @FXML
    private AnchorPane rootServer;

    @FXML
    public void initialize() {
        appendMessage("Server initialize done, add clients...");
    }

    private void appendMessage(String s) {
        Platform.runLater(() -> txtServer.appendText(s + "\n"));
    }

    private void openClient() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/client.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = new Stage();
                stage.setTitle("Chat Client");
                stage.setScene(scene);
                stage.setOnCloseRequest(windowEvent -> {
                    Client controller = loader.getController();
                    controller.closeConnection();
                });
                stage.show();
                appendMessage("New Client Added");
            } catch (IOException e) {
                appendMessage("Error while opening Client window");
                e.printStackTrace();
            }
        });
    }

    private void startServer() {
        isRunning = true;
        serverStartTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                appendMessage("Server started on port " + PORT);
                while (isRunning) {
                    Socket client = serverSocket.accept();
                    appendMessage("Client connected: " + client.getInetAddress().getHostAddress());

                    Thread clientThread = new Thread(new ClientHandler(client));
                    clientThread.start();
                }
            } catch (IOException e) {
                if (isRunning) {
                    appendMessage("Error starting server: " + e.getMessage());
                }
            }
        }).start();
    }

    @FXML
    public void btnAddClientOnAction(ActionEvent actionEvent) {
        if (!isRunning) {
            startServer();
        }
        openClient();
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String clientName;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    out.writeObject("SUBMITNAME");
                    clientName = (String) in.readObject();
                    if (clientName != null && !clientName.trim().isEmpty()) {
                        break;
                    }
                    appendMessage("Invalid name, request again");
                }
                out.writeObject("NAMEACCEPTED");
                appendMessage("Client " + clientName + " connected");
                broadcast("TEXT " + clientName + " joined the chat");

                synchronized (writers) {
                    writers.add(out);
                }

                while (true) {
                    Object message = in.readObject();
                    if (message == null) break;

                    if (message instanceof String) {
                        String text = (String) message;
                        if (text.equalsIgnoreCase("BYE")) {
                            out.writeObject("BYE Goodbye " + clientName);
                            break;
                        } else if (text.equalsIgnoreCase("TIME")) {
                            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                            String currentTime = timeFormat.format(new Date());
                            out.writeObject("TIME " + currentTime);

                        } else if (text.equalsIgnoreCase("DATE")) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            String currentDate = dateFormat.format(new Date());
                            out.writeObject("DATE " + currentDate);

                        } else if (text.equalsIgnoreCase("UPTIME")) {
                            long uptimeSeconds = (System.currentTimeMillis() - serverStartTime) / 1000;
                            out.writeObject("UPTIME " + uptimeSeconds + " seconds");
                        } else {
                            broadcast("TEXT " + clientName + ": " + text);
                        }
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                appendMessage("Client connection error: " + e.getMessage());
            } finally {
                if (clientName != null) {
                    appendMessage("Client " + clientName + " disconnected");
                    broadcast("TEXT " + clientName + " left the chat");
                }
                synchronized (writers) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    appendMessage("Error closing client socket");
                }
            }
        }

        private void broadcast(String message) {
            synchronized (writers) {
                for (ObjectOutputStream writer : writers) {
                    try {
                        writer.writeObject(message);
                        writer.flush();
                    } catch (IOException e) {
                        appendMessage("Error broadcasting message to client");
                    }
                }
            }
        }
    }
}