package com.example.studcampapp.model

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime

private object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString())
    @RequiresApi(Build.VERSION_CODES.O)
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString())
}

@Serializable
data class ChatMessage(
    val id: Int,
    val user: User,
    val text: String,
    @Serializable(with = LocalDateTimeSerializer::class) val time: LocalDateTime,
    val status: MessageStatus = MessageStatus.Sent,
    val readBy: List<String> = emptyList(),
    val fileInfo: FileInfo? = null,
    val clientMsgId: String? = null,
    @kotlinx.serialization.Transient val attachment: MessageAttachment? = null,
    @kotlinx.serialization.Transient val isSystem: Boolean = false,
    @kotlinx.serialization.Transient val isPending: Boolean = false
) {
    val author: String get() = user.login
}
