import json
import time
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

def generate_minutes(transcript: str, max_retries: int = 2) -> dict:
    """
    Calls GPT to generate structured meeting minutes from transcript.
    Includes retry logic to ensure valid JSON and schema compliance.
    """
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
