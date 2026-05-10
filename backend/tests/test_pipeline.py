import os
import pytest
import meetwise_schemas as schemas
from fastapi.testclient import TestClient
from unittest.mock import MagicMock, patch

@pytest.fixture
def client():
    from main import app
    return TestClient(app)

def test_transcribe_async_flow(mocker, client):
    # Mocking dependencies
    mocker.patch("meetings.get_current_user", return_value=MagicMock(userId=1))
    mocker.patch("meetings.get_db", return_value=MagicMock())
    
    # Simulate a file upload
    file_content = b"fake audio content"
    files = {"audio_file": ("test.m4a", file_content, "audio/mpeg")}
    
    # We expect a 202 immediately
    response = client.post("/api/v1/meetings/1/transcribe", files=files, headers={"Authorization": "Bearer fake_token"})
    
    assert response.status_code == 202
    assert "jobId" in response.json()

def test_audio_file_cleanup(mocker):
    from tasks import process_audio_task
    # Setup: Create a dummy file in temp
    temp_dir = os.path.join(os.getcwd(), "temp")
    os.makedirs(temp_dir, exist_ok=True)
    file_path = os.path.join(temp_dir, "test_cleanup.m4a")
    with open(file_path, "wb") as f:
        f.write(b"dummy data")
    
    # Mock AI services to prevent real calls
    mocker.patch("tasks.transcribe_audio", return_value="Test transcript")
    mocker.patch("tasks.generate_minutes", return_value={"summary": "test", "action_items": []})
    mocker.patch("tasks.send_meeting_minutes_notification")
    
    # Execute the background task logic directly
    process_audio_task("job_123", 1, file_path, 1)
    
    # Verify the file is deleted
    assert not os.path.exists(file_path), "Audio file should be deleted after processing"

def test_fcm_notification_trigger(mocker):
    from tasks import process_audio_task
    # Mock the notification service
    mock_notify = mocker.patch("tasks.send_meeting_minutes_notification")
    mocker.patch("tasks.transcribe_audio", return_value="Transcript")
    mocker.patch("tasks.generate_minutes", return_value={"summary": "test", "action_items": []})
    
    # Mock DB behavior
    mock_db = MagicMock()
    mock_meeting = MagicMock()
    mock_meeting.participants = [MagicMock(userId=2)]
    mock_meeting.createdBy = 1
    mock_db.query().filter().first.return_value = mock_meeting
    mock_db.query().filter().all.return_value = [MagicMock(fcmToken="token123")]
    
    mocker.patch("tasks.SessionLocal", return_value=mock_db)

    process_audio_task("job_456", 1, "fake_path", 1)
    
    # Verify notification was triggered
    assert mock_notify.called, "FCM notification should be triggered when job is done"
