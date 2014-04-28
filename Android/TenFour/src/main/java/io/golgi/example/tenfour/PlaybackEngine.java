package io.golgi.example.tenfour;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.Ringtone;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Created by briankelly on 10/04/2014.
 */
public class PlaybackEngine {
    private Object syncObj = new Object();
    private Ringtone vNoteRingtone;
    private AudioTrack vNoteAudioTrack;
    private Transmission curTransmission;
    private Vector<Transmission> rxQueue = new Vector<Transmission>();
    private Timer vNoteTimer;
    private int originalVolume;
    private GolgiService svc;

    public boolean isRunning(){
        return (vNoteTimer != null) ? true : false;
    }

    private void startVNoteTimer(){
        synchronized(syncObj){
            if(vNoteTimer == null){
                vNoteTimer = new Timer();
                AudioManager audioManager = (AudioManager)svc.getSystemService(Context.AUDIO_SERVICE);
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                vNoteTimer.schedule(new TimerTask(){
                    @Override
                    public void run() {
                        synchronized(syncObj){
                            if(vNoteRingtone != null){
                                if(!vNoteRingtone.isPlaying()){
                                    DBG.write("Ringtone complete");
                                    vNoteRingtone = null;
                                    vNoteAudioTrack.play();
                                }
                            }
                            else if(vNoteAudioTrack != null){
                                int pos = vNoteAudioTrack.getPlaybackHeadPosition();
                                // DBG("Playback Position: " + pos + "(" + curVNote.getAudioData().length + ")");
                                if(pos == curTransmission.getAudioData().length){
                                    vNoteAudioTrack.stop();
                                    vNoteAudioTrack = null;
                                    curTransmission = null;
                                }
                            }

                            if(vNoteRingtone == null && vNoteAudioTrack == null){
                                if(rxQueue.size() == 0){
                                    vNoteTimer.cancel();
                                    vNoteTimer = null;
                                    AudioManager audioManager = (AudioManager)svc.getSystemService(Context.AUDIO_SERVICE);
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                                }
                                else{
                                    try {
                                        curTransmission = rxQueue.remove(0);
                                        AudioManager audioManager = (AudioManager)svc.getSystemService(Context.AUDIO_SERVICE);
                                        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

                                        try {
                                            vNoteAudioTrack = new AudioTrack(
                                                    AudioManager.STREAM_MUSIC,
                                                    16000,
                                                    AudioFormat.CHANNEL_OUT_MONO,
                                                    AudioFormat.ENCODING_PCM_16BIT,
                                                    (int)curTransmission.getAudioData().length *2,
                                                    AudioTrack.MODE_STATIC);
                                            int rc = vNoteAudioTrack.write(curTransmission.getAudioData(), 0, curTransmission.getAudioData().length);
                                            DBG.write("Wrote " + rc);
                                            // audioTrack.setStereoVolume(1.0f, 1.0f);
                                            vNoteAudioTrack.play();                                          // Play the track

                                        }
                                        catch (Exception e){
                                            DBG.write("Audio playing exploded");
                                        }
                                        DBG.write("Audio playing may or may not have worked");
                                    }
                                    catch (Exception e){
                                    }
                                }

                            }
                        }
                    }
                                    },
                        0, 20);
            }
        }
    }

    public void play(Transmission rx){
        synchronized(syncObj){
            rxQueue.add(rx);
            startVNoteTimer();
        }
    }

    public PlaybackEngine(GolgiService svc){
        this.svc = svc;

    }

}
