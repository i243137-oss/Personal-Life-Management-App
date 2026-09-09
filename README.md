# Personal Life Management App

## Project Overview

A complete personal life-management application designed for daily use.

The application consists of:

- Native Android application
- Responsive web application
- Shared Node.js/Express backend
- MongoDB database
- Gemini AI integration

---

## Technology Stack

### Android

- Kotlin
- Jetpack Compose
- Android SDK
- Gradle
- Kotlin Coroutines
- ViewModel
- Repository Pattern
- Navigation Compose
- Material 3

> React Native and Expo must NOT be used.

### Web

- React
- Vite
- React Router
- Tailwind CSS or modern CSS

### Backend

- Node.js
- Express.js
- Mongoose

### Database

- MongoDB

### AI

- Gemini API

---

# System Architecture

```text
                 ┌──────────────────┐
                 │     MongoDB      │
                 └────────▲─────────┘
                          │
                          │
                 ┌────────┴─────────┐
                 │  Node.js/Express │
                 │     REST API     │
                 └───────▲───▲──────┘
                         │   │
              ┌──────────┘   └──────────┐
              │                         │
      ┌───────┴────────┐       ┌────────┴───────┐
      │ Native Android │       │   React Web    │
      │ Kotlin/Compose │       │    Vite        │
      └────────────────┘       └────────────────┘
