package dev.cobbleautosell

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import java.text.NumberFormat
import kotlin.math.sign

class AutoSellerScreen(menu: AutoSellerMenu, inv: Inventory, title: Component) :
    AbstractContainerScreen<AutoSellerMenu>(menu, inv, title) {

    private enum class Tab { SELL, ITEMS, TOP }

    companion object {
        private const val PANEL_W = 150
        private const val PANEL_GAP = 4
        private const val PAD = 8
        private const val INNER_W = PANEL_W - 2 * PAD

        private const val TAB_W = 46
        private const val TAB_H = 18

        private const val LABEL_Y = 28
        private const val DISPLAY_Y = 38
        private const val DISPLAY_H = 18
        private const val SELL_LABEL_Y = 62
        private const val INPUT_Y = 72
        private const val BAR_Y = 96
        private const val BAR_H = 10
        private const val CURRENT_Y = 110

        private const val SEARCH_Y = 28
        private const val LIST_Y = 44
        private const val ROW_H = 17
        private const val ROWS = 5
        private const val NAME_X = 26
        private const val PRICE_RIGHT = PANEL_W - 14
        private const val SCROLLBAR_X = PANEL_W - 11
        private const val SCROLLBAR_W = 4

        private const val SCORE_Y = 40
        private const val SCORE_ROW_H = 11
        private const val SCORE_ROWS = 8

        private val GUI_GRAY = 0xFFC6C6C6.toInt()
        private val PANEL_BORDER = 0xFF000000.toInt()
        private val PANEL_LIGHT = 0xFFFFFFFF.toInt()
        private val PANEL_DARK = 0xFF555555.toInt()
        private val DISPLAY_FILL = 0xFF373737.toInt()
        private val BAR_FILL = 0xFFFFD84A.toInt()

        private const val TEXT_DARK = 0x404040
        private const val TEXT_LIGHT = 0xFFFFFF
        private const val TEXT_GOLD = 0xFFD84A
    }

    private val texture = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png")
    private val coinIcon = ItemStack(Items.GOLD_INGOT)

    private lateinit var amountBox: EditBox
    private lateinit var searchBox: EditBox
    private lateinit var setButton: Button
    private val tabButtons = LinkedHashMap<Tab, Button>()

    private var tab = Tab.SELL
    private var boxInitialized = false
    private var scroll = 0

    private var cacheQuery = ""
    private var cacheSource: List<ClientLists.Item>? = null
    private var cacheResult: List<ClientLists.Item> = emptyList()

    private val panelX: Int get() = leftPos + imageWidth + PANEL_GAP
    private val panelRelX: Int get() = imageWidth + PANEL_GAP

    init {
        imageHeight = 132
        inventoryLabelY = imageHeight - 94
    }

    override fun init() {
        super.init()
        createTabButtons()
        createSellWidgets()
        createSearchBox()
        boxInitialized = false
        updateTabState()
    }

    private fun createTabButtons() {
        tabButtons.clear()
        Tab.entries.forEachIndexed { index, target ->
            val button = Button.builder(Component.translatable("gui.cobbleautosell.tab.${target.name.lowercase()}")) {
                switchTab(target)
            }.bounds(panelX + 4 + index * (TAB_W + 2), topPos + 4, TAB_W, TAB_H).build()
            tabButtons[target] = button
            addRenderableWidget(button)
        }
    }

    private fun createSellWidgets() {
        amountBox = EditBox(font, panelX + PAD, topPos + INPUT_Y, 90, 18, Component.translatable("gui.cobbleautosell.sell_at"))
        amountBox.setMaxLength(10)
        amountBox.setFilter { text -> text.all { it.isDigit() } }
        addRenderableWidget(amountBox)

        setButton = Button.builder(Component.translatable("gui.cobbleautosell.set")) { apply() }
            .bounds(panelX + PAD + 94, topPos + INPUT_Y - 1, 40, 20).build()
        addRenderableWidget(setButton)
    }

    private fun createSearchBox() {
        searchBox = EditBox(font, panelX + PAD, topPos + SEARCH_Y, INNER_W, 14, Component.translatable("gui.cobbleautosell.search"))
        searchBox.setMaxLength(30)
        searchBox.setHint(Component.translatable("gui.cobbleautosell.search"))
        searchBox.setResponder { scroll = 0 }
        addRenderableWidget(searchBox)
    }

    private fun switchTab(target: Tab) {
        tab = target
        updateTabState()
        if (target != Tab.SELL) ClientPlayNetworking.send(RequestListsPayload)
    }

    private fun updateTabState() {
        tabButtons.forEach { (target, button) -> button.active = target != tab }
        amountBox.visible = tab == Tab.SELL
        setButton.visible = tab == Tab.SELL
        searchBox.visible = tab == Tab.ITEMS
        amountBox.isFocused = amountBox.isFocused && amountBox.visible
        searchBox.isFocused = searchBox.isFocused && searchBox.visible
    }

    override fun containerTick() {
        super.containerTick()
        if (boxInitialized) return
        val threshold = menu.getThreshold()
        if (threshold <= 0) return
        amountBox.value = threshold.toString()
        boxInitialized = true
    }

    private fun apply() {
        val value = amountBox.value.toLongOrNull()
            ?.coerceIn(AutoSellerBlockEntity.MIN_THRESHOLD.toLong(), AutoSellerBlockEntity.MAX_THRESHOLD.toLong())
            ?.toInt() ?: return
        amountBox.value = value.toString()
        ClientPlayNetworking.send(SetThresholdPayload(value))
    }

    private fun activeBox(): EditBox? =
        listOf(amountBox, searchBox).firstOrNull { it.visible && it.isFocused }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val box = activeBox() ?: return super.keyPressed(keyCode, scanCode, modifiers)
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(keyCode, scanCode, modifiers)
        if (box === amountBox && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            apply()
            return true
        }
        box.keyPressed(keyCode, scanCode, modifiers)
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (tab != Tab.ITEMS || !inListArea(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        }
        scroll = (scroll - sign(scrollY).toInt()).coerceIn(0, maxScroll(filtered()))
        return true
    }

    private fun inListArea(mouseX: Double, mouseY: Double): Boolean =
        mouseX >= panelX + 4 && mouseX < panelX + PANEL_W - 4 &&
                mouseY >= topPos + LIST_Y && mouseY < topPos + LIST_Y + ROWS * ROW_H

    override fun hasClickedOutside(
        mouseX: Double, mouseY: Double, guiLeft: Int, guiTop: Int, mouseButton: Int
    ): Boolean {
        val inPanel = mouseX >= panelX && mouseX < panelX + PANEL_W &&
                mouseY >= topPos && mouseY < topPos + imageHeight
        return !inPanel && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)
    }

    private fun filtered(): List<ClientLists.Item> {
        val source = ClientLists.items
        val query = searchBox.value.trim().lowercase()
        if (source === cacheSource && query == cacheQuery) return cacheResult
        cacheSource = source
        cacheQuery = query
        cacheResult = if (query.isEmpty()) source else source.filter { it.nameLower.contains(query) }
        return cacheResult
    }

    private fun maxScroll(entries: List<ClientLists.Item>): Int = (entries.size - ROWS).coerceAtLeast(0)

    // ---------- background ----------

    override fun renderBg(g: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        drawMainBackground(g)
        drawRaised(g, panelX, topPos, PANEL_W, imageHeight)
        when (tab) {
            Tab.SELL -> drawSellBackground(g)
            Tab.ITEMS -> drawListBackground(g, LIST_Y - 1, ROWS * ROW_H + 2)
            Tab.TOP -> drawListBackground(g, SCORE_Y - 2, SCORE_ROWS * SCORE_ROW_H + 4)
        }
    }

    private fun drawMainBackground(g: GuiGraphics) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, 17)
        g.blit(texture, leftPos, topPos + 17, 0, 17, 7, 18)
        g.blit(texture, leftPos + 169, topPos + 17, 169, 17, 7, 18)
        g.fill(leftPos + 7, topPos + 17, leftPos + 169, topPos + 35, GUI_GRAY)
        for (i in 0 until 5) {
            g.blit(texture, leftPos + 43 + i * 18, topPos + 17, 7, 17, 18, 18)
        }
        g.blit(texture, leftPos, topPos + 35, 0, 126, imageWidth, 96)
    }

    private fun drawSellBackground(g: GuiGraphics) {
        drawInset(g, panelX + PAD, topPos + DISPLAY_Y, INNER_W, DISPLAY_H)
        drawInset(g, panelX + PAD, topPos + BAR_Y, INNER_W, BAR_H)
        drawProgress(g, panelX + PAD, topPos + BAR_Y)
    }

    private fun drawListBackground(g: GuiGraphics, relY: Int, height: Int) {
        drawInset(g, panelX + 4, topPos + relY, PANEL_W - 8, height)
    }

    private fun drawProgress(g: GuiGraphics, x: Int, y: Int) {
        val threshold = menu.getThreshold()
        if (threshold <= 0) return
        val fraction = (menu.getTotalValue().toDouble() / threshold).coerceIn(0.0, 1.0)
        val fillWidth = ((INNER_W - 2) * fraction).toInt()
        g.fill(x + 1, y + 1, x + 1 + fillWidth, y + BAR_H - 1, BAR_FILL)
    }

    private fun drawRaised(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int) {
        g.fill(x, y, x + w, y + h, PANEL_BORDER)
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, GUI_GRAY)
        g.fill(x + 1, y + 1, x + w - 1, y + 2, PANEL_LIGHT)
        g.fill(x + 1, y + 1, x + 2, y + h - 1, PANEL_LIGHT)
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, PANEL_DARK)
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, PANEL_DARK)
    }

    private fun drawInset(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int) {
        g.fill(x, y, x + w, y + h, PANEL_LIGHT)
        g.fill(x, y, x + w - 1, y + h - 1, PANEL_DARK)
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, DISPLAY_FILL)
    }

    // ---------- labels and content ----------

    override fun renderLabels(g: GuiGraphics, mouseX: Int, mouseY: Int) {
        super.renderLabels(g, mouseX, mouseY)
        when (tab) {
            Tab.SELL -> drawSellLabels(g)
            Tab.ITEMS -> drawItemRows(g)
            Tab.TOP -> drawScoreRows(g)
        }
    }

    private fun drawSellLabels(g: GuiGraphics) {
        val fmt = NumberFormat.getIntegerInstance()
        val px = panelRelX
        val value = fmt.format(menu.getTotalValue())

        g.drawString(font, Component.translatable("gui.cobbleautosell.value_inside"), px + PAD, LABEL_Y, TEXT_DARK, false)
        g.renderItem(coinIcon, px + PAD + 1, DISPLAY_Y + 1)
        g.drawString(font, value, px + PANEL_W - PAD - 4 - font.width(value), DISPLAY_Y + 5, TEXT_LIGHT, true)
        g.drawString(font, Component.translatable("gui.cobbleautosell.sell_at"), px + PAD, SELL_LABEL_Y, TEXT_DARK, false)
        g.drawString(
            font,
            Component.translatable("gui.cobbleautosell.currently", fmt.format(menu.getThreshold())),
            px + PAD, CURRENT_Y, TEXT_DARK, false
        )
    }

    private fun drawItemRows(g: GuiGraphics) {
        val px = panelRelX
        val entries = filtered()
        if (entries.isEmpty()) {
            val key = if (ClientLists.items.isEmpty()) "gui.cobbleautosell.loading" else "gui.cobbleautosell.no_results"
            g.drawString(font, Component.translatable(key), px + PAD, LIST_Y + 4, TEXT_LIGHT, true)
            return
        }

        scroll = scroll.coerceIn(0, maxScroll(entries))
        val fmt = NumberFormat.getIntegerInstance()
        for (row in 0 until ROWS) {
            val entry = entries.getOrNull(scroll + row) ?: break
            val y = LIST_Y + row * ROW_H
            val price = fmt.format(entry.price)
            val priceX = px + PRICE_RIGHT - font.width(price)
            val nameWidth = (priceX - (px + NAME_X) - 4).coerceAtLeast(0)

            g.renderItem(entry.stack, px + 6, y)
            g.drawString(font, font.plainSubstrByWidth(entry.name, nameWidth), px + NAME_X, y + 4, TEXT_LIGHT, true)
            g.drawString(font, price, priceX, y + 4, TEXT_GOLD, true)
        }
        drawScrollbar(g, px, entries.size)
    }

    private fun drawScrollbar(g: GuiGraphics, px: Int, total: Int) {
        if (total <= ROWS) return
        val trackH = ROWS * ROW_H
        val thumbH = (trackH * ROWS / total).coerceAtLeast(8)
        val thumbY = LIST_Y + (trackH - thumbH) * scroll / (total - ROWS)
        val x = px + SCROLLBAR_X
        g.fill(x, LIST_Y, x + SCROLLBAR_W, LIST_Y + trackH, PANEL_DARK)
        g.fill(x, thumbY, x + SCROLLBAR_W, thumbY + thumbH, GUI_GRAY)
    }

    private fun drawScoreRows(g: GuiGraphics) {
        val px = panelRelX
        g.drawString(font, Component.translatable("gui.cobbleautosell.top_title"), px + PAD, LABEL_Y, TEXT_DARK, false)

        val scores = ClientLists.scores.take(SCORE_ROWS)
        if (scores.isEmpty()) {
            g.drawString(font, Component.translatable("gui.cobbleautosell.no_sales"), px + PAD, SCORE_Y, TEXT_LIGHT, true)
            return
        }

        val me = minecraft?.player?.gameProfile?.name
        val fmt = NumberFormat.getIntegerInstance()
        scores.forEachIndexed { index, score ->
            val y = SCORE_Y + index * SCORE_ROW_H
            val isMe = score.name == me
            val total = fmt.format(score.total)
            val totalX = px + PANEL_W - PAD - 2 - font.width(total)
            val label = (if (isMe) "> " else "") + "${index + 1}. ${score.name}"
            val labelWidth = (totalX - (px + PAD + 2) - 4).coerceAtLeast(0)

            g.drawString(font, font.plainSubstrByWidth(label, labelWidth), px + PAD + 2, y, if (isMe) TEXT_GOLD else TEXT_LIGHT, true)
            g.drawString(font, total, totalX, y, TEXT_GOLD, true)
        }
    }

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(g, mouseX, mouseY, partialTick)
        renderTooltip(g, mouseX, mouseY)
    }
}