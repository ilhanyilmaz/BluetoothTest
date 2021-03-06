package com.digitalwonders.ilhan.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements ServiceConnection {

    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mArrayAdapter;
    private TextView textView;
    private Intent mServerIntent;
    private Intent mClientIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void init() {
        textView = (TextView) findViewById(R.id.textView);
        int REQUEST_ENABLE_BT = 1;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            textView.setText("Device does not support Bluetooth");
            return;
        }
        else
            textView.setText("Device supports Bluetooth");

        if (!mBluetoothAdapter.isEnabled()) {
            textView.setText("Bluetooth not enabled! Starting intent!");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            if(REQUEST_ENABLE_BT != RESULT_OK)
                // permission to open bluetooth not given
                return;
        }
        mArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_list_view);
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(mArrayAdapter);
        listView.setOnItemClickListener(mDeviceClickListener);

        startServerIntent();
        queryPairedDevices();


        //Log.d("BTTest", "Starting intent");
        //mServiceIntent.setData(Uri.parse(dataUrl));
    }

    protected void startClientIntent(String address) {
        Log.d("BTTest", "Starting client intent");
        mClientIntent = new Intent(this, BTSoundService.class);
        mClientIntent.putExtra(EXTRA_DEVICE_ADDRESS, address);
        this.startService(mClientIntent);
    }
    protected void startServerIntent() {
        Log.d("BTTest", "Starting server intent");
        mServerIntent = new Intent(this, BTSoundService.class);
        this.startService(mServerIntent);
    }
    private void stopServerIntent() {
        this.stopService(mServerIntent);
        mServerIntent = null;
    }


    private void queryPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
// If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            //mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);

            Log.i("BTTest", "address: " + address);
            //stopServerIntent();
            bindService(mServerIntent, MainActivity.this, Context.BIND_AUTO_CREATE);

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    startClientIntent(address);
                }
            }, 3000);
            // Create the result Intent and include the MAC address
            //startClientIntent(address);

            /*Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();*/
        }
    };

    private class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Toast.makeText(getApplicationContext(), "Message received", Toast.LENGTH_SHORT).show();
            System.out.println("Message received!");
            super.handleMessage(msg);
        }
    }

    private Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {

        Log.i("service", "connected");
        Message message = Message.obtain();
        message.replyTo = messenger;

        try {
            new Messenger(binder).send(message);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i("service", "disconnected");
        // TODO Auto-generated method stub
        stopServerIntent();
    }
}
