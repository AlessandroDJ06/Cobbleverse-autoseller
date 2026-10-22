package dev.cobbleautosell

import fr.harmex.cobbledollars.common.utils.extensions.addOfflineCobbleDollars
import fr.harmex.cobbledollars.common.utils.extensions.cobbleDollars
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.math.BigInteger
import java.util.UUID

class AutoSellerBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.AUTO_SELLER_ENTITY, pos, state), WorldlyContainer, MenuProvider {

    companion object {
        private const val SLOTS = 5
        private const val CHECK_INTERVAL_TICKS = 20
        const val MIN_THRESHOLD = 1
        const val MAX_THRESHOLD = 1_000_000_000
        const val DEFAULT_THRESHOLD = 1
        private val ALL_SLOTS = IntArray(SLOTS) { it }
        private val INT_MAX = BigInteger.valueOf(Int.MAX_VALUE.toLong())

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, be: AutoSellerBlockEntity) {
            if (be.owner == null || be.isEmpty()) return
            if (++be.cooldown < CHECK_INTERVAL_TICKS) return
            be.cooldown = 0
            val server = level.server ?: return
            be.sellIfThresholdReached(server)
        }
    }

    private val items: NonNullList<ItemStack> = NonNullList.withSize(SLOTS, ItemStack.EMPTY)
    var owner: UUID? = null
    private var cooldown = 0

    var threshold = DEFAULT_THRESHOLD
        set(value) {
            field = value.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)
            setChanged()
        }

    /**
     * Synced to the client menu. Values are sent as 16-bit shorts, so each number uses two slots:
     * 0/1 = sell threshold (low/high), 2/3 = total value of the items inside (low/high).
     */
    val dataAccess = object : ContainerData {
        override fun get(index: Int): Int = when (index) {
            0 -> threshold and 0xFFFF
            1 -> threshold ushr 16
            2 -> displayValue() and 0xFFFF
            else -> displayValue() ushr 16
        }

        override fun set(index: Int, value: Int) {}
        override fun getCount(): Int = 4
    }

    /** Total sell value of everything currently inside the block. */
    fun totalValue(): BigInteger {
        var total = BigInteger.ZERO
        for (stack in items) {
            if (stack.isEmpty) continue
            val price = FallbackPrices.priceOf(stack) ?: continue
            total = total.add(price.multiply(BigInteger.valueOf(stack.count.toLong())))
        }
        return total
    }

    private fun displayValue(): Int = totalValue().min(INT_MAX).toInt()

    private fun sellIfThresholdReached(server: MinecraftServer) {
        val ownerId = owner ?: return
        val total = totalValue()
        if (total < BigInteger.valueOf(threshold.toLong())) return

        for (i in items.indices) {
            if (!items[i].isEmpty && FallbackPrices.priceOf(items[i]) != null) {
                items[i] = ItemStack.EMPTY
            }
        }
        pay(server, ownerId, total)
        setChanged()
    }

    private fun pay(server: MinecraftServer, uuid: UUID, amount: BigInteger) {
        val player = server.playerList.getPlayer(uuid)
        if (player != null) {
            player.cobbleDollars = player.cobbleDollars.add(amount)
        } else {
            uuid.addOfflineCobbleDollars(server, amount)
        }
        SellStats.get(server).add(uuid, nameOf(server, uuid), amount)
    }

    private fun nameOf(server: MinecraftServer, uuid: UUID): String? =
        server.playerList.getPlayer(uuid)?.gameProfile?.name
            ?: server.profileCache?.get(uuid)?.orElse(null)?.name

    override fun getDisplayName(): Component =
        Component.translatable("block.cobbleautosell.auto_seller")

    override fun createMenu(id: Int, inv: Inventory, player: Player): AbstractContainerMenu =
        AutoSellerMenu(id, inv, this, dataAccess)

    override fun getContainerSize(): Int = SLOTS
    override fun isEmpty(): Boolean = items.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = items[slot]

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val result = ContainerHelper.removeItem(items, slot, amount)
        if (!result.isEmpty) setChanged()
        return result
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(items, slot)

    override fun setItem(slot: Int, stack: ItemStack) {
        items[slot] = stack
        if (stack.count > maxStackSize) stack.count = maxStackSize
        setChanged()
    }

    override fun stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)
    override fun clearContent() = items.clear()

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean =
        owner != null && FallbackPrices.priceOf(stack) != null

    override fun getSlotsForFace(side: Direction): IntArray = ALL_SLOTS
    override fun canPlaceItemThroughFace(slot: Int, stack: ItemStack, direction: Direction?): Boolean =
        canPlaceItem(slot, stack)
    override fun canTakeItemThroughFace(slot: Int, stack: ItemStack, direction: Direction): Boolean = false

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        items.clear()
        ContainerHelper.loadAllItems(tag, items, registries)
        owner = if (tag.hasUUID("Owner")) tag.getUUID("Owner") else null
        threshold = if (tag.contains("Threshold")) tag.getInt("Threshold") else DEFAULT_THRESHOLD
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        ContainerHelper.saveAllItems(tag, items, registries)
        owner?.let { tag.putUUID("Owner", it) }
        tag.putInt("Threshold", threshold)
    }
}