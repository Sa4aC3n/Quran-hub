# نظام تصميم تطبيق هُدى القرآن (Quran Hub Design System)

تم بناء نظام التصميم ليعكس هوية إسلامية أصيلة، مريحة للعين، تجمع بين **الأخضر الزمردي (Emerald)** و**الذهب الهادئ (Gold)**، مع تحقيق معايير الوصول العالمية **WCAG 2.1 AA** في التباين اللوني (4.5:1 للنصوص العادية، 3:1 للنصوص الكبيرة والأيقونات الأساسية).

---

## 1. الهيكل الأساسي (Architecture)

يعتمد نظام التصميم على مستويين متكاملين في Jetpack Compose:

1. **Material 3 ColorScheme القياسي**:
   يحدد ألوان النظام المرجعية (`primary`, `onPrimary`, `surface`, `surfaceContainer`, `outlineVariant`, إلخ) لدعم المكونات القياسية من M3.
2. **IslamicColors (توكنات دلالية متخصصة)**:
   توفر خصائص لونية مخصصة للأجواء القرآنية والإسلامية، ومتاحة في شجرة الـ Composables عبر `IslamicTheme.colors`.

```kotlin
// الوصول للتوكنات في أي Composable:
IslamicTheme.colors.onHero
IslamicTheme.colors.heroAccent
IslamicTheme.colors.selectedBadgeContainer
IslamicTheme.colors.selectedBadgeContent
IslamicTheme.colors.meetingModeIcon
```

---

## 2. جدول التوكنات الدلالية ونسب التباين (Token Matrix & Contrast)

### أ. منطقة الـ Hero (البنرات العلوية والبطاقات الرئيسية)
*الخلفية عبارة عن تدرج زمردي عميق (`heroGradient`)*

| التوكن | الوضع الفاتح (Light) | الوضع الداكن (Dark) | الاستخدام | نسبة التباين المحققة |
|---|---|---|---|---|
| `heroGradient` | `0xFF0A4430` → `0xFF145E45` | `0xFF0D3828` → `0xFF12281E` | خلفية البطاقات الرئيسية | - |
| `onHero` | `#FFFFFF` | `#FFFFFF` | العناوين والنصوص البيضاء داخل الـ Hero | **8.8:1** (فاتح) / **13.1:1** (داكن) |
| `heroAccent` | `#FCE9BE` (ذهب ناعم فاقع) | `#ECC276` (ذهب دافئ ساطع) | العدادات، المواعيد، النصوص المميزة | **7.2:1** (فاتح) / **7.8:1** (داكن) |

> **قاعدة أساسية:** لا يُستخدم `Gold500` الخام (`#C9A227`) على الخلفيات الداكنة لأنه لا يحقق تباين كافٍ، بل يُستخدم دائمًا `heroAccent`.

---

### ب. الشارات والعناصر المحددة والتشغيل (Selected & Active States)

| التوكن | الوضع الفاتح (Light) | الوضع الداكن (Dark) | الاستخدام | نسبة التباين المحققة |
|---|---|---|---|---|
| `selectedBadgeContainer` | `0xFF145E45` (زمردي غامق) | `0xFF5ED8A1` (زمردي مشرق) | خلفية شارة السورة/الصلاة المشغلة | - |
| `selectedBadgeContent` | `#FFFFFF` | `#003825` (زمردي داكن عميق) | نص أو أيقونة الشارة المحددة | **8.8:1** (فاتح) / **9.5:1** (داكن) |
| `activeContainer` | `0x1A145E45` (10% زمردي) | `0x245ED8A1` (14% زمردي) | خلفية البطاقة المحددة أو المشغلة | ناعمة ومريحة للقراءة |
| `badgeContainer` | `0x26C9A227` (15% ذهبي) | `0x33C9A227` (20% ذهبي) | خلفيات الأوسمة الذهبية | - |
| `goldText` | `0xFF8F6B00` (ذهب داكن متباين) | `0xFFECC276` (ذهب ناعم ساطع) | نصوص الأوسمة والرموز الذهبية | **4.9:1** (فاتح) / **7.8:1** (داكن) |

---

### ج. وضع الاجتماعات والساعة الذكية (Smartwatch & Meeting Mode)

| التوكن | الوضع الفاتح (Light) | الوضع الداكن (Dark) | الاستخدام | نسبة التباين المحققة |
|---|---|---|---|---|
| `meetingModeContainer` | `0xFFFFF0F1` (أحمر هادئ جداً) | `0xFF2A1517` (أحمر نبيذي داكن) | خلفية البطاقة عند تفعيل الوضع | - |
| `meetingModeBorder` | `0xFFEF9A9A` | `0xFFEF5350` | إطار البطاقة التحذيري | 3.2:1 (عنصر بياني) |
| `meetingModeOnContainer`| `0xFF5C000B` | `0xFFFFCDD2` | نصوص البطاقة والعناوين | **9.8:1** (فاتح) / **10.4:1** (داكن) |
| `meetingModeIcon` | `0xFFB71C1C` (قرمزي عميق) | `0xFFFF8A80` (مرجاني ساطع) | أيقونة وضع الاجتماعات النشط | **9.8:1** (فاتح) / **6.9:1** (داكن) |

---

## 3. إرشادات التطوير (Developer Guidelines)

1. **تجنب كتابة قيم ألوان ثابتة (No Hardcoded Hex/Color constants):**
   - ❌ لا تكتب `Color.White` أو `Gold500` أو `Color.Red` مباشرة داخل العناصر التفاعلية.
   - ✅ استخدم `MaterialTheme.colorScheme` أو `IslamicTheme.colors`.
2. **أزواج التباين المضمونة (Paired Tokens):**
   - داخل بطاقة Hero: استخدم `onHero` للنص و `heroAccent` للمميزات.
   - داخل زر تشغيل / شارة محددة: استخدم `selectedBadgeContainer` كخلفية و `selectedBadgeContent` كمحتوى.
   - عند عرض نص ذهبي على خلفية بيضاء/فاتحة: استخدم `IslamicTheme.colors.goldText`.
3. **التكيف التلقائي مع الوضع الداكن/الفاتح:**
   - جميع الدوال في `IslamicTheme.colors` محسوبة ديناميكياً بناءً على `isDark` الممرر في `IslamicTheme`.
