package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlDb {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:clients.db");
            stmt = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void crudDb() {
        try {
            stmt.execute("CREATE TABLE students ("+
                        "id    INTEGER PRIMARY KEY AUTOINCREMENT,"+
                        "Name  TEXT,"+
                        "Score INTEGER);");
            stmt.executeUpdate("INSERT INTO students (Name,Score) VALUES ('Petr',10);");
            stmt.executeUpdate("INSERT INTO students (Name,Score) VALUES ('Dima',20);");
            stmt.executeUpdate("INSERT INTO students (Name,Score) VALUES ('Vova',30);");
            stmt.executeUpdate("UPDATE students Set score=90 WHERE Name='Vova'");
            stmt.executeQuery("SELECT * FROM students");
            stmt.executeUpdate("DELETE FROM students WHERE students.Name='Petr';");
            stmt.executeUpdate("DROP TABLE students;");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
