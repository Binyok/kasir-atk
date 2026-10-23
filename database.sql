CREATE DATABASE IF NOT EXISTS copygiri CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE copygiri;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nama_lengkap VARCHAR(100) NOT NULL,
    role ENUM('OWNER','KASIR') NOT NULL DEFAULT 'KASIR',
    aktif TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS kategori (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nama VARCHAR(80) NOT NULL UNIQUE,
    keterangan VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS barang (
    id INT AUTO_INCREMENT PRIMARY KEY,
    kode VARCHAR(30) NOT NULL UNIQUE,
    nama VARCHAR(150) NOT NULL,
    merk VARCHAR(100),
    jenis VARCHAR(100),
    warna VARCHAR(50),
    ukuran VARCHAR(50),
    gambar VARCHAR(255),
    kategori_id INT,
    satuan VARCHAR(20) NOT NULL DEFAULT 'pcs',
    harga_beli DECIMAL(14,2) NOT NULL DEFAULT 0,
    harga_jual DECIMAL(14,2) NOT NULL DEFAULT 0,
    stok INT NOT NULL DEFAULT 0,
    stok_minimum INT NOT NULL DEFAULT 0,
    aktif TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_barang_nama (nama),
    CONSTRAINT fk_barang_kategori FOREIGN KEY (kategori_id) REFERENCES kategori(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS layanan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nama VARCHAR(100) NOT NULL UNIQUE,
    satuan VARCHAR(20) NOT NULL DEFAULT 'lembar',
    harga_satuan DECIMAL(14,2) NOT NULL DEFAULT 0,
    keterangan VARCHAR(255),
    aktif TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS restock (
    id INT AUTO_INCREMENT PRIMARY KEY,
    barang_id INT NOT NULL,
    jumlah INT NOT NULL,
    harga_beli DECIMAL(14,2) NOT NULL DEFAULT 0,
    supplier VARCHAR(120),
    catatan VARCHAR(255),
    user_id INT,
    tanggal DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_restock_tanggal (tanggal),
    CONSTRAINT fk_restock_barang FOREIGN KEY (barang_id) REFERENCES barang(id) ON DELETE RESTRICT,
    CONSTRAINT fk_restock_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transaksi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    kode VARCHAR(30) NOT NULL UNIQUE,
    tipe ENUM('BARANG','JASA','CAMPURAN') NOT NULL DEFAULT 'BARANG',
    subtotal DECIMAL(14,2) NOT NULL DEFAULT 0,
    diskon DECIMAL(14,2) NOT NULL DEFAULT 0,
    total DECIMAL(14,2) NOT NULL DEFAULT 0,
    bayar DECIMAL(14,2) NOT NULL DEFAULT 0,
    kembali DECIMAL(14,2) NOT NULL DEFAULT 0,
    catatan VARCHAR(255),
    user_id INT,
    tanggal DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_transaksi_tanggal (tanggal),
    CONSTRAINT fk_transaksi_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transaksi_item (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaksi_id INT NOT NULL,
    tipe ENUM('BARANG','JASA') NOT NULL,
    barang_id INT,
    layanan_id INT,
    nama_item VARCHAR(150) NOT NULL,
    qty INT NOT NULL DEFAULT 1,
    harga DECIMAL(14,2) NOT NULL DEFAULT 0,
    harga_beli DECIMAL(14,2) NOT NULL DEFAULT 0,
    subtotal DECIMAL(14,2) NOT NULL DEFAULT 0,
    KEY idx_item_transaksi (transaksi_id),
    KEY idx_item_barang (barang_id),
    KEY idx_item_layanan (layanan_id),
    CONSTRAINT fk_item_transaksi FOREIGN KEY (transaksi_id) REFERENCES transaksi(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_barang FOREIGN KEY (barang_id) REFERENCES barang(id) ON DELETE SET NULL,
    CONSTRAINT fk_item_layanan FOREIGN KEY (layanan_id) REFERENCES layanan(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS antrian (
    id INT AUTO_INCREMENT PRIMARY KEY,
    transaksi_id INT,
    layanan_id INT,
    nama_pelanggan VARCHAR(100),
    deskripsi VARCHAR(255),
    jumlah INT NOT NULL DEFAULT 1,
    status ENUM('MENUNGGU','PROSES','SELESAI','DIAMBIL') NOT NULL DEFAULT 'MENUNGGU',
    waktu_masuk DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    waktu_selesai DATETIME,
    user_id INT,
    KEY idx_antrian_status (status),
    CONSTRAINT fk_antrian_transaksi FOREIGN KEY (transaksi_id) REFERENCES transaksi(id) ON DELETE CASCADE,
    CONSTRAINT fk_antrian_layanan FOREIGN KEY (layanan_id) REFERENCES layanan(id) ON DELETE SET NULL,
    CONSTRAINT fk_antrian_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pengaturan (
    nama_kunci VARCHAR(50) PRIMARY KEY,
    nilai VARCHAR(255) NOT NULL DEFAULT ''
) ENGINE=InnoDB;

INSERT INTO pengaturan (nama_kunci, nilai) VALUES
    ('nama_toko', 'GiriATK & Print'),
    ('alamat', 'Jl. Contoh No. 1'),
    ('telepon', '0812-0000-0000'),
    ('footer_struk', 'Terima kasih telah berbelanja'),
    ('stok_minimum_default', '5')
ON DUPLICATE KEY UPDATE nilai = nilai;
