# Mebel AR (IKEA Place uslubidagi ilova)

ARCore + SceneView (Filament) + Jetpack Compose.

## Ishlatish
1. Repozitoriyga push qiling (main branch).
2. GitHub → Actions → "Build APK" → tugagach **Artifacts → MebelAR-debug-apk** ni yuklab oling.
3. APK ni Android telefonga o'rnating (ARCore / "Google Play Services for AR" qo'llab-quvvatlanadigan qurilma kerak).

## Yangi mebel qo'shish
`.glb` faylni `app/src/main/assets/models/` ga tashlang va `MainActivity.kt` dagi `catalog` ga qator qo'shing:
`Furniture("Nomi", "models/fayl.glb", metrdagiEngKattaO'lcham)`
