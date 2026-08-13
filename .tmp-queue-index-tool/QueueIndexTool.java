import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class QueueIndexTool {
    private static final Map<String, String> INDEXES = new LinkedHashMap<>();

    static {
        INDEXES.put(
                "idx_waiting_queue_schedule_member_status",
                "ALTER TABLE waiting_queue_history ADD INDEX idx_waiting_queue_schedule_member_status (schedule_id, member_id, status)"
        );
        INDEXES.put(
                "idx_waiting_queue_schedule_status_joined",
                "ALTER TABLE waiting_queue_history ADD INDEX idx_waiting_queue_schedule_status_joined (schedule_id, status, joined_at, id)"
        );
    }

    public static void main(String[] args) throws Exception {
        boolean apply = args.length == 1 && "apply".equals(args[0]);
        String url = require("DB_URL");
        String username = require("DB_USERNAME");
        String password = require("DB_PASSWORD");

        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println("database=" + connection.getCatalog());
            for (Map.Entry<String, String> index : INDEXES.entrySet()) {
                boolean exists = exists(connection, index.getKey());
                if (apply && !exists) {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(index.getValue());
                    }
                    exists = exists(connection, index.getKey());
                    System.out.println(index.getKey() + "=CREATED_AND_VERIFIED:" + exists);
                } else {
                    System.out.println(index.getKey() + "=" + (exists ? "EXISTS" : "MISSING"));
                }
            }
        }
    }

    private static boolean exists(Connection connection, String indexName) throws Exception {
        String sql = "SELECT 1 FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() AND table_name = 'waiting_queue_history' " +
                "AND index_name = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static String require(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " 환경변수가 필요합니다.");
        }
        return value;
    }
}
