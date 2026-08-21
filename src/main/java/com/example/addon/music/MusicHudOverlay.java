package com.example.addon.music;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class MusicHudOverlay
{
	private static final int WIDTH = 164;

	private MusicHudOverlay()
	{}

	public static void render(GuiGraphics graphics)
	{
		if(!MusicSettings.INSTANCE.isShowHudProgress())
			return;
		Minecraft minecraft = Minecraft.getInstance();
		if(minecraft.player == null || minecraft.screen != null)
			return;
		NeteaseMusicPlayer player = NeteaseMusicPlayer.INSTANCE;
		NeteaseSong song = player.getCurrentSong();
		if(song == null)
			return;
		boolean showLyrics = MusicSettings.INSTANCE.isShowHudLyrics();
		int left = (minecraft.getWindow().getGuiScaledWidth() - WIDTH) / 2;
		int bottom = minecraft.getWindow().getGuiScaledHeight() - 37;
		int top = bottom - (showLyrics ? 36 : 22);
		graphics.fill(left - 5, top - 4, left + WIDTH + 5, bottom + 3,
			0xA60B0E13);
		Font font = minecraft.font;
		String title = fit(font, song.name(), WIDTH);
		drawCentered(graphics, font, title, left + WIDTH / 2, top, 0xFFF3F7F6);
		long duration = player.getDurationMs();
		long position = player.getPositionMs();
		float progress = duration <= 0 ? 0
			: Mth.clamp(position / (float)duration, 0, 1);
		int barTop = top + 9;
		graphics.fill(left, barTop, left + WIDTH, barTop + 2, 0x664F5A62);
		graphics.fill(left, barTop, left + Math.round(WIDTH * progress), barTop + 2,
			0xFFEC4141);
		String time = NeteaseMusicPlayer.formatTime(position) + " / "
			+ NeteaseMusicPlayer.formatTime(duration);
		drawCentered(graphics, font, time, left + WIDTH / 2, top + 13,
			0xCCB7C0C0);
		if(showLyrics)
		{
			String lyric = currentLyric(player.getLyrics(),
				player.getAdjustedLyricPositionMs());
			drawCentered(graphics, font, fit(font, lyric, WIDTH), left + WIDTH / 2,
				top + 25, 0xFFF3F7F6);
		}
	}

	private static String currentLyric(List<LyricLine> lyrics, long positionMs)
	{
		int index = LyricParser.findCurrentIndex(lyrics, positionMs);
		return index < 0 ? "" : lyrics.get(index).text();
	}

	private static String fit(Font font, String text, int width)
	{
		float scale = MusicSettings.INSTANCE.getFontScale();
		return font.plainSubstrByWidth(text,
			Math.max(1, Math.round(width / Math.max(0.01F, scale))));
	}

	private static void drawCentered(GuiGraphics graphics, Font font,
		String text, int centerX, int y, int color)
	{
		float scale = MusicSettings.INSTANCE.getFontScale();
		graphics.pose().pushPose();
		graphics.pose().translate(centerX, y, 0);
		graphics.pose().scale(scale, scale, 1);
		graphics.drawString(font, text, -font.width(text) / 2, 0, color, false);
		graphics.pose().popPose();
	}
}
