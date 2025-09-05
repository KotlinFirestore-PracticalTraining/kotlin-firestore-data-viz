# Multi-Feature Application 

## Overview
The **Multi-Feature Application** is a comprehensive Android app built with **Kotlin** and **Jetpack Compose**, offering a suite of tools for data visualization, image editing, AI-powered vision detection, and more. Designed with a modular architecture, the app ensures scalability, maintainability, and a seamless user experience.

---

## Key Features

### 1. Data Visualization
- Interactive charts including Bar, Line, and Pie for user-generated data.
- Supports dynamic updates, customizable styles, and theme integration.

### 2. Color Analyzer
- Extracts RGB, HEX, and HSL values from images or live camera input.
- Identifies dominant and complementary colors for design or research use.

### 3. Function Plotter
- Plots mathematical functions such as linear, quadratic, and trigonometric.
- Provides zooming and panning capabilities for detailed inspection..

### 4. AI Vision Detection
- Leverages on-device ML models for object detection.
- Outputs labels with confidence scores, enabling intelligent image analysis.

### 5. Image Editor
- Transformations: Crop, Rotate, Resize.
- Adjustments: RGB balance, Brightness, Contrast, Saturation.
- Filters: Apply custom filters to enhance images.
- Save edited images to the gallery or as new files.

### 6. Food Tools
- **QR Code Scanner**: Fetches food product details via barcode/QR scan.
- **E-Code Explainer**: Decodes additives and provides health-related insights.
- **Ingredient Checker**: Analyzes ingredient lists for allergens and dietary preferences.
- **Nutrition Analyzer**: Breaks down nutritional information for informed choices.
- **Food Comparison**: Side-by-side comparison of nutritional values and ingredients.

---

## Screenshots
| **Charts**                        | **Color Analyzer**                        | **Function Plotter**                       |
|-----------------------------------|-------------------------------------------|--------------------------------------------|
| ![Charts](screenshots/Charts.png) | ![Color Analyzer](screenshots/Colors.png) | ![Function Plotter](screenshots/Graph.png) |

| **AI Vision Detection**                | **Image Editor**                             | **Food Tools**                           |
|----------------------------------------|----------------------------------------------|------------------------------------------|
| ![AI Vision](screenshots/Detector.png) | ![Image Editor](screenshots/ImageEditor.png) | ![Food Tools](screenshots/FoodTools.png) |

---
## Project Structure
The project is organized into the following main packages:
## 📂 Project Structure

```text
kotlin-firestore-data-viz/
│── app/
│   ├── manifests/
│   │   └── AndroidManifest.xml
│   │
│   ├── java/com/example/kotlin_firestore_data_viz/
│   │   ├── controller/                # UI controls & interactions
│   │   │   └── FilterControls.kt
│   │   │
│   │   ├── data/                      # Local storage & data handling
│   │   │   └── LocalAdditives.kt
│   │   │
│   │   ├── navigation/                # Navigation components
│   │   │   └── AppNavHost.kt
│   │   │
│   │   ├── network/                   # API and network services
│   │   │   ├── FoodApiService.kt
│   │   │   └── IngredientsTaxonomyService.kt
│   │   │
│   │   ├── screens/                   # UI screens
│   │   │   ├── ColorAnalysisScreen.kt
│   │   │   ├── DataVizScreen.kt
│   │   │   ├── ECodesLibraryScreen.kt
│   │   │   ├── FoodComparisonScreen.kt
│   │   │   ├── FoodSectionScreen.kt
│   │   │   ├── GraphingScreen.kt
│   │   │   ├── ImageDetectorScreen.kt
│   │   │   ├── ImageEditorScreen.kt
│   │   │   ├── IngredientCheckerScreen.kt
│   │   │   ├── LabelScannerScreen.kt
│   │   │   ├── NutritionAnalysisScreen.kt
│   │   │   └── QrScannerScreen.kt
│   │   │
│   │   ├── ui.theme/                  # App theme, colors, typography
│   │   │   ├── Color.kt
│   │   │   ├── Theme.kt
│   │   │   └── Type.kt
│   │   │
│   │   ├── utils/                     # Utility classes & helpers
│   │   │   ├── BitmapAdjustments.kt
│   │   │   ├── BitmapFilters.kt
│   │   │   └── ImageUtils.kt
│   │   │
│   │   ├── ViewModels/                # MVVM ViewModels
│   │   │   └── OpenFoodFactsViewModel.kt
│   │   │
│   │   └── MainActivity.kt            # App entry point
│   │
│   ├── assets/                        # Static assets (if any)
│   ├── res/                           # Layouts, drawables, values
│   └── res (generated)/               # Auto-generated resources
│
├── build.gradle.kts                   # Project build config
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---
## Architecture
The app follows a **modular architecture** with the following layers:
- **UI Layer**: Built with Jetpack Compose for a modern, declarative UI.
- **Domain Layer**: Contains business logic and use cases.
- **Data Layer**: Handles data sources, including Firebase Firestore and local storage.

### Key Design Patterns
- **MVVM (Model-View-ViewModel)**: Ensures separation of concerns and testability.
- **State Management**: Uses Compose's `remember` and `LaunchedEffect` for reactive state handling.

---

## Technology Stack
- **Programming Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **State Management**: Compose `remember` and `LaunchedEffect`
- **Navigation**: Jetpack Navigation Compose
- **Image Processing**: Android `Bitmap` API
- **AI/ML**: TensorFlow Lite for vision detection
- **Data Visualization**: MPAndroidChart library
- **Backend Integration**: Firebase Firestore

---

## Installation

### Prerequisites
- **Android Studio**: Version 2024.3.1 (Meerkat) or later.
- **JDK**: Java 11 or higher.
- **Gradle**: Version 7.0 or higher.

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/KotlinFirestore-PracticalTraining/kotlin-firestore-data-viz.git