package com.empire.bot.feature.tts;

import com.empire.bot.config.command.Command;
import com.empire.bot.config.command.CommandDeclaration;
import com.empire.bot.config.command.CommandRequest;
import com.empire.bot.config.command.CommandResponse;
import com.sedmelluq.discord.lavaplayer.container.wav.WavAudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.local.LocalSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import discord4j.voice.retry.VoiceGatewayException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import marytts.LocalMaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

@Slf4j
@Component
@CommandDeclaration("tts")
public class TextToSpeech implements Command {

    private final GatewayDiscordClient gateway;

    // Creates AudioPlayer instances and translates URLs to AudioTrack instances
    private final AudioPlayerManager playerManager;


    // Create an AudioPlayer so Discord4J can receive audio data
    private final AudioPlayer player;

    // We will be creating LavaPlayerAudioProvider in the next step
    private final AudioProvider provider;

    public TextToSpeech(GatewayDiscordClient gateway) {
        this.gateway = gateway;
        playerManager = new DefaultAudioPlayerManager();
        // This is an optimization strategy that Discord4J can utilize. It is not important to understand
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        // Allow playerManager to parse remote sources like YouTube links
        AudioSourceManagers.registerRemoteSources(playerManager);
        player = playerManager.createPlayer();
        provider = new LavaPlayerAudioProvider(player);
    }

    @SneakyThrows
    @Override
    public Publisher<Void> apply(CommandRequest request, CommandResponse response) {


        final Snowflake vcId = Snowflake.of("856588493300170782");
        Snowflake id = request.getMessage().getGuild().block().getId();
        return joinVoiceChannel(id, vcId, provider).and(t -> {
            log.info("playing audio");
            playAudio(request.parameters(), player);
            log.info("stopped playing audio");
        });

    }

    @SneakyThrows
    private void playAudio(final String text, final AudioPlayer player) {
        AudioInputStream stream = null;
        stream = synthesize(text);
        log.info("format {}", stream.getFormat());
        SeekableInputStream seekableInputStream = new LocalSeekableInputStream(Path.of("output.wav").toFile());
        AudioTrackInfo audioTrackInfo = new AudioTrackInfo(text, "me", stream.getFormat().getFrameSize(), "id", true, null);
        AudioTrack track = new WavAudioTrack(audioTrackInfo, seekableInputStream);
        player.playTrack(track);
    }

    private Mono<VoiceConnection> joinVoiceChannel(Snowflake guildId, Snowflake voiceChannelId,
                                                   AudioProvider audioProvider) {

//         Do not join the voice channel if the bot is already joining one
//        if (this.guildJoining.computeIfAbsent(guildId, id -> new AtomicBoolean()).getAndSet(true)) {
//            return Mono.empty();
//        }

        final Mono<Boolean> isDisconnected = gateway.getVoiceConnectionRegistry()
            .getVoiceConnection(guildId)
            .flatMapMany(VoiceConnection::stateEvents)
            .next()
            .map(VoiceConnection.State.DISCONNECTED::equals)
            .defaultIfEmpty(true);

        return gateway.getChannelById(voiceChannelId)
            .cast(VoiceChannel.class)
            // Do not join the voice channel if the current voice connection is in not disconnected
            .filterWhen(ignored -> isDisconnected)
            .doOnNext(ignored -> log.info("{Guild ID: {}} Joining voice channel...", guildId.asLong()))
            .flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(audioProvider)))
            .doOnError(VoiceGatewayException.class, err -> log.warn(err.getMessage()))
            .onErrorMap(VoiceGatewayException.class,
                err -> new RuntimeException("An unknown error occurred while joining the voice channel, please try again."))
            .doOnTerminate(() -> /*this.guildJoining.remove(guildId)*/ log.info(""));
    }

    @SneakyThrows
    public static AudioInputStream synthesize(final String text) throws IOException {
// init mary
        LocalMaryInterface mary = null;
        try {
            mary = new LocalMaryInterface();
            mary.setLocale(new Locale("ru"));
            System.out.println(mary.getAvailableVoices());
            System.out.println(mary.getAvailableLocales());
        } catch (MaryConfigurationException e) {
            System.err.println("Could not initialize MaryTTS interface: " + e.getMessage());
            throw e;
        }

        // synthesize
        AudioInputStream audio = null;
        try {
            audio = mary.generateAudio(text);
        } catch (SynthesisException e) {
            System.err.println("Synthesis failed: " + e.getMessage());
            System.exit(1);
        }


        AudioFormat.Encoding defaultEncoding = AudioFormat.Encoding.PCM_SIGNED;
        float fDefaultSampleRate = 16000;
        int nDefaultSampleSizeInBits = 16;
        int nDefaultChannels = 1;
        int frameSize = 2;
        float frameRate = 16000;
        boolean bDefaultBigEndian = false;

        AudioFormat defaultFormat = new AudioFormat(defaultEncoding, fDefaultSampleRate, nDefaultSampleSizeInBits, nDefaultChannels, frameSize, frameRate, bDefaultBigEndian);
        AudioInputStream targetaudio = AudioSystem.getAudioInputStream(defaultFormat, audio);
        AudioSystem.write(targetaudio, AudioFileFormat.Type.WAVE, new File("output.wav"));
        return audio;
    }

}
