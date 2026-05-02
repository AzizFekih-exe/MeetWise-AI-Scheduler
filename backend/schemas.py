from pydantic import BaseModel, EmailStr
from typing import Optional, List
from datetime import datetime

# User Schemas
class UserBase(BaseModel):
    email: EmailStr
    name: str
    timezone: str = "UTC"

class UserCreate(UserBase):
    password: str

class LoginRequest(BaseModel):
    email: EmailStr
    password: str

class UserResponse(UserBase):
    userId: int
    createdAt: datetime

    class Config:
        from_attributes = True

# Participant Schemas
class ParticipantBase(BaseModel):
    userId: int
    role: str = "participant"
    status: str = "pending"

class ParticipantCreate(ParticipantBase):
    pass

class ParticipantResponse(ParticipantBase):
    class Config:
        from_attributes = True

# Meeting Schemas
class MeetingBase(BaseModel):
    title: str
    dateTime: datetime
    duration: int
    location: Optional[str] = None
    status: str = "scheduled"

class MeetingCreate(MeetingBase):
    participants: List[int] = []  # List of user IDs to invite

class MeetingResponse(MeetingBase):
    meetingId: int
    createdBy: int
    participants: List[ParticipantResponse] = []

    class Config:
        from_attributes = True

# Token Schemas
class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    email: Optional[str] = None
    userId: Optional[int] = None

# Transcription Schemas
class TranscriptionJobResponse(BaseModel):
    jobId: str
