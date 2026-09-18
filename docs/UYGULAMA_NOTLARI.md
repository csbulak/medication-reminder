# Uygulama notları

İlaç Hatırlatıcı’nın günlük kullanımı, senkron davranışı, widget / komplikasyon ve sık karşılaşılan sorunlar.

## İlk açılış

1. Telefonda uygulamayı açın.
2. **Bildirim** ve **kesin alarm** izinlerini verin (Android 12+ / 13+ için kritik).
3. Saat uygulamasını açın; Bluetooth ile telefon eşli olsun (Galaxy Wearable / Watch uygulaması).
4. Telefonda bir ilaç + hatırlatma saati ekleyin.
5. Telefonda üst bardaki **senkron** ikonuna basın — snackbar “Saate gönderildi…” veya “saat bağlı değil…” demeli.
6. Saatte listede ilaç / sıradaki doz kartı görünmeli.

## Telefon ekranları

### Bugün
- Günlük dozlar, ilerleme kartı, **haftalık uyum** özeti
- Sıradaki bekleyen doz kenarlıkla vurgulanır
- **Aldım** / **Atla** / **10 dk ertele** / **30 dk ertele**
- Bakıcı notu varsa doz kartında “Bakıcı notu” bandı

### İlaçlar
- Kayıtlı ilaçlar; dokununca düzenleme
- Stok bilgisi ve bakıcı notu listede görünür
- **+** ile yeni ilaç

### İlaç düzenleme
- Ad, doz, **bakıcı notu** (kartlarda öne çıkar)
- Hatırlatma saatleri (saat seçici)
- Haftanın günleri
- Stok miktarı ve düşük stok eşiği (isteğe bağlı)
- Aktif / pasif

### Geçmiş
- Tamamlanan / atlanan / kaçırılan kayıtlar
- Kaynak: Telefon veya Saat
- Haftalık uyum kartı

### Senkron geri bildirimi
- İkon basılınca döner
- Altta snackbar:
  - bağlı saat varsa: `Saate gönderildi · N ilaç · M cihaz`
  - bağlı değilse: `Veri hazır · saat bağlı değil…`
  - hata: `Senkron başarısız: …`

## Saat ekranları

### Ana liste
- Üstte marka / özet
- **Sıradaki** doz: büyük yeşil kart (saat, göreli zaman, ad, doz, not)
- Diğer yaklaşan dozlar: saat rozetli kartlar
- **Senkronize et** → telefondan güncel paket ister; kısa “Telefondan isteniyor…” ipucu

### Hatırlatma
- Kilit ekranında açılabilir
- Aldım / Atla / 10 dk ertele / 30 dk ertele
- Aldım sonrası “Kaydedildi” animasyonu

### Komplikasyon
1. Kadranı uzun bas → komplikasyon yuvası
2. **İlaç Hatırlatıcı** / sıradaki doz sağlayıcısını seçin
3. Kısa metin olarak sıradaki saat gösterilir (yaklaşık 15 dk’da bir yenilenir)

## Widget (telefon)

1. Ana ekranda boş alana uzun bas → Widget’lar
2. **İlaç Hatırlatıcı** → sıradaki doz widget’ı ekle
3. Düşük stok varsa widget’ta kısa uyarı satırı görünür

## Senkron nasıl çalışır?

1. Telefon Room’dan tam paket üretir → `/full_sync` DataItem (urgent)
2. Bağlı Wear düğümlerine `/request_sync` mesajı gider (saatin bekleyen kuyruğunu boşaltması için)
3. Saat DataItem’ı alır, önbelleği ve alarmları günceller
4. Saatten Aldım/Atla → `/dose_response`; erteleme → `/snooze`
5. Telefon kaydı yazar, gerekirse yeniden senkronlar

Bağlantı yokken saat yerel önbellekle alarm çalar; yanıtlar kuyrukta tutulur, bağlantı gelince gönderilir.

## Stok mantığı

- Stok alanı boş bırakılırsa takip kapalıdır
- **Aldım** ile miktar 1 azalır (0’ın altına inmez)
- Miktar ≤ düşük stok eşiği → “Düşük stok” uyarısı

## Haftalık uyum

- Haftanın başından (cihaz takvimi) şu ana kadar skorlanan dozlar
- Alınan / (alınan + atlanan + kaçırılan) → yüzde
- Uzun süredir bekleyen PENDING’ler kaçırılmış sayılabilir
- Veri yoksa yüzde 0 ve bilgilendirici mesaj

## Veritabanı uyarısı

Room sürümü **2**’dir ve yıkıcı migrasyon açıktır. Şema değişince yerel DB silinebilir; ilaçları yeniden eklemeniz gerekir. Yedek/export henüz yoktur.

## ADB ile yükleme ipuçları

```bash
adb devices -l
```

| Cihaz | Nasıl ayırt edilir |
|-------|-------------------|
| Telefon | `characteristics=phone`, örn. `SM-S938B` |
| Saat | `characteristics` içinde `watch`, örn. `SM-L500` |

Birden fazla bağlantı (USB + Wi‑Fi) varsa `-s <seri>` kullanın.

Saatte Wi‑Fi hata ayıklama:
1. Geliştirici seçenekleri → ADB hata ayıklama + Wi‑Fi üzerinden hata ayıklama
2. Telefondan veya bilgisayardan `adb pair` / `adb connect`

USB telefonda “Dosya aktarımı / MTP” modu genelde yeterlidir.

## Sorun giderme

| Belirti | Olası neden / çözüm |
|---------|---------------------|
| Senkron “saat bağlı değil” | Bluetooth / Galaxy Wearable eşlemesi; saatte uygulama yüklü mü |
| Saatte liste boş | Telefondan senkron; saatte Senkronize et; ilaç aktif mi |
| Alarm çalmıyor | Kesin alarm izni; pil optimizasyonu kısıtı; boot sonrası ilk açılış |
| Bildirim yok (telefon) | POST_NOTIFICATIONS izni |
| Widget güncellenmiyor | İlaç kaydı / doz sonrası uygulama açıldığında yenilenir |
| Komplikasyon boş | Saatte en az bir yaklaşan PENDING doz; komplikasyonu yeniden seç |
| Telefonda “saat uygulaması” açıldı | Yanlışlıkla wear APK yüklendi → `mobile-debug.apk` ile yeniden kur |
| Derleme / JDK hatası | JBR 25 yerine JDK 17 veya JBR 21 kullanın |

## Bilinçli sınırlar (şu an yok)

- Bulut yedek / hesap
- JSON dışa–içe aktarma
- iOS / watchOS
- Play Store sürümü (imzalı release paketi hazır değil)

## İlgili dosyalar

- Ürün özeti: [../README.md](../README.md)
- Senkron sabitleri: `shared/.../SyncPaths.kt`
- Telefon DB: `mobile/.../AppDatabase.kt`
- Widget: `mobile/.../widget/`
- Komplikasyon: `wear/.../complication/`
