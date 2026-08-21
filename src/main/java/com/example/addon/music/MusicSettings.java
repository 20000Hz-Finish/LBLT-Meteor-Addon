package com.example.addon.music;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MusicSettings
{
	public static final MusicSettings INSTANCE = new MusicSettings();
	public static final float MIN_FONT_SCALE = 0.8F;
	public static final float MAX_FONT_SCALE = 1.25F;
	public static final float DEFAULT_FONT_SCALE = 1F;

	private volatile float fontScale = DEFAULT_FONT_SCALE;
	private volatile boolean showHudProgress = true;
	private volatile boolean showHudLyrics;

	private MusicSettings()
	{
		load();
		loadOverlaySettings();
	}

	public float getFontScale()
	{
		return fontScale;
	}

	public synchronized void setFontScale(float value)
	{
		fontScale = clamp(value);
		save();
	}

	public void adjustFontScale(float delta)
	{
		setFontScale(getFontScale() + delta);
	}

	public boolean isShowHudProgress()
	{
		return showHudProgress;
	}

	public synchronized void setShowHudProgress(boolean value)
	{
		showHudProgress = value;
		saveOverlaySettings();
	}

	public boolean isShowHudLyrics()
	{
		return showHudProgress && showHudLyrics;
	}

	public synchronized void setShowHudLyrics(boolean value)
	{
		showHudLyrics = value;
		saveOverlaySettings();
	}

	private void load()
	{
		Path file = settingsFile();
		try
		{
			if(Files.isRegularFile(file))
				fontScale = clamp(Float.parseFloat(Files.readString(file,
					StandardCharsets.UTF_8).trim()));
		}catch(IOException | NumberFormatException ignored)
		{
			fontScale = DEFAULT_FONT_SCALE;
		}
	}

	private void save()
	{
		Path file = settingsFile();
		try
		{
			Files.createDirectories(file.getParent());
			Files.writeString(file, Float.toString(fontScale),
				StandardCharsets.UTF_8);
		}catch(IOException ignored)
		{}
	}

	private void loadOverlaySettings()
	{
		Path file = MusicStorage.getFolder().resolve("hud-settings.txt");
		try
		{
			if(!Files.isRegularFile(file))
				return;
			for(String line : Files.readAllLines(file, StandardCharsets.UTF_8))
			{
				String[] pair = line.split("=", 2);
				if(pair.length != 2)
					continue;
				if("progress".equals(pair[0]))
					showHudProgress = Boolean.parseBoolean(pair[1]);
				else if("lyrics".equals(pair[0]))
					showHudLyrics = Boolean.parseBoolean(pair[1]);
			}
		}catch(IOException ignored)
		{}
	}

	private void saveOverlaySettings()
	{
		Path file = MusicStorage.getFolder().resolve("hud-settings.txt");
		try
		{
			Files.createDirectories(file.getParent());
			Files.writeString(file, "progress=" + showHudProgress + "\nlyrics="
				+ showHudLyrics + "\n", StandardCharsets.UTF_8);
		}catch(IOException ignored)
		{}
	}

	private static Path settingsFile()
	{
		return MusicStorage.getFolder().resolve("font-scale.txt");
	}

	private static float clamp(float value)
	{
		return Math.max(MIN_FONT_SCALE, Math.min(MAX_FONT_SCALE, value));
	}
}
