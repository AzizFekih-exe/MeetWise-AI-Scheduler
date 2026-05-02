from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import or_
from typing import List
from database import get_db
from dependencies import get_current_user
import models, schemas


router = APIRouter(prefix="/meetings", tags=["Meetings"])

@router.get("/", response_model=List[schemas.MeetingResponse])
def list_meetings(
    db: Session = Depends(get_db), 
    current_user: models.User = Depends(get_current_user)
):
    """
    List all meetings where the current user is either the creator or a participant.
    """
    meetings = db.query(models.Meeting).join(
        models.Participant, models.Meeting.meetingId == models.Participant.meetingId, isouter=True
    ).filter(
        or_(
            models.Meeting.createdBy == current_user.userId,
            models.Participant.userId == current_user.userId
        )
    ).distinct().all()
    
    return meetings

@router.post("/", response_model=schemas.MeetingResponse)
def create_meeting(
    meeting_data: schemas.MeetingCreate,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Create a new meeting and add participants.
    """
    # 1. Create the meeting
    new_meeting = models.Meeting(
        title=meeting_data.title,
        dateTime=meeting_data.dateTime,
        duration=meeting_data.duration,
        location=meeting_data.location,
        status=meeting_data.status,
        createdBy=current_user.userId
    )
    db.add(new_meeting)
    db.flush()  # Flush to get the meetingId
    
    # 2. Add creator as organizer participant
    organizer = models.Participant(
        meetingId=new_meeting.meetingId,
        userId=current_user.userId,
        role="organizer",
        status="accepted"
    )
    db.add(organizer)
    
    # 3. Add other participants
    for user_id in meeting_data.participants:
        if user_id == current_user.userId:
            continue  # Already added as organizer
            
        participant = models.Participant(
            meetingId=new_meeting.meetingId,
            userId=user_id,
            role="participant",
            status="pending"
        )
        db.add(participant)
    
    db.commit()
    db.refresh(new_meeting)
    return new_meeting
