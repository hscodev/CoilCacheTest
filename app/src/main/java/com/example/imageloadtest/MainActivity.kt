package com.example.imageloadtest

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Size
import coil3.toBitmap
import coil3.transform.Transformation
import com.example.imageloadtest.ui.theme.ImageLoadTestTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val selectedImage = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.getStringExtra("imageUri")?.let {
            selectedImage.value = Uri.parse(it)
        }

        setContent {
            ImageLoadTestTheme {
                val selectedImageState by selectedImage.collectAsStateWithLifecycle()
                Greeting(
                    imageUri = selectedImageState,
                    onTextClick = {
                        val intent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        startActivityForResult(intent, 1000)
                    },
                    onImageClick = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("imageUri", selectedImageState.toString())
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        selectedImage.value = data?.data
    }

}

@Composable
fun Greeting(
    imageUri: Uri?,
    onTextClick: () -> Unit = {},
    onImageClick: () -> Unit = {},
) {
    Log.e("CCOOVV", "Uri: $imageUri")
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clickable { onTextClick() },
            onClick = { onTextClick() }
        ) {
            Text(
                text = "Get Image"
            )
        }

        val url = imageUri.toString()
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
        } else {
            Log.e("CCOOVV", "useMemoryCacheKey")
            ImageRequest.Builder(LocalContext.current)
                .data(memoryCache[MemoryCache.Key(memoryCacheKey)]?.image?.toBitmap())
                .memoryCachePolicy(CachePolicy.ENABLED)
                .memoryCacheKey(bitmapCacheKey)
        }.transformations(
            object : Transformation() {
                override val cacheKey: String
                    get() = transformCacheKey

                override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                    Log.e("CCOOVV", "transform")
                    return input
                }
            }
        ).build()

        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clickable { onImageClick() },
            contentDescription = null,
            model = requestBuilder,
            contentScale = ContentScale.Crop
        )

    }

}
