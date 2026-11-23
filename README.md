<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:050816,20:172554,45:6D28D9,75:0891B2,100:06B6D4&height=230&section=header&text=NAEEM%20JARVIS&fontSize=58&fontColor=FFFFFF&fontAlignY=38&desc=Your%20Voice-Controlled%20AI%20Assistant%20for%20Android&descAlignY=60&descSize=17&animation=fadeIn" width="100%" alt="Naeem JARVIS neon blue and purple header"/>

<img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=600&size=19&pause=1100&color=22D3EE&center=true&vCenter=true&width=720&lines=JUST+SPEAK.+JARVIS+LISTENS.;AI+CHAT+%7C+VOICE+%7C+PHONE+COMMANDS;YOUR+ANDROID.+YOUR+COMMANDS.+YOUR+CONTROL." alt="Project tagline"/>

<br/>

<img src="https://img.shields.io/badge/ANDROID-111827?style=for-the-badge&logo=android&logoColor=3DDC84"/>
<img src="https://img.shields.io/badge/KOTLIN-111827?style=for-the-badge&logo=kotlin&logoColor=7F52FF"/>
<img src="https://img.shields.io/badge/AI-GEMINI%20%7C%20CHATGPT%20%7C%20GROK-111827?style=for-the-badge&logo=openai&logoColor=FFFFFF"/>
<img src="https://img.shields.io/badge/STATUS-DEVELOPMENT-F59E0B?style=for-the-badge&logo=github&logoColor=FFFFFF"/>

<br/><br/>

### A personal AI assistant that aims to let you talk naturally, get intelligent answers, and control supported Android actions with your voice.

<sub>Built as a Kotlin Android project · Created by Naeem</sub>

</div>

---

## ✨ What is Naeem JARVIS?

**Naeem JARVIS** is a voice-first personal assistant for Android. Instead of opening every app and tapping through every screen yourself, the goal is to give Jarvis a spoken command and let it perform the supported action.

Think of it as three things working together:

<table>
<tr>
<td align="center" width="33%">
  <h2>🧠</h2>
  **AI BRAIN**
  <br/><sub>Understands requests and prepares responses or actions.</sub>
</td>
<td align="center" width="33%">
  <h2>🎙️</h2>
  **VOICE**
  <br/><sub>Listens when enabled and speaks its responses.</sub>
</td>
<td align="center" width="33%">
  <h2>📱</h2>
  **PHONE ACTIONS**
  <br/><sub>Opens apps and performs supported Android actions.</sub>
</td>
</tr>
</table>

## 🚀 What can Jarvis help you do?

### 1. Open apps with your voice

Say what you want to open. The current app launcher contains mappings for these apps and can also search installed launcher apps by their visible name.

| Say something like… | Intended action |
|---|---|
| “Jarvis, open Instagram” | Launch Instagram |
| “Open WhatsApp” | Launch WhatsApp |
| “Open YouTube” | Launch YouTube |
| “Open Facebook” | Launch Facebook |
| “Open Telegram” | Launch Telegram |
| “Open Spotify” | Launch Spotify |
| “Open Chrome” | Launch Chrome |
| “Open [an installed app name]” | Search for and launch a matching installed app |

<sub>App must be installed and Android must provide a launchable activity. Actual voice understanding depends on the selected AI provider and a working connection.</sub>

### 2. Instagram and WhatsApp actions

| Example command | Code path currently present |
|---|---|
| “Open Instagram and search for [topic]” | Opens Instagram, then attempts to tap Search and type the topic using the Accessibility Service |
| “Open WhatsApp” | Launches WhatsApp |
| “Send [message] on WhatsApp” | WhatsApp send-action handler exists; requires Accessibility Service and needs real-device testing |

<sub>Automation may fail if an app changes its screen layout, the required permission is off, or the target app is not installed. Always review a message and recipient before sending. Do not assume a command has completed until you see the result in the target app.</sub>

### 3. Android navigation and system actions

The command executor contains handlers for these actions:

| Example command | Action handler |
|---|---|
| “Go Home” | Android Home action |
| “Go Back” | Android Back action |
| “Show Recent Apps” | Recents screen |
| “Swipe up” / “Swipe down” | Accessibility swipe action |
| “Lock the screen” | Lock-screen action |
| “Take a screenshot” | Screenshot action |
| “Tap [visible item]” | Accessibility click on a named target |
| “Type [text]” | Accessibility text-entry action |

<sub>These are implemented action routes, not a promise that every phrase will be recognized correctly. Accessibility Service must be enabled for the relevant actions, and Android version/device restrictions may apply. Screen actions can behave differently in different apps.</sub>

### 4. Call a saved contact

**Example:** “Call [contact name]”

The current call handler looks up a contact and requests Contacts and Phone permissions before using the phone call intent. A matching saved contact and the required permissions are necessary. Check the contact and confirm the call on your device.

### 5. Ask questions and get spoken answers

Jarvis is designed to accept typed or spoken requests, send them to the selected AI provider, and speak the response using text-to-speech.

Examples:
- “Explain how a transformer works.”
- “Help me understand this electrical engineering topic.”
- “Summarize this idea in simple words.”
- “What should I do next?”

The quality and availability of answers depend on the configured provider, internet connection, account access, and model availability.

## 🧠 AI engines

<div align="center">
<img src="https://img.shields.io/badge/GOOGLE-GEMINI-4285F4?style=for-the-badge&logo=google&logoColor=white"/>
<img src="https://img.shields.io/badge/OPENAI-CHATGPT-10A37F?style=for-the-badge&logo=openai&logoColor=white"/>
<img src="https://img.shields.io/badge/xAI-GROK-111827?style=for-the-badge"/>
</div>

| Provider | Current project status |
|---|---|
| Google Gemini | Existing API-key request path is present; build and device verification remain |
| OpenAI ChatGPT | Responses transport and sign-in integration code are on the feature branch; official authentication, permissions, and end-to-end operation are not yet verified |
| xAI Grok | Existing API-key request path is present; build and device verification remain |

**Important:** ChatGPT plan sign-in and OpenAI API-key access are different. API-key usage is billed separately by OpenAI. No API billing should be enabled without the account owner's choice. Do not paste API keys or tokens into GitHub issues, source files, or public messages.

## 🎧 How the voice experience is intended to work

<div align="center">

**TAP TO START / ENABLE VOICE**

⬇️

**SPEAK A COMMAND**

⬇️

**JARVIS UNDERSTANDS THE REQUEST**

⬇️

**AI ANSWER OR PHONE ACTION**

⬇️

**JARVIS SPEAKS THE RESULT**

</div>

The codebase contains speech, text-to-speech, and voice-session components. A complete reliable hands-free or continuous voice experience is still under development and must be tested on a real Android phone. Microphone access should remain visible and controlled by the user; Android background microphone use is subject to permissions and foreground-service rules.

## 🔐 Phone access and permissions

Jarvis does **not** automatically get unrestricted access to everything on a phone. Android controls what an app can do.

| Permission / setting | Why it may be needed |
|---|---|
| Microphone | Listen to a voice command |
| Contacts | Find a saved contact for a call |
| Phone | Start a call after permission is granted |
| Accessibility Service | Perform supported screen taps, typing, navigation, and app automation |
| Notifications / foreground service | Keep an enabled voice session visible where supported |

Only enable permissions for features you want to use. Accessibility access is powerful: grant it only to an app you trust, and turn it off when you no longer need the feature.

## 🎨 Interface and technology

The project aims for a futuristic, clean assistant interface with a dark background, neon blue accents, readable status labels, and clear voice controls.

<div align="center">
<img src="https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=1200&q=85" width="92%" alt="Decorative close-up of an electronic circuit board"/>
<br/><sub>Decorative technology artwork — not an actual app screenshot.</sub>
</div>

**Built with:**
- Kotlin and Android SDK
- Android speech recognition and text-to-speech
- Kotlin Coroutines
- OkHttp networking
- Room database components
- Android Accessibility Service for supported automation
- Git and GitHub for version control

## 🛠️ Build the Android app

**Requirements**
- Android Studio
- JDK 17
- Compatible Android SDK
- Gradle wrapper from this repository

Run:

```bash
./gradlew :app:assembleDebug
```

Expected APK location after a successful build:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 🗺️ Development roadmap

| Feature | Status |
|---|---|
| Kotlin Android foundation | 🟢 Present in source |
| App launcher mappings | 🟢 Present in source |
| Android action handlers | 🟡 Present; device testing required |
| WhatsApp / Instagram automation | 🟡 Present; Accessibility and UI testing required |
| Contact calling | 🟡 Handler present; permission/device testing required |
| Gemini API path | 🟡 Present; verification required |
| Grok API path | 🟡 Present; verification required |
| ChatGPT sign-in and Responses integration | 🟡 Integration code added; official flow and build not verified |
| Reliable continuous voice experience | ⚪ In progress |
| Separate AI-provider conversation histories | ⚪ Planned |
| Release-ready APK and full device testing | ⚪ Pending |

**Legend:** 🟢 Source code present · 🟡 Needs integration or verification · ⚪ Planned / pending

## 🛡️ Security principles

- Never commit API keys, passwords, OAuth tokens, or GitHub access tokens.
- Never authenticate by copying browser cookies or private account sessions.
- Protect credentials with Android secure storage.
- Request only the permissions needed for a feature.
- Keep microphone activity visible and user-controlled.
- Confirm sensitive actions such as sending messages or placing calls.
- Test automation carefully before relying on it.

## 📌 Current development status

This repository is an **active development project**, not a verified production release. The current work is on the `feature/chatgpt-sign-in-voice-agent` branch. The `main` branch is kept separate. The README describes code paths found in the project; it does not claim that every command has passed a real-device test.

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:06B6D4,35:0891B2,70:6D28D9,100:050816&height=120&section=footer" width="100%" alt="Blue and purple gradient footer"/>

### NAEEM JARVIS
**Speak naturally. Let your assistant handle the supported actions.**

<sub>Made with Kotlin · Android · AI</sub>

</div>
