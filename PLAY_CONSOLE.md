# Focus Flow — objava na Google Play (vodič korak po korak)

> Aplikacija: **Focus Flow** · paket `rs.vidanstojic.focusflow` · versionCode 1 / versionName 1.0
> Grafika za listing: `docs/play-store/` (ikonica 512×512, feature graphic 1024×500).
> Tekstovi u blokovima su na engleskom i spremni za kopiranje u Play Console.

---

## 0. Redosled (bitno za novi lični nalog)

Lični developer nalog napravljen posle novembra 2023. **ne može odmah u produkciju**:

1. Interni test (odmah, do 100 testera, bez pregleda) → provera da AAB radi sa Play App Signing-om.
2. **Zatvoreni test sa najmanje 12 testera koji su uključeni (opt-in) najmanje 14 dana bez prekida.**
   Tester koji izađe pa se vrati kreće od nule.
3. Dashboard → **Apply for production** (pitanja: kako ste našli testere, da li su koristili sve
   funkcije, šta ste promenili posle testa, ciljna publika, procena instalacija za prvu godinu).
4. Produkcija.

Sve deklaracije ispod se popunjavaju **pre** prvog slanja na bilo koju stazu (osim internog testa,
koji ide i bez pregleda).

---

## 1. Pre build-a

- [ ] **Privacy policy online.** Repo je javan, grana `release/play-prep` je na GitHub-u.
      GitHub → `vidanstojic/Screen-Manager` → **Settings › Pages** → *Source: Deploy from a branch*
      → Branch `release/play-prep`, folder `/docs` → Save. Posle 1–2 min otvori
      `https://vidanstojic.github.io/Screen-Manager/PRIVACY` i proveri da se stranica učitava.
      (Kad spojiš u `main`, prebaci izvor na `main` /docs.)
- [ ] `git push` nove commit-e sa `release/play-prep`.
- [ ] `./gradlew testDebugUnitTest` → sve zeleno.
- [ ] `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
- [ ] Release APK na telefonu (Build Variants → release → ▶): prođi listu iz poglavlja 8.

---

## 2. Store listing (Grow › Store presence › Main store listing)

**App name** (30)
```
Focus Flow
```

**Short description** (80)
```
Screen time limits, a Shorts & Reels blocker and a wake-up alarm with missions.
```

**Full description** (4000)
```
Focus Flow helps you spend less time on your phone — with limits you set yourself.

SCREEN TIME
• Hour-by-hour usage for today, plus weekly and monthly trends
• Per-app details: time spent, how often you open it and your 7-day average

LIMITS THAT ACTUALLY STOP YOU
• Daily limits per app
• Schedules: block chosen apps during work, study or night hours
• Interval mode: N sessions of M minutes, with a K-minute cool-down in between
• Emergency pause when you really need a blocked app for a few minutes

SHORTS & REELS
Block only the short-video feeds in YouTube and Instagram — regular videos, the feed and
messages keep working. Choose a full block, a daily budget or limited sessions.

MORNING LOCK
Keep social apps locked for the first minutes after you wake up, so the day doesn't start
with endless scrolling.

WAKE-UP ALARM
• Alarms with missions: solve math, type a phrase, or walk a number of steps
• Wake-up check: confirm a few minutes later that you are really up
• Wake-up detox: keep chosen apps and websites blocked for a while after the alarm

PRIVATE BY DESIGN
• Everything stays on your device — no account, no ads, no analytics
• The app does not use the internet

PERMISSIONS, EXPLAINED
• Usage access — to measure screen time and enforce limits
• Display over other apps — to show the block screen
• Accessibility service (optional) — only to recognize Shorts/Reels and, during a wake-up
  detox, the address bar of web browsers. The app asks for your consent first and never
  reads anything else.
• Alarms & reminders, notifications, physical activity (steps mission)

Focus Flow is not affiliated with YouTube or Instagram.
```

**Grafika**
- App icon: `docs/play-store/play-icon-512.png`
- Feature graphic: `docs/play-store/feature-graphic-1024x500.png`
- Phone screenshots: **najmanje 2** (preporuka 4–6), uslikaj na telefonu:
  Overview (današnje korišćenje), Limits, Shorts & Reels editor, blok ekran, Alarms, ekran alarma
  sa misijom. Format 9:16, PNG/JPEG.

**Kategorija i kontakt** (Store settings)
- App category: **Productivity**
- Email: `vidan.stojic+focusflow@gmail.com` (isti kao u privacy policy)
- Website: `https://github.com/vidanstojic/Screen-Manager` (opciono)

---

## 3. App content (Policy › App content)

### Privacy policy
```
https://vidanstojic.github.io/Screen-Manager/PRIVACY
```

### Ads
- Does your app contain ads? → **No**

### App access
- **All functionality is available without any access restrictions** (nema naloga ni prijave).

### Target audience and content
- Target age: **18 and over** (izbegava Families program i dodatne obaveze za decu).
- Appeals to children? → **No**

### Content rating (IARC upitnik)
- Kategorija: **All Other App Types** (utility / productivity).
- Sva pitanja o nasilju, seksualnom sadržaju, jeziku, drogama, kockanju → **No**.
- User interaction / content sharing → **No**; shares location → **No**;
  digital purchases → **No**.
- Očekivani rezultat: Everyone / PEGI 3.

### Data safety
- Does your app collect or share any of the required user data types? → **No**
  (podaci se obrađuju samo na uređaju i ne napuštaju ga; aplikacija nema INTERNET dozvolu,
  pa po Google-ovoj definiciji ništa ne "prikuplja").
- Rezultat na listingu: "No data collected · No data shared".

### Government apps / Financial features / News / Health
- Government app → **No**
- Financial features → **My app doesn't provide any financial features**
- News app → **No**
- Health apps → **My app does not have any health features**
  (dozvola ACTIVITY_RECOGNITION služi samo za brojanje koraka u misiji alarma).

### Accessibility API (declaration)
- Is your app an accessibility tool? → **No**
- Why does the app use the AccessibilityService API? → **App functionality**
- Does it collect or share personal/sensitive data through accessibility? → **No**

Opis (paste):
```
Focus Flow is a digital wellbeing app. With the user's consent, its Accessibility service
recognizes when the Shorts player in YouTube or the Reels player in Instagram is open
(by reading on-screen element IDs and labels) and, when a limit the user set applies,
leaves it (Back) or shows a block screen. Only while a user-started wake-up detox with
blocked websites is active, it also reads the address bar of web browsers to detect a
blocked site and returns to the home screen. It performs no other actions, does not type
or tap, and does not store, log or transmit screen content. The service is optional; the
app shows a prominent disclosure and asks for consent before opening the system settings.
```

Video (YouTube, može *unlisted*), redom:
1. Otvaranje aplikacije → Limits → uključi **Shorts & Reels** (ili Settings → Accessibility service).
2. Prikaže se dijalog **"Allow Accessibility access?"** — polako skroluj ceo tekst.
3. **Not now** → dijalog se zatvara, servis ostaje isključen.
4. Ponovo uključi → dijalog se ponovo pojavljuje → **Agree** → sistemska Accessibility
   podešavanja → uključi Focus Flow.
5. Otvori YouTube → Shorts → aplikacija izlazi iz Shorts-a / prikazuje poruku o limitu;
   obični video i dalje radi. Dodaj titl: *"Shorts limit applied by Focus Flow"*.

### Foreground service permissions
**specialUse** — `FocusMonitorService`
- Use case: *Other* (ručni unos)
- Description:
```
Continuously enforces screen-time rules the user created: app schedules, daily limits,
interval sessions with cool-downs, the morning lock and the wake-up detox. It reads the
foreground app from UsageStatsManager every few seconds while the screen is on (it is
idle while the screen is off) and shows a block screen over a restricted app. A
persistent notification ("Screen time tracking and limits are active") is shown.
```
- Impact if deferred or interrupted:
```
Limits stop working: a blocked app could be opened and used, cool-down timers and
session counters would be wrong, and the morning lock would not start.
```
- Video: uključi pravilo (npr. raspored ili dnevni limit za neku aplikaciju) → otvori tu
  aplikaciju → pojavi se blok ekran; prikaži notifikaciju servisa.

**mediaPlayback** — `AlarmRingService`
- Use case: *Media playback* / alarm
- Description:
```
Plays the sound of an alarm the user scheduled in the app, at the time the user chose,
until the user dismisses it (after an optional mission) or it auto-snoozes after 10
minutes. The service runs only while the alarm is ringing.
```
- Impact:
```
The user's alarm would not sound or would stop early, and the user could oversleep.
```
- Video: napravi alarm za 1 min → telefon zaključan → alarm zvoni preko celog ekrana →
  reši misiju → Dismiss.

### Full-screen intent
- Does your app use the full-screen intent permission for core functionality? →
  **Yes — Alarm clock** (alarm koji je korisnik zakazao u aplikaciji; Smart Alarm modul).

### Exact alarms
- Nije potrebna deklaracija: aplikacija koristi samo `SCHEDULE_EXACT_ALARM` (korisnik
  odobrava u "Alarms & reminders"), ne `USE_EXACT_ALARM`.

---

## 4. Release notes (1.0)

```
First release of Focus Flow: screen-time statistics, app limits and schedules, interval
mode, Shorts & Reels limits, morning lock, and a wake-up alarm with missions.
```

---

## 5. Zatvoreni test — plan

- Testing › Closed testing › Create track → upload `app-release.aab` → Testers: email lista
  (Google grupa ili lista adresa) sa **najmanje 12** ljudi (preporuka 15–20 zbog osipanja).
- Svaki tester otvori opt-in link i instalira aplikaciju **sa Play-a**.
- Zamoli ih da u 14 dana probaju: limit za jednu aplikaciju, Shorts/Reels, jedan alarm sa
  misijom, jutarnju blokadu. Prikupljaj utiske (Google Forms) — trebaju za prijavu za produkciju.

---

## 6. Šta je na Vidanu (ne može iz koda)

1. GitHub Pages (poglavlje 1) i push.
2. Snimci ekrana i dva videa (Accessibility, FGS).
3. Play Console: listing, App content, interni pa zatvoreni test.
4. Verifikacija identiteta naloga mora biti odobrena pre objave.

---

## 7. Poznata ograničenja (posle objave)

- YouTube u PiP-u i podeljenom ekranu nije blokiran.
- Shorts u browseru (`youtube.com/shorts`) se ne prepoznaje.

## 8. Lista provera na telefonu (release build)

- [ ] Prvo pokretanje: dozvole iz Settings (Usage access, overlay, battery, notifikacije).
- [ ] Limits → uključi Shorts & Reels sa isključenim servisom → pojavljuje se Accessibility
      dijalog; *Not now* čuva pravilo; *Agree* vodi u podešavanja.
- [ ] Isto iz Shorts editora (Save).
- [ ] Shorts/Reels blokada radi; obični video radi.
- [ ] Alarm sa misijom Steps (30 koraka) broji normalan hod.
- [ ] Alarm zvoni na zaključanom telefonu; posle Dismiss sistemska jačina alarma je kao pre.
- [ ] Jutarnja blokada: ne pali se posle dužeg dnevnog odsustva (npr. 5+ h popodne),
      pali se ujutru.
- [ ] Restart telefona → alarmi i praćenje rade.
