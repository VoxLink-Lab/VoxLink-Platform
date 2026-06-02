package voxlink.client.src.main.media;

import javax.sound.sampled.*;
import java.util.function.Consumer;

/**
 * Manages audio capture from microphone and playback to speakers.
 */
public class AudioManager {

    private static AudioManager instance;

    // Standard format for voice: 16kHz, 16-bit, Mono, Signed, Little-Endian
    private final AudioFormat audioFormat = new AudioFormat(16000.0f, 16, 1, true, false);

    private TargetDataLine microphone;
    private SourceDataLine speaker;

    private boolean isCapturing = false;
    private boolean isPlaying = false;

    private boolean isMuted = false;
    private boolean isDeafened = false;

    private Consumer<byte[]> audioDataListener;

    private AudioManager() {}

    public static synchronized AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }
    
    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { this.isMuted = muted; }
    
    public boolean isDeafened() { return isDeafened; }
    public void setDeafened(boolean deafened) { this.isDeafened = deafened; }

    public void setAudioDataListener(Consumer<byte[]> listener) {
        this.audioDataListener = listener;
    }

    public void startCapture() {
        if (isCapturing) return;

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("[AudioManager] Microphone not supported with format: " + audioFormat);
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(audioFormat);
            microphone.start();
            isCapturing = true;

            Thread captureThread = new Thread(() -> {
                // Buffer size approx 20ms of audio: 16000 * (16/8) * 0.02 = 640 bytes
                byte[] buffer = new byte[640];
                while (isCapturing) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0 && audioDataListener != null && !isMuted && !isDeafened) {
                        // Create a copy of the buffer to send
                        byte[] data = new byte[bytesRead];
                        System.arraycopy(buffer, 0, data, 0, bytesRead);
                        audioDataListener.accept(data);
                    }
                }
            }, "AudioCaptureThread");
            captureThread.setDaemon(true);
            captureThread.start();
            
            System.out.println("[AudioManager] Audio capture started.");

        } catch (LineUnavailableException e) {
            System.err.println("[AudioManager] Microphone unavailable: " + e.getMessage());
        }
    }

    public void stopCapture() {
        isCapturing = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
            System.out.println("[AudioManager] Audio capture stopped.");
        }
    }

    public void startPlayback() {
        if (isPlaying) return;

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("[AudioManager] Speaker not supported with format: " + audioFormat);
                return;
            }

            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(audioFormat);
            speaker.start();
            isPlaying = true;
            
            System.out.println("[AudioManager] Audio playback started.");

        } catch (LineUnavailableException e) {
            System.err.println("[AudioManager] Speaker unavailable: " + e.getMessage());
        }
    }

    public void stopPlayback() {
        isPlaying = false;
        if (speaker != null) {
            speaker.stop();
            speaker.close();
            speaker = null;
            System.out.println("[AudioManager] Audio playback stopped.");
        }
    }

    public void playAudio(byte[] audioData) {
        if (isPlaying && speaker != null && !isDeafened) {
            speaker.write(audioData, 0, audioData.length);
        }
    }
}
