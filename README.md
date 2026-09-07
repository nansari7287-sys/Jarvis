# 𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎 — Jarvis

Personal Android AI assistant with a dark neon interface, Gemini integration,
voice input/output, command routing, and optional Accessibility Service support.

## Security
Never commit API keys or GitHub tokens. The Gemini key is entered in-app and
stored locally using Android Keystore-backed encryption. Client-side keys are
still extractable from a personal APK; public production should use a backend.

## Build
`./gradlew :app:assembleDebug`
