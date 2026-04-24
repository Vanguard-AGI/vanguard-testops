import java.sql.*;

public class CheckMD5 {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3307/metersphere?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String pass = "root";

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("成功连接到数据库");

            // 1. 测试内置 MD5
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT MD5('metersphere')");
                if (rs.next()) {
                    System.out.println("内置 MD5 函数正常工作，结果: " + rs.getString(1));
                }
            } catch (SQLException e) {
                System.err.println("测试 MD5 函数失败: " + e.getMessage());
            }

            // 2. 检查 sql_mode
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT @@sql_mode");
                if (rs.next()) {
                    System.out.println("当前 sql_mode: " + rs.getString(1));
                }
            }

            // 3. 尝试设置全局参数以防万一
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET GLOBAL log_bin_trust_function_creators = 1");
                System.out.println("已设置 log_bin_trust_function_creators = 1");
            } catch (SQLException e) {
                System.err.println("设置 log_bin_trust_function_creators 失败 (可能权限不足): " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
