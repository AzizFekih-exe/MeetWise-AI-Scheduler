from fastapi import APIRouter, Depends, HTTPException, status, BackgroundTasks, UploadFile, File
from sqlalchemy.orm import Session
from sqlalchemy import or_
from typing import List
from database import get_db
from dependencies import get_current_user
import models
import meetwise_schemas as schemas


import uuid
import os
import shutil
from tasks import process_audio_task
from services.email_service import send_meeting_cancelled_email, send_meeting_scheduled_email


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
    ).filter(
        models.Meeting.status != "cancelled"
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
    
    recipient_emails = {current_user.email}

    # 3. Add other participants
    for user_id in meeting_data.participants:
        if user_id == current_user.userId:
            continue  # Already added as organizer

        invited_user = db.query(models.User).filter(models.User.userId == user_id).first()
        if invited_user:
            recipient_emails.add(invited_user.email)
            
        participant = models.Participant(
            meetingId=new_meeting.meetingId,
            userId=user_id,
            role="participant",
            status="pending"
        )
        db.add(participant)

    processed_invite_emails = set()
    for email in meeting_data.participantEmails:
        normalized_email = str(email).lower()
        if normalized_email in processed_invite_emails:
            continue
        processed_invite_emails.add(normalized_email)
        recipient_emails.add(normalized_email)
        db.add(models.MeetingInviteEmail(
            meetingId=new_meeting.meetingId,
            email=normalized_email,
        ))
        invited_user = db.query(models.User).filter(models.User.email == normalized_email).first()
        if invited_user and invited_user.userId != current_user.userId:
            participant_exists = db.query(models.Participant).filter(
                models.Participant.meetingId == new_meeting.meetingId,
                models.Participant.userId == invited_user.userId,
            ).first()
            if not participant_exists:
                db.add(models.Participant(
                    meetingId=new_meeting.meetingId,
                    userId=invited_user.userId,
                    role="participant",
                    status="pending"
                ))
    
    db.commit()
    db.refresh(new_meeting)

    for recipient_email in sorted(recipient_emails):
        send_meeting_scheduled_email(
            recipient_email=recipient_email,
            meeting_title=new_meeting.title,
            meeting_time=new_meeting.dateTime.isoformat(),
            duration_minutes=new_meeting.duration,
            location=new_meeting.location,
        )

    return new_meeting

@router.delete("/{meeting_id}", status_code=status.HTTP_204_NO_CONTENT)
def cancel_meeting(
    meeting_id: int,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Cancel a meeting. Only the creator can perform this action.
    """
    meeting = db.query(models.Meeting).filter(models.Meeting.meetingId == meeting_id).first()
    
    if not meeting:
        raise HTTPException(status_code=404, detail="Meeting not found")
    
    if meeting.createdBy != current_user.userId:
        raise HTTPException(status_code=403, detail="Only the organizer can cancel the meeting")

    recipient_emails = set()
    creator = db.query(models.User).filter(models.User.userId == meeting.createdBy).first()
    if creator:
        recipient_emails.add(creator.email)

    participant_user_ids = [
        participant.userId
        for participant in db.query(models.Participant).filter(models.Participant.meetingId == meeting_id).all()
    ]
    if participant_user_ids:
        participant_users = db.query(models.User).filter(models.User.userId.in_(participant_user_ids)).all()
        recipient_emails.update(user.email for user in participant_users)

    invite_rows = db.query(models.MeetingInviteEmail).filter(
        models.MeetingInviteEmail.meetingId == meeting_id
    ).all()
    recipient_emails.update(invite.email for invite in invite_rows)

    meeting.status = "cancelled"
    db.commit()

    for recipient_email in sorted(email.lower() for email in recipient_emails if email):
        send_meeting_cancelled_email(
            recipient_email=recipient_email,
            meeting_title=meeting.title,
            meeting_time=meeting.dateTime.isoformat(),
            duration_minutes=meeting.duration,
            location=meeting.location,
        )

    return None

@router.get("/{meeting_id}/slots", response_model=List[schemas.SlotSuggestion])
def get_meeting_slots(
    meeting_id: int,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Return hardcoded slot suggestions (Shell for Task 5).
    """
    from datetime import datetime, timedelta
    now = datetime.now()
    
    # Returning 3 hardcoded slots
    return [
        {"startTime": now + timedelta(days=1, hours=9), "endTime": now + timedelta(days=1, hours=10), "score": 0.95},
        {"startTime": now + timedelta(days=1, hours=14), "endTime": now + timedelta(days=1, hours=15), "score": 0.82},
        {"startTime": now + timedelta(days=2, hours=10), "endTime": now + timedelta(days=2, hours=11), "score": 0.75}
    ]

@router.post("/{meeting_id}/confirm", response_model=schemas.MeetingResponse)
def confirm_meeting_slot(
    meeting_id: int,
    confirmation: schemas.MeetingConfirm,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Confirm a specific slot for the meeting.
    """
    meeting = db.query(models.Meeting).filter(models.Meeting.meetingId == meeting_id).first()
    
    if not meeting:
        raise HTTPException(status_code=404, detail="Meeting not found")
    
    if meeting.createdBy != current_user.userId:
        raise HTTPException(status_code=403, detail="Only the organizer can confirm the meeting")
    
    meeting.dateTime = confirmation.startTime
    meeting.status = "confirmed"
    
    db.commit()
    db.refresh(meeting)
    
    # TODO: Dispatch Google Calendar invites here
    return meeting

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
        
    from tasks import process_audio_task, set_job_status
    
    job_id = str(uuid.uuid4())
    set_job_status(job_id, "pending", 0.30, "Audio uploaded. Waiting to transcribe.")
    
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
    
    return {"jobId": job_id, "status": "pending"}


@router.get("/{meeting_id}/minutes", response_model=schemas.MinutesResponse)
def get_meeting_minutes(
    meeting_id: int,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Retrieve the generated minutes for a specific meeting.
    """
    minutes = db.query(models.Minutes).filter(models.Minutes.meetingId == meeting_id).first()
    
    if not minutes:
        raise HTTPException(status_code=404, detail="Minutes not found for this meeting")
    
    # Optional: Check if the user was a participant in the meeting
    participant = db.query(models.Participant).filter(
        models.Participant.meetingId == meeting_id,
        models.Participant.userId == current_user.userId
    ).first()
    
    if not participant and meeting_id not in [m.meetingId for m in current_user.meetings_created]:
        raise HTTPException(status_code=403, detail="You do not have access to these minutes")
        
    return minutes

