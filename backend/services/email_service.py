import smtplib
from email.headerregistry import Address
from email.message import EmailMessage
from pathlib import Path
from typing import Optional

from config import settings

OUTBOX_DIR = Path(__file__).resolve().parents[1] / "outbox"


def _from_header() -> Address | str:
    if not settings.SMTP_FROM_EMAIL:
        return ""

    if "@" not in settings.SMTP_FROM_EMAIL:
        return settings.SMTP_FROM_EMAIL

    username, domain = settings.SMTP_FROM_EMAIL.rsplit("@", 1)
    return Address(settings.SMTP_FROM_NAME, username, domain)


def send_meeting_scheduled_email(
    recipient_email: str,
    meeting_title: str,
    meeting_time: str,
    duration_minutes: int,
    location: Optional[str] = None,
) -> bool:
    """
    Send a meeting confirmation email to the organizer.
    Returns False instead of raising when SMTP is not configured for local dev.
    """
    if not settings.SMTP_HOST or not settings.SMTP_FROM_EMAIL:
        OUTBOX_DIR.mkdir(exist_ok=True)
        safe_title = "".join(char if char.isalnum() else "_" for char in meeting_title)[:40]
        outbox_file = OUTBOX_DIR / f"meeting_scheduled_{safe_title}.txt"
        outbox_file.write_text(
            "SMTP not configured. Development email preview.\n\n"
            f"To: {recipient_email}\n"
            f"Subject: MeetWise scheduled: {meeting_title}\n\n"
            "Your MeetWise meeting has been scheduled.\n\n"
            f"Title: {meeting_title}\n"
            f"Time: {meeting_time}\n"
            f"Duration: {duration_minutes} minutes\n"
            f"Location: {location or ''}\n",
            encoding="utf-8",
        )
        print(
            "Email not sent: SMTP_HOST and SMTP_FROM_EMAIL are not configured. "
            f"Preview written to {outbox_file}."
        )
        return False

    message = EmailMessage()
    message["Subject"] = f"MeetWise scheduled: {meeting_title}"
    message["From"] = _from_header()
    message["To"] = recipient_email

    location_line = f"\nLocation: {location}" if location else ""
    message.set_content(
        "Your MeetWise meeting has been scheduled.\n\n"
        f"Title: {meeting_title}\n"
        f"Time: {meeting_time}\n"
        f"Duration: {duration_minutes} minutes"
        f"{location_line}\n"
    )

    try:
        with smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT) as smtp:
            if settings.SMTP_USE_TLS:
                smtp.starttls()
            if settings.SMTP_USERNAME and settings.SMTP_PASSWORD:
                smtp.login(settings.SMTP_USERNAME, settings.SMTP_PASSWORD)
            smtp.send_message(message)
        return True
    except Exception as exc:
        print(f"Email not sent: {exc}")
        return False


def send_meeting_cancelled_email(
    recipient_email: str,
    meeting_title: str,
    meeting_time: str,
    duration_minutes: int,
    location: Optional[str] = None,
) -> bool:
    """
    Send a meeting cancellation email to an organizer or invitee.
    """
    if not settings.SMTP_HOST or not settings.SMTP_FROM_EMAIL:
        OUTBOX_DIR.mkdir(exist_ok=True)
        safe_title = "".join(char if char.isalnum() else "_" for char in meeting_title)[:40]
        outbox_file = OUTBOX_DIR / f"meeting_cancelled_{safe_title}_{recipient_email.replace('@', '_at_')}.txt"
        outbox_file.write_text(
            "SMTP not configured. Development cancellation email preview.\n\n"
            f"To: {recipient_email}\n"
            f"Subject: MeetWise cancelled: {meeting_title}\n\n"
            "This MeetWise meeting has been cancelled.\n\n"
            f"Title: {meeting_title}\n"
            f"Time: {meeting_time}\n"
            f"Duration: {duration_minutes} minutes\n"
            f"Location: {location or ''}\n",
            encoding="utf-8",
        )
        print(
            "Cancellation email not sent: SMTP_HOST and SMTP_FROM_EMAIL are not configured. "
            f"Preview written to {outbox_file}."
        )
        return False

    message = EmailMessage()
    message["Subject"] = f"MeetWise cancelled: {meeting_title}"
    message["From"] = _from_header()
    message["To"] = recipient_email

    location_line = f"\nLocation: {location}" if location else ""
    message.set_content(
        "This MeetWise meeting has been cancelled.\n\n"
        f"Title: {meeting_title}\n"
        f"Time: {meeting_time}\n"
        f"Duration: {duration_minutes} minutes"
        f"{location_line}\n"
    )

    try:
        with smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT) as smtp:
            if settings.SMTP_USE_TLS:
                smtp.starttls()
            if settings.SMTP_USERNAME and settings.SMTP_PASSWORD:
                smtp.login(settings.SMTP_USERNAME, settings.SMTP_PASSWORD)
            smtp.send_message(message)
        return True
    except Exception as exc:
        print(f"Cancellation email not sent: {exc}")
        return False
