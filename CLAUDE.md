# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-module Android application that provides notification complications for Wear OS smartwatches. The app monitors phone notifications and displays them as watch face complications.

### Architecture

- **phone**: Android phone app that listens to notifications
- **wear**: Wear OS app that displays notifications as complications
- **common**: Shared code between phone and wear modules

Communication between phone and watch uses Google's Wearable Data API with Protocol Buffer serialization.

## Essential Commands

### Building
```bash
# Build all modules
./gradlew build

# Build specific modules
./gradlew :phone:build
./gradlew :wear:build

# Clean build
./gradlew clean build

# Assemble APKs
./gradlew assembleDebug
./gradlew assembleRelease
```

### Testing
```bash
# Run all unit tests
./gradlew test

# Run specific module tests
./gradlew :phone:test
./gradlew :wear:test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Code Quality
```bash
# Run Android lint
./gradlew lint

# Lint specific module
./gradlew :phone:lint
./gradlew :wear:lint
```

### Installation
```bash
# Install debug builds
./gradlew :phone:installDebug
./gradlew :wear:installDebug

# Install and run phone app
./gradlew :phone:installDebug && adb shell am start -n com.fuegofro.notifications_complication/.MainActivity
```

## Key Architecture Components

### Phone App
- **NotificationListener**: Core service that monitors system notifications
- **NotificationListenerLifecycleService**: Manages service lifecycle and data updates
- **EnabledPackagesDataStore**: Persists user's package selections using DataStore
- **UI Screens**: MainScreen, PackageSelectionScreen, NotificationsDebugScreen (Compose)

### Wear App
- **NotificationWearableListenerService**: Receives notification data from phone
- **NotificationComplicationDataSourceService**: Provides complications to watch faces
- **CurrentNotificationUriDataStore**: Manages current notification state

### Data Flow
1. Phone's NotificationListener captures notifications
2. Filters based on user-selected packages
3. Serializes data using Protocol Buffers
4. Sends via Wearable Data API (path: `/current_notification`)
5. Wear app receives and stores data
6. Complication service provides data to watch faces

## Development Notes

### Notification Filtering
The app filters notifications based on:
- User-selected packages (stored in DataStore)
- Notification properties (ongoing, silent, alerting, conversations)
- Special handling for messaging style notifications

### Image Processing
- Extracts large icons from notifications
- Converts images to circular bitmaps for consistent display
- Handles various notification styles (MessagingStyle, BigPictureStyle)

### Testing Approach
- Unit tests in `src/test/` directories
- Instrumented tests in `src/androidTest/`
- Manual testing requires both phone and Wear OS device/emulator

### Common Issues
- Ensure both phone and wear apps are installed for proper functionality
- Watch app requires phone companion (not standalone)
- Notification access permission must be granted manually in system settings

## Pending Features (from todo.md)
- Reset on shutdown/boot handling
- Setup instructions/walkthrough
- Cross-installation prompts
- Complication tap actions
- Option to show latest vs top notification