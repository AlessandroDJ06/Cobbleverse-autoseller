package dev.cobbleautosell

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import java.math.BigInteger

object ClientLists {

    class Item(val stack: ItemStack, val name: String, val price: BigInteger) {
        val nameLower = name.lowercase()
    }

    class Score(val name: String, val total: BigInteger)

    var items: List<Item> = emptyList()
        private set
    var scores: List<Score> = emptyList()
        private set

    fun update(payload: ListsPayload) {
        items = payload.prices.mapNotNull(::toItem).sortedBy { it.nameLower }
        scores = payload.scores.map { Score(it.name, BigInteger(it.total)) }
    }

    private fun toItem(entry: PriceEntry): Item? {
        val item = BuiltInRegistries.ITEM.getOptional(entry.id).orElse(null) ?: return null
        val stack = ItemStack(item)
        return Item(stack, stack.hoverName.string, BigInteger(entry.price))
    }
}