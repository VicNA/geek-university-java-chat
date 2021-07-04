package ru.geekbrains.javalevel3.lesson2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private AuthService authService;

    private String login;
    private String name;

    private DataInputStream in;
    private DataOutputStream out;

//  Получение сокета от сервера, создание потоков in/out,
//  запуск в отдельном потоке чтения данных получаемых от пользователя
    public ClientHandler(Server server, Socket socket, AuthService authService) {
        try {
            this.server = server;
            this.socket = socket;
            this.authService = authService;

            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> readMessages()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //  Обработка входящих данных получаемых от пользователя
    private void readMessages() {
        try {
            authentication();
            readMessage();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Клиент " + name + " отключился");
            server.unsubscribe(this);
            closeConnection();

        }
    }

    //  Обработка сообщений пользователя на наличие внутренних комманд
//  и рассылка сообщения другим пользователям
    private void readMessage() throws IOException {
        while (true) {
            String message = in.readUTF();

            if (message.startsWith("/")) {
                if (message.equals("/exit")) {
                    sendMessage("/exit");
                    break;
                }

                if (message.startsWith("/w ")) {
                    String[] tokens = message.split("\\s+", 3);
                    server.sendPersonalMessage(this, tokens[1], tokens[2]);
                    continue;
                }

                if (message.startsWith("/change_nick ")) {
                    String[] tokens = message.split("\\s+");
                    if (tokens.length == 1) {
                        sendMessage("SERVER: Введите имя пользователя");
                        continue;
                    }
                    if (tokens.length > 2) {
                        sendMessage("SERVER: Имя пользователя не должен содержать пробелы");
                        continue;
                    }
                    if (name.equalsIgnoreCase(tokens[1])) {
                        sendMessage("SERVER: Данное имя пользователя является текущим");
                        continue;
                    }
                    if (authService.isNameBusy(tokens[1])) {
                        sendMessage("SERVER: Данное имя пользователя уже занято");
                        continue;
                    }
                    authService.changeNickname(login, tokens[1]);
                    String oldName = name;
                    name = tokens[1];

                    sendMessage("/changeok " + name);
                    sendMessage("SERVER: " + oldName + " сменил имя на " + name);
                    server.broadcastClientList();
                    continue;
                }
            }
            server.broadcastMessage(name + ": " + message);
        }
    }

    //  Обработка сообщения об авторизации
    private void authentication() throws IOException {
        while (true) {
            String message = in.readUTF();

            if (!message.startsWith("/auth ")) {
                sendMessage("SERVER: Вам необходимо авторизоваться");
                continue;
            }

            String[] tokens = message.split("\\s+");

            if (tokens.length == 1) {
                sendMessage("SERVER: Вы не указали логин или пароль");
                continue;
            }

            if (tokens.length > 3) {
                sendMessage("SERVER: Логин и пароль не должны содержать пробелы");
                continue;
            }

            String[] auth = authService.loggedIn(tokens[1], tokens[2]);
            if (auth == null) {
                sendMessage("SERVER: Такой логин отстутствует");
                continue;
            }

            if (server.isNameBusy(auth[1])) {
                sendMessage("SERVER: Данный пользователь уже авторизовался");
                continue;
            }

            login = auth[0];
            name = auth[1];

//            System.out.println("sendMessage: /authok " + name);
            sendMessage("/authok " + name);
            server.subscribe(this);
            return;
        }
    }

    //  Полуение имя пользователя
    public String getName() {
        return name;
    }

    //  Отправка сообщения пользователю
    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
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
}
