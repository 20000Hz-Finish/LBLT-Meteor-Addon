package com.example.addon.gui;

import com.example.addon.modules.Personalized;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL13;

import java.util.ArrayList;
import java.util.List;

public class LBLTMainMenuScreen extends Screen {
    private static LBLTMainMenuScreen INSTANCE;
    private static final Identifier BACKGROUND = Identifier.of("lblt", "gui/background.png");

    private final List<MainMenuButton> buttons = new ArrayList<>();

    private LBLTMainMenuScreen() {
        super(Text.of("LBLT MainMenu"));
    }

    public static LBLTMainMenuScreen getInstance() {
        if (INSTANCE == null) INSTANCE = new LBLTMainMenuScreen();
        return INSTANCE;
    }

    @Override
    protected void init() {
        // If Title Interface is disabled, switch back to vanilla TitleScreen
        Personalized personalized = Personalized.INSTANCE;
        if (personalized == null || !personalized.isActive() || !personalized.titleInterface.get()) {
            MeteorClient.mc.setScreen(new TitleScreen());
            return;
        }

        buttons.clear();
        float buttonX = -107F;
        buttons.add(new MainMenuButton(buttonX, -50F, 214F, 38F,
            I18n.translate("menu.singleplayer").toUpperCase(),
            () -> MeteorClient.mc.setScreen(new SelectWorldScreen(this))));
        buttons.add(new MainMenuButton(buttonX, 0F, 214F, 38F,
            I18n.translate("menu.multiplayer").toUpperCase(),
            () -> MeteorClient.mc.setScreen(new MultiplayerScreen(this))));
        buttons.add(new MainMenuButton(buttonX, 50F, 214F, 38F,
            I18n.translate("menu.options").toUpperCase().replace(".", ""),
            () -> MeteorClient.mc.setScreen(new OptionsScreen(this, MeteorClient.mc.options))));
        buttons.add(new MainMenuButton(buttonX, 100F, 214F, 38F,
            "CLICKGUI",
            this::toggleGui));
        buttons.add(new MainMenuButton(buttonX, 150F, 214F, 38F,
            I18n.translate("menu.quit").toUpperCase(),
            () -> MeteorClient.mc.setScreen(ConfirmExitScreen.getInstance()),
            true));
    }

    private void toggleGui() {
        if (Utils.canCloseGui()) MeteorClient.mc.currentScreen.close();
        else ((Tab) Tabs.get().get(0)).openScreen(GuiThemes.get());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // If Title Interface was disabled, switch back to vanilla TitleScreen
        Personalized personalized = Personalized.INSTANCE;
        if (personalized == null || !personalized.isActive() || !personalized.titleInterface.get()) {
            MeteorClient.mc.setScreen(new TitleScreen());
            return;
        }

        renderBackground(context, mouseX, mouseY, delta);
        renderCustomBackground(context);
        renderOverlay(context);
        renderTitle(context);
        renderVersion(context);
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
        context.fillGradient(0, 0, width, height, 0x80000000, 0x00000000);
    }

    private static final Identifier LBLT_LOGO = Identifier.of("lblt", "gui/logo.png");
    private static final float LOGO_WIDTH = 380.0F;
    private static final float LOGO_HEIGHT = 52.0F;

    private void renderTitle(DrawContext context) {
        int centerX = MeteorClient.mc.getWindow().getScaledWidth() / 2;
        int screenHeight = MeteorClient.mc.getWindow().getScaledHeight();
        int y = screenHeight / 2 - 210;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, LBLT_LOGO);
        context.drawTexture(LBLT_LOGO, centerX - (int) (LOGO_WIDTH / 2.0F), y, 0.0F, 0.0F,
            (int) LOGO_WIDTH, (int) LOGO_HEIGHT, (int) LOGO_WIDTH, (int) LOGO_HEIGHT);
        RenderSystem.disableBlend();
    }

    private void renderVersion(DrawContext context) {
        String version = "Minecraft " + MeteorClient.mc.getGameVersion();
        context.drawTextWithShadow(MeteorClient.mc.textRenderer, version, 2, 2, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        buttons.forEach(b -> b.onClick(mouseX, mouseY));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private String getModVersion() {
        return "0.2";
    }

    public static class MainMenuButton {
        private final float posX;
        private final float posY;
        private final float width;
        private final float height;
        private final String name;
        private final Runnable action;
        private final boolean isExit;
        private float animationProgress = 0.0F;
        private static final float ANIM_SPEED = 0.08F;

        public MainMenuButton(float posX, float posY, float width, float height, String name, Runnable action) {
            this(posX, posY, width, height, name, action, false);
        }

        public MainMenuButton(float posX, float posY, float width, float height, String name, Runnable action, boolean isExit) {
            this.posX = posX;
            this.posY = posY;
            this.width = width;
            this.height = height;
            this.name = name;
            this.action = action;
            this.isExit = isExit;
        }

        public void render(DrawContext context, int mouseX, int mouseY) {
            float halfWidth = MeteorClient.mc.getWindow().getScaledWidth() / 2.0F;
            float halfHeight = MeteorClient.mc.getWindow().getScaledHeight() / 2.0F;
            int x = (int) (halfWidth + posX);
            int y = (int) (halfHeight + posY);
            int w = (int) width;
            int h = (int) height;
            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

            animationProgress = hovered ? Math.min(1.0F, animationProgress + ANIM_SPEED) : Math.max(0.0F, animationProgress - ANIM_SPEED);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // 玻璃色调：退出按钮偏红，普通按钮偏蓝
            int baseR = isExit ? 0xFF : 0x6B;
            int baseG = isExit ? 0x6B : 0xB8;
            int baseB = isExit ? 0x6B : 0xFF;

            // 1. 底层半透明渐变背景（玻璃质感，顶部更实底部更透）
            int alphaTop = (int) (70 + animationProgress * 60);
            int alphaBottom = (int) (30 + animationProgress * 40);
            int topColor = (alphaTop << 24) | (baseR << 16) | (baseG << 8) | baseB;
            int bottomColor = (alphaBottom << 24) | (baseR << 16) | (baseG << 8) | baseB;
            context.fillGradient(x, y, x + w, y + h, topColor, bottomColor);

            // 2. 顶部高光渐变（玻璃反光）
            int highlightAlpha = (int) (60 + animationProgress * 80);
            context.fillGradient(x + 1, y + 1, x + w - 1, y + h / 2,
                (highlightAlpha << 24) | 0xFFFFFF, 0x00FFFFFF);

            // 3. 外边框（高光描边）
            int borderAlpha = (int) (90 + animationProgress * 110);
            context.drawBorder(x, y, w, h, (borderAlpha << 24) | 0xFFFFFF);

            // 4. 顶部内高光线（液态光泽）
            int glossAlpha = (int) (160 + animationProgress * 95);
            context.fill(x + 1, y + 1, x + w - 1, y + 2, (glossAlpha << 24) | 0xFFFFFF);

            RenderSystem.disableBlend();

            // 5. 文字
            int textY = (int) (y + h / 2 - 4 - animationProgress * 2);
            int textColor = hovered ? 0xFFFFFFFF : 0xCCFFFFFF;
            context.drawCenteredTextWithShadow(MeteorClient.mc.textRenderer, name, (int) (x + w / 2), textY, textColor);
        }

        public void onClick(double mouseX, double mouseY) {
            float halfWidth = MeteorClient.mc.getWindow().getScaledWidth() / 2.0F;
            float halfHeight = MeteorClient.mc.getWindow().getScaledHeight() / 2.0F;
            int x = (int) (halfWidth + posX);
            int y = (int) (halfHeight + posY);
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                action.run();
            }
        }
    }
}
