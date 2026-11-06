# RA Terminal

**Termux asli dengan antarmuka grafis pribadi RA** — tetap satu aplikasi Android dan satu Activity. Terminal Linux asli/PTY tidak diganti oleh terminal palsu.

Fitur yang sedang dibangun:
- Logo RA vektor (di header dan launcher).
- Latar hitam dengan gelombang biru/ungu dan partikel bergerak **hanya di belakang UI**.
- Panel CPU/APP CPU, RAM, storage, network, dan PID/heap membaca data Android yang dapat diakses. Nilai tidak tersedia tidak diganti angka rekaan.
- Layar terminal, tulisan, tombol, dan panel tetap diam.
- Panel dapat disembunyikan dari tombol LIVE untuk mendapatkan area mengetik lebih lebar.

## Build APK

Source patch **sudah ada di repository ini**. Workflow mengambil Termux resmi dari commit `8629e632fcb95da272221be327db653fb24befe9` dan menerapkan patch dengan `apply_to_termux.py`.

Lihat **[GitHub Actions](https://github.com/mpsprintit-spec/termux/actions/workflows/build-ra-terminal.yml)**. Build otomatis dipicu saat kode patch berubah; kamu juga dapat memilih **Run workflow**.

Jika build sukses, buka run sukses → **Artifacts** → `RA-Terminal-v0.2-APK`, lalu ekstrak ZIP untuk memperoleh `RA_Terminal_v0.2_universal_debug.apk` dan checksum SHA256.

> **PENTING:** APK memakai signing key debug Termux yang tidak tepercaya untuk produksi. Jangan hapus Termux lama sebelum data dicadangkan, APK selesai dibangun, dan diuji instalasi serta fungsi shellnya. Source dan pipeline ini adalah eksperimen; build yang hijau pun belum membuktikan kompatibilitas penuh pada perangkatmu.
