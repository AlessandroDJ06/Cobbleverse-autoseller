package dev.cobbleautosell

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class AutoSellerScreen(menu: AutoSellerMenu, inv: Inventory, title: Component) :
    AbstractContainerScreen<AutoSellerMenu>(menu, inv, title) {

    private val texture = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png")
    private lateinit var amountBox: EditBox

    init {
        imageHeight = 168
        inventoryLabelY = imageHeight - 94
    }

    override fun init() {
        super.init()

        amountBox = EditBox(font, leftPos + 30, topPos + 48, 80, 18, Component.literal("Bedrag"))
        amountBox.setMaxLength(10)
        amountBox.setFilter { text -> text.all { it.isDigit() } }
        amountBox.value = menu.getThreshold().toString()
        addRenderableWidget(amountBox)

        addRenderableWidget(Button.builder(Component.literal("OK")) { apply() }
            .bounds(leftPos + 114, topPos + 47, 34, 20).build())
    }

    private fun apply() {
        val value = amountBox.value.toLongOrNull()
            ?.coerceIn(AutoSellerBlockEntity.MIN_THRESHOLD.toLong(), AutoSellerBlockEntity.MAX_THRESHOLD.toLong())
            ?.toInt() ?: return
        amountBox.value = value.toString()
        ClientPlayNetworking.send(SetThresholdPayload(value))
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (amountBox.isFocused) {
            if (keyCode == 257 || keyCode == 335) {
                apply()
                return true
            }
            if (keyCode != 256) {
                amountBox.keyPressed(keyCode, scanCode, modifiers)
                return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun renderBg(g: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, 71)
        g.blit(texture, leftPos, topPos + 71, 0, 126, imageWidth, 96)
    }

    override fun renderLabels(g: GuiGraphics, mouseX: Int, mouseY: Int) {
        super.renderLabels(g, mouseX, mouseY)
        g.drawString(font, Component.literal("Sell When Value is: ${menu.getThreshold()}"), 30, 38, 0x404040, false)
    }

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(g, mouseX, mouseY, partialTick)
        renderTooltip(g, mouseX, mouseY)
    }
}