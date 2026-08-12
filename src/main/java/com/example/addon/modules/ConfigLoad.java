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

public class ConfigLoad extends Module {
    public static ConfigLoad INSTANCE;

    public ConfigLoad() {
        super(com.example.addon.LBLTAddon.CATEGORY, "ConfigLoad", "加载各个客户端的配置");
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
        .description("执行 Alien 加载")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> hachimi = sgGeneral.add(new BoolSetting.Builder()
        .name("Hachimi")
        .description("执行 Hachimi 加载")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> mio = sgGeneral.add(new BoolSetting.Builder()
        .name("Mio")
        .description("执行 Mio 加载")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> careFree = sgGeneral.add(new BoolSetting.Builder()
        .name("CareFree")
        .description("执行 CareFree 加载")
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
        WButton loadButton = theme.button("Load Configs");
        loadButton.action = this::loadAll;
        table.add(loadButton).centerX();
        return table;
    }

    private void loadAll() {
        info("开始加载配置");
        if (alien.get()) {
            send(alienPrefix.get() + "load " + alienCfgName.get());
        }
        if (hachimi.get()) {
            send(hachimiPrefix.get() + "config load " + hachimiCfgName.get());
        }
        if (mio.get()) {
            send(mioPrefix.get() + "preset all load " + mioCfgName.get());
        }
        if (careFree.get()) {
            send(careFreePrefix.get() + "load " + careFreeCfgName.get());
        }
        info("加载完毕");
    }

    private void send(String command) {
        if (deBug.get()) {
            info("执行: " + command);
        }
        mc.player.networkHandler.sendChatMessage(command);
    }
}
