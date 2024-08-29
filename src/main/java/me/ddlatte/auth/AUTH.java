package me.ddlatte.auth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.Random;
import java.util.logging.Level;

public class AUTH extends JavaPlugin {

    private Connection connection;
    private String host, database, username, password;
    private int port;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        try {
            initializeDatabase();
            getLogger().info("LoginPlugin이 성공적으로 활성화되었습니다.");
        } catch (SQLException e) {
            getLogger().severe("데이터베이스 초기화 실패: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        closeConnection();
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        host = config.getString("database.host", "localhost");
        port = config.getInt("database.port", 3306);
        database = config.getString("database.name", "minecraft");
        username = config.getString("database.username", "root");
        password = config.getString("database.password", "");
    }

    private void initializeDatabase() throws SQLException {
        openConnection();
        createTable();
    }

    private void openConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) return;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + database +
                            "?useSSL=false&allowPublicKeyRetrieval=true",
                    username, password
            );
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC 드라이버를 찾을 수 없습니다.", e);
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS AUTH (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "key_code VARCHAR(5) UNIQUE, " +
                "minecraft_name VARCHAR(16), " +
                "discord_name VARCHAR(37), " +
                "is_used BOOLEAN DEFAULT false)";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "데이터베이스 연결 종료 실패", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("login")) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;
        String uuid = player.getUniqueId().toString();

        try {
            ensureConnection();
            String authCode = getOrCreateAuthCode(uuid, player.getName());
            player.sendMessage("인증 코드: " + authCode);
        } catch (SQLException e) {
            player.sendMessage("인증 코드 처리 중 오류가 발생했습니다.");
            getLogger().log(Level.SEVERE, "인증 코드 처리 실패", e);
        }

        return true;
    }

    private void ensureConnection() throws SQLException {
        if (!isConnected()) {
            openConnection();
        }
    }

    private boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private String getOrCreateAuthCode(String uuid, String playerName) throws SQLException {
        String existingCode = getUnusedAuthCode(uuid);
        if (existingCode != null) {
            return existingCode;
        }

        String newCode = generateUniqueAuthCode();
        saveAuthCode(uuid, newCode, playerName);
        return newCode;
    }

    private String getUnusedAuthCode(String uuid) throws SQLException {
        String sql = "SELECT key_code FROM AUTH WHERE uuid = ? AND is_used = false";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("key_code");
                }
            }
        }
        return null;
    }

    private String generateUniqueAuthCode() throws SQLException {
        String authCode;
        do {
            authCode = generateAuthCode();
        } while (isAuthCodeExists(authCode));
        return authCode;
    }

    private String generateAuthCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private boolean isAuthCodeExists(String authCode) throws SQLException {
        String sql = "SELECT COUNT(*) FROM AUTH WHERE key_code = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, authCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void saveAuthCode(String uuid, String authCode, String minecraftName) throws SQLException {
        String sql = "INSERT INTO AUTH (uuid, key_code, minecraft_name, is_used) VALUES (?, ?, ?, false) " +
                "ON DUPLICATE KEY UPDATE key_code = ?, minecraft_name = ?, is_used = false";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, authCode);
            pstmt.setString(3, minecraftName);
            pstmt.setString(4, authCode);
            pstmt.setString(5, minecraftName);
            pstmt.executeUpdate();
        }
    }
}