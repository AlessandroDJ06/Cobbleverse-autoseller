package dev.cobbleautosell

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.gui.screens.MenuScreens

object CobbleAutoSellClient : ClientModInitializer {
    override fun onInitializeClient() {
        MenuScreens.register(ModMenus.AUTO_SELLER) { menu, inv, title ->
            AutoSellerScreen(menu, inv, title)
        }
    }
}