from fastapi import APIRouter, HTTPException, Depends
from tasks import jobs_status
from dependencies import get_current_user
import models

router = APIRouter(prefix="/jobs", tags=["Jobs"])

@router.get("/{job_id}")
def get_job_status(
    job_id: str,
    current_user: models.User = Depends(get_current_user)
):
    """
    Poll the status of a transcription job.
    """
    status = jobs_status.get(job_id)
    
    if status is None:
        raise HTTPException(status_code=404, detail="Job ID not found")
        
    return {"jobId": job_id, "status": status}
