package com.example.addon.music.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public final class PingFangFont
{
	private PingFangFont()
	{
	}

	public static Component text(String text)
	{
		return Component.literal(text);
	}

	public static int width(Font font, String text)
	{
		return font.width(text(text));
	}

	public static String trim(Font font, String text, int maxWidth)
	{
		if(maxWidth <= 0)
			return "";
		int low = 0;
		int high = text.length();
		while(low < high)
		{
			int middle = (low + high + 1) >>> 1;
			if(width(font, text.substring(0, middle)) <= maxWidth)
				low = middle;
			else
				high = middle - 1;
		}
		return text.substring(0, low);
	}
}
