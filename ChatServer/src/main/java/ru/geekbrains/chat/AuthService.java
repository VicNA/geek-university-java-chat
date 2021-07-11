package ru.geekbrains.chat;

public interface AuthService {

    void connect();

    void disconnect();

    String[] loggedIn(String login, String password);

    void changeNickname(String login, String nickname);

    boolean isNameBusy(String nickname);
}
