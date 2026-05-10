package voxlink.server.src.main.database;

import java.sql.SQLException;

public class TestDB {
    public static void main(String[] args) throws SQLException {
        SchemaInitializer.initialize();
    }
}
