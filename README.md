# RA Terminal

**Termux asli dengan antarmuka grafis pribadi RA** — tetap satu aplikasi Android dan satu Activity. Terminal Linux asli/PTY tidak diganti oleh terminal palsu.

Fitur RA Terminal v0.5:
- Logo RA vektor (di header dan launcher).
- Latar hitam dengan **latar NASA SDO 171 Å dari video lokal (jika tersedia), dengan tiga gelombang transisi cahaya; latar matahari native lama menjadi cadangan**, plus gelombang biru tipis; seluruhnya berada di belakang UI. Video diputar menggunakan TextureView dan MediaPlayer Android di belakang TerminalView asli; tidak memakai Termux:GUI, browser, atau tmux.
- Panel CPU/APP CPU, RAM, storage, network, dan PID/heap membaca data Android yang dapat diakses. Nilai tidak tersedia tidak diganti angka rekaan.
- Layar terminal, tulisan, tombol, dan panel tetap diam.
- Panel dapat disembunyikan dari tombol LIVE untuk mendapatkan area mengetik lebih lebar.

## Build APK

Source patch **sudah ada di repository ini**. Workflow mengambil Termux resmi dari commit `8629e632fcb95da272221be327db653fb24befe9` dan menerapkan patch dengan `apply_to_termux.py`.

Lihat **[GitHub Actions](https://github.com/mpsprintit-spec/termux/actions/workflows/build-ra-terminal.yml)**. Build otomatis dipicu saat kode patch berubah; kamu juga dapat memilih **Run workflow**.

Jika build sukses, buka run sukses → **Artifacts** → `RA-Terminal-v0.5-APK`, lalu ekstrak ZIP untuk memperoleh `RA_Terminal_v0.5_universal_debug.apk` dan checksum SHA256.

> **PENTING:** APK memakai signing key debug Termux yang tidak tepercaya untuk produksi. Jangan hapus Termux lama sebelum data dicadangkan, APK selesai dibangun, dan diuji instalasi serta fungsi shellnya. Source dan pipeline ini adalah eksperimen; build yang hijau pun belum membuktikan kompatibilitas penuh pada perangkatmu.


## NASA SDO langsung di Ra Terminal (v0.5)

Integrasi dilakukan pada **RaTerminalChrome** yang sudah ada, bukan aplikasi baru.
Shell Linux/PT Y, file HOME, package manager, tombol ekstra, dan sesi Termux
tetap memakai implementasi Termux asli.

* Matahari ditempatkan sekitar 45–50% lebar layar, agak jauh dan di tengah latar hitam.
* Video NASA memakai kecepatan **1×**; tidak ada animasi panning atau pergerakan kamera buatan.
* Di akhir rekaman terjadi tiga gelombang cahaya halus: redup–terang × 3;
  crossfade antar dua video ditempatkan di gelombang kedua.
* Tidak ada permintaan jaringan dari dalam aplikasi. Video diletakkan di HOME Termux;
  jika belum tersedia atau decoder gagal, latar native RA lama tetap berfungsi.
* MediaPlayer dihentikan ketika Activity tidak aktif dan dilepas saat Activity dihancurkan.

### Menyiapkan video NASA satu kali dari shell Ra Terminal

Setelah APK v0.5 selesai dibangun dan terinstal, salin skrip
`install_nasa_solar.sh` dari repo ini ke Termux lalu jalankan:

```sh
pkg install curl ffmpeg
bash install_nasa_solar.sh
```

Skrip mengunduh rekaman NASA SVS 4246 (*The Big Sunspot of 2014*) jika tidak
diberi nama file lokal, mengubahnya menjadi biru 720p MP4 tanpa audio, dan
menyimpannya ke `$HOME/.ra-terminal/solar.mp4`. Kamu juga dapat memberi file
rekaman yang sudah tersimpan sebagai argumen:
`bash install_nasa_solar.sh /sdcard/Download/rekaman.webm`.

Jika belum menjalankan skrip, Ra Terminal tetap berjalan dengan latar
sebelumnya. Video tidak otomatis disertakan di APK.

### Batasan dan keamanan

NASA SDO bersifat observasi asli dalam bentuk timelapse, bukan bola 3D bebas
berputar. Sumber video bisa memuat perubahan teknis yang tidak dapat dihilangkan
oleh transisi. Kehalusan transisi tiga gelombang, dekoder video ganda, dan
performa perangkat harus dinilai di HP setelah APK diuji.

Build APK debug mungkin menggunakan sertifikat berbeda pada run GitHub Actions.
Jangan menghapus aplikasi/data Termux yang sekarang sampai APK pengganti telah
diverifikasi dan ada cadangan data. Proyek ini tidak menyentuh repositori Horizon.
