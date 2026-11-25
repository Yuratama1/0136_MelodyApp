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

Struktur package aplikasi saat ini:

```text
app/src/main/java/com/example/melody/
├── app/
│   ├── MainActivity.kt
│   ├── MelodyApplication.kt
│   ├── navigation/
│   │   ├── MelodyNavHost.kt
│   │   └── MelodyRoute.kt
│   └── shell/
│       └── MelodyAppShell.kt
├── core/
│   ├── common/UiState.kt
│   ├── config/SupabaseConfig.kt
│   └── di/
│       ├── AuthRepositoryModule.kt
│       ├── NetworkModule.kt
│       └── SupabaseApiKeyInterceptor.kt
├── data/
│   ├── remote/auth/                 # Supabase Auth REST API dan DTO (T-06)
│   ├── repository/auth/            # Auth Repository (T-07/T-08/T-09)
│   └── session/                    # Session model, enkripsi, penyimpanan, restore/refresh (T-08)
└── feature/
    ├── auth/                       # Login, recovery/reset password, register, verifikasi (T-07/T-09)
    ├── home/HomeScreen.kt
    ├── library/LibraryScreen.kt
    ├── profile/ProfileScreen.kt
    └── search/SearchScreen.kt
```

Konvensi penempatan tanggung jawab untuk kode berikutnya:

| Package | Tanggung jawab |
|---|---|
| `app/` | Entry point aplikasi dan Activity. |
| `core/common/` | Tipe dan komponen umum yang benar-benar digunakan lintas bagian, termasuk shared UI State. |
| `core/di/` | Hilt Module dan komponen penyedia dependency aplikasi, termasuk client jaringan. |
| `core/config/` | Konfigurasi aplikasi yang divalidasi dan digunakan lintas komponen. |
| `core/model/` | Model yang memang digunakan bersama. Belum dibuat karena belum ada model lintas fitur. |
| `data/remote/` | Remote data source saat integrasi layanan dibuat. |
| `data/repository/` | Implementasi repository untuk sumber data. |
| `feature/<nama>/` | Komponen UI dan logic presentasi per fitur ketika task fitur disetujui. |

Package yang belum memiliki kode tidak dibuat sebagai placeholder. T-07 menambahkan registrasi dan verifikasi email. T-08 menambahkan login dan fondasi session persisten terenkripsi.

`UiState<T>` di `core/common/UiState.kt` menyediakan `Loading`, `Success(data)`, `Empty`, dan `Error(message, cause)`. `AuthViewModel` T-07 memakai state tersebut untuk mengelola operasi registrasi dan verifikasi email.

Dependency injection memakai Hilt. `MelodyApplication` tetap memakai `@HiltAndroidApp` dan `MainActivity` memakai `@AndroidEntryPoint`. Hilt Module ditempatkan di `core/di/` saat ada binding konkret yang perlu disediakan.

### Fondasi integrasi Supabase

Konfigurasi lokal Supabase dibaca dari `local.properties` dan disediakan ke aplikasi melalui `BuildConfig.SUPABASE_URL` serta `BuildConfig.SUPABASE_PUBLISHABLE_KEY`. File `local.properties` sudah diabaikan Git dan tidak boleh di-commit. Untuk menyiapkannya, salin `local.properties.example` menjadi `local.properties`, pertahankan `sdk.dir` yang sudah ada, lalu isi:

```properties
supabase.url=https://YOUR_PROJECT_REF.supabase.co
supabase.publishableKey=sb_publishable_YOUR_KEY
```

Build tetap berjalan jika kedua nilai tersebut belum tersedia; konfigurasi akan berstatus belum siap dan konfigurasi invalid menghasilkan pesan tanpa menampilkan key. Project URL harus menggunakan HTTPS. Gradle hanya menerima key dengan prefix Publishable Key `sb_publishable_` dan menolak key berprefix secret.

Publishable Key adalah key publik untuk client, bukan autentikasi pengguna. Jangan pernah memasukkan Supabase Secret Key atau Service Role Key ke aplikasi, `BuildConfig`, atau repository. Pembatasan akses data harus ditegakkan oleh PostgreSQL Row Level Security (RLS). OkHttp menambahkan Publishable Key melalui header `apikey`; belum ada JWT pengguna, endpoint tabel, atau request data produk pada fondasi ini.

Supabase Project URL dan Publishable Key belum dikonfigurasi pada checkout ini. Karena itu, koneksi backend belum diuji dan tidak diklaim berhasil.

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

**Status saat ini: Fondasi proyek Android (T-02), arsitektur dasar (T-03), shell navigasi (T-04), fondasi Supabase (T-05/T-06), Register dan verifikasi email (T-07), Login dan session persistence (T-08), serta Forgot Password dan Reset Password (T-09) telah diimplementasikan untuk review.**

T-07 menyediakan registrasi dan verifikasi token email secara manual. T-08 menambahkan form login, validasi respons autentikasi, penyimpanan session terenkripsi dengan key AES-GCM yang dikelola Android Keystore, pemulihan session, dan refresh token. Data session disimpan sebagai ciphertext di `noBackupFilesDir`; token tidak dicatat melalui `toString()` DTO/model. T-09 menambahkan permintaan email pemulihan, verifikasi kode OTP bertipe `recovery`, dan penggantian password. Token recovery hanya dipakai sementara di memori untuk request update password dan tidak membentuk session login. Unit test dan Debug Build mencakup perubahan autentikasi. URL dan Publishable Key Supabase tidak tersedia di checkout ini; pengiriman email, konfigurasi template `{{ .Token }}`, dan reset password ke backend aktif belum diverifikasi.

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
