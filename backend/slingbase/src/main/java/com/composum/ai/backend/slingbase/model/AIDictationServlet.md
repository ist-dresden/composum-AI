## Usage Description of the `AIDictationServlet`

The `AIDictationServlet` is an OSGi component that serves as a RESTful service for transcribing audio files into text format using AI-driven dictation capabilities. It is built on the Apache Sling framework and is designed for integration with applications that require speech recognition services.

### Service Endpoint

- **Path**: `/bin/cpm/ai/dictate`
- **HTTP Method**: `POST` (for transcription) and `GET` (to check service availability)

### Parameters

The servlet handles requests primarily through two methods, `GET` and `POST`. The following parameters can be included in the requests:

#### For `POST` requests (transcription):
1. **Mandatory Parameter: `audioStream`**
   - The primary input parameter that contains the audio stream (audio file) to be transcribed.
   
2. **Mandatory Parameter: `contentType`**
   - The content type of the audio file (e.g., `audio/mpeg` for MP3, `audio/wav` for WAV files).

3. **Optional Parameter: `language`**
   - Specifies the language code for transcription (e.g., `en` for English). If not specified, the service will attempt to detect the language automatically.

4. **Optional Parameter: `prompt`**
   - A string that provides context to the transcription service, such as previous sentences or topics that relate to the audio content.

#### For `GET` requests (service availability):
- This method checks if dictation services are enabled for a specific resource. The response will return an HTTP status code:
  - **200 OK**: If dictation services are available.
  - **404 Not Found**: If dictation services are unavailable for the specified configuration.

### Response

- **For Successful Transcription**:
  - **HTTP Status**: `200 OK`
  - **Content-Type**: `text/plain`
  - **Character Set**: `UTF-8`
  - **Body**: The transcribed text from the audio.

- **Error Responses**:
  - **400 Bad Request**: If required parameters are missing or invalid.
  - **404 Not Found**: If dictation services are not enabled.
  - **500 Internal Server Error**: If an unexpected error occurs during processing.

### Example Usage

1. **To Check Availability**:
   ```
   GET /bin/cpm/ai/dictate
   ```

2. **To Transcribe an Audio File**:
   ```http
   POST /bin/cpm/ai/dictate
   Content-Type: multipart/form-data

   {
       "audioStream": [audio file data],
       "contentType": "audio/wav",
       "language": "en",
       "prompt": "Previous dialogue or context here."
   }
   ```
   
### Error Handling

The servlet logs errors for internal tracking using SLF4J logging framework. Any errors encountered during processing of the audio file are communicated through the HTTP response with appropriate status codes.

### Summary

The `AIDictationServlet` provides a simple interface for integrating audio transcription services into applications using standard HTTP methods. Its parameters allow flexibility in inputting different audio file formats and languages, making it applicable in a variety of contexts, from voice-to-text applications to more complex AI interactions.
