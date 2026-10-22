package dev.cobbleautosell

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer

data class PriceEntry(val id: ResourceLocation, val price: String)
data class ScoreEntry(val name: String, val total: String)

object RequestListsPayload : CustomPacketPayload {
    val TYPE = CustomPacketPayload.Type<RequestListsPayload>(CobbleAutoSell.id("request_lists"))
    val CODEC: StreamCodec<RegistryFriendlyByteBuf, RequestListsPayload> = StreamCodec.unit(RequestListsPayload)

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}

data class ListsPayload(val prices: List<PriceEntry>, val scores: List<ScoreEntry>) : CustomPacketPayload {

    companion object {
        private const val LEADERBOARD_SIZE = 8

        val TYPE = CustomPacketPayload.Type<ListsPayload>(CobbleAutoSell.id("lists"))

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ListsPayload> = StreamCodec.of(
            { buf: RegistryFriendlyByteBuf, payload: ListsPayload -> payload.write(buf) },
            { buf: RegistryFriendlyByteBuf -> read(buf) }
        )

        fun create(server: MinecraftServer) = ListsPayload(
            FallbackPrices.sellable().map { (id, price) -> PriceEntry(id, price.toString()) },
            SellStats.get(server).top(LEADERBOARD_SIZE).map { ScoreEntry(it.name, it.total.toString()) }
        )

        private fun read(buf: FriendlyByteBuf) = ListsPayload(
            buf.readList { PriceEntry(it.readResourceLocation(), it.readUtf()) },
            buf.readList { ScoreEntry(it.readUtf(), it.readUtf()) }
        )
    }

    private fun write(buf: FriendlyByteBuf) {
        buf.writeCollection(prices) { b, entry ->
            b.writeResourceLocation(entry.id)
            b.writeUtf(entry.price)
        }
        buf.writeCollection(scores) { b, entry ->
            b.writeUtf(entry.name)
            b.writeUtf(entry.total)
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE
}