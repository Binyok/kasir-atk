# GiriATK & Print

Aplikasi desktop untuk toko ATK (alat tulis kantor) yang juga melayani jasa cetak seperti print, fotokopi (FC), laminating, dan jilid. Dibuat dengan **Java Swing** dan **MySQL**, mencakup penjualan barang, jasa cetak, antrian pesanan, stok, sampai laporan keuangan.

---

## Daftar Isi

- [Fitur](#fitur)
- [Tech Stack](#tech-stack)
- [Prasyarat](#prasyarat)
- [Instalasi & Menjalankan](#instalasi--menjalankan)
- [Konfigurasi Database](#konfigurasi-database)
- [Akun Default](#akun-default)
- [Hak Akses](#hak-akses)
- [Alur Pemakaian](#alur-pemakaian)
- [Struktur Proyek](#struktur-proyek)
- [Skema Database](#skema-database)
- [Build Sendiri (Manual)](#build-sendiri-manual)
- [Backup Data](#backup-data)

---

## Fitur

- **Autentikasi & hak akses** — login dengan role Owner dan Kasir.
- **Dashboard** — omzet & laba hari ini, jumlah transaksi, antrian aktif, grafik tren 7 hari, peringatan stok menipis.
- **Transaksi** — keranjang penjualan barang & jasa, tombol tambah/kurang qty, cek stok otomatis, hitung total, bayar, dan kembalian.
- **Antrian Jasa** — dibuat otomatis dari transaksi jasa, status berjenjang (Menunggu → Proses → Selesai → Diambil) lewat dropdown, plus ubah/hapus per baris.
- **Stok & Barang** — data barang, kategori, dan restock (khusus Owner). Kasir hanya bisa melihat.
- **Layanan Jasa** — kelola jenis layanan cetak beserta harga satuannya.
- **Laporan** — harian, mingguan, bulanan, dan kustom; laba bersih, item terlaris, rekap per kategori, daftar transaksi, dan **ekspor Excel (.xlsx)** bertabel rapi: tiap tabel di sheet terpisah (Ringkasan, Rekap Harian, Item Terlaris, Per Kategori, Daftar Transaksi) dengan header berwarna, border, lebar kolom otomatis, baris TOTAL, dan format Rupiah.
- **Riwayat Transaksi** — tersedia untuk Owner & Kasir, lengkap dengan detail per transaksi.
- **Pengguna** — kelola akun dan reset password (khusus Owner).
- **Pengaturan** — profil toko dan **backup database ke file `.sql`**.

---

## Tech Stack

| Komponen | Keterangan |
|---|---|
| Bahasa | Java 22 |
| UI | Java Swing + [FlatLaf](https://www.formdev.com/flatlaf/) |
| Database | MySQL 8 |
| Driver | `mysql-connector-j` |
| Build | Apache Ant (proyek NetBeans) |
| IDE | Apache NetBeans |

---

## Prasyarat

1. **JDK 22** terpasang (`java -version`).
2. **MySQL 8** berjalan di `localhost:3306`.
3. (Opsional) **Apache NetBeans** untuk membuka proyek.

---

## Instalasi & Menjalankan

### Cara 1 — Lewat NetBeans

1. Buka NetBeans → **File → Open Project** → pilih folder `Giriatk`.
2. Sesuaikan koneksi database di `config/db.properties` bila perlu.
3. Tekan **Run** (F6). Database dan tabel akan dibuat otomatis saat pertama dijalankan.

### Cara 2 — Lewat file JAR (siap pakai)

1. Masuk ke folder `dist`.
2. Jalankan `run.bat` (Windows) atau `java -jar Giriatk.jar`.

> Penting: jalankan dari dalam folder `dist`, karena aplikasi membaca `config/db.properties` dan `database.sql` secara relatif. Bila dipindah, bawa seluruh isi folder `dist`.

---

## Konfigurasi Database

Edit file `config/db.properties`:

```properties
db.host=localhost
db.port=3306
db.name=giriatk
db.user=root
db.password=
```

- Saat pertama dijalankan, aplikasi akan **membuat database `giriatk`** (bila belum ada), membuat semua tabel dari `database.sql`, lalu mengisi data awal (akun, kategori, layanan, dan contoh barang).
- Bisa juga impor manual lewat phpMyAdmin menggunakan file `database.sql`.

---

## Akun Default

| Role | Username | Password |
|---|---|---|
| Owner | `owner` | `owner123` |
| Kasir | `kasir` | `kasir123` |

> Segera ganti password melalui menu **Pengguna** setelah aplikasi dipakai.

---

## Hak Akses

| Menu / Fitur | Owner | Kasir |
|---|:---:|:---:|
| Dashboard (omzet & laba) | ✅ | ✅ (tanpa laba) |
| Transaksi | — | ✅ |
| Antrian Jasa | — | ✅ |
| Riwayat Transaksi | ✅ | ✅ |
| Stok & Barang — lihat | ✅ | ✅ |
| Stok & Barang — tambah/ubah/hapus | ✅ | ❌ |
| Stok & Barang — Restock | ✅ | ❌ |
| Layanan Jasa | ✅ | ❌ |
| Laporan | ✅ | ❌ |
| Pengguna | ✅ | ❌ |
| Pengaturan & Backup | ✅ | ❌ |

Pembatasan tidak hanya menyembunyikan tombol, tapi juga divalidasi di dalam kode.

---

## Alur Pemakaian

**Penjualan barang/jasa:**
1. Kasir buka menu **Transaksi**.
2. Pilih tab **Barang** atau **Jasa**, klik tombol **Tambah** pada baris untuk memasukkan ke keranjang.
3. Atur qty dengan tombol `−` / `+` (stok otomatis dicek).
4. Isi jumlah **Bayar**, sistem menghitung **Kembali**.
5. Klik **Simpan Transaksi**.
   - Item barang → stok otomatis berkurang.
   - Item jasa → otomatis masuk **Antrian Jasa**.

**Memproses antrian:**
1. Buka menu **Antrian Jasa**.
2. Ubah status lewat dropdown pada kolom **Status** mengikuti alur: `Menunggu → Proses → Selesai → Diambil`.
3. Status **Diambil** hanya bisa dipilih setelah **Selesai**, dan bersifat final.

---

## Struktur Proyek

Arsitektur berlapis (layered) agar mudah menambah, mengubah, atau menghapus fitur:

```
Giriatk/
├── build.xml                  Skrip build Ant (NetBeans)
├── database.sql               Skema + data awal database
├── manifest.mf                Manifest JAR
├── run.bat                    Peluncur cepat (folder dist)
├── config/
│   └── db.properties          Konfigurasi koneksi MySQL
├── lib/                       Library (FlatLaf, MySQL Connector)
├── dist/                      Hasil build siap pakai (Giriatk.jar + lib + config)
└── src/giriatk/
    ├── Giriatk.java           Entry point aplikasi
    ├── config/                AppConfig, Db (koneksi), Schema (migrasi otomatis)
    ├── model/                 Entitas & enum (User, Barang, Transaksi, Antrian, ...)
    ├── dao/                   Akses database (UserDao, BarangDao, TransaksiDao, LaporanDao, ...)
    ├── service/               Logika bisnis (Auth, Transaksi, Restock, User, Backup, Seeder)
    ├── util/                  Theme, UiKit, Icons, Fmt, Session, PasswordUtil, Dialogs
    └── view/
        ├── LoginFrame.java    Halaman login
        ├── MainFrame.java     Kerangka utama + navigasi sidebar
        ├── panel/             Halaman per modul (Dashboard, Transaksi, Antrian, Stok, ...)
        ├── dialog/            Form input (Barang, Layanan, Pengguna, Detail Transaksi, ...)
        └── component/         Komponen UI (TrendChart, TabBar, SegmentedTabs, ScrollablePanel)
```

**Alur lapisan:** `view` → `service` → `dao` → `config/Db` → MySQL. Model dipakai lintas lapisan.

---

## Skema Database

Database: `giriatk`

| Tabel | Isi |
|---|---|
| `users` | Akun pengguna (username, password ter-hash, role, status aktif) |
| `kategori` | Kategori barang |
| `barang` | Data barang (kode, nama, kategori, satuan, harga beli/jual, stok, stok minimum) |
| `layanan` | Jenis layanan jasa & harga satuan |
| `restock` | Riwayat penambahan stok |
| `transaksi` | Header transaksi (kode, tipe, subtotal, total, bayar, kembali, kasir, tanggal) |
| `transaksi_item` | Rincian item transaksi (barang/jasa, qty, harga, harga beli, subtotal) |
| `antrian` | Antrian jasa (pelanggan, layanan, jumlah, status, waktu) |
| `pengaturan` | Pengaturan aplikasi (nama toko, alamat, telepon, footer) |

Password disimpan sebagai hash **PBKDF2-HMAC-SHA256** dengan salt acak (tidak pernah plain text).

---

## Build Sendiri (Manual)

Bila ingin membangun tanpa NetBeans:

```powershell
# 1) Kompilasi
$cp = "lib\flatlaf.jar;lib\mysql-connector-j.jar"
$src = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d build\classes -cp $cp $src

# 2) Buat JAR
jar cfm dist\Giriatk.jar manifest.mf -C build\classes .

# 3) Salin dependensi & konfigurasi ke dist
copy lib\flatlaf.jar          dist\lib\
copy lib\mysql-connector-j.jar dist\lib\
copy config\db.properties     dist\config\
copy database.sql             dist\
copy run.bat                  dist\
```

Di NetBeans, cukup gunakan **Clean & Build** — proses penyalinan `lib`, `config`, `database.sql`, dan `run.bat` ke `dist` sudah otomatis lewat `build.xml`.

---

## Backup Data

1. Login sebagai **Owner** → menu **Pengaturan**.
2. Klik **Backup Sekarang**, pilih lokasi penyimpanan.
3. File `.sql` hasil backup bisa diimpor kembali lewat phpMyAdmin bila diperlukan.

---

## Catatan

- Aplikasi tidak memakai fitur print struk thermal, harga grosir/diskon, maupun pencatatan hutang (sesuai kebutuhan toko).
- Data contoh (kategori, layanan, barang) hanya diisi saat database masih kosong.
