package config;

public final class TestConfig {
    private static final String WORKER = System.getProperty("test.worker", "1");
    private static final int THREADS = Integer.parseInt(System.getProperty("test.threads", "1"));

    public static final String API_URL = endpoint("api.url", "API_URL", "http://localhost:8085");
    public static final String UI_URL = endpoint("ui.url", "UI_URL", "http://localhost:5173");
    public static final String DB_URL = endpoint("db.url", "DB_URL", "jdbc:postgresql://localhost:5433/item_bro");
    public static final String DB_USER = value("db.user", "DB_USER", "kriksson");
    public static final String DB_PASSWORD = value("db.password", "DB_PASSWORD", "kriksson");
    public static final String REMOTE = value("selenide.remote", "SELENIDE_REMOTE", "");
    public static final String BROWSER_VERSION = value("selenide.browserVersion", "SELENIDE_BROWSER_VERSION", "");
    public static final boolean HEADLESS = Boolean.parseBoolean(
            value("selenide.headless", "SELENIDE_HEADLESS", "false"));

    private TestConfig() {}

    private static String endpoint(String property, String environment, String fallback) {
        String address = value(property, environment, fallback);
        if (THREADS > 1 && !address.contains("{worker}")) {
            throw new IllegalStateException("Для параллельного запуска " + environment
                    + " должен содержать {worker}: каждому потоку нужен отдельный стенд и база");
        }
        return address.replace("{worker}", WORKER);
    }

    private static String value(String property, String environment, String fallback) {
        return System.getProperty(property, System.getenv().getOrDefault(environment, fallback));
    }
}
