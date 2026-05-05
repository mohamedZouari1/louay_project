# 🎓 Smart Campus Manouba

A professional, high-performance digital ecosystem for the University of Manouba. This project bridges the gap between students, administration, and campus services through a seamless mobile experience and a robust Django-powered backend.

---

## 📱 Mobile App (Android)

Built with **Modern Android Architecture** and **Material Design 3**, the Smart Campus app offers a premium, LinkedIn-style social experience and a fully functional digital wallet.

### ✨ Key Features
*   **LinkedIn-Style Social Hub**:
    *   Professional "Start a Post" interface.
    *   Interactive reaction bar (Like, Comment, Repost).
    *   Author role badges (Student, Admin, Organization).
    *   Image and Text-only post support.
*   **Digital Campus Wallet**:
    *   Real-time balance tracking with persistent storage.
    *   Interactive "Top Up" and "Pay" (Café, Library) systems.
    *   Dynamic transaction history list.
*   **Campus Navigator**:
    *   Integrated maps and location search.
    *   Real-time campus statistics and event tracking.
*   **User Search**: 
    *   Professional networking cards to discover and connect with campus members.

### 🛠️ Tech Stack
*   **Language**: Java (Modern Android SDK)
*   **Networking**: Retrofit 2 + OkHttp 3 (with JSON & MultiPart support)
*   **Image Loading**: Glide
*   **Animations**: Lottie
*   **UI Components**: Material Components (M3)

---

## ⚙️ Backend API (Django)

A scalable RESTful API designed to handle high-traffic social interactions and secure user data.

### ✨ Key Features
*   **Robust Social Feed**: Paginated feed with optimized database queries using `select_related`.
*   **Resilient Serialization**: Defensive programming to handle missing user profiles gracefully without crashing.
*   **Authentication**: Secure Token-based authentication using Django REST Framework.
*   **Media Management**: Integrated support for image uploads and media storage.

### 🛠️ Tech Stack
*   **Framework**: Django 4.2 + Django REST Framework (DRF)
*   **Database**: SQLite (Development) / PostgreSQL (Production ready)
*   **Parsers**: MultiPart, JSON, and FormParser for flexible API communication.

---

## 🚀 Setup & Installation

### Backend
1. Navigate to `SmartCampusBackend/`
2. Install dependencies: `pip install -r requirements.txt` (if available) or `pip install django djangorestframework django-cors-headers pillow`
3. Run migrations: `python manage.py migrate`
4. Start the server: `python manage.py runserver 0.0.0.0:8080`

### Android
1. Open `SmartCampusAndroid/` in Android Studio.
2. Ensure `BASE_URL` in `Constants.java` matches your PC's local IP address.
3. Sync Gradle and Run on a real device or emulator.

---

## 📝 License
This project was developed for the **University of Manouba** digital transformation initiative.

---
**Developed with ❤️ for the Smart Campus Community.**
