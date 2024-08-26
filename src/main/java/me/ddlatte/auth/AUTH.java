package me.ddlatte.auth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.Random;

public class AUTH extends JavaPlugin {

    private Connection connection;
    private String host, database, username, password;
    private int port;


    @Override
    public void onEnable() {
        // 설정 파일 생성 및 로드
        saveDefaultConfig();
        reloadConfig();

        // 데이터베이스 정보 로드
        host = getConfig().getString("database.host", "localhost");
        port = getConfig().getInt("database.port", 3306);
        database = getConfig().getString("database.name", "minecraft");
        username = getConfig().getString("database.username", "root");
        password = getConfig().getString("database.password", "");

        // 데이터베이스 연결
        try {
            openConnection();
            createTable();
            getLogger().info("LoginPlugin이 성공적으로 활성화되었습니다.");
        } catch (SQLException e) {
            getLogger().severe("데이터베이스 연결 실패: " + e.getMessage());
            getLogger().warning("LoginPlugin이 제대로 작동하지 않을 수 있습니다. 설정을 확인해 주세요.");
        } catch (Exception e) {
            getLogger().severe("플러그인 활성화 중 예기치 않은 오류 발생: " + e.getMessage());
            getLogger().warning("LoginPlugin이 비활성화됩니다. 로그를 확인하고 문제를 해결해 주세요.");
            setEnabled(false);
        }
    }
    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            getLogger().severe("데이터베이스 연결 종료 실패: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("login")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
                return true;
            }

            Player player = (Player) sender;
            String uuid = player.getUniqueId().toString();

            try {
                if (!isConnected()) {
                    player.sendMessage("데이터베이스 연결 오류. 관리자에게 문의하세요.");
                    getLogger().warning("데이터베이스 연결이 끊어졌습니다. 설정을 확인해 주세요.");
                    return true;
                }

                if (hasUnusedAuthCode(uuid)) {
                    player.sendMessage("이미 사용하지 않은 인증 코드가 있습니다.");
                    return true;
                }

                String authCode = generateUniqueAuthCode();
                saveAuthCode(uuid, authCode, player.getName());
                player.sendMessage("인증 코드: " + authCode);
            } catch (SQLException e) {
                player.sendMessage("인증 코드 생성 중 오류가 발생했습니다.");
                getLogger().severe("인증 코드 생성 실패: " + e.getMessage());
            }

            return true;
        }
        return false;
    }

    private boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
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
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private boolean hasUnusedAuthCode(String uuid) throws SQLException {
        String sql = "SELECT COUNT(*) FROM AUTH WHERE uuid = ? AND is_used = false";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private void openConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC 드라이버를 찾을 수 없습니다.", e);
        } catch (SQLException e) {
            throw new SQLException("데이터베이스 연결에 실패했습니다.", e);
        }
    }

    private void createTable() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS AUTH (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "key_code VARCHAR(5) UNIQUE, " +
                    "minecraft_name VARCHAR(16), " +
                    "discord_name VARCHAR(37), " +
                    "is_used BOOLEAN DEFAULT false)";
            statement.executeUpdate(sql);
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

    // 인증 코드 사용 메서드 (추후 구현 필요)
    public void useAuthCode(String authCode) throws SQLException {
        String sql = "UPDATE AUTH SET is_used = true WHERE key_code = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, authCode);
            pstmt.executeUpdate();
        }
    }
}