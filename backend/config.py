from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field
from typing import Optional

class Settings(BaseSettings):
    # App Settings
    APP_NAME: str = "MeetWise"
    DEBUG: bool = Field(default=False, validation_alias="APP_DEBUG")

    # OpenAI API
    OPENAI_API_KEY: str = ""
    TRANSCRIPTION_PROVIDER: str = "openai"
    DEEPGRAM_API_KEY: Optional[str] = None
    DEEPGRAM_MODEL: str = "nova-3"
    ASSEMBLYAI_API_KEY: Optional[str] = None
    ASSEMBLYAI_BASE_URL: str = "https://api.assemblyai.com"
    ASSEMBLYAI_SPEECH_MODEL: str = "universal-2"
    MINUTES_PROVIDER: str = "local"

    # JWT Settings
    JWT_SECRET: str
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30

    # Database
    DATABASE_URL: str

    # FCM
    FCM_SERVER_KEY: Optional[str] = None
    FIREBASE_SERVICE_ACCOUNT_JSON: Optional[str] = None

    # Email
    SMTP_HOST: Optional[str] = None
    SMTP_PORT: int = 587
    SMTP_USERNAME: Optional[str] = None
    SMTP_PASSWORD: Optional[str] = None
    SMTP_FROM_EMAIL: Optional[str] = None
    SMTP_FROM_NAME: str = "MeetWise"
    SMTP_USE_TLS: bool = True

    # Load from .env file
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

# Instantiate settings to be imported in other modules
settings = Settings()
