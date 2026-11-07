No use of INTERNET, so Completely safe. 

### How to Compile and Download the APK:
1. Push your latest code changes to your repository.
2. Open your repository page on GitHub in a web browser and navigate to the **Actions** tab.
3. In the left-hand sidebar, click on **Android APK Build Engine**.
4. Click the blue **Run workflow** drop-down button on the right side, choose your branch, and hit the green **Run workflow** trigger.
5. Once the run finishes (indicated by a green checkmark), click into the specific workflow run entry, scroll down to the **Artifacts** section at the bottom, and download your compiled `iOS-27-Replica-Debug-APK`.
6. Then, Send the downloaded file from Computer to phone using whatsapp, or charging cable(Manual copy pasting)

# iOS 27 UI System Replica Engine

A high-performance, 1:1 hardware-accelerated user interface replica engineered to run seamlessly over the Android operating system. Built completely in pure Java and optimized for mid-range device profiles (e.g., Samsung Galaxy A21s) using high-privilege system viewports, low-overhead system overlays, and custom linear physics overrides.

---

## 🚀 Key Feature Modules

### 1. High-Privilege Foreground Window Layer
* **System-Wide UI Injections:** Implements high-priority `WindowManager` drawing loops (`TYPE_APPLICATION_OVERLAY`) to run custom interfaces globally over any system screen frame.
* **iOS Status Bar Canvas:** Replicates the iconic split top bar layout carrying real-time clock tickers, stylized 5G tags, and dynamic hardware battery layout tracking fills.

### 2. Custom Hooke's Law Physics Engine (`IOSSpringAnimator.java`)
* **Fluid Animation Loops:** Bypasses standard Android linear animation frameworks entirely.
* **Spring Mechanics Math:** Employs mass, stiffness, and damping ratios calculated frame-by-frame via Hooke's Law formulas (F = -kX - cV) to deliver signature Apple-style micro-bounce effects.

### 3. Side-Popping Volumetric HUD Intercept
* **Hardware Button Hijacking:** Captures global audio system broadcast intents to intercept physical volume key actions.
* **Dynamic Morphing Sliders:** Slides a responsive indicator capsule out from the screen edge that smoothly widens horizontally into a full-scale volume slider under direct touch input.

### 4. Full Drop-Down Control Centre Tray
* **Top-Right Corner Gesture Intercept:** Tracks downward finger drags precisely from the top-right sector of the screen before executing spring-snapped drop transitions.
* **Real Hardware Automation Hooks:** Connects the UI grid pod controls directly to the phone's physical hardware radios and sensors:
  * **Wi-Fi & Bluetooth:** Real-time state toggling pipelines.
  * **Flashlight:** Direct integration with the device camera `CameraManager` torch framework.
  * **Backlight Brightness:** Injects slider coordinates straight into the system screen display brightness maps.
  * **Now Playing Media:** Links into Android `MediaSessionManager` to extract real-time track metadata and route playback commands.

### 5. Horizontally Swiping Home Screen & Frosted Dock
* **Rigid Paged Layout Grid:** Organizes installed applications evenly across horizontally swiping desktop views (16 apps max per grid page) with interactive page indicators.
* **Asynchronous Jiggle Mode:** Long-press interactions launch independent, randomized rotational wiggling loops on app icon items and slide custom deletion tags into view.
* **Searchable App Library:** A dedicated far-right workspace screen containing a live keyword search input bar to filter and launch apps instantly.
* **Frosted Glass Utilities Dock:** A fixed, persistent bottom layout shelf hosting core shortcuts for **Phone, Google, Messages, and Spotify** with zero background draw latency.

### 6. Dynamic Island Subsystem & Live Calibrator
* **Context-Aware Alerts:** A floating top pill container that dynamically expands to display battery charging connections or active background media activity.
* **Live Position Calibration Matrix:** Integrated workspace sliders (`SeekBar`) that let you adjust the X and Y positions of the island on the fly to perfectly match your phone's unique front camera cutout.
* **Quick Toggle System:** Includes a built-in "Switch Default Home App" engine button to safely swap home app preferences on the fly.
