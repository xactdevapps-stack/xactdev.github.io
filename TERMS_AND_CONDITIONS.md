# Terms and Conditions

Last updated: 2026-04-29

## Acceptance

By using Watt Tracker, you agree to these Terms and Conditions.

## Purpose of the App

Watt Tracker provides estimated EV charging outputs for general interest and planning convenience only.

## Important Disclaimer (Read Carefully)

The app output is an estimate only.

Do not rely on this app for safety-critical, legal, financial, operational, emergency, or professional decisions.

Actual charging cost, time, and range vary based on many real-world factors, including but not limited to charger behavior, vehicle battery health, weather, tariffs, and driving conditions.

## Calculation Logic in Plain Language

The app estimates values using simple formulas:

- Energy needed (kWh): battery capacity multiplied by the selected charge percentage increase.
  - Example: A 60 kWh battery charging from 20% to 80% means a 60% increase, so energy is 60 x 0.60 = 36 kWh.

- Cost estimate: energy needed multiplied by price per kWh.
  - Example: 36 kWh at 0.30 per kWh gives 10.80.

- Time estimate (hours): energy needed divided by charge rate (kW).
  - Example: 36 kWh at 7.2 kW gives 5.0 hours.

- Expected range after charging: full-range estimate multiplied by target state of charge.
  - Example: Full range 300 km at target 80% gives 240 km.

- Added range during this session: full-range estimate multiplied by charge percentage increase.
  - Example: Full range 300 km with a 60% increase gives 180 km added.

- Distance conversion:
  - miles to kilometers: miles x 1.609344
  - kilometers to miles: kilometers / 1.609344

## License

Unless otherwise stated, this app is distributed under the MIT License.

See LICENSE for details.

## Third-Party Maps

The optional Maps feature opens an external OpenStreetMap web page in your browser. Use of that service is subject to third-party terms and policies outside this app.

Map data attribution: © OpenStreetMap contributors.

## Limitation of Liability

To the maximum extent permitted by law, Watt Tracker and its developer are not liable for any loss, damage, or claim arising from use of the app or reliance on its estimated outputs.

## Changes to These Terms

These terms may be updated over time. Continued use of the app after changes means you accept the updated terms.

## Contact

Developer: XactDev  
Support email: TBA  
Website: TBA
