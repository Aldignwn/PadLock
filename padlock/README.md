# Pad Lock

Mematikan sentuhan layar Android (tanpa root) supaya HP hanya bisa dikontrol lewat gamepad Bluetooth.
Target: Android 7.0+ (minSdk 24).

## Cara kerja
- `LockService` (AccessibilityService) memasang overlay layar penuh bertipe `TYPE_ACCESSIBILITY_OVERLAY`
  yang menelan semua sentuhan. Tombol/joystick gamepad tidak lewat jendela itu, jadi tetap sampai ke app.
- Service mendengarkan tombol gamepad secara global (`flagRequestFilterKeyEvents`).
  **Start + Select ditahan 2 detik** = kunci / buka.
- Kunci otomatis dibuka jika gamepad terputus, dan kunci ditolak jika gamepad belum terdeteksi.
- Tile Quick Settings "Kunci Sentuh" sebagai cadangan (hanya bisa dipakai saat belum terkunci).

## Build
1. Buka folder ini di Android Studio (Electric Eel / Flamingo atau lebih baru, JDK 11+).
2. Tunggu Gradle sync, lalu **Build > Build APK(s)**. Hasil: `app/build/outputs/apk/debug/app-debug.apk`.
3. Pasang APK ke HP (aktifkan "Sumber tidak dikenal" jika diminta).

## Pemakaian pertama
1. Buka app Pad Lock > "Buka Pengaturan Aksesibilitas" > pilih **Pad Lock** > aktifkan.
2. Sambungkan gamepad Bluetooth.
3. Tahan Start + Select 2 detik (HP bergetar + toast muncul). Ulangi untuk membuka.

## Jika tombol gamepad berbeda
Beberapa gamepad murah tidak mengirim `BUTTON_START`/`BUTTON_SELECT`. Ubah `KEY_A` dan `KEY_B`
di `LockService.kt` ke kode lain (mis. `KEYCODE_BUTTON_L1` + `KEYCODE_BUTTON_R1`).
Untuk mencari kode tombol, pasang app "Gamepad Tester" atau lihat `adb shell getevent -l`.

## Batasan
- Android tidak punya kursor untuk gamepad: navigasi UI hanya lewat D-pad/fokus. Game yang mendukung gamepad aman.
- Layar status bar/notifikasi yang ditarik dari atas mungkin masih lolos di beberapa perangkat; uji di HP kamu.
- Beberapa dialog sistem (izin tertentu) bisa tampil di atas overlay.
- Gamepad yang dikenali sebagai keyboard (mode HID) tidak dihitung sebagai gamepad oleh pengecekan `hasGamepad()`.
