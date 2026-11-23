<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:050816,25:172554,55:6D28D9,80:0891B2,100:06B6D4&height=230&section=header&text=NAEEM%20JARVIS&fontSize=58&fontColor=FFFFFF&fontAlignY=38&desc=Your%20Personal%20AI%20Assistant%20for%20Android&descAlignY=60&descSize=18&animation=fadeIn" width="100%" alt="Naeem JARVIS colorful neon header"/>

<a href="https://github.com/nansari7287-sys/Jarvis">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=600&size=20&pause=1000&color=22D3EE&center=true&vCenter=true&width=650&lines=VOICE-FIRST+ANDROID+ASSISTANT;GEMINI+%2B+CHATGPT+INTEGRATION;SMART+COMMANDS+%7C+PERSONAL+AI+PROJECT" alt="Project tagline"/>
</a>

<br/>

<img src="https://img.shields.io/badge/ANDROID-111827?style=for-the-badge&logo=android&logoColor=3DDC84"/>
<img src="https://img.shields.io/badge/KOTLIN-111827?style=for-the-badge&logo=kotlin&logoColor=7F52FF"/>
<img src="https://img.shields.io/badge/GRADLE-111827?style=for-the-badge&logo=gradle&logoColor=FFFFFF"/>
<img src="https://img.shields.io/badge/STATUS-IN%20DEVELOPMENT-111827?style=for-the-badge&logo=github&logoColor=FBBF24"/>

<br/><br/>

**A voice-first Android assistant project — bringing AI conversations, speech, and supported phone actions into one experience.**

</div>

---

## 🌌 About the project

**Naeem JARVIS** is a personal Android AI assistant built with Kotlin. The vision is simple: talk naturally, get useful AI responses, and access supported phone actions through a single user-controlled assistant.

The interface and experience are being developed around a modern, futuristic assistant concept. This repository is the source code project; the visual banner above is artwork, **not an in-app screenshot**.

<div align="center">
  <img src="https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=1200&q=85" width="92%" alt="Close-up of a detailed electronic circuit board"/>
  <br/>
  <sub>Technology inspiration · decorative project artwork</sub>
</div>

## ⚡ Project highlights

<table>
<tr>
<td width="50%" valign="top">

### 🎙️ Voice experience
- Speech recognition components
- Text-to-speech responses
- Voice session management
- User-controlled microphone flow

</td>
<td width="50%" valign="top">

### 🧠 AI providers
- Existing Gemini integration
- OpenAI Responses API transport added
- Provider connection work in progress
- Separate provider setup planned

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📱 Android actions
- Command routing components
- App and system action handlers
- Optional Accessibility Service
- Permission-aware action design

</td>
<td width="50%" valign="top">

### 💬 Conversations
- Room database components
- Conversation management
- Provider-separated history planned
- Persistent chat improvements planned

</td>
</tr>
</table>

## 🧩 Technology stack

<div align="center">
  <img src="https://skillicons.dev/icons?i=kotlin,androidstudio,gradle,git,github&theme=dark" alt="Kotlin, Android Studio, Gradle, Git and GitHub technology icons"/>
  <br/><br/>
  <img src="https://img.shields.io/badge/OkHttp-Networking-2563EB?style=flat-square"/>
  <img src="https://img.shields.io/badge/Room-Local%20Database-7C3AED?style=flat-square"/>
  <img src="https://img.shields.io/badge/Coroutines-Async%20Tasks-0891B2?style=flat-square"/>
  <img src="https://img.shields.io/badge/Android%20Speech-Voice%20I%2FO-059669?style=flat-square"/>
</div>

## 🤖 AI provider status

### 🟦 Google Gemini
The codebase contains an existing Gemini API-key integration. Its current runtime behaviour and compatibility need to be verified with a real build and device test.

### 🟩 OpenAI ChatGPT
The development branch contains a streaming client for the OpenAI Responses API. The official ChatGPT sign-in flow is **not yet ready to use**.

Still required:
- OAuth application registration and redirect handling
- Token validation, protected storage, and refresh
- Provider selection and integration with the main assistant
- End-to-end testing

**Billing clarity:** ChatGPT plan sign-in and OpenAI API-key usage are different routes. API-key usage is billed separately through an OpenAI API account. No paid usage should be enabled without the user's explicit decision.

Existing ChatGPT conversations and memories are not automatically imported into this app.

## 🎧 Intended voice flow

<div align="center">

`START SESSION`　 ➜　 `LISTEN`　 ➜　 `AI RESPONSE`　 ➜　 `SPEAK`　 ➜　 `LISTEN AGAIN`

</div>

The project includes speech recognition, text-to-speech, and voice-session components. A complete, reliable continuous voice experience still needs integration and real-device testing.

Microphone use must remain visible and user-controlled. Android background microphone access is subject to platform permissions and foreground-service rules.

## 🛠️ Build the project

**Requirements**
- Android Studio
- JDK 17
- Compatible Android SDK
- Gradle wrapper included in this repository

Run a debug build:

```bash
./gradlew :app:assembleDebug
```

Expected output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 🔐 Security first

- Never commit API keys, passwords, OAuth tokens, or GitHub tokens.
- Never use copied browser cookies or ChatGPT session data for authentication.
- Keep credentials in protected storage; client-side secrets can still be extracted from an APK.
- A backend is recommended before public production distribution.
- Request only the Android permissions that a feature actually needs.
- Require confirmation before sensitive or irreversible device actions.

## 🗺️ Development roadmap

| Feature | Progress |
|---|---|
| Kotlin Android foundation | 🟢 Present |
| Gemini integration | 🟡 Needs verification |
| OpenAI Responses streaming transport | 🟡 Added; not yet wired into app |
| Official ChatGPT sign-in | ⚪ Pending |
| Unified provider selector | ⚪ Pending |
| Separate provider chat histories | ⚪ Pending |
| Complete continuous voice flow | 🟡 Components present; testing pending |
| Device-action safety and confirmations | 🟡 Needs end-to-end verification |
| Release APK build and device testing | ⚪ Pending |

**Legend:** 🟢 Present in source · 🟡 In progress / needs verification · ⚪ Pending

## 🧪 Current development status

This is an active development project, not a verified production release. The ChatGPT transport change and this README redesign are on the `feature/chatgpt-sign-in-voice-agent` branch. A successful Android build and real-device test have not yet been confirmed.

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:06B6D4,35:0891B2,70:6D28D9,100:050816&height=120&section=footer" width="100%" alt="Blue and purple gradient footer"/>

**NAEEM JARVIS** · Built with Kotlin for Android

<sub>Designed for a smarter, more helpful mobile assistant.</sub>

</div>
