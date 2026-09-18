# İlaç Hatırlatıcı (Wear OS + Android)

Samsung Galaxy Watch 4+ (Wear OS) ve Android telefon için **yerel depolamalı** ilaç hatırlatıcı. Bulut veya hesap yoktur; kaynak gerçek telefonda tutulur, saat önbellek + alarm ile çalışır.

## Özellikler

### Telefon (`mobile/`)

| Özellik | Açıklama |
|--------|----------|
| İlaç yönetimi | Ekleme, düzenleme, silme; ad, doz, bakıcı notu |
| Program | Günlük saatler + haftanın günleri |
| Bugün | Doz listesi, ilerleme halkası, sıradaki doz vurgusu |
| Aldım / Atla / Ertele | 10 veya 30 dk erteleme; haptic + onay animasyonu |
| Stok | İsteğe bağlı stok sayacı; alındığında düşer; düşük stok uyarısı |
| Haftalık uyum | Yüzde + halka + kısa mesaj (Bugün ve Geçmiş) |
| Bakıcı notu | Doz kartlarında ve ilaç listesinde vurgulu |
| Geçmiş | Telefon veya saatten gelen yanıtlar |
| Senkron | Üst bardaki ikon; dönen animasyon + snackbar geri bildirimi |
| Widget | Ana ekran Glance widget — sıradaki doz |
| Yedek bildirim | Saat bağlı değilse telefonda hatırlatma |

### Saat (`wear/`)

| Özellik | Açıklama |
|--------|----------|
| Liste | Sıradaki doz hero kartı, göreli zaman (“X dk içinde”), bakıcı notu |
| Hatırlatma | Titreşim + ekran; Aldım / Atla / 10–30 dk ertele |
| Senkron | Telefondan Data Layer; saatte “Senkronize et” |
| Offline | Önbellek; bağlantı gelince yanıt kuyruğu boşalır |
| Komplikasyon | Kadranda sıradaki doz saati (SHORT_TEXT) |

## Mimari

```text
Telefon (Room)  ──Data Layer──►  Saat (önbellek + AlarmManager)
     ▲                                │
     └──── /dose_response, /snooze ───┘
```

| Modül | Rol |
|-------|-----|
| `mobile/` | Kaynak gerçek (Room), yönetim UI, widget, yedek bildirim |
| `wear/` | Önbellek, alarm, hatırlatma UI, komplikasyon |
| `shared/` | Ortak modeller ve JSON senkron sözleşmesi |

**Senkron yolları**

| Yol | Tür | Amaç |
|-----|-----|------|
| `/full_sync` | DataItem | İlaçlar, programlar, olaylar (tam paket) |
| `/dose_response` | Message | Aldım / Atla yanıtı |
| `/request_sync` | Message | Saat → telefon: senkron iste / kuyruk boşalt |
| `/snooze` | Message | Erteleme (dakika) |

## Gereksinimler

- Android Studio Ladybug veya üzeri
- **JDK 17** (Gradle için; makinede JBR 21 de kullanılabilir — JBR 25 ile derleme sorunlu olabilir)
- Android SDK 35
- Fiziksel Galaxy Watch 4+ veya Wear OS emülatör (telefon ile eşli)
- Google Play Services (Wearable Data Layer)

## Kurulum (Android Studio)

1. Projeyi açın: `medication-reminder`
2. Gradle senkronunun bitmesini bekleyin
3. `mobile` → telefona, `wear` → saate yükleyin (**aynı imza / debug keystore**)
4. Telefonda ilaç ekleyin → senkron ikonu veya saatte **Senkronize et**
5. Hatırlatmada Aldım/Atla; sonuç **Geçmiş** sekmesinde görünür

## Kurulum (komut satırı)

```bash
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home"  # örnek
./gradlew :mobile:assembleDebug :wear:assembleDebug

# Telefon (USB seri numarası)
adb -s <TELEFON_SERI> install -r mobile/build/outputs/apk/debug/mobile-debug.apk

# Saat (Wi‑Fi ADB; model genelde SM-Lxxx, characteristics=watch)
adb -s <SAAT_SERI> install -r wear/build/outputs/apk/debug/wear-debug.apk
```

> **Dikkat:** `mobile` ve `wear` aynı `applicationId` kullanır. Yanlış cihaza yanlış APK yüklemeyin — telefon APK’sını saate, wear APK’sını telefona kurmayın.

Ayrıntılı kullanım, izinler ve sorun giderme: **[docs/UYGULAMA_NOTLARI.md](docs/UYGULAMA_NOTLARI.md)**

## Veri

- Kalıcı veri: telefonda Room (`medication_reminder.db`, şema sürümü **2**)
- Bulut / hesap yok
- Saat: son senkron önbelleği SharedPreferences’ta
- Şema yükseltmede `fallbackToDestructiveMigration()` vardır → **veritabanı sıfırlanabilir**; ilaçları yeniden eklemeniz gerekebilir

## İzinler (özet)

**Telefon:** bildirimler, kesin alarm, boot sonrası yeniden planlama, titreşim  
**Saat:** bildirimler, kesin alarm, boot, titreşim, komplikasyon sağlayıcı

## Dağıtım

Şu an debug / yan yükleme içindir. Play Store (imzalı release, mağaza listesi) sonraki adımdır.

## Dökümantasyon

| Dosya | İçerik |
|-------|--------|
| [README.md](README.md) | Genel bakış, özellikler, kurulum |
| [docs/UYGULAMA_NOTLARI.md](docs/UYGULAMA_NOTLARI.md) | Kullanım, senkron, widget, komplikasyon, sorun giderme |
