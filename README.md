# BrightDate Android Alarm

An Android alarm clock app that uses [BrightDate](https://brightdate.brightchain.org)
— a single scalar count of SI days since the J2000.0 astronomical epoch — as its
native time format. The app includes a home-screen widget, a full alarm scheduler,
snooze support, and a BrightDate conversion utility.

```
BRIGHTDATE
9627.47168
```

## Features

- **Alarm clock** — set alarms using BrightDate values (future dates). Alarms fire
  at the exact TAI-corrected moment the target BrightDate is reached.
- **Snooze** — dismiss or snooze a firing alarm from the notification or lock screen.
- **Home-screen widget** — live display of the current BrightDate, updating every
  30 seconds.
- **Conversion utility** — translate between BrightDate, Unix timestamp, and
  human-readable UTC/local date-time in both directions.

## Layout

- `app/` — single-module Android app, Kotlin, AGP 8.7 / Kotlin 2.0, minSdk 26.
- `brightdate/` — symlink to the upstream BrightDate reference repo (read-only;
  used here only as documentation for the J2000.0 / TAI math).

## Build

This project does not commit the Gradle wrapper jar. Generate it once:

```bash
gradle wrapper --gradle-version 8.10.2
```

Then open the project in Android Studio (Koala / Ladybug or newer) and run the
`app` configuration, or from the command line:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

After install, open the **BrightDate Alarm** app to manage alarms and use the
conversion utility. To add the widget, long-press the home screen → Widgets →
BrightDate Widget.

## Math

BrightDate is computed exactly as the upstream library defines it
(`app/src/main/java/org/brightchain/brightdate/widget/BrightDate.kt`):

```
brightdate = (taiUnixSeconds − 946_727_967.816) / 86_400
taiUnixSeconds = unixSeconds + (TAI − UTC)
```

The TAI−UTC offset is hard-coded to **37 seconds** (the value in force since
2017-01-01). If the IERS announces a future leap second, bump
`TAI_MINUS_UTC_SECONDS` in `BrightDate.kt`.

## Update cadence

The Android `AppWidgetProviderInfo.updatePeriodMillis` minimum is 30 minutes,
which is unusably slow for a live time display. We instead run a repeating
inexact `AlarmManager` tick (default **30 s**) while at least one widget
instance exists, re-armed in `onUpdate`, `onEnabled`, and `BootReceiver`.

5 fractional digits = ~0.864 s resolution, so the last digit or two will visibly
jump between updates. Lower the interval in `BrightDateWidgetProvider.UPDATE_INTERVAL_MS`
if you want smoother ticks at the cost of battery.
