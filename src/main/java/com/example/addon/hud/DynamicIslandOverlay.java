package com.example.addon.hud;

import java.util.IdentityHashMap;
import java.util.Map;

import com.example.addon.music.ui.FlatRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * A compact top-center notification inspired by Suncat's Dynamic Island HUD.
 * It observes every Meteor module, including modules registered by this addon.
 */
public final class DynamicIslandOverlay
{
	private static final long DISPLAY_MS = 5_000;
	private static final long FADE_MS = 180;
	private static final Map<Module, Boolean> moduleStates =
		new IdentityHashMap<>();

	private static boolean initialized;
	private static String displayText = "";
	private static boolean enabled;
	private static long changedAt;

	private DynamicIslandOverlay()
	{}

	public static void tick(Minecraft minecraft)
	{
		if(minecraft.level == null)
		{
			moduleStates.clear();
			initialized = false;
			displayText = "";
			return;
		}
		try
		{
			Module changed = null;
			boolean changedState = false;
			for(Module module : Modules.get().getAll())
			{
				boolean active = module.isActive();
				Boolean previous = moduleStates.put(module, active);
				if(initialized && previous != null && previous != active)
				{
					changed = module;
					changedState = active;
				}
			}
			initialized = true;
			if(changed != null)
			{
				enabled = changedState;
				displayText = changed.title;
				changedAt = System.currentTimeMillis();
			}
		}catch(RuntimeException ignored)
		{
			// Meteor may not have initialized its module system during startup.
		}
	}

	public static void render(GuiGraphics graphics)
	{
		if(displayText.isBlank())
			return;
		long elapsed = System.currentTimeMillis() - changedAt;
		if(elapsed >= DISPLAY_MS + FADE_MS)
		{
			displayText = "";
			return;
		}
		float alpha = elapsed < FADE_MS ? ease(elapsed / (float)FADE_MS)
			: elapsed > DISPLAY_MS ? 1 - ease((elapsed - DISPLAY_MS)
				/ (float)FADE_MS) : 1;
		if(alpha <= 0.01F)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		String suffix = enabled ? "  \u5df2\u5f00\u542f" : "  \u5df2\u5173\u95ed";
		String text = displayText + suffix;
		int textWidth = font.width(text);
		int width = Math.max(112, textWidth + 38);
		int height = 26;
		int left = (minecraft.getWindow().getGuiScaledWidth() - width) / 2;
		int top = 10 - Math.round((1 - alpha) * 12);
		int color = enabled ? 0xFF79E7A3 : 0xFFFF8190;
		int background = withAlpha(0xFF090B0F, 0.94F * alpha);
		FlatRenderer.fillRoundedRect(graphics, left - 2, top + 2,
			left + width + 2, top + height + 3, height / 2 + 2,
			withAlpha(0xFF000000, 0.35F * alpha));
		FlatRenderer.fillRoundedRect(graphics, left, top, left + width,
			top + height, height / 2, background);
		FlatRenderer.drawRoundedOutline(graphics, left, top, left + width,
			top + height, height / 2, withAlpha(0xFFFFFFFF, 0.13F * alpha));
		FlatRenderer.fillRoundedRect(graphics, left + 10, top + 10, left + 16,
			top + 16, 3, withAlpha(color, alpha));
		graphics.drawString(font, text, left + 24,
			top + (height - font.lineHeight) / 2, withAlpha(0xFFF5F7F6, alpha),
			false);
	}

	private static float ease(float value)
	{
		float clamped = Mth.clamp(value, 0, 1);
		return 1 - (1 - clamped) * (1 - clamped);
	}

	private static int withAlpha(int color, float alpha)
	{
		return Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24
			| color & 0x00FFFFFF;
	}
}
