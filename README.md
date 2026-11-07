# Personal Restaurant Guide

COMP3074 Mobile Application Development I, Team P66. An Android app for keeping track of the
restaurants you visit, rating them and sharing them by email.

**Team:** Alikhan Zhilkibayev, Kerem Bas, Rasul Dadashbayli, Pintoo Himani

## Status: Functional Prototype

| Feature | State |
|---|---|
| Add, edit and remove restaurants (name, address, phones, description, tags) | Done, saved in a Room database |
| Extensible tags | Done |
| Address help: Verify formats the address and finds its coordinates | Done, Android Geocoder |
| 1-5 star rating | Done |
| Restaurant list, search by name or tag, tag filter | Done |
| Details screen | Done |
| Map preview and full-screen map with current location | Done, Google Maps SDK (needs an API key, see below) |
| Directions from your current location | Done, opens Google Maps |
| Share by email | Done |
| About screen with team members, Splash screen with logo | Done |
| Share to Facebook or Twitter (optional bonus) | Not started |

## Run it

Open the project in Android Studio and run the `app` configuration, or use `gradlew installDebug`.
The app starts with six sample restaurants, which are added the first time the database is created.

### Google Maps API key

The maps need a key, which each developer adds locally. The key is never committed.

1. In the [Google Cloud console](https://console.cloud.google.com/), create a project, turn on
   billing and enable **Maps SDK for Android**.
2. Create an API key. Restrict it to Android apps, using the package name
   `ca.georgebrown3074.p66.personalrestaurantguide` and your SHA-1 from `gradlew signingReport`.
3. Add this line to `local.properties`, which git ignores, and rebuild:

   ```properties
   MAPS_API_KEY=your_key_here
   ```

Without a key the app still runs, and the map areas show a message instead of a map.

## How it is built

- **Language and platform:** Kotlin, Android Views with Material 3, minimum API 26
- **Screens:** one activity per screen: Splash, Main (list), AddEditRestaurant, RestaurantDetails,
  FullMap and About
- **Data access layer (`data/`):** `Restaurant` is the Room entity, `RestaurantDao` holds the queries
  and `AppDatabase` creates the database and adds the sample data. Screens observe the database
  through Kotlin Flow, so they update whenever the data changes.
- **Address lookup:** `AddressLookup` wraps Geocoder. If the lookup can't run, for example offline,
  the restaurant is still saved and the details screen tries again later.
- **Location:** the full map asks for location permission only when you tap the location button, then
  uses the Fused Location Provider.

## Tests

Database tests are in `RestaurantDaoTest`. With a device or emulator connected, run
`gradlew connectedDebugAndroidTest`.
