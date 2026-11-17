package com.safetube.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** অ্যাপের ভেতরে ছোট ভাসমান ভিডিও: চাপলে আবার বড় হয়, ✕ চাপলে বন্ধ হয় */
@Composable
fun MiniVideo(vm: MainVm, t: YtTheme, modifier: Modifier) {
    val p = Video.player
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .width(280.dp)
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(Color.Black)
            .border(1.dp, t.line, shape)
            .clickable { vm.minimized = false }
    ) {
        VideoSurface(p, Modifier.fillMaxSize())
        Row(Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
            Text(
                if (p.isPlaying) "⏸" else "▶", color = Color.White, fontSize = 16.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable { p.toggle() }
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            )
            Text(
                "✕", color = Color.White, fontSize = 16.sp,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable { vm.closePlayer() }
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            )
        }
    }
}
