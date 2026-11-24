# نیچ مارکت 📚

**اپلیکیشن اندروید بانک سوالات و آزمون‌های استخدامی دستگاه‌های اجرایی** — فروش بسته‌های تست از طریق پرداخت درون‌برنامه‌ای **مایکت**.

سازنده: **پارسا اپس** (سازنده «جزیره فندقی؛ آموزش و بازی کودک»)

---

## امکانات

- 🏠 **دسته‌بندی آزمون‌ها** بر اساس دستگاه‌های استخدامی (عمومی، آموزش و پرورش، بانک‌ها، دادگستری و …)
- 🧪 **آزمون شبیه‌ساز زمان‌دار** با شمارش معکوس و نوار پیشرفت
- ✅ **نمره‌گذاری خودکار** + کارنامه با درصد، تعداد درست/غلط/بی‌جواب
- 🔍 **مرور تشریحی سوالات** بعد از آزمون (پاسخ صحیح + توضیح)
- ❤️ **علاقه‌مندی‌ها** (بوکمارک سوال‌های مهم)
- 📊 **تاریخچه کارنامه‌ها** با درصد و رتبه‌بندی قبلی
- 🛒 **پرداخت درون‌برنامه‌ای مایکت** با بازیابی خریدها
- 🌙 پشتیبانی کامل **فارسی و RTL** + تم تیره/روشن — کاملاً **آفلاین** (بدون سرور)
- 🤖 **بیلد خودکار APK** با GitHub Actions (پوش کنید، APK آماده دانلود شود)

## تکنولوژی‌ها

| مورد | انتخاب |
|---|---|
| زبان | Kotlin |
| UI | Jetpack Compose + Material 3 |
| معماری | MVVM (ViewModel + StateFlow) |
| ناوبری | Navigation Compose |
| ذخیره‌سازی | DataStore + kotlinx.serialization |
| داده سوالات | فایل‌های JSON داخل `assets/questions/` |
| پرداخت | SDK رسمی مایکت (`ir.myket:billing:2.2`) |
| CI | GitHub Actions → خروجی APK |

---

## راه‌اندازی سریع

### ۱. باز کردن پروژه

پروژه را در **Android Studio** (نسخه Ladybug به بعد) باز کنید و اجازه دهید Gradle Sync شود.

> 💡 فایل `gradle/wrapper/gradle-wrapper.jar` (باینری) در مخزن قرار داده نشده است. قبل از اولین Sync آن را از **فایل ZIP سورس** همین پروژه کپی کنید، یا از یکی از پروژه‌های اندرویدی قبلی خودتان پوشه `gradle/wrapper` را کپی کنید، یا اگر Gradle نصب دارید در ریشه پروژه اجرا کنید:
> ```bash
> gradle wrapper --gradle-version 8.10.2
> ```

### ۲. ساخت اپ در پنل مایکت

1. در [پنل توسعه‌دهندگان مایکت](https://developers.myket.ir) یک اپ جدید با شناسه `com.parsaapps.nichmarket` بسازید.
2. برای هر بسته پولی، یک **محصول درون‌برنامه‌ای (In-App Product)** بسازید و **SKU** آن را دقیقاً مثل فایل JSON وارد کنید. SKUهای فعلی:
   - `pkg_omoomi_full_1`
   - `pkg_amoozesh_full_1`
   - `pkg_bank_full_1`
   - `pkg_dadgostari_full_1`
   > ⚠️ قیمت درج‌شده در پنل مایکت باید با `priceToman` فایل JSON یکی باشد.
3. **کلید عمومی (RSA Base64)** اپ را از پنل کپی کنید و در `app/build.gradle.kts` جایگزین `REPLACE_WITH_YOUR_MYKET_PUBLIC_KEY` کنید:
   ```kotlin
   buildConfigField("String", "IAB_PUBLIC_KEY", "\"کلید_عمومی_شما\"")
   ```
   > 🔒 طبق توصیه مایکت، کلید واقعی را در مخزن عمومی کامیت نکنید؛ می‌توانید از local.properties یا متغیر محیطی بخوانید.

### ۳. بیلد و تست

- بیلد دیباگ: `./gradlew assembleDebug` (در لینوکس/مک اگر خطای دسترسی گرفتید: `sh ./gradlew assembleDebug`)
- روی گوشی با **مایکت نصب و لاگین‌شده** تست خرید کنید (در حالت دیباگ، مایکت پرداخت آزمایشی دارد).

### ۴. انتشار

نسخه Release با فایل امضا (keystore) بسازید و فایل AAB/APK را در پنل مایکت بارگذاری کنید:
```bash
./gradlew bundleRelease
```

---

## بیلد خودکار APK با GitHub Actions 🤖

فایل `.github/workflows/build-apk.yml` در ریپو موجود است و به‌صورت **خودکار** بیلد می‌گیرد:

- **Debug APK** — هر بار که به شاخه `main` پوش کنید (یا از تب Actions دستی اجرا کنید). خروجی قابل نصب برای تست است و در بخش **Artifacts** همان اجرا دانلود می‌شود. نیازی به `gradle-wrapper.jar` ندارد (Gradle در CI نصب می‌شود).
- **Release APK امضاشده** — فقط در صورتی که ۴ Secret زیر را در Settings → Secrets and variables → Actions تنظیم کنید:
  | Secret | مقدار |
  |---|---|
  | `ANDROID_KEYSTORE_B64` | فایل keystore شما به‌صورت Base64 (`base64 -w0 my-release-key.jks`) |
  | `ANDROID_KEYSTORE_PASSWORD` | رمز فایل keystore |
  | `ANDROID_KEY_ALIAS` | نام alias کلید |
  | `ANDROID_KEY_PASSWORD` | رمز کلید |

  سپس در ریپو از تب **Actions → Build APK → Run workflow** اجرا کنید؛ APK امضاشده در Artifacts ظاهر می‌شود و آماده انتشار در مایکت است.

> نکته: کلید عمومی مایکت (`IAB_PUBLIC_KEY`) برای بیلد لازم نیست — فقط برای عملکرد خرید داخل اپ. برای بیلد امن در CI می‌توانید آن را به‌صورت Secret به BuildConfig تزریق کنید.

---

## افزودن / ویرایش بانک سوالات

سوالات در `app/src/main/assets/questions/` نگهداری می‌شوند:

- `index.json` لیست فایل‌های دسته‌بندی است.
- هر فایل یک **دسته (Category)** با چند **بسته (Pack)** است:

```json
{
  "id": "bank",
  "title": "استخدامی بانک‌ها",
  "emoji": "🏦",
  "description": "توضیح کوتاه دسته",
  "packages": [
    {
      "id": "bank_free_demo",
      "title": "بسته رایگان آشنایی",
      "description": "...",
      "sku": null,
      "priceToman": 0,
      "durationMinutes": 8,
      "questions": [
        {
          "id": 1,
          "text": "متن سوال؟",
          "options": ["گزینه ۱", "گزینه ۲", "گزینه ۳", "گزینه ۴"],
          "answerIndex": 2,
          "explanation": "پاسخ تشریحی"
        }
      ]
    }
  ]
}
```

**قواعد:**

- `sku: null` ⇒ بسته رایگان است؛ اگر SKU محصول مایکت را بگذارید، بسته پولی و قابل خرید می‌شود.
- `answerIndex` از صفر شروع می‌شود (۰ = گزینه اول).
- `id` سوال در هر بسته باید یکتا باشد (بوکمارک‌ها بر اساس `packId:questionId` ذخیره می‌شوند).
- بعد از افزودن فایل جدید، نامش را در `index.json` اضافه کنید.
- ⚠️ سوالات فعلی **نمونه** هستند؛ قبل از انتشار، بانک سوالات واقعی (سوالات سال‌های اخیر آزمون‌ها) را جایگزین کنید.

---

## ساختار پروژه

```
app/src/main/java/com/parsaapps/nichmarket/
├── NichMarketApp.kt            # Application + ServiceLocator (Graph)
├── MainActivity.kt             # نقطه ورود Compose
├── data/
│   ├── model/Models.kt         # مدل‌های Category / Pack / Question / Result
│   ├── QuestionRepository.kt   # بارگذاری JSON از assets
│   └── UserStore.kt            # خریدها/بوکمارک/کارنامه با DataStore
├── billing/
│   └── MyketBillingManager.kt  # پیاده‌سازی خرید درون‌برنامه‌ای مایکت
└── ui/
    ├── theme/                  # Material 3 + دارک مود
    ├── navigation/             # NavHost + BottomBar
    ├── components/             # کامپوننت‌های مشترک
    └── screens/
        ├── home/               # خانه (دسته‌بندی‌ها)
        ├── category/           # لیست بسته‌ها + خرید
        ├── exam/               # آزمون زمان‌دار
        ├── result/             # کارنامه + مرور تشریحی
        ├── bookmarks/          # علاقه‌مندی‌ها
        ├── results/            # تاریخچه کارنامه
        └── about/              # درباره + لینک جزیره فندقی
```

## نکته‌های فروش (درآمدزایی)

- **بسته رایگان** (نمونه ۱۰ سوالی) برای جذب کاربر و نمایش کیفیت؛ بسته‌های پولی قیمت ۳۹ تا ۴۹ هزار تومان دارند (قابل تغییر در JSON و پنل مایکت).
- با رشد کاربران، **بسته‌های جدید** (سال‌های جدید آزمون، دستگاه‌های بیشتر) فقط با اضافه‌کردن JSON و محصول در پنل مایکت عرضه می‌شوند — بدون تغییر کد.
- در صفحه «درباره»، لینک **جزیره فندقی** در مایکت و بازار برای تبلیغ متقابل اپ‌هایتان قرار دارد.

## لینک‌های مفید

- اپ‌های پارسا: [جزیره فندقی در مایکت](https://myket.ir/app/com.parsaapps.amoozesh_fandoghi) | [جزیره فندقی در بازار](https://cafebazaar.ir/app/com.parsaapps.amoozesh_fandoghi)
- مستندات SDK پرداخت مایکت: [myketstore/billing-sample-android](https://github.com/myketstore/billing-sample-android)

---

*ساخته‌شده با ❤️ توسط پارسا اپس*
