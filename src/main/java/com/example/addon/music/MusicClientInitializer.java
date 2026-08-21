package com.example.addon.music;

import com.example.addon.hud.DynamicIslandOverlay;
import com.example.addon.music.gui.NeteaseMusicScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class MusicClientInitializer implements ClientModInitializer
{
	private static KeyMapping openMusicKey;

	@Override
	public void onInitializeClient()
	{
		openMusicKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.lblt.open_music",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_MINUS,
			"category.lblt"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			DynamicIslandOverlay.tick(client);
			if(openMusicKey.consumeClick())
			{
				Minecraft mc = Minecraft.getInstance();
				MusicDebug.announce();
				mc.setScreen(new NeteaseMusicScreen(mc.screen));
			}
		});
		HudRenderCallback.EVENT.register((graphics, tickCounter) ->
		{
			MusicHudOverlay.render(graphics);
			DynamicIslandOverlay.render(graphics);
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			NeteaseMusicPlayer.INSTANCE.shutdown();
		});
	}
}
