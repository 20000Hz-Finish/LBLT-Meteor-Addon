package com.example.addon.music;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;

import com.example.addon.music.MusicStorage;

public enum NeteaseMusicPlayer
{
	INSTANCE;

	private final NeteaseCloudApi api = new NeteaseCloudApi();
	private final MusicAccountManager accountManager =
		new MusicAccountManager();
	private Clip clip;
	private final ExecutorService executor = Executors.newFixedThreadPool(2,
		task -> {
			Thread thread = new Thread(task, "LMA-Netease-Music");
			thread.setDaemon(true);
			return thread;
		});
	private final AtomicLong requestId = new AtomicLong();
	private final Object playerLock = new Object();
	private final Map<Long, Long> lyricOffsets = new ConcurrentHashMap<>();

	private volatile PlaybackState state = PlaybackState.IDLE;
	private volatile NeteaseSong currentSong;
	private volatile List<NeteaseSong> playlist = List.of();
	private volatile List<LyricLine> lyrics = List.of();
	private volatile int currentIndex = -1;
	private volatile long positionMs;
	private volatile long durationMs;
	private volatile long seekOffset;
	private volatile boolean seeking;
	private volatile boolean changingSong;
	private volatile float volume = 1;
	private volatile String error = "";
	private volatile PlaybackMode playbackMode = PlaybackMode.LOOP_ALL;

	NeteaseMusicPlayer()
	{
	}

	public CompletableFuture<List<NeteaseSong>> search(String query)
	{
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				return api.search(query, 30);
			}catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}, executor);
	}

	public CompletableFuture<List<NeteaseSong>> loadHomeSongs()
	{
		return supplyAsync(() -> api.topNewSongs(20));
	}

	public CompletableFuture<List<NeteasePlaylist>> loadRecommendedPlaylists()
	{
		return supplyAsync(() -> api.recommendedPlaylists(6));
	}

	public CompletableFuture<List<NeteaseSong>> loadPlaylist(
		NeteasePlaylist playlist)
	{
		return supplyAsync(() -> api.playlistSongs(playlist.id(), 50, 0));
	}

	public CompletableFuture<List<NeteaseSong>> loadLikedSongs(int offset)
	{
		return supplyAsync(() -> api.likedSongs(50, offset));
	}

	public CompletableFuture<NeteaseCloudApi.LoginResult> sendCaptcha(
		String phone, String countryCode)
	{
		return supplyAsync(() -> api.sendCaptcha(phone, countryCode));
	}

	public CompletableFuture<NeteaseCloudApi.LoginResult> loginWithCaptcha(
		String phone, String captcha, String countryCode)
	{
		return supplyAsync(
			() -> api.loginWithCaptcha(phone, captcha, countryCode));
	}

	public CompletableFuture<NeteaseCloudApi.LoginResult> loginWithCookie(
		String cookie)
	{
		return supplyAsync(() -> api.loginWithCookie(cookie));
	}

	public MusicAccountManager getAccountManager()
	{
		return accountManager;
	}

	public CompletableFuture<NeteaseCloudApi.QrLogin> beginQrLogin()
	{
		return supplyAsync(api::beginQrLogin);
	}

	public CompletableFuture<NeteaseCloudApi.QrCheck> checkQrLogin(String key)
	{
		return supplyAsync(() -> api.checkQrLogin(key));
	}

	private <T> CompletableFuture<T> supplyAsync(ThrowingSupplier<T> supplier)
	{
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				return supplier.get();
			}catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}, executor);
	}

	public void play(List<NeteaseSong> songs, int index)
	{
		if(songs == null || songs.isEmpty() || index < 0
			|| index >= songs.size())
			return;
		playlist = List.copyOf(songs);
		currentIndex = index;
		load(playlist.get(index));
	}

	public void play(NeteaseSong song)
	{
		play(List.of(song), 0);
	}

	private void load(NeteaseSong song)
	{
		long expectedRequest = requestId.incrementAndGet();
		changingSong = true;
		state = PlaybackState.LOADING;
		error = "";
		positionMs = 0;
		durationMs = song.durationMs();
		lyrics = List.of();

		CompletableFuture.runAsync(() -> {
			try
			{
				NeteaseCloudApi.SongResource resource = api.resolve(song);
				String extension = resource.type().isBlank() ? "mp3"
					: resource.type().replaceAll("[^a-zA-Z0-9]", "");
				Path target = cacheFolder().resolve(song.id() + "." + extension);
				if(!hasCompleteCacheFile(target, resource.size()))
					api.download(resource, target);
				if(expectedRequest != requestId.get())
					return;

				synchronized(playerLock)
				{
					stopClip();
					try
					{
						openClip(target.toFile());
					}catch(Exception firstOpenFailure)
					{
						// A stale partial cache file must not make a song permanently unplayable.
						Files.deleteIfExists(target);
						api.download(resource, target);
						openClip(target.toFile());
					}
					setClipVolume(volume);
					currentSong = song;
					seekOffset = 0;
					clip.start();
				}
				state = PlaybackState.PLAYING;
				CompletableFuture.runAsync(() -> loadLyrics(song.id(),
					expectedRequest), executor);
			}catch(Exception e)
			{
				if(expectedRequest == requestId.get())
				{
					state = PlaybackState.ERROR;
					error = readableMessage(e);
					MusicDebug.report("歌曲播放失败", e);
				}
			}finally
			{
				if(expectedRequest == requestId.get())
					changingSong = false;
			}
		}, executor);
	}

	private void loadLyrics(long songId, long expectedRequest)
	{
		try
		{
			List<LyricLine> loaded = api.lyrics(songId);
			if(expectedRequest == requestId.get())
				lyrics = loaded;
		}catch(Exception ignored)
		{
			if(expectedRequest == requestId.get())
				lyrics = List.of();
		}
	}

	private Path cacheFolder()
	{
		return MusicStorage.getFolder().resolve("music-cache");
	}

	private static boolean hasCompleteCacheFile(Path file, long expectedSize)
		throws Exception
	{
		if(!Files.isRegularFile(file))
			return false;
		long actualSize = Files.size(file);
		return actualSize > 0 && (expectedSize <= 0
			|| actualSize >= expectedSize * 95 / 100);
	}

	public void toggle()
	{
		if(state == PlaybackState.PLAYING)
			pause();
		else if(state == PlaybackState.PAUSED)
			resume();
		else if(currentSong != null)
			load(currentSong);
	}

	public void pause()
	{
		executor.execute(() -> {
			synchronized(playerLock)
			{
				if(clip != null)
					clip.stop();
			}
			state = PlaybackState.PAUSED;
		});
	}

	public void resume()
	{
		executor.execute(() -> {
			synchronized(playerLock)
			{
				if(clip != null)
					clip.start();
			}
			state = PlaybackState.PLAYING;
		});
	}

	public void playNext()
	{
		List<NeteaseSong> songs = playlist;
		if(songs.isEmpty() || changingSong)
			return;
		currentIndex = nextIndex(songs.size(), currentIndex, playbackMode);
		load(songs.get(currentIndex));
	}

	public void playPrevious()
	{
		List<NeteaseSong> songs = playlist;
		if(songs.isEmpty() || changingSong)
			return;
		currentIndex = currentIndex > 0 ? currentIndex - 1 : songs.size() - 1;
		load(songs.get(currentIndex));
	}

	private void playAfterCompletion()
	{
		List<NeteaseSong> songs = playlist;
		if(songs.isEmpty() || changingSong)
			return;
		if(playbackMode != PlaybackMode.REPEAT_ONE)
			currentIndex = nextIndex(songs.size(), currentIndex, playbackMode);
		load(songs.get(currentIndex));
	}

	static int nextIndex(int size, int current, PlaybackMode mode)
	{
		if(size <= 1)
			return 0;
		if(mode == PlaybackMode.SHUFFLE)
		{
			int candidate = ThreadLocalRandom.current().nextInt(size - 1);
			return candidate >= current ? candidate + 1 : candidate;
		}
		return (current + 1) % size;
	}

	public void seekTo(long targetMs)
	{
		long duration = durationMs;
		if(currentSong == null || duration <= 0)
			return;
		long target = Math.max(0, Math.min(duration, targetMs));
		positionMs = target;
		seekOffset = 0;
		seeking = true;
		executor.execute(() -> {
			try
			{
				synchronized(playerLock)
				{
					if(clip != null)
						clip.setMicrosecondPosition(target * 1000);
				}
			}catch(Exception e)
			{
				error = readableMessage(e);
			}finally
			{
				seeking = false;
			}
		});
	}

	public void setVolume(float value)
	{
		volume = Math.max(0, Math.min(1, value));
		executor.execute(() -> {
			synchronized(playerLock)
			{
				setClipVolume(volume);
			}
		});
	}

	public void stop()
	{
		requestId.incrementAndGet();
		changingSong = true;
		synchronized(playerLock)
		{
			stopClip();
		}
		state = PlaybackState.IDLE;
		positionMs = 0;
		changingSong = false;
	}

	public void clearPlaylist()
	{
		stop();
		playlist = List.of();
		currentIndex = -1;
		currentSong = null;
		lyrics = List.of();
		durationMs = 0;
		seekOffset = 0;
	}

	public void cyclePlaybackMode()
	{
		playbackMode = switch(playbackMode)
		{
			case LOOP_ALL -> PlaybackMode.REPEAT_ONE;
			case REPEAT_ONE -> PlaybackMode.SHUFFLE;
			case SHUFFLE -> PlaybackMode.LOOP_ALL;
		};
	}

	public PlaybackMode getPlaybackMode()
	{
		return playbackMode;
	}

	public long getLyricOffsetMs()
	{
		NeteaseSong song = currentSong;
		return song == null ? 0 : lyricOffsets.getOrDefault(song.id(), 0L);
	}

	public void adjustLyricOffset(long deltaMs)
	{
		NeteaseSong song = currentSong;
		if(song == null)
			return;
		lyricOffsets.compute(song.id(), (id, value) -> Math.max(-10_000,
			Math.min(10_000, (value == null ? 0 : value) + deltaMs)));
	}

	public void resetLyricOffset()
	{
		NeteaseSong song = currentSong;
		if(song != null)
			lyricOffsets.remove(song.id());
	}

	public long getAdjustedLyricPositionMs()
	{
		return Math.max(0, positionMs + getLyricOffsetMs());
	}

	public void shutdown()
	{
		stop();
		executor.shutdownNow();
	}

	public boolean isLoggedIn()
	{
		return api.isLoggedIn();
	}

	public NeteaseUserProfile getUserProfile()
	{
		return api.getUserProfile();
	}

	public void logout()
	{
		api.logout();
	}


	private void openClip(File file) throws Exception
	{
		try(AudioInputStream source = AudioSystem.getAudioInputStream(file))
		{
			AudioFormat input = source.getFormat();
			AudioFormat output = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
				input.getSampleRate(), 16, input.getChannels(),
				input.getChannels() * 2, input.getSampleRate(), false);
			try(AudioInputStream decoded = AudioSystem.getAudioInputStream(output,
				source))
			{
				DataLine.Info info = new DataLine.Info(Clip.class, output);
				clip = (Clip)AudioSystem.getLine(info);
				clip.open(decoded);
				durationMs = Math.max(durationMs, clip.getMicrosecondLength() / 1000);
				clip.addLineListener(this::handleLineEvent);
			}
		}
	}

	private void stopClip()
	{
		if(clip == null)
			return;
		clip.stop();
		clip.close();
		clip = null;
	}

	private void setClipVolume(float value)
	{
		if(clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
			return;
		FloatControl gain = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
		float decibels = value <= 0 ? gain.getMinimum()
			: (float)(20 * Math.log10(value));
		gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(),
			decibels)));
	}

	private void handleLineEvent(LineEvent event)
	{
		if(event.getType() != LineEvent.Type.STOP || seeking || changingSong)
			return;
		Clip source = clip;
		if(source == null)
			return;
		positionMs = source.getMicrosecondPosition() / 1000;
		if(positionMs < durationMs * 0.9)
			return;
		state = PlaybackState.IDLE;
		playAfterCompletion();
	}

	private String readableMessage(Throwable error)
	{
		Throwable current = error;
		while(current.getCause() != null)
			current = current.getCause();
		String message = current.getMessage();
		return message == null || message.isBlank() ? current.getClass().getSimpleName()
			: message;
	}

	public PlaybackState getState()
	{
		return state;
	}

	public NeteaseSong getCurrentSong()
	{
		return currentSong;
	}

	public List<NeteaseSong> getPlaylist()
	{
		return playlist;
	}

	public List<LyricLine> getLyrics()
	{
		return lyrics;
	}

	public int getCurrentIndex()
	{
		return currentIndex;
	}

	public long getPositionMs()
	{
		Clip source = clip;
		if(source != null && !seeking)
			positionMs = source.getMicrosecondPosition() / 1000;
		return positionMs;
	}

	public long getDurationMs()
	{
		return durationMs;
	}

	public float getVolume()
	{
		return volume;
	}

	public String getError()
	{
		return error;
	}

	public static String formatTime(long milliseconds)
	{
		long totalSeconds = Math.max(0, milliseconds) / 1000;
		return String.format("%02d:%02d", totalSeconds / 60,
			totalSeconds % 60);
	}

	public enum PlaybackState
	{
		IDLE,
		LOADING,
		PLAYING,
		PAUSED,
		ERROR
	}

	public enum PlaybackMode
	{
		LOOP_ALL,
		REPEAT_ONE,
		SHUFFLE
	}

	@FunctionalInterface
	private interface ThrowingSupplier<T>
	{
		T get() throws Exception;
	}
}
