# Release build (Google Play)

## 1. Keystore (jednom, samo na svom računaru)

Android Studio → **Build › Generate Signed App Bundle or APK › Android App Bundle › Create new…**
ili iz terminala:

```bash
keytool -genkeypair -v -keystore focusflow-upload.jks -alias upload \
        -keyalg RSA -keysize 2048 -validity 10000
```

- Ovo je **upload key**; Play App Signing čuva pravi ključ za potpis aplikacije.
- `.jks` i lozinke čuvaj van repozitorijuma i napravi rezervnu kopiju (izgubljen upload key
  se može resetovati preko Play podrške, ali to traje danima).
- `*.jks` i `keystore.properties` su u `.gitignore`.

## 2. `keystore.properties` (koren projekta, pored `settings.gradle.kts`)

```properties
storeFile=C:/Users/<ti>/keys/focusflow-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

`storeFile` može biti apsolutna putanja ili relativna u odnosu na koren projekta.
Bez ovog fajla release build se pravi nepotpisan, a debug radi kao i ranije.

## 3. Build

```bash
./gradlew bundleRelease      # app/build/outputs/bundle/release/app-release.aab → Play Console
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk → test na telefonu
```

Pre svakog novog upload-a povećaj `versionCode` (i po potrebi `versionName`) u `app/build.gradle.kts`.

## 4. Pre objave proveri na release build-u

R8 (minify + shrinkResources) je uključen samo za release, pa se release testira posebno:
otvaranje svih ekrana, izmena i čuvanje pravila (rotacija ekrana usred forme), alarm,
blokade, Shorts/Reels, restart telefona.

## 5. Google Play

Ceo postupak (Pages za privacy policy, listing, deklaracije, zatvoreni test sa 12 testera / 14 dana,
lista provera) je u [`PLAY_CONSOLE.md`](PLAY_CONSOLE.md); grafika za listing je u `docs/play-store/`.
