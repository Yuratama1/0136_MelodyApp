# Melody — Aplikasi Katalog dan Pemutar Musik

Melody adalah aplikasi mobile berbasis Android untuk mengelola koleksi dan memutar musik secara praktis. Aplikasi ini menyediakan katalog lagu, artis, album, genre, playlist, favorit, pencarian musik, serta fitur pemutaran audio.

Repository ini merupakan **Mobile Repository** yang digunakan untuk menyimpan kode sumber aplikasi Android Melody.

## Tujuan Proyek

1. Memudahkan pengguna menelusuri dan mencari katalog musik.
2. Menyediakan fitur untuk membuat dan mengelola playlist pribadi.
3. Memungkinkan pengguna menyimpan lagu ke daftar favorit dan melihat riwayat pemutaran.
4. Menyediakan pemutar musik dengan kontrol dasar dan antrean pemutaran.
5. Menyediakan hak akses berbeda untuk pengguna dan Admin dalam pengelolaan katalog musik.

## Ruang Lingkup Fitur

Fitur berikut merupakan ruang lingkup MVP yang telah disetujui dan akan dikembangkan secara bertahap. Daftar ini **bukan pernyataan bahwa seluruh fitur sudah tersedia**.

### Fitur Pengguna

- Registrasi, verifikasi email, login, pemulihan kata sandi, dan logout.
- Melihat katalog lagu, artis, album, dan genre.
- Mencari lagu, artis, dan album.
- Melihat detail lagu, artis, album, genre, dan playlist.
- Membuat, mengubah, dan menghapus playlist pribadi.
- Menambahkan atau menghapus lagu dari playlist.
- Mengelola daftar lagu favorit.
- Melihat riwayat pemutaran dan daftar lagu yang baru diputar.
- Memutar lagu dengan kontrol dasar, mini player, antrean, shuffle, dan repeat.
- Mengelola informasi profil pengguna.

### Fitur Admin

- Melihat ringkasan katalog musik.
- Menambah, melihat, mengubah, dan menghapus data lagu.
- Mengelola data artis.
- Mengelola data album.
- Mengelola data genre.

Hak akses Admin dan pengguna akan ditegakkan oleh backend, bukan hanya melalui tampilan aplikasi.

## Teknologi yang Digunakan

| Teknologi | Kegunaan |
|---|---|
| Kotlin | Bahasa pemrograman aplikasi |
| Jetpack Compose | Pembangunan antarmuka pengguna |
| Navigation Compose | Navigasi antarhalaman |
| MVVM | Pola arsitektur aplikasi |
| Repository Pattern | Pengelolaan akses dan sumber data |
| Supabase | Layanan backend utama |
| PostgreSQL | Basis data utama |
| Supabase Auth | Autentikasi dan pengelolaan sesi |
| Supabase REST API (PostgREST) | Komunikasi data melalui REST API |
| Retrofit | Klien komunikasi jaringan |
| Kotlin Serialization | Serialisasi dan parsing JSON |
| Hilt | Dependency injection |
| ViewModel dan StateFlow | Pengelolaan state antarmuka |
| Media3 ExoPlayer | Pemutaran file audio |
| Supabase Storage | Penyimpanan file audio dan gambar |
| Android Internal Storage | Penyimpanan lokal sampul playlist |

## Arsitektur Aplikasi

Melody menggunakan MVVM yang dipadukan dengan Repository Pattern. Batas tanggung jawab yang digunakan adalah:

- **UI Compose** menampilkan UI State dan meneruskan interaksi pengguna.
- **ViewModel** mengelola state dan logika presentasi untuk UI.
- **Repository** menjadi perantara antara ViewModel dan sumber data.
- **Remote Data Source** menangani komunikasi dengan layanan eksternal.

Alur akses data yang direncanakan:

```text
Antarmuka Jetpack Compose
          |
          v
       ViewModel
          |
          v
      Repository
          |
          v
  Remote Data Source
          |
          v
   Supabase REST API
          |
          v
      PostgreSQL
```

### Struktur Package

Struktur yang sudah dibuat pada T-03:

```text
app/src/main/java/com/example/melody/
├── app/
│   ├── MainActivity.kt
│   └── MelodyApplication.kt
└── core/
    └── common/
        └── UiState.kt
```

Konvensi penempatan tanggung jawab untuk kode berikutnya:

| Package | Tanggung jawab |
|---|---|
| `app/` | Entry point aplikasi dan Activity. |
| `core/common/` | Tipe dan komponen umum yang benar-benar digunakan lintas bagian, termasuk shared UI State. |
| `core/di/` | Hilt Module saat ada binding dependency yang diperlukan. Belum ada module karena belum ada binding aplikasi. |
| `core/model/` | Model yang memang digunakan bersama. Belum dibuat karena belum ada model lintas fitur. |
| `data/remote/` | Remote data source saat integrasi layanan dibuat. |
| `data/repository/` | Implementasi repository untuk sumber data. |
| `feature/<nama>/` | Komponen UI dan logic presentasi per fitur ketika task fitur disetujui. |

Package yang belum memiliki kode tidak dibuat sebagai placeholder. Belum ada ViewModel atau Repository produk pada T-03.

`UiState<T>` di `core/common/UiState.kt` menyediakan `Loading`, `Success(data)`, `Empty`, dan `Error(message, cause)`. Model ini menetapkan bentuk state bersama; ViewModel pemakaiannya akan dibuat saat task fitur terkait.

Dependency injection memakai Hilt. `MelodyApplication` tetap memakai `@HiltAndroidApp` dan `MainActivity` memakai `@AndroidEntryPoint`. Hilt Module ditempatkan di `core/di/` saat ada binding konkret yang perlu disediakan.

Autentikasi menggunakan Supabase Auth. File audio katalog dan gambar katalog disimpan melalui Supabase Storage. Pencarian metadata musik publik dilakukan secara terpisah melalui MusicBrainz API dan tidak menggantikan basis data utama Melody.

## Penyimpanan dan Pemutaran Musik

- File audio katalog disimpan pada Supabase Storage.
- Gambar album dan artis disimpan pada Supabase Storage.
- Sampul playlist pengguna disimpan pada penyimpanan internal aplikasi di perangkat.
- Pemutaran audio menggunakan Media3 ExoPlayer.
- Fitur unduh musik untuk pemutaran offline dan pemutaran di latar belakang tidak termasuk ruang lingkup MVP.

## Repository Terkait

Backend dan dokumentasi API dikelola pada repository terpisah:

- [Melody API Repository](https://github.com/Yuratama1/0136_API_MelodyApp)

## Status Pengembangan

**Status saat ini: Fondasi proyek Android (T-02) dan arsitektur dasar (T-03) telah dibuat dan diverifikasi.**

Repository memiliki struktur Android dasar, konfigurasi Gradle Kotlin DSL, dependency awal, dan shared UI State. Gradle Sync, unit test UiState, dan Debug Build berhasil. Belum ada fitur produk seperti autentikasi, katalog, atau pemutar musik yang diimplementasikan.

Pengembangan dilakukan secara bertahap berdasarkan daftar task dan kriteria penerimaan yang telah disetujui. Fitur hanya akan dinyatakan selesai setelah implementasi dan bukti pengujiannya ditinjau.

## Tahapan Pengembangan

Pengembangan Melody dibagi menjadi delapan fase:

1. Persiapan proyek dan fondasi aplikasi.
2. Autentikasi pengguna.
3. Katalog musik dan pengelolaan data Admin.
4. Katalog musik untuk pengguna.
5. Fitur musik personal: playlist, favorit, riwayat, dan profil.
6. Pemutaran musik dan antrean.
7. Integrasi penyimpanan file dan API publik.
8. Pengujian, keamanan, dokumentasi, dan persiapan demo.

## Pedoman Pengembangan

- Mengikuti requirement, keputusan, dan task proyek yang telah disetujui.
- Tidak menambahkan fitur di luar ruang lingkup tanpa persetujuan.
- Mengembangkan aplikasi secara bertahap dan mencatat perubahan melalui commit yang bermakna.
- Memperbarui dokumentasi progres mingguan berdasarkan pekerjaan yang benar-benar dilakukan.
- Tidak memasukkan kredensial rahasia atau kunci akses privat ke dalam repository.
- Memastikan README selalu mencerminkan kondisi implementasi yang sebenarnya.

---

**Pemilik proyek:** User  
**Nama proyek:** Melody  
**Platform:** Android  
**Repository:** Mobile Application
