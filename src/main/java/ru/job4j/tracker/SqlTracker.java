package ru.job4j.tracker;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SqlTracker implements Store {

    private Connection connection;

    public SqlTracker() {
        init();
    }

    public SqlTracker(Connection connection) {
        this.connection = connection;
    }

    private void init() {
        try (InputStream input = SqlTracker.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            Properties config = new Properties();
            config.load(input);
            Class.forName(config.getProperty("driver-class-name"));
            connection = DriverManager.getConnection(
                    config.getProperty("url"),
                    config.getProperty("username"),
                    config.getProperty("password")
            );
            createTableIfNotExists(connection);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void createTableIfNotExists(Connection connection) throws SQLException {
        String createTableSQL = String.format(
                "CREATE TABLE IF NOT EXISTS items(id serial primary key, name text, created timestamp);");
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }

    private Item createItemFromResultSet(ResultSet rs) throws SQLException {
        return new Item(rs.getInt("id"), rs.getString("name"), rs.getTimestamp("created").toLocalDateTime());
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public Item add(Item item) {
        Timestamp timestampFromLDT = Timestamp.valueOf(item.getCreated());
        String sql = "INSERT INTO items(name, created) VALUES (?, ?) RETURNING id";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, item.getName());
            preparedStatement.setTimestamp(2, timestampFromLDT);
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                item.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return item;
    }

    @Override
    public boolean replace(int id, Item item) {
        boolean res = false;
        Timestamp timestampFromLDT = Timestamp.valueOf(item.getCreated());
        String sql = "UPDATE items SET name = ?, created = ?  WHERE id = ?;";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, item.getName());
            preparedStatement.setTimestamp(2, timestampFromLDT);
            preparedStatement.setInt(3, id);
            preparedStatement.execute();
            res = preparedStatement.getUpdateCount() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Item> findAll() {
        List<Item> res = new ArrayList<>();
        String sql = "SELECT id, name, created FROM items";
        try (PreparedStatement prepStmt = connection.prepareStatement(sql)) {
            ResultSet rs = prepStmt.executeQuery();
            while (rs.next()) {
                res.add(createItemFromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    @Override
    public List<Item> findByName(String key) {
        List<Item> res = new ArrayList<>();
        String sql = "SELECT id, name, created FROM items WHERE name LIKE ?";
        try (PreparedStatement prepStmt = connection.prepareStatement(sql)) {
            prepStmt.setString(1, "%" + key + "%");
            ResultSet rs = prepStmt.executeQuery();
            while (rs.next()) {
                res.add(createItemFromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    @Override
    public Item findById(int id) {
        Item res = null;
        String sql = "SELECT id, name, created FROM items WHERE id = ?";
        try (PreparedStatement prepStmt = connection.prepareStatement(sql)) {
            prepStmt.setInt(1, id);
            ResultSet rs = prepStmt.executeQuery();
            if (rs.next()) {
                res = createItemFromResultSet(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return res;
    }
}