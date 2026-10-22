package dev.cobbleautosell

import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.gui.screens.MenuScreens
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

object CobbleAutoSellClient : ClientModInitializer {


    override fun onInitializeClient() {
        MenuScreens.register(ModMenus.AUTO_SELLER) { menu, inv, title ->
            AutoSellerScreen(menu, inv, title)
        }

        ClientPlayNetworking.registerGlobalReceiver(ListsPayload.TYPE) { payload, context ->
            context.client().execute { ClientLists.update(payload) }
        }
    }
}