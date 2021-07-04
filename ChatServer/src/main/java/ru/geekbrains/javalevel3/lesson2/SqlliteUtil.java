package ru.geekbrains.javalevel3.lesson2;

import java.sql.*;

public class SqlliteUtil implements ConnectionService, AuthService {

    private Connection connection;
    private Statement statement;

    public SqlliteUtil() {
        connect();
    }

    @Override
    public void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:demodb.db");
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String[] loggedIn(String login, String password) {
        String sql = "SELECT login, nickname FROM User WHERE login = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, password);

            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new String[] {rs.getString("login"), rs.getString("nickname") };
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void changeNickname(String login, String nickname) {
        String sql = "UPDATE User SET nickname = ? WHERE login = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nickname);
            statement.setString(2, login);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //  Проверка, занятно ли указанное имя пользователя (nickname)
    @Override
    public boolean isNameBusy(String nickname) {
        String sql = "SELECT nickname FROM User WHERE nickname = ?";
        boolean isBusy = false;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nickname);
            ResultSet rs =  statement.executeQuery();
            isBusy = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isBusy;
    }
}
