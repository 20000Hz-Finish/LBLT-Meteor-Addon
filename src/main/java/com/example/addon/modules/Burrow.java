package com.example.addon.modules;

import meteordevelopment.meteorclient.systems.modules.Module;

public class Burrow extends Module {
    public Burrow() {
        super(com.example.addon.LBLTAddon.CATEGORY, "Burrow", "Implement burrow in LBLT.XYZ");
    }

    @Override
    public void onActivate() {
        mc.player.networkHandler.sendChatCommand("burrow");
        toggle();
    }
}
