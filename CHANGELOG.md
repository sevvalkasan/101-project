# Değişiklik Günlüğü

## Yayınlanmamış MVP Sürümü

### Eklendi

- Yakındaki masaların koordinat, mesafe ve boş yer bilgileriyle sorgulanması
- Harita ve liste tabanlı masa keşif ekranı
- Konum izinleri, konum hataları ve simülatör/cihaz konum desteği
- Mobil uygulamadan masa oluşturma ve aktif masayı yönetme
- Ev sahibinin katılım isteklerini listelemesi, kabul veya reddetmesi
- Kullanıcının sohbet edebildiği masaları listeleyen API ve mobil ekran
- STOMP WebSocket üzerinden gerçek zamanlı sohbet ekranı
- Firebase Cloud Messaging cihaz kaydı ve kaldırma akışı
- Katılım isteği ile kabul/ret sonuçları için asenkron push bildirimleri
- Android konum/bildirim izinleri ve iOS bildirim entitlement yapılandırması
- Cihaz kaydı için `user_devices` Flyway migration'ı
- Sohbet, masa modelleri, cihaz kaydı ve push bildirimleri için testler

### Değiştirildi

- Masa cevaplarına koordinatlar, oyuncu sayıları ve durum bilgisi eklendi.
- PostgreSQL enum alanları Hibernate named enum olarak eşlendi.
- Masa dolduğunda kalan bekleyen istekler otomatik reddedilip bildirim üretiyor.
- Çıkış sırasında kayıtlı FCM token'ı backend'den kaldırılıyor.
- Ağ hataları mobil uygulamada kullanıcı dostu mesajlara dönüştürülüyor.

### Düzeltildi

- PostgreSQL native projection içindeki `Instant` değerinin `OffsetDateTime` alanına çevrilirken oluşturduğu yakındaki masalar API hatası giderildi.
- Harita sorgusuna eksik enlem ve boylam alanları eklendi.
- PostgreSQL enum alanlarında oluşabilecek tür uyuşmazlıkları giderildi.

### Bilinen Eksikler

- Push bildirimlerinin Android ve iOS gerçek cihazlarda uçtan uca doğrulanması gerekiyor.
- Mobil widget ve entegrasyon testlerinin kapsamı sınırlı.
- Production tile sağlayıcısı, deployment ve CI/CD yapılandırması bulunmuyor.
- Android/iOS release signing ve mağaza hazırlıkları tamamlanmadı.
- Proje henüz production kullanıma hazır değil.
