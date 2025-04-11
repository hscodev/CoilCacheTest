package com.example.imageloadtest

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.request.transformations
import coil3.size.Size
import coil3.toBitmap
import coil3.transform.Transformation
import com.example.imageloadtest.ui.theme.ImageLoadTestTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repeatCount = (application as App).settingDataSource.repeatCount
        setContent {
            ImageLoadTestTheme {
                Greeting(
                    modifier = Modifier.clickable {
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                    repeatCount = repeatCount
                )
            }
        }
    }

}

@Composable
fun Greeting(
    repeatCount: Int,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Fixed(1),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        items(repeatCount) {
            val url = "https://prs.ohouse.com/apne2/home/profileImageUrl/v1-255784025448448.jpg?w=720&q=30"
            val memoryCacheKey = url
            val bitmapCacheKey = url + "bitmapCacheKey"
            val transformCacheKey = url + "transformCacheKey"
            val memoryCache = LocalContext.current.applicationContext.imageLoader.memoryCache

            val requestBuilder = if (memoryCache?.get(MemoryCache.Key(memoryCacheKey)) == null) {
                Log.e("CCOOVV", "useUrl")
                ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey(memoryCacheKey)
                    .allowHardware(false)
            } else {
                Log.e("CCOOVV", "useMemoryCacheKey")
                ImageRequest.Builder(LocalContext.current)
                    .data(memoryCache[MemoryCache.Key(memoryCacheKey)]?.image?.toBitmap())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey(bitmapCacheKey)
            }.transformations(
                object: Transformation() {
                    override val cacheKey: String
                        get() = transformCacheKey

                    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                        Log.e("CCOOVV", "transform")
                        return input
                    }
                }
            ).allowHardware(false).build()

            AsyncImage(
                modifier = Modifier.size(100.dp),
                contentDescription = null,
                model = requestBuilder,
                contentScale = ContentScale.Crop
            )
        }
    }
}

