package com.example.addon.music;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Built-in equivalent of music-api's netease.php?type=songid endpoint.
 * It follows NetEase's public outer URL directly, so no PHP server is needed.
 */
final class MusicApiResolver
{
	private static final String OUTER_URL =
		"https://music.163.com/song/media/outer/url?id=";
	private static final HttpClient CLIENT = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER).build();

	private MusicApiResolver()
	{
	}

	static NeteaseCloudApi.SongResource resolve(long songId)
		throws IOException, InterruptedException
	{
		// Request the MP3 rendition. Without the extension, some CDN responses
		// select AAC/M4A, which is not supported by the bundled Java decoder.
		URI current = URI.create(OUTER_URL + songId + ".mp3");
		for(int redirect = 0; redirect < 5; redirect++)
		{
			HttpRequest request = HttpRequest.newBuilder(current)
				.timeout(Duration.ofSeconds(15))
				.header("Referer", "https://music.163.com/")
				.header("User-Agent", "Mozilla/5.0 LMA-Music-Addon/1.0")
				.method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
			HttpResponse<Void> response = CLIENT.send(request,
				HttpResponse.BodyHandlers.discarding());
			int status = response.statusCode();
			if(status / 100 == 2)
			{
				long size = response.headers().firstValueAsLong("Content-Length")
					.orElse(0);
				return new NeteaseCloudApi.SongResource(current, size,
					extension(current));
			}
			if(status / 100 != 3)
				throw new IOException("内置 music-api 解析失败：HTTP " + status);
			String location = response.headers().firstValue("Location")
				.orElse("");
			if(location.isBlank())
				throw new IOException("内置 music-api 未返回重定向地址");
			try
			{
				current = current.resolve(location);
			}catch(IllegalArgumentException e)
			{
				throw new IOException("内置 music-api 返回了无效播放地址", e);
			}
		}
		throw new IOException("内置 music-api 重定向次数过多");
	}

	private static String extension(URI uri)
	{
		String path = uri.getPath();
		if(path == null)
			return "mp3";
		int dot = path.lastIndexOf('.');
		return dot < 0 || dot == path.length() - 1 ? "mp3"
			: path.substring(dot + 1).replaceAll("[^a-zA-Z0-9]", "");
	}

}
