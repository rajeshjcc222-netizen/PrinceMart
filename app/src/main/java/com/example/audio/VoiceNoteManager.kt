package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.File

data class RecordedVoiceNote(
    val base64: String,
    val durationSec: Int
)

class VoiceNoteManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var recordStartMs: Long = 0L

    private var mediaPlayer: MediaPlayer? = null
    private var playbackTempFile: File? = null

    fun startRecording(): Boolean {
        stopRecordingInternal(deleteFile = true)
        return try {
            val file = File(context.cacheDir, "princemart_vn_${System.currentTimeMillis()}.3gp")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setMaxDuration(30_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            outputFile = file
            recordStartMs = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start voice recording", e)
            stopRecordingInternal(deleteFile = true)
            false
        }
    }

    fun stopRecording(): RecordedVoiceNote? {
        val file = outputFile
        val elapsedSec = ((System.currentTimeMillis() - recordStartMs) / 1000L).toInt().coerceIn(1, 30)
        stopRecordingInternal(deleteFile = false)

        if (file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            return null
        }

        return try {
            val bytes = file.readBytes()
            file.delete()
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (encoded.length > 290_000) {
                null
            } else {
                RecordedVoiceNote(base64 = encoded, durationSec = elapsedSec)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode voice note", e)
            file.delete()
            null
        }
    }

    fun cancelRecording() {
        stopRecordingInternal(deleteFile = true)
    }

    private fun stopRecordingInternal(deleteFile: Boolean) {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        if (deleteFile) {
            outputFile?.delete()
        }
        outputFile = null
    }

    fun playBase64(base64Audio: String, onCompleted: () -> Unit, onError: () -> Unit) {
        stopPlayback()
        try {
            val bytes = Base64.decode(base64Audio, Base64.DEFAULT)
            val file = File(context.cacheDir, "princemart_play_${System.currentTimeMillis()}.3gp")
            file.writeBytes(bytes)
            playbackTempFile = file

            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopPlayback()
                    onCompleted()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    onError()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play voice note", e)
            stopPlayback()
            onError()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        playbackTempFile?.delete()
        playbackTempFile = null
    }

    fun playIncomingOrderAlert() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    toneGen.release()
                } catch (_: Exception) {
                }
            }, 500)
        } catch (e: Exception) {
            Log.w(TAG, "Alert chime unavailable: ${e.message}")
        }
    }

    fun releaseAll() {
        stopRecordingInternal(deleteFile = true)
        stopPlayback()
    }

    private companion object {
        const val TAG = "VoiceNoteManager"
    }
}
