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
        ServerPlayNetworking.registerGlobalReceiver(SetThresholdPayload.TYPE) { payload, context ->
            val menu = context.player().containerMenu as? AutoSellerMenu ?: return@registerGlobalReceiver
            menu.setThreshold(context.player(), payload.value)
        }
    }
}