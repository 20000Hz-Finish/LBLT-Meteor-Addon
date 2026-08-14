package com.example.addon;

import com.example.addon.modules.ConfigLoad;
import com.example.addon.modules.ConfigSave;
import com.example.addon.modules.FriendsManager;
import com.example.addon.modules.Personalized;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class LBLTAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("LBLT");

    @Override
    public void onInitialize() {
        LOG.info("Initializing LBLT Meteor Addon");

        add(new com.example.addon.modules.Burrow());
        add(new FriendsManager());
        add(new ConfigSave());
        add(new ConfigLoad());
        add(new Personalized());
    }

    private void add(Module module) {
        Modules.get().add(module);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("20000Hz", "LBLT-Meteor-Addon");
    }
}
