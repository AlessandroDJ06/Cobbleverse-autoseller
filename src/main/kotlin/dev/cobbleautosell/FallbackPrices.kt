package dev.cobbleautosell

import com.google.gson.JsonParser
import fr.harmex.cobbledollars.common.CobbleDollars
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import java.math.BigInteger
import java.nio.file.Files

object FallbackPrices {
    private val prices = HashMap<ResourceLocation, BigInteger>()

    fun load() {
        try {
            val file = FabricLoader.getInstance().configDir.resolve("cobbleautosell/fallback_bank.json")
            if (!Files.exists(file)) {
                Files.createDirectories(file.parent)
                FallbackPrices::class.java.getResourceAsStream("/fallback_bank.json")!!
                    .use { Files.copy(it, file) }
            }
            prices.clear()
            val root = JsonParser.parseString(Files.readString(file)).asJsonObject
            for (entry in root.getAsJsonArray("bank")) {
                val o = entry.asJsonObject
                val id = ResourceLocation.tryParse(o["item"].asString) ?: continue
                prices[id] = o["price"].asBigInteger
            }
        } catch (e: Exception) {
            CobbleAutoSell.LOGGER.error("Failed to load fallback_bank.json", e)
        }
    }

    fun priceOf(stack: ItemStack): BigInteger? =
        CobbleDollars.bankConfig.bank.get(stack)?.price
            ?: prices[BuiltInRegistries.ITEM.getKey(stack.item)]
}