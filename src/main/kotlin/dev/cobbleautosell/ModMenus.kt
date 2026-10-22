package dev.cobbleautosell

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType

object ModMenus {
    lateinit var AUTO_SELLER: MenuType<AutoSellerMenu>
        private set

    fun register() {
        AUTO_SELLER = Registry.register(
            BuiltInRegistries.MENU, CobbleAutoSell.id("auto_seller"),
            MenuType({ id, inv -> AutoSellerMenu(id, inv) }, FeatureFlags.VANILLA_SET)
        )
    }
}