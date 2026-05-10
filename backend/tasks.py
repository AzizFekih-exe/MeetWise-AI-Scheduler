import os
import json
from datetime import datetime, timezone
from sqlalchemy.orm import Session
from database import SessionLocal
import models
from services.ai_service import transcribe_audio, generate_minutes
from services.notification_service import send_meeting_minutes_notification

# Global dictionary to track background job status
# In a production app, you'd use Redis or a DB table for this.
jobs_status = {}

def set_job_status(job_id: str, status: str, progress: float, message: str, error_message: str = None):
    jobs_status[job_id] = {
        "jobId": job_id,
        "status": status,
        "progress": progress,
        "message": message,
        "errorMessage": error_message,
    }

def process_audio_task(job_id: str, meeting_id: int, file_path: str, user_id: int):
    """
    Background job to process meeting audio and generate minutes.
    """
    set_job_status(job_id, "transcribing", 0.35, "Transcribing content")
    db: Session = SessionLocal()
    try:
        # 1. Transcribe audio
        transcript = transcribe_audio(file_path)

        # 2. Generate minutes with GPT
        set_job_status(job_id, "generating", 0.72, "Generating minutes")
        minutes_data = generate_minutes(transcript)
        
        # 3. Save to database
        existing_minutes = db.query(models.Minutes).filter(models.Minutes.meetingId == meeting_id).first()
        if existing_minutes:
            existing_minutes.summaryText = minutes_data.get("summary", "")
            existing_minutes.actionItems = minutes_data.get("action_items", [])
            existing_minutes.rawNotes = transcript
            existing_minutes.generatedAt = datetime.now(timezone.utc)
        else:
            db.add(models.Minutes(
                meetingId=meeting_id,
                summaryText=minutes_data.get("summary", ""),
                actionItems=minutes_data.get("action_items", []),
                rawNotes=transcript
            ))
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
        set_job_status(job_id, "done", 1.0, "Minutes ready")
                
    except Exception as e:
        print(f"Error in process_audio_task for job {job_id}: {e}")
        failed_progress = jobs_status.get(job_id, {}).get("progress", 0.35)
        set_job_status(job_id, "failed", failed_progress, "Transcription job failed", str(e))
        db.rollback()
    finally:
        db.close()
        # 5. Delete file within 60s
        if os.path.exists(file_path):
            try:
                os.remove(file_path)
            except Exception as e:
                print(f"Error deleting file {file_path}: {e}")
