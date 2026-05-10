import json
import mimetypes
import time
import urllib.error
import urllib.parse
import urllib.request
from openai import OpenAI
from config import settings

client = OpenAI(api_key=settings.OPENAI_API_KEY)

def _require_real_openai_key() -> None:
    if not settings.OPENAI_API_KEY or settings.OPENAI_API_KEY.startswith("dev-"):
        raise RuntimeError("OpenAI API key is not configured. Set OPENAI_API_KEY in backend/.env.")

def transcribe_audio(file_path: str) -> str:
    """
    Transcribes the given audio file using the configured provider.
    """
    provider = settings.TRANSCRIPTION_PROVIDER.lower()
    if provider == "assemblyai":
        return transcribe_audio_with_assemblyai(file_path)
    if provider == "deepgram":
        return transcribe_audio_with_deepgram(file_path)
    if provider != "openai":
        raise RuntimeError(f"Unsupported transcription provider: {settings.TRANSCRIPTION_PROVIDER}")

    _require_real_openai_key()

    with open(file_path, "rb") as audio_file:
        transcript = client.audio.transcriptions.create(
            model="whisper-1",
            file=audio_file
        )
    return transcript.text

def transcribe_audio_with_deepgram(file_path: str) -> str:
    """
    Calls Deepgram's pre-recorded audio API for local uploaded meeting audio.
    """
    if not settings.DEEPGRAM_API_KEY or settings.DEEPGRAM_API_KEY.startswith("replace_"):
        raise RuntimeError("Deepgram API key is not configured. Set DEEPGRAM_API_KEY in backend/.env.")

    query = urllib.parse.urlencode({
        "model": settings.DEEPGRAM_MODEL,
        "smart_format": "true",
        "punctuate": "true",
    })
    url = f"https://api.deepgram.com/v1/listen?{query}"
    content_type = mimetypes.guess_type(file_path)[0] or "audio/mp4"

    with open(file_path, "rb") as audio_file:
        request = urllib.request.Request(
            url=url,
            data=audio_file.read(),
            method="POST",
            headers={
                "Authorization": f"Token {settings.DEEPGRAM_API_KEY}",
                "Content-Type": content_type,
            },
        )

    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        if exc.code == 401:
            raise RuntimeError(
                "Deepgram rejected DEEPGRAM_API_KEY. Create a project API key in Deepgram Console "
                "with Listen access, paste that key into backend/.env, and restart the backend."
            ) from exc
        raise RuntimeError(f"Deepgram transcription failed ({exc.code}): {body}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Deepgram transcription failed: {exc.reason}") from exc

    transcript = (
        payload.get("results", {})
        .get("channels", [{}])[0]
        .get("alternatives", [{}])[0]
        .get("transcript", "")
        .strip()
    )
    if not transcript:
        raise RuntimeError("Deepgram returned an empty transcript.")
    return transcript

def _assemblyai_headers(content_type: str | None = None) -> dict:
    if not settings.ASSEMBLYAI_API_KEY or settings.ASSEMBLYAI_API_KEY.startswith("replace_"):
        raise RuntimeError("AssemblyAI API key is not configured. Set ASSEMBLYAI_API_KEY in backend/.env.")

    headers = {"Authorization": settings.ASSEMBLYAI_API_KEY}
    if content_type:
        headers["Content-Type"] = content_type
    return headers

def _assemblyai_request_json(path: str, method: str = "GET", data: dict | bytes | None = None, content_type: str | None = None) -> dict:
    url = f"{settings.ASSEMBLYAI_BASE_URL.rstrip('/')}{path}"
    body = None
    if isinstance(data, dict):
        body = json.dumps(data).encode("utf-8")
        content_type = content_type or "application/json"
    elif isinstance(data, bytes):
        body = data

    request = urllib.request.Request(
        url=url,
        data=body,
        method=method,
        headers=_assemblyai_headers(content_type),
    )
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        if exc.code in (401, 403):
            raise RuntimeError(
                "AssemblyAI rejected ASSEMBLYAI_API_KEY. Copy a valid API key from the AssemblyAI dashboard, "
                "paste it into backend/.env, and restart the backend."
            ) from exc
        raise RuntimeError(f"AssemblyAI request failed ({exc.code}): {body}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"AssemblyAI request failed: {exc.reason}") from exc

def transcribe_audio_with_assemblyai(file_path: str) -> str:
    """
    Uploads local audio to AssemblyAI, submits it for transcription, and polls until complete.
    """
    with open(file_path, "rb") as audio_file:
        upload_payload = _assemblyai_request_json(
            path="/v2/upload",
            method="POST",
            data=audio_file.read(),
            content_type="application/octet-stream",
        )

    upload_url = upload_payload.get("upload_url")
    if not upload_url:
        raise RuntimeError("AssemblyAI upload did not return an upload_url.")

    transcript_payload = _assemblyai_request_json(
        path="/v2/transcript",
        method="POST",
        data={
            "audio_url": upload_url,
            "speech_models": [settings.ASSEMBLYAI_SPEECH_MODEL],
            "punctuate": True,
            "format_text": True,
        },
    )
    transcript_id = transcript_payload.get("id")
    if not transcript_id:
        raise RuntimeError("AssemblyAI did not return a transcript id.")

    for _ in range(90):
        transcript = _assemblyai_request_json(path=f"/v2/transcript/{transcript_id}")
        status = transcript.get("status")
        if status == "completed":
            text = (transcript.get("text") or "").strip()
            if not text:
                raise RuntimeError("AssemblyAI returned an empty transcript.")
            return text
        if status == "error":
            raise RuntimeError(f"AssemblyAI transcription failed: {transcript.get('error') or 'Unknown error'}")
        time.sleep(2)

    raise RuntimeError("AssemblyAI transcription timed out.")

def generate_minutes(transcript: str, max_retries: int = 2) -> dict:
    """
    Calls GPT to generate structured meeting minutes from transcript.
    Includes retry logic to ensure valid JSON and schema compliance.
    """
    if settings.MINUTES_PROVIDER.lower() == "local":
        return generate_local_minutes(transcript)
    if settings.MINUTES_PROVIDER.lower() != "openai":
        raise RuntimeError(f"Unsupported minutes provider: {settings.MINUTES_PROVIDER}")

    _require_real_openai_key()

    system_prompt = """
    You are a professional meeting secretary. Extract structured information
    from meeting transcripts. Always respond with valid JSON only.
    No preamble, no markdown, no backticks.
    """
    
    user_prompt = f"""
    Return a JSON object with exactly these fields:
    {{
        "summary": "one paragraph, 3-5 sentences",
        "topics": ["topic 1", "topic 2"],
        "decisions": ["decision 1", "decision 2"],
        "action_items": [
            {{"task": "...", "owner": "First name", "deadline": "YYYY-MM-DD or null", "done": false}}
        ]
    }}

    Transcript:
    {transcript}
    """
    
    for attempt in range(max_retries):
        try:
            response = client.chat.completions.create(
                model="gpt-4o-mini", # Using gpt-4o-mini for better instruction following
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                response_format={ "type": "json_object" }
            )
            
            content = response.choices[0].message.content
            data = json.loads(content)
            
            # Basic validation of required keys
            required_keys = ["summary", "action_items"]
            if all(key in data for key in required_keys):
                return data
            else:
                raise ValueError("Missing required keys in GPT response")
                
        except (json.JSONDecodeError, ValueError) as e:
            print(f"Attempt {attempt + 1} failed for LLM JSON: {e}")
            if attempt == max_retries - 1:
                # Fallback: return raw notes if all retries fail
                return {
                    "summary": "Minutes generation failed after retries.",
                    "action_items": [],
                    "rawNotes": transcript
                }
            time.sleep(1) # Short pause before retry
            
    return {}

def generate_local_minutes(transcript: str) -> dict:
    """
    Creates simple minutes locally from the transcript so demos do not require paid LLM credits.
    """
    clean_transcript = " ".join(transcript.split())
    if not clean_transcript:
        return {
            "summary": "No speech was detected in this recording.",
            "action_items": [],
        }

    sentences = [
        sentence.strip()
        for sentence in clean_transcript.replace("?", ".").replace("!", ".").split(".")
        if sentence.strip()
    ]
    summary_source = sentences[:3] if sentences else [clean_transcript[:240]]
    summary = ". ".join(summary_source)
    if summary and not summary.endswith("."):
        summary += "."

    action_items = []
    action_keywords = ("action", "todo", "to do", "follow up", "will", "need to", "should")
    for sentence in sentences:
        lower = sentence.lower()
        if any(keyword in lower for keyword in action_keywords):
            action_items.append({
                "task": sentence,
                "owner": "Unassigned",
                "deadline": None,
                "done": False,
            })

    return {
        "summary": summary or clean_transcript[:240],
        "topics": [],
        "decisions": [],
        "action_items": action_items[:5],
    }
