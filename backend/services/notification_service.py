import firebase_admin
from firebase_admin import credentials, messaging
from config import settings
from typing import List

def _initialize_firebase():
    try:
        firebase_admin.get_app()
    except ValueError:
        if settings.FIREBASE_SERVICE_ACCOUNT_JSON:
            try:
                cred = credentials.Certificate(settings.FIREBASE_SERVICE_ACCOUNT_JSON)
                firebase_admin.initialize_app(cred)
            except Exception as e:
                print(f"Warning: Failed to initialize Firebase Admin SDK: {e}")
        else:
            print("Warning: FIREBASE_SERVICE_ACCOUNT_JSON not set. FCM notifications will not work.")

_initialize_firebase()

def send_meeting_minutes_notification(fcm_tokens: List[str], meeting_title: str):
    """
    Sends FCM notification to devices that meeting minutes are ready.
    """
    if not fcm_tokens:
        return
        
    # Check if app is initialized
    try:
        firebase_admin.get_app()
    except ValueError:
        return
        
    try:
        message = messaging.MulticastMessage(
            notification=messaging.Notification(
                title="Minutes Ready",
                body=f"The meeting minutes for '{meeting_title}' are now available."
            ),
            tokens=fcm_tokens
        )
        response = messaging.send_multicast(message)
        print(f"{response.success_count} messages were sent successfully")
    except Exception as e:
        print(f"Error sending FCM notification: {e}")
