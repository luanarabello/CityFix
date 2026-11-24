# CityFix

An Android mobile application for civic participation and urban issue tracking, enabling citizens to report, geolocate, and follow the resolution lifecycle of local infrastructure problems.

Developed as a team academic project for the **Mobile Architectures** course.

---

## Key Features

* **Three User Roles:**
  * **Anonymous Citizen:** view the metrics dashboard and active public reports within a 10 km radius, with author privacy protection.
  * **Registered Citizen:** create and edit issue reports with multimedia attachments (photo/video) and map location, track status updates, exchange messages, and request reopening within the allowed window.
  * **Municipal Operator:** validate citizen registrations, manage categories, update issue statuses (`ABERTO`, `PEDIDO_DE_INFORMACOES`, `EM_RESOLUCAO`, `RESOLVIDO`, `ENCERRADO`), and attach resolution evidence.
* **Geolocation & Proximity:** interactive map location picker and visualization (**Google Maps**) with proximity alerts.
* **Multimedia & Background Uploads:** photo/video capture and selection (`FileProvider`, local image compression, and thumbnails) with network-constrained asynchronous uploads via **WorkManager**.
* **Notifications & Localization:** local notifications (messages, status changes, and alerts) and bilingual support (**EN / PT**).

---

## Tech Stack & Architecture

* **Language:** Kotlin
* **UI:** Jetpack Compose & Material 3
* **Architecture:** MVVM with `StateFlow` and layered separation (`model`, `data`, `viewmodel`, `ui`, `services`, `utils`)
* **Navigation:** Type-safe Navigation Compose with `@Serializable` routes
* **Backend / Cloud:** Firebase Authentication, Cloud Firestore, and Firebase Storage
* **Background & Maps:** WorkManager, Google Maps Compose, Coil 3

---

## Setup

To build and run the project locally in Android Studio:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/luanarabello/CityFix.git
   ```

2. **Configure the Google Maps API key:**
   * In the project root, create a `.env` file based on `.env.example`:
     ```env
     MAPS_API_KEY=your_api_key_here
     ```

3. **Configure Firebase:**
   * Create a project in the [Firebase Console](https://console.firebase.google.com/) with **Authentication**, **Cloud Firestore**, and **Storage** enabled.
   * Download your Android app's `google-services.json` file and place it at:
     ```text
     app/google-services.json
     ```

4. **Run the app:**
   * Open the project in Android Studio, run **Sync Project with Gradle Files**, and launch the app on an emulator or physical device (Android 8.0+ / `minSdk 26`).
