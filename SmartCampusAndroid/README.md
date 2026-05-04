# Smart Campus Android App

A native Android application for the University of Manouba Smart Campus platform.

## Features
- **Onboarding** — 3-slide intro with smooth ViewPager2 animations
- **Authentication** — Login/Register with Django REST backend (Token auth)
- **Dashboard** — Image carousel, campus stats grid, events, quick actions
- **Interactive Map** — Full Leaflet/OSM map with 70+ POIs, search, dark mode
- **Profile** — User info, settings, favorites, logout

## Tech Stack
- **Language:** Java
- **UI:** XML Layouts + Material Design 3
- **Navigation:** Navigation Component
- **Networking:** Retrofit 2 + OkHttp
- **Map:** WebView + Leaflet.js + OpenStreetMap
- **Backend:** Django 5 + Django REST Framework

## Setup

### 1. Backend (Django)
```bash
cd SmartCampusBackend
setup_and_run.bat
```
This creates a virtual environment, installs dependencies, runs migrations, loads campus data, and starts the server at `http://127.0.0.1:8000/api/`.

### 2. Android App
1. Open `SmartCampusAndroid/` in **Android Studio**
2. Wait for Gradle sync to complete
3. If using an emulator, the default API URL (`10.0.2.2:8000`) will work
4. If using a real device, update `BASE_URL` in `Constants.java` to your PC's IP
5. Run on device/emulator

## API Endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/auth/register/` | No | Create account |
| POST | `/api/auth/login/` | No | Login |
| POST | `/api/auth/logout/` | Yes | Logout |
| GET/PUT | `/api/profile/` | Yes | User profile |
| GET | `/api/locations/` | No | Campus POIs |
| GET | `/api/events/` | No | Events |
| GET | `/api/stats/` | No | Statistics |
| POST | `/api/reports/` | Yes | Submit report |
| GET/POST/DELETE | `/api/favorites/` | Yes | Favorites |

## Project Structure
```
SmartCampusAndroid/
├── app/src/main/
│   ├── java/com/smartcampus/manouba/
│   │   ├── activities/AuthActivity.java
│   │   ├── fragments/ (6 screens)
│   │   ├── adapters/ (4 adapters)
│   │   ├── network/ (Retrofit + Auth)
│   │   └── utils/ (SharedPrefManager)
│   ├── res/ (layouts, drawables, anims, nav, menu, values)
│   └── assets/ (map.html, map.js, map.css)
```
