package com.vcsm.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class VoiceFeatureService {
    
    public double[] extractFeatures(byte[] audioData, int sampleRate) {
        List<Double> features = new ArrayList<>();
        
        // 1. Zero Crossing Rate (ZCR)
        double zcr = calculateZeroCrossingRate(audioData);
        features.add(zcr);
        
        // 2. Energy
        double energy = calculateEnergy(audioData);
        features.add(energy);
        
        // 3. Spectral Centroid
        double spectralCentroid = calculateSpectralCentroid(audioData);
        features.add(spectralCentroid);
        
        // 4. RMS Amplitude
        double rms = calculateRMS(audioData);
        features.add(rms);
        
        // 5. Pitch approximation
        double pitch = estimatePitch(audioData, sampleRate);
        features.add(pitch);
        
        double[] result = new double[features.size()];
        for (int i = 0; i < features.size(); i++) {
            result[i] = features.get(i);
        }
        
        return result;
    }
    
    private double calculateZeroCrossingRate(byte[] audioData) {
        int zeroCrossings = 0;
        for (int i = 1; i < audioData.length; i++) {
            if ((audioData[i - 1] >= 0 && audioData[i] < 0) || 
                (audioData[i - 1] < 0 && audioData[i] >= 0)) {
                zeroCrossings++;
            }
        }
        return (double) zeroCrossings / audioData.length;
    }
    
    private double calculateEnergy(byte[] audioData) {
        double energy = 0;
        for (byte b : audioData) {
            energy += (b * b);
        }
        return energy / audioData.length;
    }
    
    private double calculateSpectralCentroid(byte[] audioData) {
        double sum = 0;
        double weightedSum = 0;
        for (int i = 0; i < audioData.length; i++) {
            double amplitude = Math.abs(audioData[i]);
            sum += amplitude;
            weightedSum += i * amplitude;
        }
        return sum > 0 ? weightedSum / sum : 0;
    }
    
    private double calculateRMS(byte[] audioData) {
        double sum = 0;
        for (byte b : audioData) {
            sum += (b * b);
        }
        return Math.sqrt(sum / audioData.length);
    }
    
    private double estimatePitch(byte[] audioData, int sampleRate) {
        int maxLag = sampleRate / 50;
        double bestCorrelation = 0;
        int bestLag = 0;
        
        for (int lag = 20; lag < maxLag; lag++) {
            double correlation = 0;
            for (int i = 0; i < audioData.length - lag; i++) {
                correlation += (audioData[i] * audioData[i + lag]);
            }
            correlation /= (audioData.length - lag);
            
            if (correlation > bestCorrelation) {
                bestCorrelation = correlation;
                bestLag = lag;
            }
        }
        
        return bestLag > 0 ? (double) sampleRate / bestLag : 0;
    }
    
    public double calculateCosineSimilarity(double[] features1, double[] features2) {
        if (features1.length != features2.length) {
            throw new IllegalArgumentException("Feature vectors must have same length");
        }
        
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        
        for (int i = 0; i < features1.length; i++) {
            dotProduct += features1[i] * features2[i];
            norm1 += Math.pow(features1[i], 2);
            norm2 += Math.pow(features2[i], 2);
        }
        
        if (norm1 == 0 || norm2 == 0) {
            return 0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    public String featuresToJson(double[] features) {
        StringBuffer json = new StringBuffer("[");
        for (int i = 0; i < features.length; i++) {
            json.append(features[i]);
            if (i < features.length - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }
    
    public double[] jsonToFeatures(String json) {
        String[] parts = json.replace("[", "").replace("]", "").split(",");
        double[] features = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            features[i] = Double.parseDouble(parts[i]);
        }
        return features;
    }
}