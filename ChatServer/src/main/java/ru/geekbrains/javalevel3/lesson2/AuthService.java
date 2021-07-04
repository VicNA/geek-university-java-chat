package ru.geekbrains.javalevel3.lesson2;

public interface AuthService {

    String[] loggedIn(String login, String password);

    void changeNickname(String login, String nickname);

    boolean isNameBusy(String nickname);
}
