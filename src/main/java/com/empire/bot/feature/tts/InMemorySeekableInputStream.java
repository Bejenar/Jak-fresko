package com.empire.bot.feature.tts;

import com.sedmelluq.discord.lavaplayer.source.local.LocalSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.ExtendedBufferedInputStream;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class InMemorySeekableInputStream extends SeekableInputStream {
    private static final Logger log = LoggerFactory.getLogger(LocalSeekableInputStream.class);
    //    private final FileChannel channel;
    private final InputStream inputStream;
    private final ExtendedBufferedInputStream bufferedStream;
    private long position;

    public InMemorySeekableInputStream(InputStream inputStream) {
        super(0, 0L);
        this.inputStream = inputStream;
        this.bufferedStream = new ExtendedBufferedInputStream(inputStream);
//            this.channel = this.inputStream.getChannel();

    }

    public int read() throws IOException {
        int result = this.bufferedStream.read();
        if (result >= 0) {
            ++this.position;
        }

        return result;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int read = this.bufferedStream.read(b, off, len);
        this.position += (long) read;
        return read;
    }

    public long skip(long n) throws IOException {
        long skipped = this.bufferedStream.skip(n);
        this.position += skipped;
        return skipped;
    }

    public int available() throws IOException {
        return this.bufferedStream.available();
    }

    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    public boolean markSupported() {
        return false;
    }

    public void close() throws IOException {
        try {
            this.bufferedStream.close();
        } catch (IOException var2) {
            log.debug("Failed to close channel", var2);
        }

    }

    public long getPosition() {
        return this.position;
    }

    public boolean canSeekHard() {
        return true;
    }

    public List<AudioTrackInfoProvider> getTrackInfoProviders() {
        return Collections.emptyList();
    }

    protected void seekHard(long position) throws IOException {
        this.inputStream.skip(position);
        this.position = position;
        this.bufferedStream.discardBuffer();
    }
}
