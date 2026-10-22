package dev.cobbleautosell

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import java.math.BigInteger
import java.util.UUID

class SellStats : SavedData() {

    class Row(val id: UUID, val name: String, val total: BigInteger)

    private val totals = HashMap<UUID, BigInteger>()
    private val names = HashMap<UUID, String>()

    companion object {
        private const val KEY = "cobbleautosell_stats"

        private val FACTORY = SavedData.Factory<SellStats>({ SellStats() }, { tag, _ -> load(tag) }, null)

        fun get(server: MinecraftServer): SellStats =
            server.overworld().dataStorage.computeIfAbsent(FACTORY, KEY)

        private fun load(tag: CompoundTag): SellStats {
            val stats = SellStats()
            val list = tag.getList("Players", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until list.size) {
                val entry = list.getCompound(i)
                val id = entry.getUUID("Id")
                stats.totals[id] = BigInteger(entry.getString("Total"))
                if (entry.contains("Name")) stats.names[id] = entry.getString("Name")
            }
            return stats
        }
    }

    fun add(id: UUID, name: String?, amount: BigInteger) {
        totals[id] = (totals[id] ?: BigInteger.ZERO).add(amount)
        if (name != null) names[id] = name
        setDirty()
    }

    fun top(limit: Int): List<Row> = totals.entries
        .sortedByDescending { it.value }
        .take(limit)
        .map { Row(it.key, names[it.key] ?: it.key.toString().take(8), it.value) }

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        val list = ListTag()
        for ((id, total) in totals) {
            val entry = CompoundTag()
            entry.putUUID("Id", id)
            entry.putString("Total", total.toString())
            names[id]?.let { entry.putString("Name", it) }
            list.add(entry)
        }
        tag.put("Players", list)
        return tag
    }
}