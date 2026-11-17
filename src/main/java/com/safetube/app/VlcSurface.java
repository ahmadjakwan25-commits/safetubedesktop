package com.safetube.app;

import java.nio.ByteBuffer;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

/**
 * VLC থেকে ভিডিওর ফ্রেম (BGRA) নিয়ে Kotlin-এ পাঠায়।
 * vlcj-এর ভার্সন ভেদে মেথডের সংখ্যা আলাদা হয়, তাই দুই ধরনের display/newFormatSize-ই রাখা আছে (@Override ছাড়া)।
 */
public class VlcSurface implements BufferFormatCallback, RenderCallback {
    public interface Sink {
        void format(int srcWidth, int srcHeight);
        void frame(int width, int height, byte[] bgra);
    }

    private static final int MAX_W = 1920;
    private static final int MAX_H = 1080;
    private final Sink sink;

    public VlcSurface(Sink sink) {
        this.sink = sink;
    }

    public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
        float s = Math.min(1f, Math.min(MAX_W / (float) sourceWidth, MAX_H / (float) sourceHeight));
        int w = Math.max(2, ((int) (sourceWidth * s)) & ~1);
        int h = Math.max(2, ((int) (sourceHeight * s)) & ~1);
        sink.format(sourceWidth, sourceHeight);
        return new RV32BufferFormat(w, h);
    }

    public void newFormatSize(int bufferWidth, int bufferHeight, int displayWidth, int displayHeight) {
    }

    public void allocatedBuffers(ByteBuffer[] buffers) {
    }

    public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
        emit(nativeBuffers[0], bufferFormat);
    }

    public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat, int displayWidth, int displayHeight) {
        emit(nativeBuffers[0], bufferFormat);
    }

    private void emit(ByteBuffer b, BufferFormat bf) {
        int w = bf.getWidth();
        int h = bf.getHeight();
        byte[] arr = new byte[w * h * 4];
        b.rewind();
        b.get(arr, 0, Math.min(arr.length, b.remaining()));
        sink.frame(w, h, arr);
    }
}
