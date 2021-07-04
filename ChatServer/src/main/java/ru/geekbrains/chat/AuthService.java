package ru.geekbrains.chat;

public interface AuthService {

    String[] loggedIn(String login, String password);

    void changeNickname(String login, String nickname);

    boolean isNameBusy(String nickname);
}
