import json
from openai import OpenAI
from config import settings

client = OpenAI(api_key=settings.OPENAI_API_KEY)

def transcribe_audio(file_path: str) -> str:
    """
    Calls Whisper API to transcribe the given audio file.
    """
    with open(file_path, "rb") as audio_file:
        transcript = client.audio.transcriptions.create(
            model="whisper-1",
            file=audio_file
        )
    return transcript.text

def generate_minutes(transcript: str) -> dict:
    """
    Calls GPT to generate meeting minutes (summary and action items) from transcript.
    Expected output: JSON with "summaryText" (string) and "actionItems" (list of strings).
    """
    prompt = f"""
You are an expert meeting assistant. Read the following transcript and extract:
1. A concise summary of the meeting.
2. A list of action items.

Format your response as a valid JSON object with EXACTLY these keys:
"summaryText": A string containing the summary.
"actionItems": An array of strings, each being an action item.

Transcript:
{transcript}
"""
    response = client.chat.completions.create(
        model="gpt-3.5-turbo",
        messages=[
            {"role": "system", "content": "You are a helpful assistant designed to output JSON."},
            {"role": "user", "content": prompt}
        ],
        response_format={ "type": "json_object" }
    )
    
    content = response.choices[0].message.content
    return json.loads(content)
