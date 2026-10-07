# ScreenManager (Focus Flow) — kako sistem radi

> Dokument opisuje stanje grane `refactor/ui` (engine: `refactor/phase1-engine`, commit-i `f18f733`, `e61ca36`, `2d199e3`, `fcfdb9c`; UI: poglavlje 10).
> Za svaki deo: **šta** radi, **gde** je u kodu, **zašto** je tako urađeno i **kako** radi iznutra.
> Putanje su relativne u odnosu na `app/src/main/java/com/example/screenmanager/`.

---

## Sadržaj

1. [Velika slika](#1-velika-slika)
2. [Slojevi i paketi](#2-slojevi-i-paketi)
3. [Tok podataka: od Android događaja do grafika](#3-tok-podataka-od-android-događaja-do-grafika)
4. [Baza podataka (Room v5)](#4-baza-podataka-room-v5)
5. [Pravila i RulesEngine](#5-pravila-i-rulesengine)
6. [FocusMonitorService — sprovođenje pravila](#6-focusmonitorservice--sprovođenje-pravila)
7. [Blok ekran (overlay)](#7-blok-ekran-overlay)
8. [Shorts/Reels blocker (AccessibilityService)](#8-shortsreels-blocker-accessibilityservice)
9. [Jutarnja blokada i veza sa Smart Alarm-om](#9-jutarnja-blokada-i-veza-sa-smart-alarm-om)
10. [UI sloj](#10-ui-sloj)
11. [Pokretanje, dozvole i životni ciklus](#11-pokretanje-dozvole-i-životni-ciklus)
12. [Vreme, dani i DST](#12-vreme-dani-i-dst)
13. [Testovi i verifikacija](#13-testovi-i-verifikacija)
14. [Dijagnostika na uređaju](#14-dijagnostika-na-uređaju)
15. [Istorija bagova i ispravki](#15-istorija-bagova-i-ispravki)
16. [Otvorena pitanja i sledeći koraci](#16-otvorena-pitanja-i-sledeći-koraci)

---

## 1. Velika slika

```
 Android UsageStatsManager ──► UsageEventSource ──► SessionReconstructor ──► app_usage_log (sirove sesije)
        (događaji)              (adapter)            (čist Kotlin)                 │
                                                                                   ▼
                                                    UsageSplitter (seče po satima) ──► usage_hourly (rollup)
                                                                                   │
                     ┌─────────────────────────────────────────────────────────────┤
                     ▼                                                             ▼
          UI (Dashboard, Details)                               FocusMonitorService (današnja potrošnja)
                                                                                   │
 SettingsRepository ──► RulesSnapshot ─┐                                           ▼
 RuntimeStateRepository ─► RuntimeState ├──────────────────────────────►  RulesEngine.evaluate()
                                       ┘                                           │
                                                                                   ▼
                                                                     BlockOverlayController (blok ekran)
```

Tri ključne ideje:

- **Jedan izvor istine za potrošnju:** samo sync (`UsageStatsRepository.syncUsageEvents`) upisuje potrošnju. Sve ostalo je čita.
- **Jedan izvor istine za odluku o blokadi:** `RulesEngine`, čista Kotlin klasa bez Android zavisnosti. Pravila koja korisnik podesi u UI-ju su tačno ona koja se izvršavaju.
- **Logika je odvojena od Androida:** rekonstrukcija sesija, sečenje po satima, pravila i interval mod su u `domain/usage` i `domain/rules`, testirani JUnit-om bez emulatora. Android klase su tanki adapteri oko njih.

---

## 2. Slojevi i paketi

| Paket | Uloga | Zavisi od Androida? |
|---|---|---|
| `model/` | Korisnička pravila (`AppLimitRule`, `ScheduleRule`, `SessionLimitRule`, `ShortVideoConfig`, `WakeUpConfig`, `EmergencySessionConfig`, `AlarmRule`, `AppOption`) | **Ne** |
| `domain/usage/` | `SessionReconstructor`, `ForegroundResolver`, `UsageSplitter` | **Ne** |
| `domain/rules/` | `RulesEngine`, `ScheduleWindows`, `SessionLimitTracker`, `RulesModels` | **Ne** |
| `domain/wakeup/` | Ugovor `WakeUpLockoutTrigger` + `WakeUpSource` | **Ne** |
| `domain/` (ostalo) | `TimeBuckets`, `UsageStatsRepository`, `ServiceLocator`, `PermissionStateChecker`, `AlarmScheduler` | Da |
| `data/local/` | Room entiteti, DAO-i, baza | Da (Room) |
| `data/repository/` | Repozitorijumi nad DAO-ima, `SettingsRepository`, `RuntimeStateRepository`, `WakeUpLockoutController` | Da |
| `data/usage/` | `UsageEventSource` (adapter nad `UsageStatsManager`) | Da |
| `data/apps/` | `InstalledAppsSource` — imena i lista instaliranih aplikacija (PackageManager, keširano) | Da |
| `service/` | `FocusMonitorService`, `ShortsAccessibilityService`, `ShortsDetector`, overlay, receiver-i | Da |
| `worker/` | `UsageAggregationWorker` (periodični sync + retencija) | Da |
| `ui/` | Compose UI: tema, komponente, navigacija i po jedan folder za svaki ekran (vidi [poglavlje 10](#10-ui-sloj)) | Da |

**Zašto je logika odvojena:** TRS traži da engine bude odvojen od UI-ja i spreman za Phase 2 (Smart Alarm, cloud backup). Paketi `domain/usage` i `domain/rules` mogu se bez izmena prebaciti u zaseban Gradle modul (`:core:engine`).

**Dependency injection:** `domain/ServiceLocator.kt` je jednostavna fabrika. Room baza je singleton, a `SettingsRepository` se kešira da bi UI, servisi i worker delili iste Flow-ove. Ostali repozitorijumi su jeftini stateless omotači nad DAO-ima.

---

## 3. Tok podataka: od Android događaja do grafika

### 3.1 Izvor: `data/usage/UsageEventSource.kt`

**Šta:** čita `UsageStatsManager.queryEvents(from, to)` i prevodi sistemske tipove u naš model `RawUsageEvent(timestamp, packageName, kind)`:

| Android tip | Naš `RawEventKind` |
|---|---|
| `ACTIVITY_RESUMED` | `RESUMED` |
| `ACTIVITY_PAUSED` | `PAUSED` |
| `SCREEN_NON_INTERACTIVE` | `SCREEN_OFF` |
| `DEVICE_SHUTDOWN` | `SHUTDOWN` |

Ostali tipovi (STOPPED, NOTIFICATION_INTERRUPTION, STANDBY_BUCKET_CHANGED...) se ignorišu.

**Dodatno:**
- `excludedPackages`: svi launcher-i (aktivnosti sa `CATEGORY_HOME`, npr. `com.sec.android.app.launcher`), sama aplikacija i `com.android.systemui`. Ne broje se kao korišćenje, kao što ni Digital Wellbeing ne broji launcher.
- `hasAccess()` proverava AppOps `GET_USAGE_STATS`. **Zašto je važno:** bez dozvole `queryEvents` ne baca izuzetak nego vraća **prazan rezultat**. Upravo to je izazvalo bag „svuda 0“ (vidi §15).

### 3.2 Rekonstrukcija sesija: `domain/usage/UsageSessions.kt` → `SessionReconstructor`

**Šta:** od niza događaja pravi listu sesija `UsageSession(packageName, start, end)`.

**Kako (model „jedna aplikacija u prvom planu“):**
- `RESUMED(p)`: ako je trenutno otvorena druga aplikacija, zatvara je u tom trenutku i otvara `p`. Ako je `p` na listi isključenih (launcher), samo zatvara prethodnu.
- `PAUSED(p)`: zatvara `p` samo ako je `p` trenutno otvorena. Na Samsung-u redosled često ide RESUMED(novi) pa PAUSED(stari), pa ovo pravilo sprečava lažna zatvaranja.
- `SCREEN_OFF` / `SHUTDOWN`: zatvara sve.
- **Spajanje:** ako se ista aplikacija ponovo pojavi u roku od 5 s posle zatvaranja (prelaz između aktivnosti iste aplikacije, npr. WhatsApp Home → Conversation), to je ista sesija. Bez ovoga bi broj sesija bio nerealno veliki.
- Sesije kraće od 1 s se odbacuju.

**Rezultat** (`ReconstructionResult`):
- `closedSessions`: završene sesije.
- `openSession`: sesija koja još traje (kraj = `to`).
- `safeCursor`: od kog trenutka sledeći sync sme da čita a da ne izgubi početak otvorene sesije. To je start otvorene sesije, ili `to` ako ništa nije otvoreno.

**Zašto `safeCursor`:** stari sync je čitao od „poslednjeg sync-a“. Sesija koja je počela pre tog trenutka ostajala je bez početka i bila izgubljena, pa su prošli dani bili pogrešni (B5).

### 3.3 Sync: `domain/UsageStatsRepository.kt` → `syncUsageEvents()`

Ovo je **jedini upisivač potrošnje**. Poziva ga:
- `FocusMonitorService` na svakih 60 s dok je ekran upaljen, i pri gašenju ekrana;
- `ui/AppViewModel` svaki put kada aplikacija dođe u prvi plan (ON_RESUME), ako postoji Usage Access;
- `UsageAggregationWorker` na svakih 6 h, kao rezerva.

Globalni `Mutex` garantuje da u procesu radi samo jedan sync istovremeno.

**Koraci:**
1. **Provera dozvole.** Bez nje sync odmah izlazi (`SyncResult.NoAccess`) i **ne dira kursor**.
2. **Određivanje početka čitanja (`cursor`):**
   - `lookbackStart` = ponoć pre `SYNC_LOOKBACK_DAYS = 10` dana. Android na testiranom telefonu čuva događaje za 10 dana.
   - **Backfill:** ako za tekuću `BACKFILL_VERSION` pun backfill još nije uspeo, ili je `app_usage_log` prazna, kursor se ignoriše i čita se ceo prozor od 10 dana. Pre upisa se brišu sesije iz tog prozora (`deleteStartingFrom`), da ne ostanu delimične sesije iz ranijih pogrešnih sync-ova.
   - Inače: `cursor = USAGE_SYNC_CURSOR` (ograničen na `[lookbackStart, now]`).
3. `events = eventSource.read(cursor - 1, now)`, zatim `reconstruct(events, now)`.
4. **Upis sirovih sesija:** `app_usage_log.upsertAll(...)` sa `REPLACE` po jedinstvenom ključu `(packageName, startTimeStamp)`. Otvorena sesija se pri sledećem sync-u prepisuje dužim krajem umesto da se duplira.
5. **Preračun rollup-a:** uzimaju se sve sirove sesije koje se preklapaju sa `[ponoć dana kursora, now]`, seku se po satima (`UsageSplitter`), pa se u **jednoj transakciji** (`replaceFromDay`) brišu stari i upisuju novi redovi `usage_hourly` od tog dana.
6. **Kursor se pomera samo ako je pročitan bar jedan događaj.** Prazan prozor se sledeći put čita ponovo. Posle uspešnog backfill-a upisuje se `USAGE_BACKFILL_VERSION`.
7. Log: `UsageSync: Sync backfill=… from=… events=… sessions=… hourlyRows=…`.

**Kako ponovo povući celu istoriju** (npr. posle nove ispravke sync-a): povećaj `BACKFILL_VERSION` u `UsageStatsRepository`.

### 3.4 Sečenje po satima: `domain/usage/UsageSplitter.kt`

**Šta:** sesiju deli na delove koji padaju u pojedinačne lokalne sate. `HourSlice(epochDay, hour, durationMs)`.

**Zašto:** ranije je cela sesija pripisivana satu i danu u kom je **počela**. Dva sata YouTube-a od 23:30 završavala su kao 120 min u 23h (B6).

**Kako:** koristi `ZonedDateTime.truncatedTo(HOURS).plusHours(1)`, pa su granice tačne i oko prelaska na letnje i zimsko vreme (DST). Pri vraćanju sata unazad dva „02h“ sata se sabiraju u `hour = 2`. `aggregate()` sabira po `(paket, dan, sat)` i broji početke sesija (`sessionStarts`) samo u satu u kom je sesija počela.

### 3.5 Čitanje za UI

Svi grafici čitaju iz `usage_hourly` (`UsageHourlyDao`), ne računaju ništa u realnom vremenu:

| Metoda (`UsageStatsRepository`) | Upit | Koristi |
|---|---|---|
| `observeHourlyUsageForDay(dayStart)` | suma po satu za dan | Day grafik |
| `observeTotalsForDay(dayStart)` | suma po aplikaciji za dan | lista aplikacija (prati DayPicker) |
| `observeWeeklyDailyBreakdown()` | suma po danu, poslednjih 7 dana (indeks 0..6) | Week grafik |
| `observeRollingWeekTotals()` | suma po aplikaciji za istih 7 dana | lista aplikacija u Week prikazu |
| `observeMonthlyDailyBreakdown()` | suma po danu od 1. u mesecu | Month grafik |
| `observeMonthlyTotals()` | suma po aplikaciji od 1. u mesecu | lista aplikacija u Month prikazu |
| `observeHourlyUsageForApp`, `observeDailyUsageForApp`, `observeSessionCountForApp` | isto, za jednu aplikaciju | AppDetails |
| `todayUsageByPackage()` | današnja suma po paketu | RulesEngine (dnevni limiti) |

Room Flow-ovi se sami osvežavaju posle svakog sync-a (invalidacija tabele), pa je dashboard „živ“.

### 3.6 Retencija (`cleanupOldLogs`, poziva worker)

- Sirove sesije: `RAW_LOG_RETENTION_DAYS = 12` dana.
- Satni rollup: `ROLLUP_RETENTION_DAYS = 400` dana. Zato mesečni i godišnji prikazi mogu da postoje iako Android čuva događaje samo 10 dana.

---

## 4. Baza podataka (Room v5)

Fajl: `data/local/ScreenManagerDatabase.kt`, ime `screen_manager.db`, `version = 6`.

- **5 → 6:** prava migracija `MIGRATION_5_6` (4× `ALTER TABLE short_video_configs ADD COLUMN ...` za `mode`, `sessionLengthMinutes`, `maxSessions`, `cooldownMinutes`), pa se pravila i istorija čuvaju. Default vrednosti u SQL-u moraju biti iste kao `@ColumnInfo(defaultValue)` u `ShortVideoConfigEntity`, inače Room pri otvaranju baze prijavi da migracija nije ispravna. Provereno: `PRAGMA table_info` posle migracije je jednak Room-ovom očekivanom `TableInfo`.
- **Starije od 5:** `fallbackToDestructiveMigration(dropAllTables = true)`. Pravila se ponovo seed-uju, a istorija se povlači iz UsageStats.

| Tabela | Entitet | Sadržaj | Ključ |
|---|---|---|---|
| `app_usage_log` | `AppUsageLog` | sirove sesije | `id`; UNIQUE `(packageName, startTimeStamp)`; indeks `endTimeStamp` |
| `usage_hourly` | `UsageHourlyEntity` | rollup `(paket, epochDay, sat) → durationMs, sessionStarts` | PK `(packageName, epochDay, hour)` |
| `app_internal_state` | `AppInternalState` | key → Long (kursor, kazne, ekran...) | `stateKey` |
| `app_limit_rules` | `AppLimitRuleEntity` | dnevni limiti | `id` |
| `schedule_rules` | `ScheduleRuleEntity` | zakazane blokade | `id` |
| `session_limit_rules` | `SessionLimitRuleEntity` | interval mod (M/N/K) | `id` |
| `session_limit_state` | `SessionLimitStateEntity` | runtime stanje interval moda (i `shorts:<paket>` za Shorts sesije) | `ruleId` |
| `short_video_configs` | `ShortVideoConfigEntity` | Shorts/Reels pravilo (jedan red): mod i parametri | `"shorts_global"` |
| `wake_up_configs` | `WakeUpConfigEntity` | jutarnja blokada (jedan red) | `"wakeup_global"` |
| `emergency_sessions` | `EmergencySessionConfigEntity` | emergency sesija (`activeUntilMillis`) | `"emergency_global"` |
| `alarms` | `AlarmEntity` | Smart Alarm | — |

**Ključevi u `app_internal_state`** (`domain/AppStateKeys.kt`):

| Ključ | Značenje |
|---|---|
| `usage_sync_cursor` | odakle sledeći sync čita |
| `last_usage_event_sync_at` | kad je bio poslednji sync |
| `usage_backfill_version` | da li je pun backfill uspeo za tekuću verziju |
| `wakeup_blocked_until` | kraj jutarnje blokade |
| `shorts_penalty_until:<paket>` | kraj Shorts kazne za aplikaciju |
| `shorts_watched_ms:<paket>` + `shorts_watched_day:<paket>` | današnje Shorts vreme |
| `last_screen_off_at`, `last_screen_on_at`, `last_user_interaction_at` | za wake-up heuristiku |

**Liste aplikacija u pravilima** se čuvaju kao string razdvojen zarezima (`"com.a,com.b"`), a mapiraju se u `List<String>` u `toModel()`.

**`UsageHourlyDao` je `abstract class`**, a ne interface, jer ima `@Transaction open suspend fun replaceFromDay(...)` (brisanje i upis u jednoj transakciji).

---

## 5. Pravila i RulesEngine

### 5.1 Pravila (korisnička konfiguracija) — `model/SettingsModel.kt`

| Pravilo | Polja | Semantika |
|---|---|---|
| `AppLimitRule` | `selectedAppIds`, `dailyLimitMinutes`, `blockDurationMinutes` | **Grupni** dnevni limit: sabira se potrošnja svih aplikacija u pravilu. Kad se potroši, grupa je blokirana **do ponoći**. `blockDurationMinutes` se trenutno ne koristi (otvoreno pitanje, §16). |
| `ScheduleRule` | `startTime`, `endTime`, `daysOfWeek`, `selectedAppIds` | Blokada u vremenskom prozoru. Prozor preko ponoći pripada danu u kom je **počeo**. |
| `SessionLimitRule` | `sessionLengthMinutes` (M), `maxSessions` (N), `cooldownMinutes` (K) | Interval mod (TRS 2.4), vidi §5.4. |
| `ShortVideoConfig` | `mode`, `maxReelsWatchMinutes`, `fullAppBlockMinutes`, `sessionLengthMinutes`, `maxSessions`, `cooldownMinutes`, `selectedAppIds` | Ograničava **samo Shorts/Reels**, ostatak aplikacije radi normalno. Modovi: **BLOCKED** (uvek zatvori), **BUDGET** (dnevni budžet po aplikaciji, posle njega kazna za celu aplikaciju i Shorts zaključan do ponoći), **SESSIONS** (M/N/K samo za vreme u Shorts/Reels, po aplikaciji). Vidi §8. |
| `WakeUpConfig` | `inactivityHours`, `triggerDelayMinutes`, `blockDurationMinutes`, `selectedAppIds` | Jutarnja blokada, vidi §9. |
| `EmergencySessionConfig` | `defaultDurationMinutes`, `activeUntilMillis` | Privremeno gasi **sva** pravila. `isActive` i `activeUntilLabel` su izvedeni iz apsolutnog trenutka isteka. |

`data/repository/SettingsRepository.kt` je fasada nad svim pravilima. UI piše kroz nju, a servisi čitaju **`observeRulesSnapshot()`**, jedan `Flow<RulesSnapshot>` koji spaja svih šest izvora (`combine` + `distinctUntilChanged`).

### 5.2 Runtime stanje — `data/repository/RuntimeStateRepository.kt`

„Činjenice“, a ne pravila: kraj jutarnje blokade, Shorts kazne i brojači po paketu, stanja interval sesija, trenuci gašenja i paljenja ekrana. U engine ulazi kao `RuntimeState(todayUsageMs, wakeUpBlockedUntil, shortsPenaltyUntil, sessionStates)`.

### 5.3 `domain/rules/RulesEngine.kt`

```kotlin
fun evaluate(packageName, now, zone, rules: RulesSnapshot, state: RuntimeState): BlockDecision?
```

Redosled provere:
1. **Emergency aktivna** → `null` (ništa se ne blokira).
2. Sakupljaju se svi kandidati:
   - `SCHEDULE`: `ScheduleWindows.activeUntil(rule, now, zone)`;
   - `DAILY_LIMIT`: suma grupe ≥ limit → do ponoći;
   - `SESSION_COOLDOWN` / `SESSION_POOL_EXHAUSTED`: iz `SessionState`;
   - `SHORTS_PENALTY`: kazna za paket još traje;
   - `WAKE_UP`: jutarnja blokada traje i paket je na listi.
3. Vraća se kandidat sa **najkasnijim** `blockedUntil`, da korisnik vidi realno vreme do otključavanja.

`BlockDecision(packageName, reason, blockedUntil, ruleName)` sadrži i `message` za overlay i `remainingMs(now)`.

**Zašto čista funkcija:** svi ulazi, uključujući `now` i `zone`, dolaze spolja, pa se svaki scenario testira bez uređaja (`RulesEngineTest`).

### 5.4 Interval mod — `domain/rules/SessionLimitTracker.kt`

Čista state-mašina. Servis je zove na svaki tick (2 s) za svako uključeno pravilo:

```kotlin
advance(rule, previous: SessionState?, foregroundInRule: Boolean, now, zone): SessionState
```

- Novi dan → stanje se resetuje.
- **Sesija traje:**
  - Ako je od poslednjeg viđenja prošlo više od `RESUME_GRACE_MS = 30 s`, sesija je završena u trenutku poslednjeg viđenja i počinje pauza K. Isto važi kad je ekran bio ugašen ili servis nije radio.
  - Inače, ako je aplikacija u prvom planu, `activeMs` raste za delta vremena, ograničeno na 10 s po tick-u. Kad `activeMs ≥ M`, sesija se završava i počinje pauza `now + K`.
- **Sesija ne traje:** ako je aplikacija u prvom planu, nije pauza i `sessionsUsed < N`, počinje nova sesija (`sessionsUsed + 1`).

Engine iz stanja zaključuje:
- `frozenUntil > now` → `SESSION_COOLDOWN`;
- `sessionsUsed ≥ N` i sesija ne traje → `SESSION_POOL_EXHAUSTED` do ponoći.

Stanje se čuva u `session_limit_state` pri svakoj strukturnoj promeni (nova ili završena sesija, pauza, novi dan) i najmanje na 15 s. Tako preživljava restart procesa.

### 5.5 `domain/rules/ScheduleWindows.kt`

Za noćni prozor (start > end, npr. 22:00–07:00):
- posle starta → aktivan ako je **danas** u listi dana;
- pre kraja → aktivan ako je **juče** u listi dana (prozor je počeo juče).

Primer: PON–ČET 22–07 blokira petak u 02:00, ali ne i ponedeljak u 02:00. Stari engine je gledao tekući dan i grešio.

---

## 6. FocusMonitorService — sprovođenje pravila

Fajl: `service/FocusMonitorService.kt`. Foreground servis tipa `specialUse`, sa trajnom notifikacijom.

**Pokretanje:** `FocusMonitorService.start(context)`. Servis se pokreće samo ako postoji Usage Access. Zove se iz `MainActivity.onResume()` i iz `BootReceiver`-a (BOOT_COMPLETED, MY_PACKAGE_REPLACED).

**Dve petlje** (korutine na `Dispatchers.Default`). Obe su suspendovane dok je ekran ugašen (`interactive.first { it }`), pa tada nema CPU rada:

1. **Monitor, na svake 2 s** (`tick()`):
   1. `pollForeground()` čita samo **nove** događaje od prošlog tick-a i hrani `ForegroundResolver`, koji pamti trenutnu aplikaciju između poziva. Aplikacija u kojoj korisnik sedi satima bez novih događaja i dalje važi kao aktivna (stari bag B3: 15 s prozor).
   2. Ako emergency nije aktivna, `SessionLimitTracker.advance` za svako interval pravilo.
   3. `RulesEngine.evaluate(...)` sa trenutnim `RulesSnapshot`-om. Pravila, wake-up kraj i Shorts kazne su `StateFlow`-ovi, pa nema DB upita po tick-u.
   4. `withContext(Dispatchers.Main)`: `overlay.show(decision)` ili `overlay.hide()`.
2. **Sync, na svakih 60 s:** `syncUsageEvents()`, zatim keš `todayUsage = todayUsageByPackage()`.

**Današnja potrošnja za limite:** `liveUsage()` = keš iz poslednjeg sync-a + deo tekuće sesije posle poslednjeg sync-a (`now - max(lastSyncAt, foreground.since)`). Limit je tačan u sekundi, a ne „do 60 s kasni“.

**Ekran ugašen / upaljen** (dinamički `BroadcastReceiver`):
- OFF: skriva overlay, pamti `last_screen_off_at`, čuva stanje sesija, radi sync.
- ON: ako je ekran bio ugašen ≥ `inactivityHours`, pokreće proveru jutarnje blokade (§9).

---

## 7. Blok ekran (overlay)

Fajlovi: `service/BlockOverlayController.kt` (prozor), `service/OverlayLifecycleOwner.kt`, `ui/feature/blocking/BlockScreen.kt` (izgled).

- `ComposeView` u prozoru `TYPE_APPLICATION_OVERLAY` preko cele površine (zahteva `SYSTEM_ALERT_WINDOW`).
- Prikazuje razlog i ime pravila (`decision.message`), odbrojavanje (MM:SS ili H:MM:SS), vreme kada blokada ističe i dugme „Go to home screen“. Koristi istu temu kao aplikacija.
- **Jedan ComposeView se ponovo koristi:** nova odluka samo menja `mutableStateOf`, bez uklanjanja i ponovnog dodavanja prozora, pa nema treperenja.
- **Sve metode su `@MainThread`.** Ranije su zvane sa pozadinske niti, što ruši `WindowManager` i `LifecycleRegistry` (B12).
- **`OverlayLifecycleOwner` implementira `LifecycleOwner` i `SavedStateRegistryOwner`.** Compose van Activity-ja bez `ViewTreeSavedStateRegistryOwner` baca `IllegalStateException` (B13).
- Kad korisnik ode na Home, sledeći tick vidi launcher i skriva overlay. Sistemski Back ne zatvara overlay.

---

## 8. Shorts/Reels blocker (AccessibilityService)

Fajlovi: `service/ShortsAccessibilityService.kt`, `service/ShortsDetector.kt`, `res/xml/accessibility_service_config.xml`.

**Konfiguracija servisa:** `packageNames="com.google.android.youtube,com.instagram.android"`, pa sistem uopšte ne budi servis za druge aplikacije. Događaji: window state/content change i view scrolled.

**Detekcija (`ShortsDetector.isShortFormVisible`):**
1. Brza nativna pretraga `findAccessibilityNodeInfosByViewId` po poznatim ID-jevima plejera:
   - YouTube: `reel_player_page_container`, `reel_recycler`, `reel_watch_player`;
   - Instagram: `clips_viewer_view_pager`, `clips_viewer_container`, `root_clips_layout`.
2. Rezervni BFS, ograničen na 600 čvorova, po ID-jevima koji sadrže ključne reči plejera, uz isključivanje ID-jeva sa `tab`, `shelf`, `nav`, `pivot`, `button`, `icon`.
3. Pogodak važi samo ako je čvor vidljiv i pokriva ≥ 60% ekrana.

**Zašto:** staro pravilo („tekst sadrži shorts“) hvatalo je i tab „Shorts“ u donjoj navigaciji, pa se ceo YouTube brojao kao Shorts (B7).

**Performanse (B8):**
- `onAccessibilityEvent` (main thread) samo zakazuje skeniranje sa debounce-om od 250 ms.
- Skeniranje ide na **serijskom pozadinskom dispatcher-u** (`Dispatchers.Default.limitedParallelism(1)`), pa su sve promenljive stanja jednonitne.
- Dok je plejer vidljiv, ponovo se skenira na svake 2 s (follow-up), jer pasivno gledanje jednog Short-a ne mora da šalje događaje.
- `markInteraction` se upisuje najviše jednom u 5 s. Dump stabla u logcat ide samo u debuggable build-u.

**Modovi** (`ShortVideoConfig.mode`; bira se u Settings → „Shorts and Reels“ ili u AddLimit → Shorts/Reels):

| Mod | Šta se dešava kad je plejer vidljiv | Gde je logika |
|---|---|---|
| `BLOCKED` | odmah `GLOBAL_ACTION_BACK` i Toast „Shorts/Reels su blokirani“ | `domain/rules/ShortsPolicy.kt` |
| `SESSIONS` | `SessionLimitTracker.advance()` sa sintetičkim pravilom `shorts:<paket>`, uz tick samo dok je plejer vidljiv (skeniranje na 2 s). Dok sesija traje, gledanje je dozvoljeno. Tokom pauze K i posle N-te sesije (do ponoći) sledi BACK i Toast sa preostalim vremenom. Stanje se čuva u `session_limit_state` pod `shorts:<paket>`; FocusMonitorService te ključeve ne učitava, da ih ne bi pregazio. | `ShortsPolicy` + `SessionLimitTracker` |
| `BUDGET` | brojanje vremena gledanja i kazna (opisano ispod) | `ShortsAccessibilityService.onBudgetVisible` |

Kazna za celu aplikaciju (`SHORTS_PENALTY` u `RulesEngine`) važi samo u BUDGET modu.

**BUDGET: brojanje i kazna:**
- Vreme između dve uzastopne detekcije, ako je razmak ≤ 4 s, dodaje se u `pendingWatchMs`. Na disk (`addShortsWatchedMs`) upisuje se na svakih 5 s.
- Kad današnji zbir **pređe** budžet, upisuje se `shorts_penalty_until:<paket> = now + fullAppBlockMinutes` i izvršava se `GLOBAL_ACTION_BACK`.
- Dok je budžet potrošen, svaki ulazak u Shorts/Reels do kraja dana odmah dobija BACK.
- Blokadu cele aplikacije tokom kazne ne sprovodi accessibility servis, nego `FocusMonitorService` preko `RulesEngine`-a (`SHORTS_PENALTY`).

**Kalibracija ako YouTube ili Instagram promene ID-jeve:** pusti debug build, otvori Shorts, pogledaj logcat tag `ShortsAccessibility` (ispisuje ID-jeve iz stabla) i dopuni liste u `ShortsDetector`.

---

## 9. Jutarnja blokada i veza sa Smart Alarm-om

- **Ugovor:** `domain/wakeup/WakeUpLockout.kt`, `interface WakeUpLockoutTrigger { suspend fun trigger(source: WakeUpSource, now): Long? }`, sa izvorima `SCREEN_HEURISTIC`, `ALARM_DISMISSED`, `MANUAL`.
- **Implementacija:** `data/repository/WakeUpLockoutController.kt`. Čita `WakeUpConfig` i, ako je uključen i ima aplikacija, upisuje `wakeup_blocked_until = now + blockDurationMinutes`. Već aktivnu dužu blokadu ne skraćuje.
- **Heuristika** (u `FocusMonitorService`): ekran se upalio posle ≥ `inactivityHours` mraka. Posle `triggerDelayMinutes`, ako je ekran i dalje upaljen i korisnik koristi neku aplikaciju, poziva se `trigger(SCREEN_HEURISTIC)`.
- **Smart Alarm (Phase 2):** notifikacija alarma ima akciju „Dismiss“ → `AlarmReceiver.ACTION_DISMISS_ALARM` → `trigger(ALARM_DISMISSED)`. Alarm modul zavisi samo od interfejsa, ne od engine-a (TRS 2.5).
- **Sprovođenje:** `RulesEngine` vraća `WAKE_UP` za pakete iz `WakeUpConfig.selectedAppIds` dok blokada traje.

---

## 10. UI sloj

UI je refaktorisan na grani `refactor/ui` (07.10.2026): kod je organizovan **po ekranima**, aplikacija ima početni ekran sa izborom između dva odvojena dela (Screen Manager i Smart Alarms), a izgled je prečišćena tamna tema. Tekstovi u aplikaciji su na engleskom.

### 10.1 Gde se šta nalazi

Putanje su relativne u odnosu na `ui/`.

| Folder | Šta je unutra | Pravilo |
|---|---|---|
| `theme/` | `Color.kt` (paleta `AppColors`), `Type.kt`, `Dimens.kt` (`Spacing`, oblici), `Theme.kt` (`ScreenManagerTheme`, `AppTheme`) | Boje i veličine se menjaju SAMO ovde. Ekrani ne kucaju `Color(0xFF…)`. |
| `components/` | Gradivni elementi: `AppScreen` (kostur ekrana), `AppCard`, `ListRow`, `SectionHeader`, dugmad, `SegmentedControl`, `AppSwitch`, `StepperRow`, `SliderRow`, `BarChart`, `AppIcon`, `AppUsageRow`, `DayStrip`, `AppPickerDialog`, `WeekdaySelector`, `AppTimePickerDialog` | Ne znaju ništa o ekranima ni o ViewModel-ima. |
| `common/` | Bez izgleda: `Formatters.kt` (sva trajanja i vremena), `AppIconLoader`, `OnResume`, `rememberNow` / `tickerFlow` | |
| `model/` | Modeli koje ekrani prikazuju: `UsageRange`, `AppUsageItem`, `DayOption` + mapiranja iz Room projekcija | Trajanja su u ms; u tekst se pretvaraju tek pri prikazu. |
| `navigation/` | `Screen` (lista svih ekrana), `Navigator` (back-stack), `AppNavHost` (ekran → composable), `BottomBar`, `SwipeBackContainer` | Novi ekran: dodaj ga u `Screen` i mapiraj u `AppNavHost`. |
| `feature/launcher/` | Početni ekran: izbor Screen Manager / Smart Alarms | Jedino mesto koje čita podatke oba dela. |
| `feature/overview/` | Overview tab (ulaz u Screen Manager) | |
| `feature/stats/` | Stats tab (Day / Week / Month) | |
| `feature/appdetails/` | Detalji jedne aplikacije | |
| `feature/limits/` | Limits tab + `editor/` (forme za svako pravilo) | |
| `feature/alarms/` | Smart Alarms: lista alarma + forma alarma | Ne zavisi od Screen Manager ekrana. |
| `feature/settings/` | Dozvole i „About“ | |
| `feature/blocking/` | Blok ekran (crta ga servis preko druge aplikacije) | |

Svaki `feature/<x>/` folder ima isti oblik:
- `XRoute` — stateful: uzima ViewModel, skuplja stanje, prosleđuje akcije;
- `XScreen` — stateless: prima `XUiState` + lambde i samo crta (može u Preview);
- `XViewModel` — izlaže **jedan** `StateFlow<XUiState>` i funkcije za akcije.

### 10.2 Navigacija

Aplikacija ima **dva odvojena dela**, a bira se na početnom ekranu:

```
Launcher ──► Screen Manager:  Overview · Stats · Limits   (donja traka)
   │                              └─► App details, forme pravila
   └──────► Smart Alarms:    lista alarma ─► forma alarma
```

- `Navigator` drži back-stack (`rememberSaveable`, preživljava rotaciju). Koren je uvek `Launcher`.
- Iznad korena je najviše jedan „ulaz u deo“: tab Screen Manager-a ili `Alarms`. Tabovi se međusobno **zamenjuju**, pa „nazad“ sa bilo kog taba vodi na početni ekran.
- Donja traka (Overview · Stats · Limits) pripada Screen Manager-u; u Smart Alarms-u je nema.
- Settings (dozvole) se otvara ikonicom na početnom ekranu i na Overview-u (tačka upozorenja dok fali neka dozvola).
- Sistemski back, swipe-back sa leve ivice i strelica u naslovu zovu isti `Navigator.back()`.
- Stanje ekrana (skrol, izabrani stubić, nesnimljena forma) se čuva dok je ekran na stack-u (`SaveableStateHolder`).
- Pravila ponašanja back-stack-a su pokrivena testom `ui/navigation/NavigatorTest`.

### 10.3 Ekrani

| Ekran | Šta prikazuje | ViewModel → izvor podataka |
|---|---|---|
| Launcher | dve kartice sa statusom: Screen Manager (vreme danas, broj uključenih pravila) i Smart Alarms (sledeći alarm) | `LauncherViewModel` → `UsageStatsRepository`, `SettingsRepository`, `AlarmRepository`, `AlarmScheduler` |
| Overview | današnje vreme i poređenje sa jučerašnjim danom **do istog doba dana**, mini grafik po satima, 3 najkorišćenije aplikacije, broj uključenih pravila po vrsti, aktivna emergency pauza / jutarnja blokada | `OverviewViewModel` → `UsageStatsRepository`, `SettingsRepository.observeRulesSnapshot()`, `RuntimeStateRepository` |
| Stats | Day (24 sata, izbor dana) / Week (7 dana) / Month (dani tekućeg meseca): ukupno, dnevni prosek, stubičasti grafik (dodir stubića prikazuje vrednost), lista aplikacija **za isti period** | `StatsViewModel` → `UsageStatsRepository` |
| App details | potrošnja jedne aplikacije po satu / po danu, **broj otvaranja**, 7-dnevni prosek, pravila koja je pokrivaju, „Limit this app“ | `AppDetailsViewModel` → `UsageStatsRepository`, `SettingsRepository` |
| Limits | sva pravila grupisana po vrsti: Daily limits, Session limits, Schedules, Shorts & Reels, Morning lock, Emergency pause. Switch uključuje/isključuje, klik otvara formu. | `LimitsViewModel` → `SettingsRepository` |
| Forme pravila (`limits/editor/`) | jedna forma po vrsti; rade nad radnom kopijom, u bazu upisuju tek na **Save**; brisanje uz potvrdu | `LimitsViewModel` |
| Smart Alarms | lista alarma, „Next alarm in …“, forma sa točkovima za vreme, danima i nazivom, brisanje | `AlarmsViewModel` → `AlarmRepository`, `AlarmScheduler` |
| Settings | 4 dozvole sa stanjem i dugmetom „Allow“, verzija | `AppViewModel` → `PermissionStateChecker` |

Važni detalji:
- `ui/AppViewModel` na svaki `ON_RESUME` osvežava dozvole i radi sync potrošnje. `LauncherViewModel`, `OverviewViewModel` i `StatsViewModel` tada proveravaju i da li je prošla ponoć (ViewModel živi koliko i aktivnost).
- `RuleEditorRoute` čeka da se pravila učitaju iz baze pre nego što napravi radnu kopiju — forma nikad ne kreće od podrazumevanih vrednosti umesto stvarnih. Radne kopije preživljavaju rotaciju jer su modeli pravila `Serializable`.
- Imena aplikacija i lista instaliranih aplikacija dolaze iz `data/apps/InstalledAppsSource` (keširano, van main thread-a). Ikonice učitava `AppIconLoader` na IO niti. Pravila koriste **packageName**.
- Opis pravila u jednom redu (`feature/limits/RuleSummaries.kt`) je isti na Limits, Overview i App details ekranu.
- `AlarmScheduler.nextTriggerAt()` je javan da bi UI („Next alarm“) i stvarno zakazivanje koristili isti račun.
- Shorts & Reels forma nudi samo YouTube i Instagram (`ShortsApps.supported`), jer samo njih detektor podržava.

**Uklonjeno iz starog UI-a** (lažni ili nefunkcionalni elementi): Menu ekran, donja traka bez funkcije na Home-u, hardkodovane vrednosti („4h 12m“, „82%“, „Sleep quality“), Apps/Categories prekidač, „Mobile Web / Desktop App“ legenda, „Reset Filters“, Stats/Settings tabovi u detaljima, Sound/Vibration/Snooze prekidači u alarmu (nisu se čuvali — `AlarmRule` nema ta polja), prekidač „Manual override“ (engine ga ne koristi).

---

## 11. Pokretanje, dozvole i životni ciklus

| Dozvola | Za šta | Gde se traži |
|---|---|---|
| `PACKAGE_USAGE_STATS` (Usage Access) | statistika + FGS | Settings ekran → `ACTION_USAGE_ACCESS_SETTINGS` |
| `SYSTEM_ALERT_WINDOW` | blok ekran | Settings ekran → `ACTION_MANAGE_OVERLAY_PERMISSION` |
| Accessibility servis | Shorts/Reels | Settings ekran → `ACTION_ACCESSIBILITY_SETTINGS` |
| `POST_NOTIFICATIONS` | FGS i alarm notifikacije | runtime zahtev u `MainActivity.onCreate`; kasnije Settings ekran |
| `FOREGROUND_SERVICE(_SPECIAL_USE)` | monitoring servis | manifest |
| `RECEIVE_BOOT_COMPLETED` | restart posle boot-a | manifest (`BootReceiver`) |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Smart Alarm | manifest |
| `QUERY_ALL_PACKAGES` | lista instaliranih aplikacija, launcher-i | manifest |

**Tok pri pokretanju aplikacije:**
1. `ScreenManagerApplication.onCreate` asinhrono seed-uje podrazumevana pravila ako ih nema.
2. `MainActivity.onCreate` traži dozvolu za notifikacije i pokreće Compose.
3. `MainActivity.onResume` → `FocusMonitorService.start()` ako postoji Usage Access.
4. `ScreenManagerApp` → `AppViewModel.onResume()` → osvežavanje dozvola i sync, ako postoji dozvola.
5. `FocusMonitorService.onCreate` zakazuje `UsageAggregationWorker` (6 h).

---

## 12. Vreme, dani i DST

`domain/TimeBuckets.kt` sve računa preko `java.time` + `ZoneId.systemDefault()`:
- `epochDay(millis)`, `startOfDay(epochDay)`, `startOfToday`, `startOfNextDay`, `startOfWeek` (ponedeljak), `startOfMonth`, `startOfRollingWeek`.
- **Nikad `± n * 24h` u milisekundama.** Dan oko DST prelaza ima 23 ili 25 sati (npr. 25.10.). Isto važi za `lastSevenDays()` u `ui/model/UsageUiModels.kt`.
- U bazi je dan `epochDay` (`LocalDate.toEpochDay()`), a ne timestamp ponoći, pa je grupisanje po danu trivijalno i tačno.

---

## 13. Testovi i verifikacija

JUnit 4 testovi u `app/src/test/java/com/example/screenmanager/` (60 testova: 27 za engine u `domain/`, 33 za UI logiku u `ui/`):

| Test | Pokriva |
|---|---|
| `usage/SessionReconstructorTest` | zatvaranje i otvaranje sesija, spajanje aktivnosti, launcher, gašenje ekrana, idempotentnost preko `safeCursor`, **stvarni Samsung redosled događaja**, `ForegroundResolver` |
| `usage/UsageSplitterTest` | sečenje po satima i danima, **DST 25.10.**, brojanje početaka sesija |
| `rules/RulesEngineTest` | noćni schedule, grupni dnevni limit do ponoći, emergency preko ponoći, prioritet najrestriktivnijeg, interval pauza i iscrpljen pool |
| `rules/ShortsPolicyTest` | BLOCKED uvek izbacuje; BUDGET prepušten servisu; SESSIONS: M minuta dozvoljeno, pa pauza K, pa nova sesija; posle N-te sesije zaključano do ponoći |
| `rules/SessionLimitTrackerTest` | istek M → pauza K, nema nove sesije tokom pauze, izlazak duži od grace perioda, povratak posle duge rupe, iscrpljenje N i reset sledećeg dana |
| `ui/navigation/NavigatorTest` | back-stack: koren Launcher, tabovi se zamenjuju pa „nazad“ vodi na početni ekran, Smart Alarms je odvojen od tabova, push/pop, bez duplih ekrana |
| `ui/common/FormattersTest` | format trajanja, odbrojavanja i vremena |
| `ui/feature/limits/RuleSummariesTest` | opisi pravila u jednom redu, nazivi grupa dana |
| `ui/feature/alarms/AlarmFormatTest` | zapis dana alarma ↔ `DayOfWeek` (isti tokeni koje čita `AlarmScheduler`), „in 7h 20m“ |
| `ui/model/UsageUiModelsTest` | popunjavanje sati/dana nulama, poslednjih 7 dana |
| `ui/feature/limits/RuleDraftSerializationTest` | radne kopije pravila (sa listama i skupovima kakve pravi baza) preživljavaju serijalizaciju → forme ne padaju pri rotaciji |

Pokretanje: `./gradlew testDebugUnitTest`.

**Kako je rađena provera bez Gradle-a u sesiji:** ceo `app/src/main` je kompajliran Kotlin 2.0.21 kompajlerom iz lokalnog Gradle keša, sa Compose i Parcelize pluginom, protiv `android-35.jar`. Room 2.7.0 kapt je generisao DAO klase, što validira SQL upite. Generisani Java kod je kompajliran. Stvarni `dumpsys usagestats` sa telefona (1460 događaja) pušten je kroz `SessionReconstructor`, i rezultat je odgovarao Digital Wellbeing-u (npr. YouTube 51 min).

---

## 14. Dijagnostika na uređaju

Folder `diag/` (isključen iz git-a preko `.git/info/exclude`):

- `run_diag.bat`: appops, stanje servisa, kopija baze (`run-as`, samo debug build), `dumpsys usagestats`, logcat.
- `run_diag2.bat`: `dumpsys usagestats database-info` (koliko dana sistem čuva: 10 dnevnih, 4 nedeljna, 6 mesečnih fajlova), baza, log `UsageSync`.

Korisni logcat tagovi: `UsageSync` (svaki sync), `FocusMonitorService`, `ShortsAccessibility`.

Brza provera baze (Python):
```python
import sqlite3, datetime
c = sqlite3.connect('screen_manager.db')   # zajedno sa -wal i -shm fajlovima
for d, m in c.execute("select epochDay, sum(durationMs)/60000 from usage_hourly group by epochDay"):
    print(datetime.date.fromordinal(d + 719163), m, "min")
```

---

## 15. Istorija bagova i ispravki

| ID | Problem | Uzrok | Ispravka | Commit |
|---|---|---|---|---|
| B1 | crash „Room cannot verify the data integrity“ | uklonjen indeks bez podizanja verzije | vraćen UNIQUE, verzija 5 | `f18f733` |
| B2 | blokiranje nikad ne radi | `FocusMonitorService` se nigde ne pokreće | start iz `MainActivity` i `BootReceiver` | `f18f733` |
| B3 | blokirana aplikacija dostupna posle 15 s | foreground = poslednji RESUMED u 15 s | inkrementalni `ForegroundResolver` | `f18f733` |
| B4 | duplo brojanje | servis i sync upisivali iste sesije | sync je jedini upisivač | `f18f733` |
| B5 | prošli dani pogrešni | sesije na granici sync-a gubile početak | `safeCursor` | `f18f733` |
| B6 | pogrešni sati, dani, DST | cela sesija u satu početka, `/86400000` | `UsageSplitter` + `epochDay` | `f18f733` |
| B7 | ceo YouTube se broji kao Shorts | tekst „Shorts“ u nav tabu | `ShortsDetector` po ID-ju i veličini | `f18f733` |
| B8 | ANR i baterija | obilazak stabla na main thread-u | pozadinski serijski dispatcher, debounce | `f18f733` |
| B9 | emergency ističe odmah preko ponoći | poređenje „HH:mm“ labele | `activeUntilMillis` | `f18f733` |
| B10 | nema mesečnog prikaza | retencija 7 dana | `usage_hourly` 400 dana | `f18f733` |
| B11 | pravila iz AddLimit/Scheduled se ne čuvaju | samo lokalni Compose state | čuvanje kroz `SettingsViewModel` | `f18f733` |
| B12 | crash overlay-a | WindowManager sa pozadinske niti | `withContext(Main)`, `@MainThread` | `f18f733` |
| B13 | crash Compose u overlay-u | nema `SavedStateRegistryOwner` | `OverlayLifecycleOwner` ga implementira | `f18f733` |
| B14 | **svuda 0 vremena** | sync bez dozvole je dobio prazan rezultat i pomerio kursor na „sada“ | `hasAccess()`, kursor samo uz događaje | `e61ca36` |
| B15 | **prethodni dani prazni** | samoisceljenje „prazna tabela“ se nije okinulo | eksplicitni backfill (`BACKFILL_VERSION`), lookback 10 dana | `2d199e3` |

Rezervna kopija stanja pre refaktora: `git stash apply refs/backup/pre-refactor`.

---

## 16. Otvorena pitanja i sledeći koraci

**Odluke koje čekaju (od 26.09.):**
1. `AppLimitRule.blockDurationMinutes`: dati mu značenje (npr. kazna, pa novi budžet) ili ga ukloniti iz modela i UI-ja?
2. Dnevni limit: **grupni** (sada) ili po aplikaciji?
3. Pauza K: da li važi i kad korisnik sam izađe iz aplikacije (sada važi)?

**Nije urađeno:**
- UI refaktor (grana `refactor/ui`) je kompajliran i pokriven testovima logike, ali **izgled i ponašanje na uređaju još nisu provereni**.
- Tekstovi UI-a su u Kotlin kodu, ne u `strings.xml` (prebaciti ako bude trebala lokalizacija).
- Približni podaci za dane starije od 10 dana iz agregata `queryUsageStats` (bucket-i ne počinju u ponoć).
- Izdvajanje `domain/usage` + `domain/rules` u Gradle modul `:core:engine` (TRS „Module Isolation“).
