package io.golgi.example.tenfour;

/**
 * Created by briankelly on 10/04/2014.
 */
public class Transmission {
    private short[] audioData;
    private long timestamp;

    public short[] getAudioData(){
        return audioData;
    }

    public long getTimestamp(){
        return timestamp;
    }

    Transmission(short[] audioData){
        this.audioData = audioData;
        this.timestamp = System.currentTimeMillis();
    }

}
