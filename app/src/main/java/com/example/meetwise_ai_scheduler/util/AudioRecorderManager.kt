package com.example.meetwise_ai_scheduler.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null

    fun startRecording(outputFile: File) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            
            try {
                prepare()
                start()
                Log.d("AudioRecorder", "Recording started: ${outputFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Failed to start recording", e)
            }
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.d("AudioRecorder", "Recording stopped and released")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording", e)
        }
    }

    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
