package com.vcsm.service;

import org.springframework.stereotype.Service;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class VoiceQualityService {

    private static final int QUALITY_THRESHOLD_WARN = 40;
    private static final int QUALITY_THRESHOLD_BLOCK = 20;
    private static final float MIN_ACCEPTABLE_RMS = 0.01f;
    private static final float MAX_ACCEPTABLE_RMS = 0.9f;

    public Map<String, Object> analyzeAudioQuality(String audioFilePath) {
        Map<String, Object> result = new HashMap<>();

        try {
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                result.put("quality_score", 0);
                result.put("status", "error");
                result.put("message", "Audio file not found");
                return result;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFileFormat format = AudioSystem.getAudioFileFormat(audioFile);

            byte[] audioData = new byte[4096];
            int bytesRead;
            double sumSquares = 0;
            int sampleCount = 0;

            while ((bytesRead = audioStream.read(audioData)) != -1) {
                for (int i = 0; i < bytesRead; i += 2) {
                    if (i + 1 < bytesRead) {
                        int sample = ((audioData[i + 1] & 0xFF) << 8) | (audioData[i] & 0xFF);
                        if (sample > 32767) {
                            sample -= 65536;
                        }
                        double normalized = sample / 32768.0;
                        sumSquares += normalized * normalized;
                        sampleCount++;
                    }
                }
            }

            audioStream.close();

            float rms = (float) Math.sqrt(sumSquares / sampleCount);
            int clippingRatio = detectClipping(audioData);
            int noiseFloor = estimateNoiseFloor(audioData);

            int qualityScore = calculateQualityScore(rms, clippingRatio, noiseFloor);

            result.put("quality_score", qualityScore);
            result.put("rms_level", String.format("%.4f", rms));
            result.put("clipping_ratio", clippingRatio + "%");
            result.put("noise_floor", noiseFloor);

            if (qualityScore < QUALITY_THRESHOLD_BLOCK) {
                result.put("status", "blocked");
                result.put("message", "Audio quality too low. Please retry with better conditions.");
            } else if (qualityScore < QUALITY_THRESHOLD_WARN) {
                result.put("status", "warning");
                result.put("message", "Audio quality is poor. Transcription accuracy may be affected.");
            } else {
                result.put("status", "acceptable");
                result.put("message", "Audio quality is acceptable.");
            }

        } catch (Exception e) {
            result.put("quality_score", 0);
            result.put("status", "error");
            result.put("message", "Error analyzing audio: " + e.getMessage());
        }

        return result;
    }

    private int detectClipping(byte[] audioData) {
        int clippedSamples = 0;
        int totalSamples = 0;

        for (int i = 0; i < audioData.length; i += 2) {
            if (i + 1 < audioData.length) {
                int sample = ((audioData[i + 1] & 0xFF) << 8) | (audioData[i] & 0xFF);
                if (sample > 32767) {
                    sample -= 65536;
                }
                double normalized = Math.abs(sample / 32768.0);
                if (normalized > 0.99) {
                    clippedSamples++;
                }
                totalSamples++;
            }
        }

        return totalSamples > 0 ? (int) ((clippedSamples * 100.0) / totalSamples) : 0;
    }

    private int estimateNoiseFloor(byte[] audioData) {
        int silentSamples = 0;
        int totalSamples = 0;

        for (int i = 0; i < audioData.length; i += 2) {
            if (i + 1 < audioData.length) {
                int sample = ((audioData[i + 1] & 0xFF) << 8) | (audioData[i] & 0xFF);
                if (sample > 32767) {
                    sample -= 65536;
                }
                double normalized = Math.abs(sample / 32768.0);
                if (normalized < 0.05) {
                    silentSamples++;
                }
                totalSamples++;
            }
        }

        return totalSamples > 0 ? (int) ((silentSamples * 100.0) / totalSamples) : 0;
    }

    private int calculateQualityScore(float rms, int clippingRatio, int noiseFloor) {
        int score = 100;

        if (rms < MIN_ACCEPTABLE_RMS) {
            score -= 50;
        } else if (rms < 0.1) {
            score -= 20;
        }

        if (rms > MAX_ACCEPTABLE_RMS) {
            score -= 30;
        }

        if (clippingRatio > 10) {
            score -= 40;
        } else if (clippingRatio > 5) {
            score -= 20;
        }

        if (noiseFloor > 80) {
            score -= 30;
        } else if (noiseFloor > 60) {
            score -= 15;
        }

        return Math.max(0, score);
    }

    public boolean shouldBlockTranscription(String audioFilePath) {
        Map<String, Object> analysis = analyzeAudioQuality(audioFilePath);
        int score = (int) analysis.getOrDefault("quality_score", 0);
        return score < QUALITY_THRESHOLD_BLOCK;
    }

    public boolean shouldWarnUser(String audioFilePath) {
        Map<String, Object> analysis = analyzeAudioQuality(audioFilePath);
        int score = (int) analysis.getOrDefault("quality_score", 0);
        return score < QUALITY_THRESHOLD_WARN && score >= QUALITY_THRESHOLD_BLOCK;
    }
}
