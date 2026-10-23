package giriatk.service;

import giriatk.dao.UserDao;
import giriatk.model.User;
import giriatk.util.PasswordUtil;

import java.sql.SQLException;

public class AuthService {

    private final UserDao userDao = new UserDao();

    public User login(String username, String password) throws SQLException {
        User user = userDao.findByUsername(username);
        if (user == null || !PasswordUtil.verify(password, user.getPassword())) {
            return null;
        }
        if (!user.isAktif()) {
            throw new SQLException("Akun ini sedang dinonaktifkan.");
        }
        if (PasswordUtil.needsRehash(user.getPassword())) {
            userDao.updatePasswordHash(user.getId(), PasswordUtil.hash(password));
        }
        user.setPassword(null);
        return user;
    }
}
