package com.digitalwonders.ilhan.bluetoothtest;

import android.app.Activity;
import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by ilhan on 06.05.2015.
 */
public class BTSoundService extends IntentService {

    private BluetoothServerSocket mmServerSocket;
    //private final BluetoothDevice mmDevice;
    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private static final String NAME = "BluetoothChatSecure";
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private BluetoothAdapter mBluetoothAdapter = null;

    private AudioTrack track = null;

    protected boolean timerRunning = false;
    private Timer timer;
    private TimerTask timerTask;

    final Handler handler = new Handler();


    private AudioManager am;

    private BluetoothChatService mChatService = null;

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
        //String dataString = workIntent.getDataString();
        String address = null;
        if(workIntent.getExtras() != null)
            address= workIntent.getExtras().getString(MainActivity.EXTRA_DEVICE_ADDRESS);



        Log.d("BTTest", "address:" + address);


        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(mHandler);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(address != null) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            mChatService.connect(device);
        }

        am = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        //if(am.isSpeakerphoneOn())
            //Log.d("BTTest", "test");

        // Request audio focus for playback
        /*int result = am.requestAudioFocus(afChangeListener,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);


        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Start playback.
            //result = am.abandonAudioFocus(afChangeListener);
            Log.d("BTTest", "focus granted");
        }*/


    }

    private int numHighAmp(short[] values, int bufferlength) {
        int n=0;
        int max=0;
        for(int i=0; i<bufferlength; i+=1) {
            if(values[i] > max)
                max = values[i];
            if (values[i]>5000)
                n++;
        }
        //if (max>5000)
        //    Log.i("BTTest max", ":" +max );
        return n;
    }
    private void initCapture() {

        //timer = new Timer();
        initializeTimerTask();
        Log.i("Audio", "Running Audio Thread");
        AudioRecord recorder = null;

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
            Log.i("BBTest", "buffer size: " + N);
            //track.play();
            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            while(true)
            {
                //Log.i("Map", "Writing new data to buffer");
                short[] buffer = buffers[ix++ % buffers.length];

                N = recorder.read(buffer,0,buffer.length);
                amp = numHighAmp(buffer, buffer.length);
                //Log.i("BTTest", "Amplitute: " + amp);


                if(amp > 3) {

                    if(timerRunning) {
                        timer.cancel();
                        timer.purge();
                        timer = null;
                    }
                    else {
                        timerRunning = true;
                        //am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                        //track.play();
                    }

                    timer = new Timer();
                    initializeTimerTask();
                    timer.schedule(timerTask, 2000);
                    //track.write(buffer, 0, buffer.length);
                    ByteBuffer bb = ByteBuffer.allocate(buffer.length*2);
                    bb.asShortBuffer().put(buffer);
                    //Log.i("BBTest", "record buffer: " + bb.array());
                    mChatService.write(bb.array());
                }
                else if(timerRunning) {
                    //track.write(buffer, 0, buffer.length);
                    ByteBuffer bb = ByteBuffer.allocate(buffer.length*2);
                    bb.asShortBuffer().put(buffer);
                    mChatService.write(bb.array());
                }
                else {
                    //track.stop();
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
            timer.cancel();
            timer = null;
            recorder.stop();
            recorder.release();
            track.stop();
            track.release();
        }
    }
    protected void streamSound(byte[] buffer) {
        Log.i("BBTest", "output buffer 1: " + buffer);
        //am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        if(track == null)
            return;
        track.play();
        //track.write(buffer, 0, buffer.length);
        ByteBuffer bb = ByteBuffer.allocate(buffer.length);
        bb.put(buffer);
        track.write(bb.asShortBuffer().array(), 0, buffer.length / 2);
        Log.i("BBTest", "output buffer 2: " + buffer);
        track.stop();
    }

    private void noSoundActions() {
        timerRunning = false;
        am.abandonAudioFocus(afChangeListener);
    }

    /*private void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 5000, 300); //
    }
    private void stoptimertask() {

        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }*/

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {

                        noSoundActions();

                    }
                });
            }
        };
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        //actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        //actionBar.setSubtitle(subTitle);
    }


    /**
     * The Handler that gets information back from the BluetoothChatService
     */

    protected String mConnectedDeviceName;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.connected_to, mConnectedDeviceName));
                            initCapture();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    //byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer

                    //own voice actions, if any

                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Log.i("Handler", "message: " + readMessage);
                    streamSound(readBuf);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    /*if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }*/
                    break;
                case Constants.MESSAGE_TOAST:
                    /*if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }*/
                    break;
            }
        }
    };
}