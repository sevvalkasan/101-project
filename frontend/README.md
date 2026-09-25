# MasaBul Flutter App

This folder contains the Phase 6 Flutter foundation.

Before running the app locally:

1. Install Flutter SDK.
2. From this folder, generate platform scaffolding if it does not exist yet:

   ```bash
   flutter create .
   ```

3. Configure Firebase for the target platforms:

   ```bash
   flutterfire configure
   ```

4. Start the backend and run the app with the backend base URL:

   ```bash
   flutter run --dart-define=MASABUL_API_BASE_URL=http://localhost:8080
   ```

For Android emulator, use:

```bash
flutter run --dart-define=MASABUL_API_BASE_URL=http://10.0.2.2:8080
```
