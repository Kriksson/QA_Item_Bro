package API;


import config.TestConfig;
import org.junit.jupiter.api.BeforeAll;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public abstract class TestBaseAPI {

    @BeforeAll
    static void setUp() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                TestConfig.DB_URL,
                TestConfig.DB_USER,
                TestConfig.DB_PASSWORD);
        Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE TABLE order_messages, orders, advices, games, users RESTART IDENTITY");
            ApiUtility.registerAndReturnData(
                    TestConfig.API_URL, "ADMIN", "Admin123!"
            );

            ApiUtility.registerAndReturnData(
                    TestConfig.API_URL, "MAJOR", "Major123!"
            );
            statement.execute("UPDATE users SET role = 'ADMIN' WHERE username = 'ADMIN'");
            statement.execute("UPDATE users SET balance = '100000' WHERE username = 'MAJOR'");
        }
    }
}
