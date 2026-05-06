from fastapi import FastAPI, Depends
from database import engine, Base
import auth, meetings, jobs, users, models

# Create database tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title="MeetWise API", version="1.0.0")

# Include Routers
app.include_router(auth.router, prefix="/api/v1")
app.include_router(meetings.router, prefix="/api/v1")
app.include_router(jobs.router, prefix="/api/v1")
app.include_router(users.router, prefix="/api/v1")



@app.get("/api/v1/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "MeetWise Backend",
        "version": "1.0.0"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
