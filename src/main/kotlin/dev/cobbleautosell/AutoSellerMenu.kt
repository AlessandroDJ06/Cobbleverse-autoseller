package dev.cobbleautosell

import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class AutoSellerMenu(
    id: Int,
    playerInv: Inventory,
    private val container: Container = SimpleContainer(5),
    private val data: ContainerData = SimpleContainerData(2)
) : AbstractContainerMenu(ModMenus.AUTO_SELLER, id) {

    init {
        checkContainerSize(container, 5)
        checkContainerDataCount(data, 2)
        container.startOpen(playerInv.player)

        for (i in 0 until 5) {
            addSlot(object : Slot(container, i, 44 + i * 18, 18) {
                override fun mayPlace(stack: ItemStack) = container.canPlaceItem(i, stack)
            })
        }
        for (row in 0 until 3) for (col in 0 until 9)
            addSlot(Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 85 + row * 18))
        for (col in 0 until 9)
            addSlot(Slot(playerInv, col, 8 + col * 18, 143))

        addDataSlots(data)
    }

    /** Huidige drempel, werkt op zowel client als server. */
    fun getThreshold(): Int = (data.get(1) shl 16) or (data.get(0) and 0xFFFF)

    /** Wordt op de server aangeroepen vanuit het netwerkpakket. */
    fun setThreshold(player: Player, value: Int) {
        val be = container as? AutoSellerBlockEntity ?: return
        val owner = be.owner
        if (owner != null && owner != player.uuid) return // alleen de eigenaar
        be.threshold = value
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasItem()) {
            val stack = slot.item
            result = stack.copy()
            if (index < 5) {
                if (!moveItemStackTo(stack, 5, slots.size, true)) return ItemStack.EMPTY
            } else if (!moveItemStackTo(stack, 0, 5, false)) {
                return ItemStack.EMPTY
            }
            if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        }
        return result
    }

    override fun stillValid(player: Player): Boolean = container.stillValid(player)

    override fun removed(player: Player) {
        super.removed(player)
        container.stopOpen(player)
    }
}