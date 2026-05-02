from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Boolean, JSON, CheckConstraint, UniqueConstraint
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database import Base

class User(Base):
    __tablename__ = "users"

    userId = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    passwordHash = Column(String, nullable=False)
    timezone = Column(String, default="UTC")
    createdAt = Column(DateTime(timezone=True), server_default=func.now())

    meetings_created = relationship("Meeting", back_populates="creator")
    availability = relationship("Availability", back_populates="user")

class Meeting(Base):
    __tablename__ = "meetings"

    meetingId = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    dateTime = Column(DateTime(timezone=True), nullable=False)
    duration = Column(Integer)  # in minutes
    location = Column(String, nullable=True)
    status = Column(String, default="scheduled")  # scheduled, ongoing, completed, cancelled
    createdBy = Column(Integer, ForeignKey("users.userId"))

    creator = relationship("User", back_populates="meetings_created")
    participants = relationship("Participant", back_populates="meeting")
    minutes = relationship("Minutes", back_populates="meeting", uselist=False)

class Participant(Base):
    __tablename__ = "participants"

    meetingId = Column(Integer, ForeignKey("meetings.meetingId"), primary_key=True)
    userId = Column(Integer, ForeignKey("users.userId"), primary_key=True)
    role = Column(String, default="participant")  # organizer, participant
    status = Column(String, default="pending")  # accepted, declined, pending

    meeting = relationship("Meeting", back_populates="participants")

class Availability(Base):
    __tablename__ = "availability"

    availId = Column(Integer, primary_key=True, index=True)
    userId = Column(Integer, ForeignKey("users.userId"))
    dayOfWeek = Column(Integer, nullable=False)  # 0-6
    startHour = Column(Integer, nullable=False)  # 0-23
    endHour = Column(Integer, nullable=False)    # 0-23
    isRecurring = Column(Boolean, default=True)
    validFrom = Column(DateTime(timezone=True), nullable=True)
    validUntil = Column(DateTime(timezone=True), nullable=True)

    __table_args__ = (
        CheckConstraint('dayOfWeek >= 0 AND dayOfWeek <= 6', name='check_day_of_week'),
    )

    user = relationship("User", back_populates="availability")

class Minutes(Base):
    __tablename__ = "minutes"

    minutesId = Column(Integer, primary_key=True, index=True)
    meetingId = Column(Integer, ForeignKey("meetings.meetingId"), unique=True)
    summaryText = Column(String, nullable=False)
    actionItems = Column(JSON, nullable=False)
    generatedAt = Column(DateTime(timezone=True), server_default=func.now())
    rawNotes = Column(String, nullable=True)

    meeting = relationship("Meeting", back_populates="minutes")
