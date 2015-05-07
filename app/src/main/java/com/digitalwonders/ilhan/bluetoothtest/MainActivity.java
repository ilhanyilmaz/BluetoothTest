package com.digitalwonders.ilhan.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;


public class MainActivity extends Activity {

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

        startClientIntent();
        queryPairedDevices();


        //Log.d("BTTest", "Starting intent");
        //mServiceIntent.setData(Uri.parse(dataUrl));
    }

    protected void startServerIntent(String address) {
        Log.d("BTTest", "Starting intent");
        this.stopService(mClientIntent);
        mClientIntent = null;
        mServerIntent = new Intent(this, BTSoundService.class);
        mServerIntent.putExtra(EXTRA_DEVICE_ADDRESS, address);
        this.startService(mServerIntent);
    }
    protected void startClientIntent() {
        Log.d("BTTest", "Starting intent");
        mClientIntent = new Intent(this, BTSoundService.class);
        this.startService(mClientIntent);
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
            String address = info.substring(info.length() - 17);

            Log.i("BTTest", "address: " + address);
            // Create the result Intent and include the MAC address
            startServerIntent(address);

            /*Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();*/
        }
    };
}
