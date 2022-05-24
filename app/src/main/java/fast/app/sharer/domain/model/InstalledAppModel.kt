package fast.app.sharer.domain.model

import android.graphics.drawable.Drawable
import java.io.File

class InstalledAppModel(
    val name: String?,
    val packageName: String?,
    val versionName: String?,
    val icon: Int?,
    val iconDrawable: Drawable?,
    val isSystemApp: Boolean?,
    val sourceDir: String?,
    val file: File?,
    val size: Long?,

    ) {
    override fun toString(): String {
        return "InstalledAppModel(name=$name, packageName=$packageName, versionName=$versionName, icon=$icon, iconDrawable=$iconDrawable, isSystemApp=$isSystemApp, sourceDir=$sourceDir, file=$file, size=$size)"
    }
}