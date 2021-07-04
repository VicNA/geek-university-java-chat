package ru.geekbrains.chat;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

public class ClientController {

    private final int LAST_LINES = 2;

    @FXML
    public VBox clientsBox;
    @FXML
    public Label currentUser;
    @FXML
    public PasswordField userPasswordField;
    @FXML
    private ListView<String> clientsListView;
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField messageField, loginField;
    @FXML
    private HBox authPanel, msgPanel;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    //  Кнопка авторизации
    public void connect(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            openConnect();
            new Thread(() -> createThreadChat()).start();
        }

        try {
            out.writeUTF("/auth " + loginField.getText() + " " + userPasswordField.getText());
        } catch (IOException e) {
            showError("Невозможно отправить запрос авторизации на сервер");
        }
    }

    //  Создание/открытие подключения к серверу
    public void openConnect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            showError("Невозможно подключиться к серверу");
        }
    }

    //  Отдельный поток основной логики работы
    private void createThreadChat() {
        try {
            tryToAuth();
            runChat();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
            setAuthorized(false);
        }
    }

    //  Скрытие/разскрытие панелей графического клиента при авторизации
    public void setAuthorized(boolean authorized) {
        msgPanel.setVisible(authorized);
        msgPanel.setManaged(authorized);
        authPanel.setVisible(!authorized);
        authPanel.setManaged(!authorized);
        clientsBox.setVisible(authorized);
        clientsBox.setManaged(authorized);
    }

    //  Обработка входящих ответов сервера на попытки авторизации
    private void tryToAuth() throws IOException {
        File file = new File(loginField.getText() + ".txt");
        if (!file.exists()) file.createNewFile();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (true) {
                String inputMessage = in.readUTF();
                if (inputMessage.equals("/exit")) {
                    closeConnection();
                }
                if (inputMessage.startsWith("/authok ")) {
//                System.out.println("inputMessage: /authok");
                    setAuthorized(true);
                    String connectedUser = "Ваше имя: " + inputMessage.split("\\s+")[1];
                    Platform.runLater(() -> currentUser.setText(connectedUser));
                    List<String> lines = reader.lines().collect(Collectors.toList());
                    if (lines.size() <= LAST_LINES) {
                        for (String line : lines) {
                            chatArea.appendText(line + "\n");
                        }
                    } else {
                        for (int i = lines.size() - LAST_LINES; i < lines.size(); i++) {
                            chatArea.appendText(lines.get(i) + "\n");
                        }
                    }
                    break;
                }
                chatArea.appendText(inputMessage + "\n");
            }
        }
    }

    //  Обработка входящих ответов сервера на посылаемые пользователем сообщения в чате
//  и отображение этих сообщений в графическом клиенте
    private void runChat() throws IOException {
        String fileName = loginField.getText() + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            while (true) {
                String message = in.readUTF();
                if (message.startsWith("/")) {
                    if (message.equals("/exit")) {
                        break;
                    }

                    if (message.startsWith("/changeok ")) {
                        Platform.runLater(() -> {
                            String[] tokens = message.split("\\s+");
                            currentUser.setText("Ваше имя: " + tokens[1]);
                        });
                    }

                    if (message.startsWith("/clients_list ")) {
                        Platform.runLater(() -> {
                            String[] tokens = message.split("\\s+");
                            clientsListView.getItems().clear();
                            for (int i = 1; i < tokens.length; i++) {
                                clientsListView.getItems().add(tokens[i]);
                            }
                        });
                    }
                    continue;
                }
                chatArea.appendText(message + "\n");
                writer.write(message);
                writer.newLine();
                writer.flush();
            }
        }
    }

    //  Отправка сообщений пользователя из текстового поля к серверу
    public void sendMessage(ActionEvent actionEvent) {
        try {
            out.writeUTF(messageField.getText());
            messageField.clear();
            messageField.requestFocus();
        } catch (IOException e) {
            showError("Невозможно отправить сообщение на сервер");
        }
    }

    //  Закрытие подключения сокета и потоков данных
    private void closeConnection() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //  Отправки команды серверу о выходе из чата
    public void exitApplication() {
        try {
            if (out != null) {
                out.writeUTF("/exit");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //  Вызов диалогового окна с системными сообщениями об ошибках
    public void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    //  Обработка двойного клика по именам пользователей в ListView
    public void clientsListDoubleClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            String selectedUser = clientsListView.getSelectionModel().getSelectedItem();
            messageField.setText("/w " + selectedUser + " ");
            messageField.requestFocus();
            messageField.selectEnd();
        }
    }
}
