package com.example.addon.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL13;

import java.util.ArrayList;
import java.util.List;

public class ConfirmExitScreen extends Screen {
    private static ConfirmExitScreen INSTANCE;
    private static final Identifier BACKGROUND = Identifier.of("lblt", "gui/background.png");

    private final List<LBLTMainMenuScreen.MainMenuButton> buttons = new ArrayList<>();
    private final List<MainMenuText> texts = new ArrayList<>();

    private ConfirmExitScreen() {
        super(Text.of("LBLT 退出确认"));
        texts.add(new MainMenuText(-100.0F, 10.0F, "你真的要退出游戏吗？"));
        buttons.add(new LBLTMainMenuScreen.MainMenuButton(-110.0F, 50.0F, 220F, 38F,
            "确认退出", () -> {
                MeteorClient.mc.setScreen(null);
                MeteorClient.mc.scheduleStop();
            }, true));
        buttons.add(new LBLTMainMenuScreen.MainMenuButton(-110.0F, 100.0F, 220F, 38F,
            "取消", () -> MeteorClient.mc.setScreen(LBLTMainMenuScreen.getInstance())));
    }

    public static ConfirmExitScreen getInstance() {
        if (INSTANCE == null) INSTANCE = new ConfirmExitScreen();
        return INSTANCE;
    }

    @Override
    protected void init() {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderCustomBackground(context);
        renderOverlay(context);
        texts.forEach(t -> t.render(context));
        buttons.forEach(b -> b.render(context, mouseX, mouseY));
    }

    private void renderCustomBackground(DrawContext context) {
        float screenWidth = MeteorClient.mc.getWindow().getScaledWidth();
        float screenHeight = MeteorClient.mc.getWindow().getScaledHeight();
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
    }

    private void renderOverlay(DrawContext context) {
        int width = MeteorClient.mc.getWindow().getScaledWidth();
        int height = MeteorClient.mc.getWindow().getScaledHeight();
        context.fillGradient(0, 0, width, height, 0xAA000000, 0x80000000);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        buttons.forEach(b -> b.onClick(mouseX, mouseY));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
}
