package com.example.addon.music.gui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import com.example.addon.music.MusicDebug;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

final class NeteaseImageCache implements AutoCloseable
{
	private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
	private static final long RETRY_DELAY_MS = Duration.ofSeconds(5).toMillis();
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(8))
		.followRedirects(HttpClient.Redirect.ALWAYS).build();

	private final Map<String, Entry> entries = new ConcurrentHashMap<>();
	private volatile boolean closed;

	Texture get(String url)
	{
		if(closed || url == null || url.isBlank())
			return null;
		String normalizedUrl = normalizeUrl(url);
		long now = System.currentTimeMillis();
		Entry entry = entries.compute(normalizedUrl, (ignored, current) ->
			current == null || current.shouldRetry(now) ? load(normalizedUrl) : current);
		return entry.texture;
	}

	boolean isClosed()
	{
		return closed;
	}

	private Entry load(String url)
	{
		Entry entry = new Entry(ResourceLocation.fromNamespaceAndPath("lblt-meteor-addon", "netease/"
			+ Integer.toUnsignedString(url.hashCode(), 16)), System.currentTimeMillis());
		URI uri;
		try
		{
			uri = URI.create(url);
		}catch(IllegalArgumentException ex)
		{
			entry.fail(ex);
			MusicDebug.report("封面地址无效", ex);
			return entry;
		}
		HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
			.timeout(Duration.ofSeconds(15))
			.header("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
			.header("Accept", "image/png,image/jpeg,image/*;q=0.8,*/*;q=0.5")
			.header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
			.GET();
		String host = uri.getHost();
		if(host != null && (host.endsWith("music.126.net") || host.endsWith("163.com")
			|| host.endsWith("netease.com")))
			builder.header("Referer", "https://music.163.com/");
		HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
			.thenAccept(response -> decode(entry, response))
			.exceptionally(error -> {
				entry.fail(error);
				MusicDebug.report("封面下载失败", error);
				return null;
			});
		return entry;
	}

	private void decode(Entry entry, HttpResponse<byte[]> response)
	{
		byte[] bytes = response.body();
		if(response.statusCode() / 100 != 2)
		{
			System.err.println("[NeteaseImage] HTTP " + response.statusCode()
				+ " for " + response.uri());
			entry.fail(null);
			MusicDebug.report("封面请求失败", "HTTP " + response.statusCode());
			return;
		}
		if(bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES || closed)
		{
			if(!closed)
			{
				entry.fail(null);
				MusicDebug.report("封面数据无效", bytes == null || bytes.length == 0
					? "响应为空" : "文件过大");
			}
			return;
		}
		try
		{
			NativeImage image = readImage(bytes);
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				if(closed)
				{
					image.close();
					return;
				}
				try
				{
					int width = image.getWidth();
					int height = image.getHeight();
					int accent = sampleAccent(image);
					DynamicTexture dynamicTexture = new DynamicTexture(image);
					dynamicTexture.setFilter(true, false);
					client.getTextureManager().register(entry.location, dynamicTexture);
					entry.texture = new Texture(entry.location, width, height, accent);
				}catch(Exception ex)
				{
					entry.fail(ex);
					MusicDebug.report("封面纹理注册失败", ex);
				}
			});
		}catch(Exception ex)
		{
			entry.fail(ex);
			MusicDebug.report("封面解码失败", ex);
		}
	}

	private static NativeImage readImage(byte[] bytes) throws IOException
	{
		try
		{
			return NativeImage.read(new ByteArrayInputStream(bytes));
		}catch(IOException pngFailure)
		{
			BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
			if(decoded == null)
				throw pngFailure;
			try(ByteArrayOutputStream png = new ByteArrayOutputStream())
			{
				if(!ImageIO.write(decoded, "png", png))
					throw pngFailure;
				return NativeImage.read(new ByteArrayInputStream(png.toByteArray()));
			}
		}
	}

	private static String normalizeUrl(String url)
	{
		try
		{
			URI uri = URI.create(url);
			String host = uri.getHost();
			if(host != null && host.endsWith("music.126.net"))
			{
				if("http".equalsIgnoreCase(uri.getScheme()))
					url = "https" + url.substring(4);
				if(!hasThumbnailParameter(uri.getQuery()))
					url += uri.getQuery() == null ? "?param=200y200"
						: "&param=200y200";
			}
		}catch(IllegalArgumentException ignored)
		{
		}
		return url;
	}

	private static boolean hasThumbnailParameter(String query)
	{
		if(query == null || query.isBlank())
			return false;
		for(String parameter : query.split("&"))
			if(parameter.startsWith("param="))
				return true;
		return false;
	}

	private static final int DEFAULT_ACCENT = 0xFFEC4141;

	private static int sampleAccent(NativeImage image)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		if(width <= 0 || height <= 0)
			return DEFAULT_ACCENT;
		int stepX = Math.max(1, width / 8);
		int stepY = Math.max(1, height / 8);
		int best = DEFAULT_ACCENT;
		float bestScore = -1;
		for(int y = stepY / 2; y < height; y += stepY)
			for(int x = stepX / 2; x < width; x += stepX)
			{
				int pixel = image.getPixelRGBA(x, y);
				if(pixel >>> 24 < 160)
					continue;
				int red = pixel & 0xFF;
				int green = pixel >>> 8 & 0xFF;
				int blue = pixel >>> 16 & 0xFF;
				float max = Math.max(red, Math.max(green, blue));
				float min = Math.min(red, Math.min(green, blue));
				float luminance = (0.299F * red + 0.587F * green
					+ 0.114F * blue) / 255F;
				if(luminance < 0.14F || luminance > 0.9F)
					continue;
				float saturation = max == 0 ? 0 : (max - min) / max;
				float score = saturation
					* (1F - Math.abs(luminance - 0.45F) * 1.4F);
				if(score > bestScore)
				{
					bestScore = score;
					best = 0xFF000000 | red << 16 | green << 8 | blue;
				}
			}
		return best;
	}

	@Override
	public void close()
	{
		closed = true;
		Minecraft client = Minecraft.getInstance();
		for(Entry entry : entries.values())
			if(entry.texture != null)
				client.getTextureManager().release(entry.location);
		entries.clear();
	}

	record Texture(ResourceLocation location, int width, int height, int accent)
	{}

	private static final class Entry
	{
		private final ResourceLocation location;
		private final long requestedAt;
		private volatile Texture texture;
		private volatile long failedAt;

		private Entry(ResourceLocation location, long requestedAt)
		{
			this.location = location;
			this.requestedAt = requestedAt;
		}

		private boolean shouldRetry(long now)
		{
			return texture == null && now - (failedAt == 0 ? requestedAt : failedAt)
				>= RETRY_DELAY_MS;
		}

		private void fail(Throwable error)
		{
			failedAt = System.currentTimeMillis();
			if(error != null)
				error.printStackTrace();
		}
	}
}
