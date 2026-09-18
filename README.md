# İlaç Hatırlatıcı (Wear OS + Android)

Samsung Galaxy Watch 4+ (Wear OS) ve Android telefon için yerel depolamalı ilaç hatırlatıcı.

## Özellikler

**Telefon**
- İlaç ekleme / düzenleme / silme (ad, doz, not)
- Günlük saat programı ve haftanın günleri
- Bugünkü doz listesi (Aldım / Atla)
- Geçmiş (telefon veya saatten gelen yanıtlar)
- Saat bağlı değilse yedek bildirim

**Saat**
- Yaklaşan doz kartları
- Zamanında titreşim + hatırlatma ekranı
- Aldım / Atla → telefona senkron
- Offline önbellek; bağlantı gelince kuyruk boşaltılır

## Mimari

| Modül | Rol |
|-------|-----|
| `mobile/` | Kaynak gerçek (Room DB), yönetim UI |
| `wear/` | Önbellek, alarm, hatırlatma UI |
| `shared/` | Ortak modeller ve JSON senkron sözleşmesi |

Senkron: Google Play Services **Wearable Data Layer** (`/full_sync` DataItem + `/dose_response` mesajı).

## Gereksinimler

- Android Studio Ladybug veya üzeri
- JDK 17
- Android SDK 35
- Fiziksel Galaxy Watch veya Wear OS emülatör (telefon ile eşli)

## Kurulum

1. Projeyi Android Studio ile açın (`medication-reminder`).
2. Gradle senkronizasyonunun bitmesini bekleyin.
3. Telefona `mobile` uygulamasını, saate `wear` uygulamasını yükleyin (aynı imza / debug keystore).
4. Telefonda bir ilaç ekleyin → saatte **Senkronize et** veya otomatik Data Layer güncellemesi.
5. Hatırlatma saatinde saatte Aldım/Atla; sonuç telefon **Geçmiş** sekmesinde görünür.

```bash
# İsteğe bağlı komut satırı
./gradlew :mobile:assembleDebug :wear:assembleDebug
```

## Veri

- Tüm kalıcı veri telefonda Room veritabanındadır (`medication_reminder.db`).
- Bulut / hesap yoktur.
- Saat yalnızca son senkron önbelleğini SharedPreferences’ta tutar.

## Notlar

- Bildirim ve kesin alarm izinlerini ilk açılışta verin.
- Saat ile telefon Bluetooth / Wi‑Fi üzerinden bağlı olmalıdır.
- Play Store dağıtımı sonraki adımdır; şu an debug kurulum içindir.
