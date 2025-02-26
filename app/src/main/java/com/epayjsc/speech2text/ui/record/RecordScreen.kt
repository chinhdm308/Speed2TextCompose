package com.epayjsc.speech2text.ui.record

import android.Manifest
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(viewModel: RecordViewModel = viewModel()) {
    val context = LocalContext.current
    val recordingState = viewModel.recordingState
    val isLoading = viewModel.isLoading
    val responseMessage = viewModel.responseMessage

    var recordedFiles by remember { mutableStateOf(viewModel.getSavedRecordings(context)) }
    var selectedFile by remember { mutableStateOf<File?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()

    fun refreshFileList() {
        recordedFiles = viewModel.getSavedRecordings(context)
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            delay(1500)
            refreshFileList()
            pullToRefreshState.endRefresh()
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                viewModel.startRecording(context)
            } else {
                Toast.makeText(context, "❌ Bạn cần cấp quyền để tiếp tục!", Toast.LENGTH_SHORT).show()
            }
        }

    fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hiệu ứng sóng ghi âm
        if (recordingState == RecordViewModel.RecordingState.RECORDING) {
            RecordingWaveAnimation()
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (recordingState) {
            RecordViewModel.RecordingState.IDLE -> {
                Spacer(modifier = Modifier.height(100.dp))
                Button(
                    onClick = { requestPermissions() },
                ) {
                    Text(
                        "🎤 BẮT ĐẦU GHI ÂM",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "📂 Danh sách ghi âm:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(10.dp)
                )
                Box(Modifier.nestedScroll(pullToRefreshState.nestedScrollConnection)) {
                    LazyColumn(
                        modifier = Modifier.height(600.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recordedFiles) { file ->
                            RecordingItem(file, onPlay = {
                                viewModel.playRecording(file)
                            }, onDelete = {
                                viewModel.deleteRecording(file)
                                refreshFileList()
                            }, onSelect = {
                                selectedFile = file
                            })
                        }
                    }
                    if (pullToRefreshState.progress > 0.5F || pullToRefreshState.isRefreshing) {
                        PullToRefreshContainer(
                            modifier = Modifier.align(Alignment.TopCenter),
                            state = pullToRefreshState,
                        )
                    }
                }

                selectedFile?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("📌 File đã chọn: ${it.name}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Button(onClick = {
                            viewModel.uploadFileAsync(it)
                        }) {
                            Text(
                                "📤 Gửi file lên API",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                // 📝 Hiển thị kết quả API
                responseMessage?.let { message ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = message,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp,
                    )
                }
            }

            RecordViewModel.RecordingState.RECORDING -> {
                Button(onClick = {
                    viewModel.stopRecording()
                    refreshFileList()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text(
                        "⏹ DỪNG GHI ÂM",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            RecordViewModel.RecordingState.PLAYING -> {
                Button(
                    onClick = { viewModel.stopPlaying() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Text(
                        "⏸ DỪNG PHÁT",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RequestPermissions(
    onGranted: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                onGranted()
            } else {
                Toast.makeText(context, "❌ Bạn cần cấp quyền để tiếp tục!", Toast.LENGTH_SHORT).show()
            }
        }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}


@Composable
fun RecordingItem(file: File, onPlay: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE1E1E1), shape = RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFA6A6A6), shape = RoundedCornerShape(20.dp))
            .clip(shape = RoundedCornerShape(20.dp))
            .clickable { onSelect() }
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = file.name, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "💿 Dung lượng: ${file.length() / 1024} KB", fontSize = 18.sp, color = Color.DarkGray)
        }
        Row {
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Blue,
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}