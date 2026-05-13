# MeetWise

MeetWise is an Android meeting scheduler with a local FastAPI backend. It helps users schedule meetings from natural language, invite participants by email, record meetings, transcribe audio, generate meeting minutes, and export or share the results.

## Features

- Email/password account creation and sign in
- Natural-language meeting scheduling, including phrases like:
  - `meeting tomorrow in the afternoon`
  - `review meeting after Thursday at 8pm`
  - `meeting today`
  - `next hour`
- Multiple invite emails separated by commas, spaces, or semicolons
- Meeting confirmation and cancellation emails
- Meeting cancellation flow with temporary cancelled state before removal
- Meeting recording and audio upload
- Transcription through AssemblyAI or Deepgram
- Local meeting-minutes generation for demos without paid LLM credits
- Optional OpenAI-powered minutes generation
- Meeting minutes history
- PDF export for minutes
- Audio sharing/export flow
- Search meetings by title, status, or date
- Calendar tab grouped by meeting day
- Dark theme, larger text, and reduced motion settings
- Smooth screen transitions, swipe navigation, and animated recommendation cards
- Custom MeetWise app icon and app name

## Tech Stack

- Android Kotlin
- Jetpack Compose
- Hilt
- Room
- Retrofit / OkHttp
- FastAPI
- SQLAlchemy
- SQLite for local development
- SMTP for meeting emails
- AssemblyAI or Deepgram for transcription

## Project Structure

```text
.
+-- app/                  Android application
+-- backend/              FastAPI backend
+-- gradlew.bat           Android Gradle wrapper for Windows
`-- local.properties      Local Android SDK and API URL config
```

## Backend Setup

From the project root:

```powershell
cd backend
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

Create or update `backend/.env`:

```env
DATABASE_URL=sqlite:///./meetwise.db
JWT_SECRET=change-this-for-local-dev
JWT_ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=10080

TRANSCRIPTION_PROVIDER=assemblyai
ASSEMBLYAI_API_KEY=your_assemblyai_key
ASSEMBLYAI_BASE_URL=https://api.assemblyai.com
ASSEMBLYAI_SPEECH_MODEL=universal-2

DEEPGRAM_API_KEY=
DEEPGRAM_MODEL=nova-3

MINUTES_PROVIDER=local
OPENAI_API_KEY=

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_gmail_app_password
SMTP_FROM_EMAIL=your_email@gmail.com
SMTP_FROM_NAME=MeetWise
SMTP_USE_TLS=true
```

Start the backend:

```powershell
cd backend
.\venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Health check:

```text
http://127.0.0.1:8000/health
```

## Android Setup

The app reads the backend URL from `local.properties`.

For local emulator/device development:

```properties
api.base.url=http://127.0.0.1:8000/
```

When testing on a physical Android phone through USB, keep the backend running and run:

```powershell
adb reverse tcp:8000 tcp:8000
```

Build and install:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

## Email Setup

For Gmail SMTP, use a Google App Password, not your normal Gmail password.

1. Enable 2-Step Verification on the Gmail account.
2. Create an App Password.
3. Put that value in `SMTP_PASSWORD`.
4. Keep `SMTP_USERNAME` and `SMTP_FROM_EMAIL` as the same Gmail address.

If SMTP is not configured, the backend writes development email previews to `backend/outbox`.

## Transcription Setup

The app supports:

- `assemblyai`
- `deepgram`
- `openai`

For the current local setup, AssemblyAI is the recommended free-friendly option:

```env
TRANSCRIPTION_PROVIDER=assemblyai
ASSEMBLYAI_API_KEY=your_key
ASSEMBLYAI_SPEECH_MODEL=universal-2
```

Minutes generation can stay local:

```env
MINUTES_PROVIDER=local
```

Use OpenAI minutes only when you have quota:

```env
MINUTES_PROVIDER=openai
OPENAI_API_KEY=your_key
```

## Reset Local Data

To wipe local accounts, meetings, invite emails, and minutes, stop the backend first, then delete the SQLite database:

```powershell
Remove-Item .\backend\meetwise.db
```

The backend recreates the database when it starts again.

## Current Development Notes

- The app is intended to run locally with the backend on port `8000`.
- Render deployment was removed from the active flow; local development is the expected setup.
- `backend/.env`, `local.properties`, database files, Python caches, and build outputs are ignored by Git.
- The tracked app icon is `app/src/main/res/drawable/meetwise.png`.

## Useful Commands

Run backend:

```powershell
cd backend
.\venv\Scripts\python.exe -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Reconnect Android device to local backend:

```powershell
adb reverse tcp:8000 tcp:8000
```

Build Android app:

```powershell
.\gradlew.bat :app:assembleDebug
```

Install Android app:

```powershell
.\gradlew.bat :app:installDebug
```

Run backend syntax check:

```powershell
cd backend
.\venv\Scripts\python.exe -m compileall meetings.py meetwise_schemas.py
```
