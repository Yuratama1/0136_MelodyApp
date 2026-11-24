# Panduan Kerja AI Software Engineer — Melody

## Identitas Proyek

- Melody adalah aplikasi katalog dan pemutar musik berbasis mobile.
- Proyek menggunakan Android dan Jetpack Compose.
- Repository ini adalah source of truth untuk implementasi.

## Prinsip Kerja

- Implementasikan hanya task yang diberikan.
- Jangan mengarang requirement atau menambah fitur di luar scope.
- Jangan melakukan refactor besar tanpa persetujuan.
- Pahami kode yang sudah ada sebelum mengubahnya.

## Workflow Implementasi

1. Analisis task terlebih dahulu.
2. Periksa struktur repository.
3. Buat rencana implementasi singkat.
4. Implementasikan sesuai task.
5. Jalankan testing yang relevan.
6. Laporkan file yang diubah, hasil test, kendala, dan hal yang belum selesai.

## Arsitektur dan Kualitas Kode

- Gunakan Jetpack Compose untuk UI.
- Utamakan struktur kode yang modular dan mudah dipelihara.
- Pisahkan UI, logic, dan data sesuai kebutuhan arsitektur.
- Hindari hardcoding yang tidak semestinya.
- Gunakan komponen reusable jika relevan.
- Jangan menambahkan library tanpa alasan teknis yang jelas.

## Scope dan Keamanan

- Jangan menghapus data atau file tanpa persetujuan.
- Jangan mengubah konfigurasi penting secara sembarangan.
- Jangan melakukan commit atau push tanpa instruksi yang jelas.
- Jangan menganggap pekerjaan selesai jika belum diverifikasi.

## Git

- Periksa status Git sebelum melakukan perubahan.
- Jangan menghapus atau menimpa perubahan existing milik user.
- Dokumentasikan perubahan secara jelas.
- Pertahankan riwayat repository.
- Ikuti ketentuan minimal commit proyek yang telah ditetapkan; jangan membuat commit palsu atau commit kosong.

## Dokumentasi

- Dokumentasikan progress implementasi secara relevan.
- Perbarui README jika task memang memerlukan perubahan dokumentasi.
- Jangan mengubah requirement atau keputusan PM secara sepihak.

## Handoff

Setiap implementasi harus memiliki laporan yang mencakup:

- Task yang dikerjakan.
- File yang diubah.
- Testing yang dijalankan beserta hasilnya.
- Kendala yang ditemui.
- Status acceptance criteria.
