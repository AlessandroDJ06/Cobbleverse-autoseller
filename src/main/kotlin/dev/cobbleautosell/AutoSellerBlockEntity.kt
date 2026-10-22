package dev.cobbleautosell

import fr.harmex.cobbledollars.common.CobbleDollars
import fr.harmex.cobbledollars.common.utils.extensions.addOfflineCobbleDollars
import fr.harmex.cobbledollars.common.utils.extensions.cobbleDollars
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.world.ContainerHelper
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.math.BigInteger
import java.util.UUID

class AutoSellerBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlocks.AUTO_SELLER_ENTITY, pos, state), WorldlyContainer {

    companion object {
        private const val SLOTS = 5
        private const val SELL_INTERVAL_TICKS = 8L
        private val ALL_SLOTS = IntArray(SLOTS) { it }

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, be: AutoSellerBlockEntity) {
            if (level.gameTime % SELL_INTERVAL_TICKS != 0L) return
            val server = level.server ?: return
            be.sellAll(server)
        }
    }

    private val items: NonNullList<ItemStack> = NonNullList.withSize(SLOTS, ItemStack.EMPTY)
    var owner: UUID? = null

    private fun sellAll(server: MinecraftServer) {
        val ownerId = owner ?: return
        val bank = CobbleDollars.bankConfig.bank
        var total = BigInteger.ZERO

        for (i in items.indices) {
            val stack = items[i]
            if (stack.isEmpty) continue
            val price = FallbackPrices.priceOf(stack) ?: continue
            total = total.add(price.multiply(BigInteger.valueOf(stack.count.toLong())))
            items[i] = ItemStack.EMPTY
        }

        if (total.signum() > 0) {
            pay(server, ownerId, total)
            setChanged()
        }
    }



    private fun pay(server: MinecraftServer, uuid: UUID, amount: BigInteger) {
        val player = server.playerList.getPlayer(uuid)
        if (player != null) {
            player.cobbleDollars = player.cobbleDollars.add(amount)
        } else {
            uuid.addOfflineCobbleDollars(server, amount)
        }
    }


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

    override fun stillValid(player: Player): Boolean = false
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
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        ContainerHelper.saveAllItems(tag, items, registries)
        owner?.let { tag.putUUID("Owner", it) }
    }
}
