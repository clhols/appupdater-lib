package dk.youtec.appupdater

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Output(
        val version: Int,
        val applicationId: String,
        val variantName: String,
        val elements: List<Element>
) : Parcelable

@Parcelize
@Serializable
data class Element(
        val type: String,
        val versionCode: Long,
        val versionName: String,
        val outputFile: String) : Parcelable