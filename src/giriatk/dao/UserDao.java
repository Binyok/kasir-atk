package giriatk.dao;

import giriatk.config.Db;
import giriatk.model.Role;
import giriatk.model.User;
import giriatk.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password, nama_lengkap, role, aktif FROM users WHERE username = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT id, username, password, nama_lengkap, role, aktif FROM users WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, username, password, nama_lengkap, role, aktif FROM users ORDER BY role, nama_lengkap";
        List<User> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public int insert(User user, String plainPassword) throws SQLException {
        String sql = "INSERT INTO users (username, password, nama_lengkap, role, aktif) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, PasswordUtil.hash(plainPassword));
            ps.setString(3, user.getNamaLengkap());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isAktif());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
        }
        return user.getId();
    }

    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, nama_lengkap = ?, role = ?, aktif = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getNamaLengkap());
            ps.setString(3, user.getRole().name());
            ps.setBoolean(4, user.isAktif());
            ps.setInt(5, user.getId());
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String plainPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(plainPassword));
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updatePasswordHash(int userId, String storedHash) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, storedHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public boolean usernameTaken(String username, int exceptId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND id <> ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, exceptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int countAktifOwner() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'OWNER' AND aktif = 1";
        try (Connection conn = Db.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setNamaLengkap(rs.getString("nama_lengkap"));
        user.setRole(Role.fromDb(rs.getString("role")));
        user.setAktif(rs.getBoolean("aktif"));
        return user;
    }
}
