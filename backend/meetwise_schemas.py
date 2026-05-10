from pydantic import BaseModel, EmailStr
from typing import Optional, List
from datetime import datetime

# Job/Transcription Schemas (Moved to top to prevent AttributeError)
class TranscriptionJobResponse(BaseModel):
    jobId: str
    status: str

class JobStatusResponse(BaseModel):
    jobId: str
    status: str
    progress: float
    message: str
    errorMessage: Optional[str] = None

# User Schemas
class UserBase(BaseModel):
    email: EmailStr
    name: str
    timezone: str = "UTC"

class UserCreate(UserBase):
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
    participantEmails: List[EmailStr] = []  # External or registered emails to notify

class MeetingResponse(MeetingBase):
    meetingId: int
    createdBy: int
    participants: List[ParticipantResponse] = []

    class Config:
        from_attributes = True

# Slot Schemas
class SlotSuggestion(BaseModel):
    startTime: datetime
    endTime: datetime
    score: float

class MeetingConfirm(BaseModel):
    startTime: datetime
    endTime: datetime

# Minutes Schemas
class ActionItem(BaseModel):
    task: str
    owner: str
    deadline: Optional[str] = None
    done: bool = False

class MinutesResponse(BaseModel):
    minutesId: int
    meetingId: int
    summaryText: str
    actionItems: List[ActionItem]
    generatedAt: datetime
    rawNotes: Optional[str] = None

    class Config:
        from_attributes = True

# Availability Schemas
class AvailabilityBase(BaseModel):
    dayOfWeek: int
    startHour: int
    endHour: int
    isRecurring: bool = True

class AvailabilityUpdate(BaseModel):
    dayOfWeek: Optional[int] = None
    startHour: Optional[int] = None
    endHour: Optional[int] = None
    isRecurring: Optional[bool] = None

class AvailabilityResponse(AvailabilityBase):
    availId: int
    userId: int

    class Config:
        from_attributes = True

# Device Schemas
class DeviceRegister(BaseModel):
    fcmToken: str

# Token Schemas
class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    email: Optional[str] = None
    userId: Optional[int] = None

class LoginRequest(BaseModel):
    email: str
    password: str
