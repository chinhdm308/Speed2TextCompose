package com.epayjsc.speech2text.ui.record

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.IOException

class RecordViewModel : ViewModel() {

    var recordingState by mutableStateOf(RecordingState.IDLE)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var responseMessage by mutableStateOf<String?>(null)
        private set

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null

    // 📌 Lấy thư mục Android/data/[package_name]/files/
    private fun getAppExternalDir(context: Context): File {
        return File(context.getExternalFilesDir(null), "Recordings").apply { if (!exists()) mkdirs() }
    }

    fun startRecording(context: Context) {
        if (recordingState == RecordingState.IDLE) {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

//                audioFile = File(
//                    context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
//                    "audio_record_${System.currentTimeMillis()}.3gp"
//                )

//                val fileName = "audio_record_${System.currentTimeMillis()}.3gp"
//                audioFile = File(context.filesDir, fileName) // Lưu vào Internal Storage

                val fileName = "audio_record_${System.currentTimeMillis()}.3gp"
                audioFile = File(getAppExternalDir(context), fileName) // 📌 Lưu vào Android/data

                setOutputFile(audioFile?.absolutePath)
                try {
                    prepare()
                    start()
                    recordingState = RecordingState.RECORDING
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopRecording() {
        if (recordingState == RecordingState.RECORDING) {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            recordingState = RecordingState.IDLE
        }
    }

    fun playRecording(file: File) {
        if (recordingState == RecordingState.IDLE) {
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    recordingState = RecordingState.PLAYING
                    setOnCompletionListener {
                        recordingState = RecordingState.IDLE
                        release()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopPlaying() {
        if (recordingState == RecordingState.PLAYING) {
            mediaPlayer?.release()
            mediaPlayer = null
            recordingState = RecordingState.IDLE
        }
    }

    fun getSavedRecordings(context: Context): List<File> {
//        return context.filesDir.listFiles { file -> file.extension == "3gp" }?.toList() ?: emptyList()
        return getAppExternalDir(context).listFiles { file -> file.extension == "3gp" }?.toList() ?: emptyList()
    }

    fun deleteRecording(file: File) {
        file.delete()
    }

    // 📌 Gửi file lên API
    private suspend fun uploadFile(file: File) = withContext(Dispatchers.IO) {
        try {
            val requestBody = file.asRequestBody("audio/3gp".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

            val response = ApiService.instance.uploadFile(filePart)
            if (response.isSuccessful) {
                println("📤 Upload thành công!")
                val responseBody = response.body()?.string() ?: "Empty response"
                println("Response: $responseBody")
                responseMessage = responseBody
            } else {
                println("❌ Lỗi upload!")
                responseMessage = "⚠ Lỗi upload!"
            }
        } catch (e: Exception) {
            println("❌ Lỗi upload! ${e.message}")
            responseMessage = "⚠ Lỗi upload! ${e.message}"
        }
    }

    fun uploadFileAsync(file: File) {
        viewModelScope.launch {
            responseMessage = null
            isLoading = true
            uploadFile(file)
            isLoading = false
        }
    }


    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }

    enum class RecordingState {
        IDLE, RECORDING, PLAYING
    }
}

// ✅ Interface API
interface ApiService {
    @Multipart
    @POST("/speech2text")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Response<ResponseBody>

    companion object {
        private val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        private val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        private val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.98.100:8009/")  // 🔥 Thay URL API của bạn
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val instance: ApiService by lazy { retrofit.create(ApiService::class.java) }
    }
}