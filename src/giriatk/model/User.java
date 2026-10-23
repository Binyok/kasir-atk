package giriatk.model;

public class User {

    private int id;
    private String username;
    private String password;
    private String namaLengkap;
    private Role role = Role.KASIR;
    private boolean aktif = true;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNamaLengkap() {
        return namaLengkap;
    }

    public void setNamaLengkap(String namaLengkap) {
        this.namaLengkap = namaLengkap;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAktif() {
        return aktif;
    }

    public void setAktif(boolean aktif) {
        this.aktif = aktif;
    }

    public boolean isOwner() {
        return role == Role.OWNER;
    }

    @Override
    public String toString() {
        return namaLengkap == null ? username : namaLengkap;
    }
}
