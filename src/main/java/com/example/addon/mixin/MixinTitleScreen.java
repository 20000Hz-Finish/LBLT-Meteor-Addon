package com.example.addon.mixin;

import com.example.addon.gui.LBLTMainMenuScreen;
import com.example.addon.modules.Personalized;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {
    @Inject(
        method = {"init"},
        at = {@At("RETURN")}
    )
    private void lblt$onInit(CallbackInfo ci) {
        Personalized personalized = Personalized.INSTANCE;
        if (personalized == null) return;
        // 仅在首次启动时默认激活模块，之后尊重用户设置
        if (personalized.firstLaunch.get()) {
            personalized.firstLaunch.set(false);
            personalized.ensureActive();
        }
        if (!personalized.isActive() || !personalized.titleInterface.get()) return;
        if (MeteorClient.mc.world != null) return;
        MeteorClient.mc.setScreen(LBLTMainMenuScreen.getInstance());
    }
}
