package dev.cobbleautosell

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour

object ModBlocks {
    lateinit var AUTO_SELLER: AutoSellerBlock
        private set
    lateinit var AUTO_SELLER_ENTITY: BlockEntityType<AutoSellerBlockEntity>
        private set
    lateinit var AUTO_SELLER_ITEM: BlockItem
        private set

    fun register() {
        val id = CobbleAutoSell.id("auto_seller")

        AUTO_SELLER = Registry.register(
            BuiltInRegistries.BLOCK, id,
            AutoSellerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER))
        )

        AUTO_SELLER_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
            BlockEntityType.Builder.of({ pos, state -> AutoSellerBlockEntity(pos, state) }, AUTO_SELLER).build(null)
        )

        AUTO_SELLER_ITEM = Registry.register(
            BuiltInRegistries.ITEM, id,
            BlockItem(AUTO_SELLER, Item.Properties())
        )

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register {
            it.accept(AUTO_SELLER_ITEM)
        }
    }
}
