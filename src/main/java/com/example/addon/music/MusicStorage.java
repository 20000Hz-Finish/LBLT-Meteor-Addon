package com.example.addon.music;

import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;

public final class MusicStorage
{
	private MusicStorage()
	{
	}

	public static Path getFolder()
	{
		return FabricLoader.getInstance().getConfigDir().resolve("lblt-music");
	}
}
