package dev.cobbleautosell

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

object CobbleAutoSell : ModInitializer {
    const val MOD_ID = "cobbleautosell"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

    override fun onInitialize() {
        ModBlocks.register()
        ModMenus.register()
        FallbackPrices.load()

        PayloadTypeRegistry.playC2S().register(SetThresholdPayload.TYPE, SetThresholdPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(RequestListsPayload.TYPE, RequestListsPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(ListsPayload.TYPE, ListsPayload.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(SetThresholdPayload.TYPE) { payload, context ->
            val menu = context.player().containerMenu as? AutoSellerMenu ?: return@registerGlobalReceiver
            menu.setThreshold(context.player(), payload.value)
        }

        ServerPlayNetworking.registerGlobalReceiver(RequestListsPayload.TYPE) { _, context ->
            if (context.player().containerMenu !is AutoSellerMenu) return@registerGlobalReceiver
            ServerPlayNetworking.send(context.player(), ListsPayload.create(context.server()))
        }
    }
}