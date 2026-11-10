package API;


import org.junit.jupiter.api.BeforeAll;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public abstract class TestBaseAPI {

    @BeforeAll
    static void setUp() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5433/item_bro",
                "kriksson",
                "kriksson");
        Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE order_messages, orders, advices, games, users RESTART IDENTITY");
            ApiUtility.registerAndReturnData(
                    "http://localhost:8085", "ADMIN", "Admin123!"
            );

            ApiUtility.registerAndReturnData(
                    "http://localhost:8085", "MAJOR", "Major123!"
            );
            statement.execute("UPDATE users SET role = 'ADMIN' WHERE username = 'ADMIN'");
            statement.execute("UPDATE users SET balance = '100000' WHERE username = 'MAJOR'");
        }
    }
}
