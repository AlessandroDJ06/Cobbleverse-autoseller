package dev.cobbleautosell

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import java.text.NumberFormat

class AutoSellerScreen(menu: AutoSellerMenu, inv: Inventory, title: Component) :
    AbstractContainerScreen<AutoSellerMenu>(menu, inv, title) {

    companion object {
        private const val PANEL_W = 118
        private const val PANEL_H = 76
        private const val PANEL_GAP = 4

        private val GUI_GRAY = 0xFFC6C6C6.toInt()
        private val PANEL_BORDER = 0xFF000000.toInt()
        private val PANEL_LIGHT = 0xFFFFFFFF.toInt()
        private val PANEL_DARK = 0xFF555555.toInt()
    }

    private val texture = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png")
    private lateinit var amountBox: EditBox
    private var boxInitialized = false

    private val panelX: Int get() = leftPos + imageWidth + PANEL_GAP

    init {
        imageHeight = 132
        inventoryLabelY = imageHeight - 94
    }

    override fun init() {
        super.init()

        amountBox = EditBox(font, panelX + 8, topPos + 42, 68, 18, Component.translatable("gui.cobbleautosell.sell_at"))
        amountBox.setMaxLength(10)
        amountBox.setFilter { text -> text.all { it.isDigit() } }
        addRenderableWidget(amountBox)
        boxInitialized = false

        addRenderableWidget(
            Button.builder(Component.translatable("gui.cobbleautosell.set")) { apply() }
                .bounds(panelX + 80, topPos + 41, 30, 20).build()
        )
    }

    override fun containerTick() {
        super.containerTick()
        if (!boxInitialized && menu.getThreshold() > 0) {
            amountBox.value = menu.getThreshold().toString()
            boxInitialized = true
        }
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
            if (keyCode == 257 || keyCode == 335) { // Enter / numpad Enter
                apply()
                return true
            }
            if (keyCode != 256) { // everything except Escape goes to the text field
                amountBox.keyPressed(keyCode, scanCode, modifiers)
                return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun hasClickedOutside(
        mouseX: Double, mouseY: Double, guiLeft: Int, guiTop: Int, mouseButton: Int
    ): Boolean {
        val inPanel = mouseX >= panelX && mouseX < panelX + PANEL_W &&
                mouseY >= topPos && mouseY < topPos + PANEL_H
        return !inPanel && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)
    }

    override fun renderBg(g: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // Main menu: title strip, a single row with only 5 slots, then the player inventory
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, 17)
        g.blit(texture, leftPos, topPos + 17, 0, 17, 7, 18)
        g.blit(texture, leftPos + 169, topPos + 17, 169, 17, 7, 18)
        g.fill(leftPos + 7, topPos + 17, leftPos + 169, topPos + 35, GUI_GRAY)
        for (i in 0 until 5) {
            g.blit(texture, leftPos + 43 + i * 18, topPos + 17, 7, 17, 18, 18)
        }
        g.blit(texture, leftPos, topPos + 35, 0, 126, imageWidth, 96)

        // Side panel
        val x = panelX
        val y = topPos
        g.fill(x, y, x + PANEL_W, y + PANEL_H, PANEL_BORDER)
        g.fill(x + 1, y + 1, x + PANEL_W - 1, y + PANEL_H - 1, GUI_GRAY)
        g.fill(x + 1, y + 1, x + PANEL_W - 1, y + 2, PANEL_LIGHT)
        g.fill(x + 1, y + 1, x + 2, y + PANEL_H - 1, PANEL_LIGHT)
        g.fill(x + 1, y + PANEL_H - 2, x + PANEL_W - 1, y + PANEL_H - 1, PANEL_DARK)
        g.fill(x + PANEL_W - 2, y + 1, x + PANEL_W - 1, y + PANEL_H - 1, PANEL_DARK)
    }

    override fun renderLabels(g: GuiGraphics, mouseX: Int, mouseY: Int) {
        super.renderLabels(g, mouseX, mouseY)

        val fmt = NumberFormat.getIntegerInstance()
        val px = imageWidth + PANEL_GAP + 8

        g.drawString(font, Component.translatable("gui.cobbleautosell.value_inside"), px, 7, 0x404040, false)
        g.drawString(font, Component.literal(fmt.format(menu.getTotalValue())), px, 18, 0x1B7A1B, false)
        g.drawString(font, Component.translatable("gui.cobbleautosell.sell_at"), px, 32, 0x404040, false)
        g.drawString(
            font,
            Component.translatable("gui.cobbleautosell.currently", fmt.format(menu.getThreshold())),
            px, 64, 0x707070, false
        )
    }

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(g, mouseX, mouseY, partialTick)
        renderTooltip(g, mouseX, mouseY)
    }
}