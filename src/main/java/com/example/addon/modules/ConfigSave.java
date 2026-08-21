package com.example.addon.modules;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;

public class ConfigSave extends Module {
    public static ConfigSave INSTANCE;

    public ConfigSave() {
        super(com.example.addon.LBLTAddon.CATEGORY, "ConfigSave", "If you have multiple clients installed in Minecraft, this module can save the configs for all of them at the same time.");
        INSTANCE = this;
    }

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    public final Setting<Boolean> deBug = sgGeneral.add(new BoolSetting.Builder()
        .name("DeBug")
        .description("IQ+++")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> alien = sgGeneral.add(new BoolSetting.Builder()
        .name("Alien")
        .description("执行 Alien 保存")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> hachimi = sgGeneral.add(new BoolSetting.Builder()
        .name("Hachimi")
        .description("执行 Hachimi 保存")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> mio = sgGeneral.add(new BoolSetting.Builder()
        .name("Mio")
        .description("执行 Mio 保存")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> careFree = sgGeneral.add(new BoolSetting.Builder()
        .name("CareFree")
        .description("执行 CareFree 保存")
        .defaultValue(true)
        .build()
    );

    private final SettingGroup sgAlien = this.settings.createGroup("Alien");
    public final Setting<String> alienPrefix = sgAlien.add(new StringSetting.Builder()
        .name("AlienPrefix")
        .description("Alien 前缀")
        .defaultValue(";")
        .build()
    );
    public final Setting<String> alienCfgName = sgAlien.add(new StringSetting.Builder()
        .name("AlienCfgName")
        .description("Alien 配置文件名")
        .defaultValue("default")
        .build()
    );

    private final SettingGroup sgHachimi = this.settings.createGroup("Hachimi");
    public final Setting<String> hachimiPrefix = sgHachimi.add(new StringSetting.Builder()
        .name("HachimiPrefix")
        .description("Hachimi 前缀")
        .defaultValue(";")
        .build()
    );
    public final Setting<String> hachimiCfgName = sgHachimi.add(new StringSetting.Builder()
        .name("HachimiCfgName")
        .description("Hachimi 配置文件名")
        .defaultValue("default")
        .build()
    );

    private final SettingGroup sgMio = this.settings.createGroup("Mio");
    public final Setting<String> mioPrefix = sgMio.add(new StringSetting.Builder()
        .name("MioPrefix")
        .description("Mio 前缀")
        .defaultValue(".")
        .build()
    );
    public final Setting<String> mioCfgName = sgMio.add(new StringSetting.Builder()
        .name("MioCfgName")
        .description("Mio 配置文件名")
        .defaultValue("default")
        .build()
    );

    private final SettingGroup sgCareFree = this.settings.createGroup("CareFree");
    public final Setting<String> careFreePrefix = sgCareFree.add(new StringSetting.Builder()
        .name("CareFreePrefix")
        .description("CareFree 前缀")
        .defaultValue(";")
        .build()
    );
    public final Setting<String> careFreeCfgName = sgCareFree.add(new StringSetting.Builder()
        .name("CareFreeCfgName")
        .description("CareFree 配置文件名")
        .defaultValue("default")
        .build()
    );

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable table = theme.table();
        WButton saveButton = theme.button("Save Configs");
        saveButton.action = this::saveAll;
        table.add(saveButton).centerX();
        return table;
    }

    private void saveAll() {
        info("开始保存配置");
        if (alien.get()) {
            send(alienPrefix.get() + "save " + alienCfgName.get());
        }
        if (hachimi.get()) {
            send(hachimiPrefix.get() + "config save " + hachimiCfgName.get());
        }
        if (mio.get()) {
            send(mioPrefix.get() + "preset all save " + mioCfgName.get());
        }
        if (careFree.get()) {
            send(careFreePrefix.get() + "save " + careFreeCfgName.get());
        }
        info("保存完毕");
    }

    private void send(String command) {
        if (deBug.get()) {
            info("执行: " + command);
        }
        mc.player.connection.sendChat(command);
    }
}
