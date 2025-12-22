# Target Discriminator

An Android application designed to help law enforcement, military, and private citizens train their minds to identify threats and non-threats quickly and safely.

## Features

- **Splash Screen**: Initial loading screen when the app starts
- **Session Configuration**: Configure training sessions with:
  - Toggle videos on/off
  - Toggle photos on/off
  - Set session duration (1-30 minutes)
- **Training Session**: Interactive training with:
  - Random display of videos and photos
  - Tap gesture for threat identification
  - Swipe gesture for non-threat identification
  - Real-time feedback (correct/incorrect)
  - Session timer
  - Score tracking

## Architecture

The app follows **Clean Architecture** principles with:

- **Domain Layer**: Business logic and models
  - Models: `SessionConfig`, `MediaItem`, `ThreatType`, `MediaType`, etc.
  
- **Data Layer**: Data sources and repositories
  - `MediaRepository`: Manages media files from assets
  
- **Presentation Layer**: UI and ViewModels
  - **MVI Pattern**: State, Events, and Effects for state management
  - Fragments: `SplashFragment`, `SessionConfigFragment`, `TrainingFragment`
  - ViewModels: `SessionConfigViewModel`, `TrainingViewModel`

## Project Structure

```
app/src/main/
├── assets/
│   ├── videos/
│   │   ├── threat/          # Place threat videos here
│   │   └── non_threat/      # Place non-threat videos here
│   └── photos/
│       ├── threat/          # Place threat photos here
│       └── non_threat/      # Place non-threat photos here
├── java/com/targetdiscriminator/
│   ├── data/
│   │   └── repository/      # Data repositories
│   ├── domain/
│   │   └── model/           # Domain models
│   └── presentation/
│       ├── mvi/             # MVI base classes
│       ├── splash/          # Splash screen
│       ├── session_config/  # Session configuration
│       ├── training/        # Training session
│       └── MainActivity.kt
└── res/
    ├── layout/              # XML layouts
    ├── navigation/          # Navigation graph
    └── values/              # Strings, colors, themes
```

## Adding Media Files

1. **Videos**: Place video files (MP4, WebM, etc.) in:
   - `app/src/main/assets/videos/threat/` for threat videos
   - `app/src/main/assets/videos/non_threat/` for non-threat videos

2. **Photos**: Place image files (JPG, PNG, etc.) in:
   - `app/src/main/assets/photos/threat/` for threat photos
   - `app/src/main/assets/photos/non_threat/` for non-threat photos

The app will automatically discover and use these files during training sessions.

## User Interaction

- **Tap**: Identifies the current media as a **threat**
- **Swipe (left or right)**: Identifies the current media as a **non-threat**

## Technologies

- **Kotlin**: Programming language
- **Material 3**: UI components
- **Navigation Component**: Fragment navigation
- **ExoPlayer**: Video playback
- **ViewBinding**: View binding
- **Coroutines & Flow**: Asynchronous operations
- **MVI Pattern**: State management

## Building the App

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run on an Android device or emulator (API 24+)

## Requirements

- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Kotlin 1.9.20+
- Gradle 8.2.0+

