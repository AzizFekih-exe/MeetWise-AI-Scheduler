from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import Optional

class Settings(BaseSettings):
    # App Settings
    APP_NAME: str = "MeetWise"
    DEBUG: bool = False

    # OpenAI API
    OPENAI_API_KEY: str

    # JWT Settings
    JWT_SECRET: str
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30

    # Database
    DATABASE_URL: str

    # FCM
    FCM_SERVER_KEY: Optional[str] = None
    FIREBASE_SERVICE_ACCOUNT_JSON: Optional[str] = None

    # Load from .env file
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

# Instantiate settings to be imported in other modules
settings = Settings()
