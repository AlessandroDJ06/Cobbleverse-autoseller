package dev.cobbleautosell

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class SetThresholdPayload(val value: Int) : CustomPacketPayload {

    companion object {
        val TYPE = CustomPacketPayload.Type<SetThresholdPayload>(CobbleAutoSell.id("set_threshold"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SetThresholdPayload> = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SetThresholdPayload::value,
            { v: Int -> SetThresholdPayload(v) }
        )
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}