import os
import json
from sqlalchemy.orm import Session
from database import SessionLocal
import models
from services.ai_service import transcribe_audio, generate_minutes
from services.notification_service import send_meeting_minutes_notification

# Global dictionary to track background job status
# In a production app, you'd use Redis or a DB table for this.
jobs_status = {}

def process_audio_task(job_id: str, meeting_id: int, file_path: str, user_id: int):
    """
    Background job to process meeting audio and generate minutes.
    """
    jobs_status[job_id] = "processing"
    db: Session = SessionLocal()
    try:
        # 1. Transcribe audio
        transcript = transcribe_audio(file_path)
        
        # 2. Generate minutes with GPT
        minutes_data = generate_minutes(transcript)
        
        # 3. Save to database
        new_minutes = models.Minutes(
            meetingId=meeting_id,
            summaryText=minutes_data.get("summaryText", ""),
            actionItems=minutes_data.get("actionItems", []),
            rawNotes=transcript
        )
        db.add(new_minutes)
        db.commit()
        
        # 4. Notify user
        meeting = db.query(models.Meeting).filter(models.Meeting.meetingId == meeting_id).first()
        if meeting:
            user_ids = [p.userId for p in meeting.participants]
            user_ids.append(meeting.createdBy)
            user_ids = list(set(user_ids))
            
            devices = db.query(models.UserDevice).filter(models.UserDevice.userId.in_(user_ids)).all()
            tokens = [d.fcmToken for d in devices if d.fcmToken]
            
            if tokens:
                send_meeting_minutes_notification(tokens, meeting.title)
        
        # Mark as done
        jobs_status[job_id] = "done"
                
    except Exception as e:
        print(f"Error in process_audio_task for job {job_id}: {e}")
        jobs_status[job_id] = "failed"
        db.rollback()
    finally:
        db.close()
        # 5. Delete file within 60s
        if os.path.exists(file_path):
            try:
                os.remove(file_path)
            except Exception as e:
                print(f"Error deleting file {file_path}: {e}")
