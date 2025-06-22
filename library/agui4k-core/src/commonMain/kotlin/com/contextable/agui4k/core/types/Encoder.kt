package com.contextable.agui4k.core.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object Encoder : KSerializer<Message> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Message")

    override fun serialize(encoder: Encoder, value: Message) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer can only be used with Json")

        // Use the encoder's Json instance to respect its configuration
        val json = jsonEncoder.json

        val jsonObject = when (value) {
            is DeveloperMessage -> json.encodeToJsonElement(DeveloperMessage.serializer(), value)
            is SystemMessage -> json.encodeToJsonElement(SystemMessage.serializer(), value)
            is AssistantMessage -> json.encodeToJsonElement(AssistantMessage.serializer(), value)
            is UserMessage -> json.encodeToJsonElement(UserMessage.serializer(), value)
            is ToolMessage -> json.encodeToJsonElement(ToolMessage.serializer(), value)
        }.jsonObject

        jsonEncoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): Message {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer can only be used with Json")

        val json = jsonDecoder.json
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val role = jsonObject["role"]?.jsonPrimitive?.content
            ?: throw SerializationException("Missing 'role' field")

        return when (role) {
            "developer" -> json.decodeFromJsonElement(DeveloperMessage.serializer(), jsonObject)
            "system" -> json.decodeFromJsonElement(SystemMessage.serializer(), jsonObject)
            "assistant" -> json.decodeFromJsonElement(AssistantMessage.serializer(), jsonObject)
            "user" -> json.decodeFromJsonElement(UserMessage.serializer(), jsonObject)
            "tool" -> json.decodeFromJsonElement(ToolMessage.serializer(), jsonObject)
            else -> throw SerializationException("Unknown role: $role")
        }
    }
}