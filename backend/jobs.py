from fastapi import APIRouter, HTTPException, Depends
from tasks import jobs_status
from dependencies import get_current_user
import models
import meetwise_schemas as schemas

router = APIRouter(prefix="/jobs", tags=["Jobs"])

@router.get("/{job_id}", response_model=schemas.JobStatusResponse)
def get_job_status(
    job_id: str,
    current_user: models.User = Depends(get_current_user)
):
    """
    Poll the status of a transcription job.
    """
    job = jobs_status.get(job_id)
    
    if job is None:
        raise HTTPException(status_code=404, detail="Job ID not found")

    if isinstance(job, str):
        progress_by_status = {
            "pending": 0.25,
            "processing": 0.55,
            "done": 1.0,
            "failed": 0.55,
        }
        return {
            "jobId": job_id,
            "status": job,
            "progress": progress_by_status.get(job, 0.25),
            "message": job.replace("_", " ").title(),
            "errorMessage": None,
        }

    return job
