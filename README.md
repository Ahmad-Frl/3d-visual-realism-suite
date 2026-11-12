# 3D Visual Realism Suite — Grafika Komputer Minggu 11

## Teknologi
Java 17+, LWJGL 3.3.3, JOML, OpenGL 3.3 Core, GLSL 330, STBImage.

## Menjalankan di NetBeans 31
1. Ekstrak ZIP.
2. Open Project → pilih folder yang berisi `pom.xml` (Maven Project).
3. Pastikan JDK 17 atau 21 dipilih.
4. Tunggu Maven mengunduh dependensi.
5. Jalankan `com.grafkom.viewing3d.ThreeDViewing` atau `mvn clean compile exec:java`.

## Antarmuka
Aplikasi dilengkapi HUD profesional dengan Scene Hierarchy, panel Render Settings, indikator FPS, dan daftar shortcut. HUD dirender sebagai overlay OpenGL.

## Kontrol
- B: Phong / Blinn-Phong
- F: Flat / Smooth normal
- Z: Z-Buffer Depth Test ON/OFF
- C: Back-Face Culling ON/OFF
- T: Texture ON/OFF
- N: Nearest / Bilinear + Mipmap
- G: Shadow Mapping ON/OFF
- M: ganti objek aktif (Sphere ↔ Cube); lantai tetap ditampilkan
- Panah: orbit kamera; W/S: zoom; ESC: keluar

## Catatan implementasi
Program memakai VAO/VBO dan GLSL modern (bukan immediate mode). Shadow mapping memakai depth-only FBO dan dua pass. File tekstur berada di `src/main/resources/textures/material.png` dan dimuat dari classpath, sehingga tetap dapat ditemukan saat aplikasi dijalankan dari NetBeans maupun JAR.

## Perbaikan versi revisi
- Tombol M sekarang benar-benar mengganti geometri aktif antara Sphere dan Cube.
- Shadow/depth pass menggunakan objek aktif yang sama dengan pass utama agar bayangan sesuai bentuk yang terlihat.
- HUD menandai objek aktif pada Scene Hierarchy.
- Tekstur hanya diterapkan ke objek utama; ground plane tetap memakai warna materialnya sendiri.


### Pengujian Back-Face Culling
Tekan `C` untuk mengaktifkan/menonaktifkan Back-Face Culling. Quad kecil berwarna magenta adalah **culling probe** dengan urutan vertex (winding) sengaja terbalik: quad ini harus menghilang saat culling ON dan terlihat saat culling OFF. Probe ini hanya alat demonstrasi dan tidak ikut ke shadow pass. Sphere/Cube yang tertutup penuh dapat tampak hampir sama saat culling ON/OFF karena sisi belakangnya tetap tertutup oleh depth test.
