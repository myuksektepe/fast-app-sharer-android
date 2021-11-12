package fast.app.sharer.model

import android.graphics.drawable.Drawable
import java.io.File

class InstalledAppModel(
    val name: String?,
    val packageName: String,
    val versionName: String,
    val icon: Int,
    val iconDrawable: Drawable,
    val isSystemApp: Boolean,
    val sourceDir: String,
    val file: File,
    val size: Long,
)