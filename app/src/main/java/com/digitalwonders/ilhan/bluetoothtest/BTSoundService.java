package com.digitalwonders.ilhan.bluetoothtest;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by ilhan on 06.05.2015.
 */
public class BTSoundService extends IntentService {

    private AudioManager am;

    public BTSoundService() {
        super("BTSoundService");

    }

    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                // Pause playback
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                //am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                am.abandonAudioFocus(afChangeListener);
                // Stop playback
            }
        }
    };

    @Override
    protected void onHandleIntent(Intent workIntent) {
        // Gets data from the incoming Intent
        String dataString = workIntent.getDataString();

        Log.d("BTTest", "test");

        am = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        // Request audio focus for playback
        int result = am.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);


        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Start playback.
            //result = am.abandonAudioFocus(afChangeListener);
            Log.d("BTTest", "focus granted");
        }

        initCapture();
        // Do work here, based on the contents of dataString



    }

    private boolean docking = false;

    private int numHighAmp(short[] values, int bufferlength) {
        int n=0;
        for(int i=0; i<bufferlength; i+=5)
            if(300 < values[i])
                n++;
        return n;
    }
    private void initCapture() {

        Log.i("Audio", "Running Audio Thread");
        AudioRecord recorder = null;
        AudioTrack track = null;
        short[][]   buffers  = new short[256][160];
        int ix = 0;
        int amp = 0;
        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        try
        {
            int N = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, N*10, AudioTrack.MODE_STREAM);
            recorder.startRecording();
            track.play();
            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            while(true)
            {
                //Log.i("Map", "Writing new data to buffer");
                short[] buffer = buffers[ix++ % buffers.length];
                N = recorder.read(buffer,0,buffer.length);
                amp = numHighAmp(buffer, 100);
                Log.i("BTTest", "Amplitute: " + amp);
                if(amp > 5) {
                    if(!docking) {
                        track.play();
                        //am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                        docking = true;
                    }
                    track.write(buffer, 0, buffer.length);
                }
                else if(docking) {
                    docking = false;
                    //am.abandonAudioFocus(afChangeListener);
                    track.stop();
                }
            }
        }
        catch(Throwable x)
        {
            Log.w("Audio", "Error reading voice audio", x);
        }
        /*
         * Frees the thread's resources after the loop completes so that it can be run again
         */
        finally
        {
            recorder.stop();
            recorder.release();
            track.stop();
            track.release();
        }
    }
}