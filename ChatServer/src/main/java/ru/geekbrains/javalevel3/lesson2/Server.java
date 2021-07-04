package ru.geekbrains.javalevel3.lesson2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Server {

    private final int PORT = 8189;

    private LinkedList<ClientHandler> clients = new LinkedList<>();

    private ConnectionService connection;

    public Server() {
//  Создание сервера сокета
        try (ServerSocket serverSocket = new ServerSocket(PORT);) {
            System.out.println("Сервер запущен. Ожидаем подключение клиентов");

            connection = new SqlliteUtil();

//          Ожидание подключении пользователей и создание клиентского обработчика
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Подключился новый клиент");
                new ClientHandler(this, socket, (AuthService) connection);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
    }

//  Добавление пользователя в список и рассылка сообщения остальным пользователям
//  о новом пользователя в чате
    public synchronized void subscribe(ClientHandler client) {
        broadcastMessage("Пользователь " + client.getName() + " вошел в чат ");
        clients.add(client);
        broadcastClientList();
    }

//  Удаление пользователя из списка и рассылка сообщения остальным пользователям
//  о выходе пользователя из чате
    public synchronized void unsubscribe(ClientHandler client) {
        clients.remove(client);
        broadcastMessage("Пользователь " + client.getName() + " вышел из чата");
        broadcastClientList();
    }

//  Рассылка сообщения пользователям из списка
    public synchronized void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }

//  Рассылка команды-сообщения клиентам со списком пользоваетелей
    public synchronized void broadcastClientList() {
        StringBuilder sb = new StringBuilder(clients.size() * 10);
        sb.append("/clients_list ");
        for (ClientHandler client : clients) {
            sb.append(client.getName() + " ");
        }
        broadcastMessage(sb.toString());
    }

//  Отправка личных сообщений
    public synchronized void sendPersonalMessage(ClientHandler sender, String receiverName, String message) {
        if (sender.getName().equalsIgnoreCase(receiverName)) {
            sender.sendMessage("Нельзя отправлять личные сообщения самому себе");
            return;
        }

        for (ClientHandler client : clients) {
            if (client.getName().equalsIgnoreCase(receiverName)) {
                client.sendMessage("от " + sender.getName() + ": " + message);
                sender.sendMessage("пользователю " + receiverName + ": " + message);
                return;
            }
        }

        sender.sendMessage("Пользователь " + receiverName + " не в сети");
    }

    //  Проверка, занятно ли указанное имя пользователя
    public synchronized boolean isNameBusy(String userName) {
        for (ClientHandler client : clients) {
            if (userName.equalsIgnoreCase(client.getName())) {
                return true;
            }
        }
        return false;
    }
}
