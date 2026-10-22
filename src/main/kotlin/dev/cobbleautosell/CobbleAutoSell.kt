package dev.cobbleautosell

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

object CobbleAutoSell : ModInitializer {
    const val MOD_ID = "cobbleautosell"
    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

    override fun onInitialize() {
        ModBlocks.register()
        FallbackPrices.load()
    }
}