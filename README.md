# 𝑫𝒓𝒂𝒌𝒐𝑿𝑵𝒂𝒆𝒆𝒎 — Jarvis

Personal Android AI assistant with a dark neon interface, Gemini integration,
voice input/output, command routing, and optional Accessibility Service support.

## AI providers

- **Gemini:** Existing API-key integration remains available.
- **ChatGPT API:** The existing API-key route is separate and is billed through the OpenAI API account.
- **Sign in with ChatGPT (open-source plan usage):** The open-source OAuth flow can let an eligible user authorize Responses API usage against their ChatGPT plan. This is not the same as ordinary identity-only sign-in, does not expose an API key, and does not import ChatGPT conversations or memories.

The new `OpenAiPlanResponsesClient` implements the streamed Responses API transport for a valid Sign in with ChatGPT access token. It sends `store: false` and `stream: true`, and only treats a response as successful after `response.completed`.

**Important implementation status:** OAuth registration/callback handling, ID-token/JWKS validation, protected token storage/refresh, and wiring the sign-in profile into the app UI/provider router are still required before the ChatGPT plan option is usable. Do not paste ChatGPT cookies or session tokens into the app. Do not put credentials in source control or logs.

## Voice status

The current full-duplex `VoiceService` still contains a placeholder OpenAI Realtime URL/key and must not be considered a working ChatGPT voice connection. The existing speech-recognition/TTS conversation path is separate from that raw-audio service. Continuous voice should only be enabled after a provider transport is configured and tested; the microphone must remain user-controlled with a visible foreground notification.

## Security

Never commit API keys or GitHub tokens. The Gemini key is entered in-app and stored locally using Android Keystore-backed encryption. Client-side keys are still extractable from a personal APK; public production should use a backend. OAuth access and refresh tokens must be stored in protected local storage, never in URLs, logs, analytics, or source control.

## Build

`./gradlew :app:assembleDebug`
