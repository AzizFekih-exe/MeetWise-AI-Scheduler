from fastapi import APIRouter, Depends, HTTPException, status, BackgroundTasks, UploadFile, File
from sqlalchemy.orm import Session
from sqlalchemy import or_
from typing import List
from database import get_db
from dependencies import get_current_user
import models, schemas
import uuid
import os
import shutil
from tasks import process_audio_task


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

@router.post("/{meeting_id}/transcribe", status_code=status.HTTP_202_ACCEPTED, response_model=schemas.TranscriptionJobResponse)
def transcribe_meeting(
    meeting_id: int,
    background_tasks: BackgroundTasks,
    audio_file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Initiate transcription for a meeting. Returns a job ID immediately.
    """
    meeting = db.query(models.Meeting).filter(models.Meeting.meetingId == meeting_id).first()
    if not meeting:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Meeting not found")
        
    job_id = str(uuid.uuid4())
    
    # Save file to temp directory
    temp_dir = os.path.join(os.getcwd(), "temp")
    os.makedirs(temp_dir, exist_ok=True)
    file_ext = audio_file.filename.split('.')[-1] if '.' in audio_file.filename else 'm4a'
    file_path = os.path.join(temp_dir, f"job_{job_id}.{file_ext}")
    
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(audio_file.file, buffer)
        
    # Queue background task
    background_tasks.add_task(
        process_audio_task,
        job_id=job_id,
        meeting_id=meeting_id,
        file_path=file_path,
        user_id=current_user.userId
    )
    
    return {"jobId": job_id}
