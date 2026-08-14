package com.example.addon.modules;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Personalized extends Module {
    public static Personalized INSTANCE;

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    public final Setting<Boolean> titleInterface = sgGeneral.add(new BoolSetting.Builder()
        .name("Title Interface")
        .description("自定义 Minecraft 主界面")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> meteorGuiBackground = sgGeneral.add(new BoolSetting.Builder()
        .name("Meteor GUI BackGround")
        .description("自定义 Meteor GUI 背景")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> firstLaunch = sgGeneral.add(new BoolSetting.Builder()
        .name("first-launch")
        .description("标记模块是否已完成首次默认激活。")
        .defaultValue(true)
        .visible(() -> false)
        .build()
    );

    public Personalized() {
        super(com.example.addon.LBLTAddon.CATEGORY, "Personalized", "Personalize your client.");
        INSTANCE = this;
        runInMainMenu = true;
    }

    public void ensureActive() {
        if (!isActive()) toggle();
    }
}
