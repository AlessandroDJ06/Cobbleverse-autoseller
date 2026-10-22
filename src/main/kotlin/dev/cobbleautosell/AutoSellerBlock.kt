package dev.cobbleautosell

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult

class AutoSellerBlock(properties: Properties) : BaseEntityBlock(properties) {

    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
        val CODEC: MapCodec<AutoSellerBlock> = BlockBehaviour.simpleCodec { AutoSellerBlock(it) }
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AutoSellerBlockEntity(pos, state)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun setPlacedBy(
        level: Level, pos: BlockPos, state: BlockState,
        placer: LivingEntity?, stack: ItemStack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack)
        if (!level.isClientSide && placer is ServerPlayer) {
            (level.getBlockEntity(pos) as? AutoSellerBlockEntity)?.let {
                it.owner = placer.uuid
                it.setChanged()
            }
        }
    }

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        (level.getBlockEntity(pos) as? MenuProvider)?.let { player.openMenu(it) }
        return InteractionResult.CONSUME
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(
        level: Level, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide || type != ModBlocks.AUTO_SELLER_ENTITY) return null
        return BlockEntityTicker { lvl, pos, st, be ->
            AutoSellerBlockEntity.serverTick(lvl, pos, st, be as AutoSellerBlockEntity)
        }
    }
}