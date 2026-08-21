package com.example.addon.music.ui;

import net.minecraft.client.gui.GuiGraphics;

public final class FlatRenderer
{
	private FlatRenderer()
	{
	}

	public static void fillRoundedRect(GuiGraphics context, int x1, int y1,
		int x2, int y2, int radius, int color)
	{
		RoundedRectRenderer.fill(context, x1, y1, x2, y2, radius, color);
	}

	public static void drawRoundedOutline(GuiGraphics context, int x1, int y1,
		int x2, int y2, int radius, int color)
	{
		RoundedRectRenderer.outline(context, x1, y1, x2, y2, radius, color);
	}
}
