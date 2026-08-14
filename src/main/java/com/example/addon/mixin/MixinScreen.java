package com.example.addon.mixin;

import com.example.addon.modules.Personalized;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL13;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen {
    private static final Identifier BACKGROUND = Identifier.of("lblt", "gui/background.png");

    @Inject(
        method = {"renderPanoramaBackground"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void lblt$onRenderPanoramaBackground(DrawContext context, float delta, CallbackInfo ci) {
        Personalized personalized = Personalized.INSTANCE;
        if (personalized == null || !personalized.isActive() || !personalized.meteorGuiBackground.get()) return;
        if (MeteorClient.mc.world != null) return;

        int screenWidth = MeteorClient.mc.getWindow().getScaledWidth();
        int screenHeight = MeteorClient.mc.getWindow().getScaledHeight();
        float scaleFactor = 1.2F;
        int scaledWidth = (int) (screenWidth * scaleFactor);
        int scaledHeight = (int) (screenHeight * scaleFactor);
        int offsetX = (int) -((scaledWidth - screenWidth) / 2.0F);
        int offsetY = (int) -((scaledHeight - screenHeight) / 2.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        GL13.glEnable(32925);
        RenderSystem.setShaderTexture(0, BACKGROUND);
        context.drawTexture(BACKGROUND, offsetX, offsetY, scaledWidth, scaledHeight, 0.0F, 0.0F, 3840, 2160, 3840, 2160);
        GL13.glDisable(32925);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        ci.cancel();
    }
}
