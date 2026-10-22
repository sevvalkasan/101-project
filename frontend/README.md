# MasaBul Flutter Uygulaması

Bu klasör MasaBul'un Flutter mobil istemcisini içerir. Uygulamada Firebase telefon girişi, profil tamamlama, konum tabanlı masa keşfi, harita ve liste görünümleri, masa yönetimi, katılım istekleri, STOMP tabanlı sohbet ve FCM bildirim kaydı bulunur.

## Çalıştırma

Firebase platform yapılandırmaları eksikse önce çalıştırın:

```bash
flutterfire configure
```

Bağımlılıkları yükleyin:

```bash
flutter pub get
```

iOS Simulator için:

```bash
flutter run --dart-define='MASABUL_API_BASE_URL=http://127.0.0.1:8080'
```

Android Emulator için:

```bash
flutter run --dart-define='MASABUL_API_BASE_URL=http://10.0.2.2:8080'
```

Fiziksel cihaz kullanılırken API adresini bilgisayarın aynı ağdaki IP adresiyle değiştirin.

## Kontroller

```bash
flutter test
flutter analyze
```
