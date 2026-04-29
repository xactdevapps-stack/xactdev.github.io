# EV Charge Calc

An Android app for estimating EV charging time, energy, cost, and range.

## Prerequisites

- Android Studio Iguana or newer
- Android SDK with API level 34
- JDK 17

## Getting Started

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run on an emulator/device.

## Common Commands

- Build debug APK: `./gradlew.bat assembleDebug`
- Unit tests: `./gradlew.bat testDebugUnitTest`
- Instrumentation tests: `./gradlew.bat connectedDebugAndroidTest`

## Versioning

- Current version name: 0.3.0
- Current version code: 5

Version values are defined in app/build.gradle.kts.

## About Metadata

- Developer: Col Dawe
- Copyright: Col Dawe (2026)
- Support email: TBA
- Website: TBA
- Privacy policy: PRIVACY_POLICY.md
- Terms and conditions: TERMS_AND_CONDITIONS.md
- License: MIT

## Legal Documents

- License: LICENSE
- Privacy policy: PRIVACY_POLICY.md
- Terms and conditions: TERMS_AND_CONDITIONS.md

## How the Estimates Are Calculated (Plain English)

The app uses straightforward estimation math:

- Energy needed (kWh)
	- Take battery size and multiply by how much the charge percentage increases.
	- Example: 60 kWh battery, charging from 20% to 80% means 60% increase.
	- Energy needed = 60 x 0.60 = 36 kWh.

- Cost estimate
	- Multiply the energy needed by price per kWh.
	- Example: 36 kWh x 0.30 = 10.80.

- Time estimate (hours)
	- Divide energy needed by charger power in kW.
	- Example: 36 kWh / 7.2 kW = 5.0 hours.

- Expected range after charging
	- Multiply full-range estimate by target charge percentage.
	- Example: 300 km at 80% target = 240 km.

- Added range for the session
	- Multiply full-range estimate by percentage increase.
	- Example: 300 km and a 60% increase = 180 km added.

- Distance conversions
	- Miles to kilometers: miles x 1.609344
	- Kilometers to miles: kilometers / 1.609344

## Disclaimer

Outputs are estimates for general interest only.
Do not rely on this app for safety-critical, legal, financial, emergency, or professional decisions.
