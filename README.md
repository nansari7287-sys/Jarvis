<div align="center">

# NAEEM JARVIS

### Your personal AI assistant for Android

*A voice-first assistant project with AI provider integration, conversational controls, and Android device actions.*

<br/>

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Build](https://img.shields.io/badge/Build-Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Status](https://img.shields.io/badge/Project-Under_Development-FFB020?style=for-the-badge)

</div>

---

## Overview

**Naeem JARVIS** is a personal Android assistant project designed to bring AI conversations and useful phone actions together in one place. The project combines a Kotlin-based Android app with voice input, spoken responses, AI provider connectivity, and optional Accessibility Service support.

The goal is a reliable, user-controlled assistant that can understand requests, respond naturally, and perform supported actions on the device with appropriate permissions.

## Core capabilities

| Area | Current project status |
|---|---|
| Android application | Kotlin Android app is present |
| Gemini | Existing API integration is present; runtime configuration and end-to-end behaviour require testing |
| ChatGPT | Responses API transport added on the development branch; sign-in flow and app integration are still in progress |
| Voice input and speech output | Speech recognition and text-to-speech components are present |
| Continuous voice session | Session-management code is present; full end-to-end operation needs testing |
| Device actions | Command routing and optional Accessibility Service components are present |
| Conversation storage | Room database components are present; provider-separated history is still to be completed |

> **Development note:** Feature files in the repository do not automatically mean a feature is production-ready. Items marked as in progress must be integrated and tested on a real Android device.

## AI providers

### Google Gemini
The project contains an existing Gemini API integration. Configure it only through the app's intended settings flow. Do not commit API keys to this repository.

### OpenAI ChatGPT
The development branch includes a streamed Responses API client intended for the official **Sign in with ChatGPT** open-source authorization flow.

The ChatGPT plan connection is **not ready to use yet**. OAuth setup, redirect handling, token validation and secure token storage, token refresh, provider selection, and app-level conversation integration still need to be completed.

The OpenAI API-key route is separate from ChatGPT plan sign-in and is billed independently through an OpenAI API account. No paid API usage should be enabled without the user's explicit choice.

ChatGPT sign-in does not automatically import existing ChatGPT conversations or memories.

## Voice and device control

The project includes speech-recognition, text-to-speech, voice-session, command-routing, and Accessibility Service components. The intended experience is:

1. The user starts the voice session and grants microphone permission.
2. JARVIS listens for a request.
3. The selected AI provider generates a response.
4. JARVIS displays and speaks the response.
5. Listening resumes while the session remains active.

Microphone use must remain visible and user-controlled. Background microphone operation is subject to Android permissions and foreground-service restrictions. Device actions must use supported Android APIs and granted permissions; sensitive actions should require confirmation.

## Technology

- **Language:** Kotlin
- **Platform:** Android
- **Networking:** OkHttp
- **Local database:** Room
- **Asynchronous work:** Kotlin Coroutines
- **Voice:** Android Speech Recognizer and Text-to-Speech components
- **AI connectivity:** Gemini integration and OpenAI Responses API transport

## Build locally

Requirements:
- Android Studio with a compatible Android SDK
- JDK 17
- Gradle wrapper included in the repository

Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

The generated APK is normally located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Security

- Never commit API keys, OAuth tokens, passwords, or GitHub access tokens.
- Never use browser cookies or copied ChatGPT session data as an authentication method.
- Store credentials using platform-appropriate protected storage.
- Client-side credentials can still be extracted from an APK; a backend is recommended for a public production release.
- Request only the Android permissions required for a feature.
- Keep microphone activity visible and provide a clear stop control.

## Project roadmap

- [ ] Complete official ChatGPT sign-in and secure token lifecycle
- [ ] Connect ChatGPT and Gemini through a unified provider selector
- [ ] Keep each provider's conversations separate
- [ ] Complete the continuous voice conversation loop
- [ ] Verify supported phone actions and confirmation safeguards
- [ ] Build, test, and document a release APK

## Project status

Naeem JARVIS is actively being developed. The current development branch is for integration work and has not yet been confirmed as a tested release. Check the commit history and roadmap for the latest progress.

---

<div align="center">

**Built with Kotlin for Android.**

</div>
