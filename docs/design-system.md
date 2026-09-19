# نظام تصميم تطبيق هُدى القرآن (Quran Hub Design System)

تم بناء نظام التصميم ليعكس هوية إسلامية أصيلة، مريحة للعين، تجمع بين **الأخضر الزمردي (Emerald)** و**الذهب الهادئ (Gold)**، مع الالتزام بمتطلبات التباين اللوني وفق **WCAG 2.1 AA** (4.5:1 للنصوص العادية، 3:1 للأيقونات الوظيفية والنصوص الكبيرة).

---

## 1. الهيكل الأساسي (Architecture)

يعتمد نظام التصميم على مستويين متكاملين في Jetpack Compose:

1. **Material 3 ColorScheme القياسي**:
   يحدد ألوان النظام المرجعية (`primary`, `onPrimary`, `surface`, `surfaceContainer`, `outlineVariant`, إلخ) المعتمدة للمكونات القياسية.
2. **IslamicColors (توكنات دلالية متخصصة)**:
   توفر خصائص لونية مخصصة للأجواء القرآنية والإسلامية، ومتاحة عبر `IslamicTheme.colors`.

---

## 2. جدول التوكنات الدلالية ونسب التباين المحسوبة آلياً

تم حساب جميع نسب التباين وفق معادلة sRGB Relative Luminance الرسمية:
$$L = 0.2126 R + 0.7152 G + 0.0722 B$$
$$\text{Contrast Ratio} = \frac{L_1 + 0.05}{L_2 + 0.05}$$

### أ. منطقة الـ Hero (البنرات العلوية والبطاقات الرئيسية)

| التوكن | الوضع الفاتح (Light) | الوضع الداكن (Dark) | الاستخدام | نسبة التباين المحسوبة آلياً |
|---|---|---|---|---|
| `heroGradient` | `Color(0xFF0E563E)` → `Color(0xFF0A4430)` | `Color(0xFF0D3828)` → `Color(0xFF08261B)` → `Color(0xFF051911)` | خلفية البطاقات والبنرات الرئيسية | - |
| `onHero` | `#FFFFFF` | `#FFFFFF` | نصوص وعناوين الـ Hero | **8.68:1** (فاتح أعلى) / **11.16:1** (فاتح أسفل)<br>**13.02:1** (داكن أعلى) / **18.20:1** (داكن أسفل) |
| `heroAccent` | `#FCE9BE` | `#ECC276` (`Gold400`) | أرقام العدادات، المواعيد، التمييز | **7.25:1** (فاتح أعلى) / **9.32:1** (فاتح أسفل)<br>**7.78:1** (داكن أعلى) / **10.87:1** (داكن أسفل) |

---

### ب. شارات العناصر المحددة والتشغيل (Selected & Active States)

| التوكن | الوضع الفاتح (Light) | الوضع الداكن (Dark) | الاستخدام | نسبة التباين المحسوبة آلياً |
|---|---|---|---|---|
| `selectedBadgeContainer` | `Color(0xFF145E45)` (`IslamicLightPrimary`) | `Color(0xFF5ED8A1)` (`IslamicDarkPrimary`) | خلفية شارة السورة/الصلاة المشغلة | - |
| `selectedBadgeContent` | `#FFFFFF` (`IslamicLightOnPrimary`) | `Color(0xFF003825)` (`IslamicDarkOnPrimary`) | نص وأيقونة الشارة المحددة | **7.73:1** (فاتح) / **7.42:1** (داكن) |
| `activeContainer` | `Color(0xFFD6EFE3)` (نعناعي فاتح معتم) | `Color(0xFF0E3828)` (زمردي ليلي معتم) | خلفية البطاقة المحددة أو المشغلة | **6.37:1** مع `onActiveContainer` الفاتح (`#145E45`)<br>**7.77:1** مع `onActiveContainer` الداكن (`#ECC276`) |
| `badgeContainer` | `Color(0xFFFBF2DC)` (`IslamicLightSecondaryContainer`) | `Gold500` بنسبة 20% شفافية فوق سطح الداكن `#0D281E` الناتج: `#32401F` | خلفيات الأوسمة الذهبية | **9.76:1** مع `badgeContent` الفاتح (`#4E3A00`)<br>**6.64:1** مع `badgeContent` الداكن (`#ECC276`) |
| `goldText` | `Color(0xFF7A5809)` (`GoldTextLight`) | `Color(0xFFECC276)` (`GoldTextDark`) | نصوص ورموز الأوسمة الذهبية | **6.51:1** فوق الأبيض `#FFFFFF` (و **6.20:1** فوق `#F8FAF7`)<br>**9.37:1** فوق سطح الداكن `#0D281E` |

---

### ج. أزرار مشغلي الصوت (Full Audio Player Sheet & Mini Audio Player)

| المكون | العنصر | اللون في الفاتح | اللون في الداكن | نسبة التباين المحسوبة آلياً |
|---|---|---|---|---|
| زر التشغيل الرئيسي (`btn_main_play_pause`) | الخلفية | `MaterialTheme.colorScheme.primary` (`#145E45`) | `MaterialTheme.colorScheme.primary` (`#5ED8A1`) | - |
| زر التشغيل الرئيسي | أيقونة التشغيل/الإيقاف | `MaterialTheme.colorScheme.onPrimary` (`#FFFFFF`) | `MaterialTheme.colorScheme.onPrimary` (`#003825`) | **7.73:1** (فاتح) / **7.42:1** (داكن) |
| زر التشغيل الرئيسي | مؤشر التحميل (`Buffering`) | `MaterialTheme.colorScheme.onPrimary` (`#FFFFFF`) | `MaterialTheme.colorScheme.onPrimary` (`#003825`) | **7.73:1** (فاتح) / **7.42:1** (داكن) |
| المشغل المصغر (`MiniAudioPlayer`) | مؤشر التحميل (`Buffering`) | `MaterialTheme.colorScheme.onPrimary` (`#FFFFFF`) | `MaterialTheme.colorScheme.onPrimary` (`#003825`) | **7.73:1** (فاتح) / **7.42:1** (داكن) |
| أزرار التحكم النشطة (خلط، تكرار، مؤقت نوم) | الأيقونة عند التفعيل | `MaterialTheme.colorScheme.primary` (`#145E45`) | `MaterialTheme.colorScheme.primary` (`#5ED8A1`) | **7.73:1** (فاتح) / **7.42:1** (داكن) |

---

### د. وضع الاجتماعات والساعة الذكية (Smartwatch & Meeting Mode)

| التوكن | الوضع الفاتح (Light) | الوضع الداكن (Dark) | الاستخدام | نسبة التباين المحسوبة آلياً |
|---|---|---|---|---|
| `meetingModeContainer` | `Color(0xFFFFEBEE)` | `Color(0xFF2C1314)` | خلفية بطاقة وضع الاجتماعات | - |
| `meetingModeBorder` | `Color(0xFFEF9A9A)` | `Color(0xFFEF5350)` بنسبة 50% شفافية | إطار البطاقة التحذيري | عنصر بياني محدد |
| `meetingModeOnContainer`| `Color(0xFFC62828)` | `Color(0xFFFFCDD2)` | نصوص البطاقة والعناوين | **4.92:1** (فاتح) / **12.32:1** (داكن) |
| `meetingModeIcon` | `Color(0xFFB71C1C)` | `Color(0xFFFF8A80)` | أيقونة وضع الاجتماعات النشط | **5.75:1** (فاتح) / **7.60:1** (داكن) |

---

## 3. تصحيح استنتاجات استخدام لون `Gold500`

- لون الذهب الخام `Gold500` (`#C9A227`):
  - **على الخلفيات الفاتحة البيضاء (`#FFFFFF`):** تباينه ضعيف (**2.42:1**) $\implies$ **ممنوع استخدامه** للنصوص أو الأيقونات الوظيفية على الأسطح الفاتحة، ويُستبدل بـ `GoldTextLight` (`#7A5809` بتباين **6.51:1**).
  - **على الخلفيات الداكنة العميقة (مثل `#0D3828`):** يحقق تباين **5.38:1** $\implies$ **صالح ومقبول** لمعايير WCAG AA.
  - الحكم على صلاحية اللون يعتمد على الخلفية المستخدمة خلفه مباشرة وليس على اسم اللون منفرداً.

---

## 4. مستويات التحقق من عناصر النظام

| العنصر المفحوص | تحقق ساكن من الكود | حساب آلي للألوان | اختبار واجهة / وحدة منفذ | لم يُختبر (يحتاج جهازاً فعلياً) |
|---|:---:|:---:|:---:|:---:|
| أزواج `primary` و `onPrimary` في الفاتح والداكن | ✅ | ✅ (7.73:1 و 7.42:1) | ✅ (`ThemeContrastUnitTest`) | - |
| أزواج `heroGradient` و `onHero` و `heroAccent` | ✅ | ✅ (> 7.2:1) | ✅ (`ThemeContrastUnitTest`) | - |
| زر التشغيل ومؤشر التحميل في المشغلين | ✅ | ✅ (7.73:1 و 7.42:1) | ✅ (`ThemeContrastUnitTest`) | - |
| توكنات وضع الاجتماعات `meetingMode` | ✅ | ✅ (4.92:1 إلى 12.32:1) | ✅ (`ThemeContrastUnitTest`) | - |
| ثيم البداية الباردة ومنع وميض الشاشة | ✅ | - | ✅ (`ThemeColdStartUnitTest`) | - |
| مظهر أشرطة النظام وحواف Edge-to-Edge | ✅ | ✅ | - | ⚠️ (فحص بصري مرئي على شاشة الجهاز) |
| نصوص القرآن بالرسم العثماني بجميع الأحجام | ✅ | - | - | ⚠️ (مراجعة جودة الخط العثماني عيانياً) |
