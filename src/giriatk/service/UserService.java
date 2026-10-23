package giriatk.service;

import giriatk.dao.UserDao;
import giriatk.model.Role;
import giriatk.model.User;

import java.sql.SQLException;

public class UserService {

    private final UserDao userDao = new UserDao();

    public void simpan(User user, String passwordBaru) throws SQLException {
        boolean baru = user.getId() == 0;
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new SQLException("Username wajib diisi.");
        }
        if (user.getNamaLengkap() == null || user.getNamaLengkap().isBlank()) {
            throw new SQLException("Nama lengkap wajib diisi.");
        }
        if (baru) {
            if (passwordBaru == null || passwordBaru.length() < 4) {
                throw new SQLException("Password minimal 4 karakter.");
            }
        } else {
            User lama = userDao.findById(user.getId());
            if (lama == null) {
                throw new SQLException("Pengguna tidak ditemukan.");
            }
            boolean ownerAktifHilang = lama.isOwner() && lama.isAktif()
                    && (user.getRole() != Role.OWNER || !user.isAktif());
            if (ownerAktifHilang && userDao.countAktifOwner() <= 1) {
                throw new SQLException("Harus ada minimal satu owner yang aktif.");
            }
        }
        if (userDao.usernameTaken(user.getUsername(), user.getId())) {
            throw new SQLException("Username sudah dipakai.");
        }

        if (baru) {
            userDao.insert(user, passwordBaru);
        } else {
            userDao.update(user);
            if (passwordBaru != null && !passwordBaru.isBlank()) {
                if (passwordBaru.length() < 4) {
                    throw new SQLException("Password minimal 4 karakter.");
                }
                userDao.updatePassword(user.getId(), passwordBaru);
            }
        }
    }

    public void resetPassword(int userId, String passwordBaru) throws SQLException {
        if (passwordBaru == null || passwordBaru.length() < 4) {
            throw new SQLException("Password minimal 4 karakter.");
        }
        userDao.updatePassword(userId, passwordBaru);
    }

    public void hapus(User user) throws SQLException {
        User lama = userDao.findById(user.getId());
        if (lama == null) {
            return;
        }
        if (lama.isOwner() && lama.isAktif() && userDao.countAktifOwner() <= 1) {
            throw new SQLException("Owner terakhir tidak bisa dihapus.");
        }
        userDao.delete(user.getId());
    }
}
