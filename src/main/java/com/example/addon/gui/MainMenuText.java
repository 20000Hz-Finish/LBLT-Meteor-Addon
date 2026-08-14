package com.example.addon.gui;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public class MainMenuText {
    private final float posX;
    private final float posY;
    private final String text;

    public MainMenuText(float posX, float posY, String text) {
        this.posX = posX;
        this.posY = posY;
        this.text = text;
    }

    public void render(DrawContext context) {
        float halfWidth = MeteorClient.mc.getWindow().getScaledWidth() / 2.0F;
        float halfHeight = MeteorClient.mc.getWindow().getScaledHeight() / 2.0F;
        int x = (int) (halfWidth + posX);
        int y = (int) (halfHeight + posY);

        context.drawCenteredTextWithShadow(MeteorClient.mc.textRenderer, text, x, y, 0xFFFFFFFF);
    }
}
