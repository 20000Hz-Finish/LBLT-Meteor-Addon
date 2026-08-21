package com.example.addon.music;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class MusicDebug
{
	private static final long DEDUPLICATION_MS = 4_000;
	private static final Map<String, Long> LAST_REPORTS =
		new ConcurrentHashMap<>();
	private static volatile boolean enabled = true;
	private static volatile boolean announced;

	private MusicDebug()
	{
	}

	public static void announce()
	{
		if(enabled && !announced)
		{
			announced = true;
			message("音乐调试已启用：播放和封面错误会显示在这里。");
		}
	}

	public static void report(String type, Throwable error)
	{
		Throwable cause = rootCause(error);
		String detail = cause == null ? "" : describe(cause);
		report(type, detail);
	}

	public static void report(String type, String detail)
	{
		if(!enabled)
			return;
		String text = detail == null || detail.isBlank() ? type
			: type + "：" + detail;
		long now = System.currentTimeMillis();
		Long previous = LAST_REPORTS.put(text, now);
		if(previous == null || now - previous >= DEDUPLICATION_MS)
			message("[音乐调试] " + text);
	}

	private static void message(String text)
	{
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> {
			if(client.gui != null)
				client.gui.getChat().addMessage(Component.literal(text));
		});
	}

	private static Throwable rootCause(Throwable error)
	{
		Throwable current = error;
		while(current != null && current.getCause() != null
			&& current.getCause() != current)
			current = current.getCause();
		return current;
	}

	private static String describe(Throwable error)
	{
		if(error instanceof UnsupportedAudioFileException)
			return "音频格式不受支持";
		if(error instanceof LineUnavailableException)
			return "系统音频设备不可用";
		if(error instanceof java.net.http.HttpTimeoutException)
			return "网络请求超时";
		if(error instanceof java.net.ConnectException)
			return "无法连接到服务器";
		String message = error.getMessage();
		return message == null || message.isBlank()
			? error.getClass().getSimpleName() : message;
	}
}
