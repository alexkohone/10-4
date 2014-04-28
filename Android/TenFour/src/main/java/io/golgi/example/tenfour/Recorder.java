package io.golgi.example.tenfour;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Created by briankelly on 10/04/2014.
 */
public class Recorder extends Thread {
    private short[] audioData;
    private short[] data;
    private int recorded = 0;
    private int rate = -1;
    private boolean shutdown;
    private boolean recording;

    private static void DBG(String str) {
        DBG.write("AUDIO", str);
    }


    public void run() {
        AudioRecord recorder = null;
        int sampleLen;
        int avail;
        int rc;

        int[] samplingRates = { 16000, 8000, 44100, 22050, 16000, 11025,
                8000 };


        synchronized (this) {
            rate = 0;
            for (int i = 0; i < samplingRates.length; i++) {
                recorder = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        samplingRates[i],
                        AudioFormat.CHANNEL_CONFIGURATION_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        samplingRates[i] * 4);
                if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                    rate = samplingRates[i];
                    break;
                }
                recorder.release();
                recorder = null;
            }
            DBG("Recording rate being used: " + rate);
            if(recorder != null){
                recorder.startRecording();
                recording = true;
            }
            this.notifyAll();
        }
        if(recorder != null){
            sampleLen = rate / 10;
            data = new short[rate * 10]; // 10 seconds max
            avail = data.length;
            for(int i = 0; i < rate/5; i++){
                data[recorded++] = (short)(Math.random() * 2000.0 - 1000.0);
                avail--;
            }
            while(avail >= sampleLen && !shutdown){
                rc = recorder.read(data, recorded, sampleLen);
                recorded += rc;
                avail -= rc;
            }

            DBG("VoiceRecorder stopping");
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
                recording = false;
            }
            //
            // Now add on a beep at the end.
            // Lets go for 250mS @ 2kHz
            //
            double theta = 0.0f;
            for(int i = 0; i < rate/4; i++){
                theta = ((double)i / (double)rate) * (Math.PI * 2.0 * 2000.0);
                data[recorded++] = (short)(Math.sin(theta) * 10000.0);
                avail--;
            }
            for(int i = 0; i < rate/5; i++){
                data[recorded++] = (short)(Math.random() * 2000.0 - 1000.0);
                avail--;
            }


        }
        synchronized (this){
            if(data != null){
                audioData = new short[recorded];
                System.arraycopy(data, 0, audioData, 0, recorded);
            }
            else{
                audioData = new short[0];
            }
            this.notifyAll();
        }
    }

    public boolean recording(){
        return recording;
    }

    public void stopRecording(){
        shutdown = true;
    }

    public int getRate(){
        return rate;
    }

    public short[] getAudioData(){
        synchronized (this){
            if(audioData == null){
                stopRecording();
                try{
                    this.wait(1000);
                }
                catch(InterruptedException iex){
                }
            }
            if(audioData == null){
                audioData = new short[0];
            }
            return audioData;
        }
    }

    public Recorder(){
        rate = -1;
        recorded = 0;

        this.start();

        synchronized (this) {
            while (rate < 0) {
                try {
                    this.wait();
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}
