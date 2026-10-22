# MasaBul

MasaBul, yakındaki 101 Okey masalarını keşfetmek, yeni masa oluşturmak ve oyuncuların masalara katılmasını kolaylaştırmak için geliştirilen konum tabanlı bir mobil uygulamadır.

## Özellikler

- Telefon numarasıyla Firebase Authentication
- Kullanıcı profili oluşturma ve güncelleme
- Konuma göre yakındaki masaları listeleme
- Yeni 101 Okey masası oluşturma ve iptal etme
- Masaya katılım isteği gönderme ve istekleri yanıtlama
- Masa katılımcıları için gerçek zamanlı sohbet altyapısı
- Katılım istekleri ve kararları için push bildirimleri
- Harita ve liste görünümleri
- Ev sahibinin masa ve katılım isteklerini yönetmesi

## Teknolojiler

### Mobil uygulama

- Flutter ve Dart
- Riverpod
- Dio
- Firebase Authentication
- Firebase Cloud Messaging
- flutter_map ve OpenStreetMap
- Geolocator
- STOMP WebSocket istemcisi

### Backend

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- WebSocket
- Flyway
- Firebase Admin SDK

### Veritabanı

- PostgreSQL 16
- PostGIS
- Docker Compose

## Proje Yapısı

```text
101Project/
|-- backend/           Spring Boot REST API ve WebSocket servisi
|-- frontend/          Flutter mobil uygulaması
|-- docker-compose.yml PostgreSQL/PostGIS geliştirme ortamı
`-- .env.example       Yerel ortam değişkenleri örneği
```

## Gereksinimler

- Java 17 veya üzeri
- Maven 3.9 veya üzeri
- Flutter SDK 3.4 veya üzeri
- Docker ve Docker Compose
- Yapılandırılmış bir Firebase projesi

## Yerel Kurulum

### 1. Projeyi klonlayın

```bash
git clone https://github.com/sevvalkasan/101-project.git
cd 101-project
```

### 2. Ortam değişkenlerini hazırlayın

```bash
cp .env.example .env
```

`.env` dosyasındaki geliştirme parolalarını ve Firebase bilgilerini kendi ortamınıza göre güncelleyin. Firebase Admin servis hesabı dosyasını repoya eklemeyin; dosya yolunu `FIREBASE_SERVICE_ACCOUNT_PATH` ile belirtin. Alternatif olarak servis hesabı JSON'unu Base64 biçiminde `FIREBASE_SERVICE_ACCOUNT_JSON_BASE64` değişkenine verebilirsiniz.

### 3. Veritabanını başlatın

```bash
docker compose up -d postgres
```

PostgreSQL varsayılan olarak `localhost:5432` adresinde çalışır. Veritabanı şeması backend başlatıldığında Flyway tarafından otomatik oluşturulur.

### 4. Backend'i çalıştırın

```bash
cd backend
mvn spring-boot:run
```

API varsayılan olarak `http://localhost:8080` adresinde çalışır.

### 5. Flutter uygulamasını çalıştırın

Yeni bir terminalde:

```bash
cd frontend
flutter pub get
flutter run --dart-define=MASABUL_API_BASE_URL=http://localhost:8080
```

Android emülatörü kullanıyorsanız ana makineye erişmek için şu adresi kullanın:

```bash
flutter run --dart-define=MASABUL_API_BASE_URL=http://10.0.2.2:8080
```

Firebase platform dosyaları eksikse önce FlutterFire CLI ile yapılandırmayı tamamlayın:

```bash
flutterfire configure
```

## Testler

Backend testleri:

```bash
cd backend
mvn test
```

Flutter testleri ve statik analiz:

```bash
cd frontend
flutter test
flutter analyze
```

## Güncel Durum

Bu sürümde MVP'nin temel kullanıcı akışları kodlanmıştır:

- Telefon numarasıyla giriş ve profil tamamlama
- Konum izni alma ve yakındaki masaları harita/liste üzerinde gösterme
- Masa oluşturma, aktif masayı görüntüleme ve iptal etme
- Masaya katılım isteği gönderme; ev sahibinin kabul veya ret vermesi
- Kabul edilen masa katılımcıları için sohbet listesi, mesaj geçmişi ve gerçek zamanlı mesajlaşma
- FCM cihaz kaydı ile katılım isteği ve karar bildirimleri

Son doğrulamada backend testleri başarıyla tamamlanmış, Flutter testleri geçmiş ve `flutter analyze` herhangi bir sorun bulmamıştır. PostGIS şema entegrasyon testi çalışan bir Docker/Testcontainers ortamı gerektirir.

## Bilinen Eksikler

- Gerçek cihazlarda uçtan uca bildirim ve APNs doğrulaması tamamlanmalıdır.
- İki farklı kullanıcıyla masa kurma, katılım, sohbet ve bildirim akışları birlikte test edilmelidir.
- Mobil widget ve entegrasyon testi kapsamı genişletilmelidir.
- OpenStreetMap genel tile sunucusu production kullanımı için değerlendirilip uygun bir tile sağlayıcısı seçilmelidir.
- Android release signing, iOS signing/bundle ayarları ve mağaza hazırlıkları tamamlanmalıdır.
- Production backend, veritabanı, gizli ortam değişkenleri ve CI/CD kurulumu henüz yapılmamıştır.
- Çevrimdışı kullanım, bağlantı yeniden deneme ve kullanıcıya gösterilen hata durumları geliştirilmelidir.

Sürümdeki değişikliklerin özeti için [CHANGELOG.md](CHANGELOG.md) dosyasına bakın.

## Güvenlik

Gerçek ortam parolalarını, `.env` dosyasını veya Firebase Admin servis hesabı anahtarını repoya göndermeyin. Örnek yapılandırmalar için yalnızca `.env.example` dosyasını kullanın.

## Durum

Proje aktif olarak geliştirilmektedir ve henüz production kullanıma hazır değildir.
