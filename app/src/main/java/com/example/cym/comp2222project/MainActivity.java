package com.example.cym.comp2222project;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_FINE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_CONNECT_BT = 3;

    private TextView distance;
    private MediaPlayer mp;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mBTService;

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        distance = (TextView) findViewById(R.id.distance);
        mp = MediaPlayer.create(getApplicationContext(), R.raw.alarm);
        mp.setLooping(true);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT).show();
        }

        mBTService = new BluetoothService(MainActivity.this, mHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBTService.stop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_CONNECT_BT);
                break;
            case REQUEST_CONNECT_BT:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mBTService.connect(device);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.bluetooth:
                checkLocationPermission();
                if (mBluetoothAdapter == null) {
                    // Device does not support Bluetooth
                    Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT).show();
                    break;
                }
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(intent, REQUEST_CONNECT_BT);
                }
                break;
            case R.id.setting:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    if (readBuf[0] == 97) {
                        int value = (readBuf[1] * 256 + readBuf[2]) / 100;
                        Log.i(TAG, "data: "+String.valueOf(value));
                        if (value >= 0) {
                            playSound(value);
                            if (value <= sharedPref.getInt(getResources().getString(R.string.danger_key), 10)) {
                                setActivityBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.danger));
                            } else if (value <= sharedPref.getInt(getResources().getString(R.string.warning_key), 25)) {
                                setActivityBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.warning));
                            } else {
                                setActivityBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                            }
                            if (value >= 99) {
                                distance.setText(">"+String.valueOf(value));
                            } else {
                                distance.setText(String.valueOf(value));
                            }
                            mBTService.setInterval(100);
                        } else {
                            mBTService.setInterval(0);
                        }
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void playSound(int value) {
        if (!sharedPref.getBoolean(getResources().getString(R.string.enable_sound_key), false)) {
            return;
        }
        if (value <= sharedPref.getInt(getResources().getString(R.string.danger_key), 10) && !mp.isPlaying()) {
            mp.start();
        }
        if (value > sharedPref.getInt(getResources().getString(R.string.danger_key), 10) || mp.isPlaying()) {
            mp.pause();
        }
    }

    public void setActivityBackgroundColor(int color) {
        View view = this.getWindow().getDecorView();
        view.setBackgroundColor(color);
    }

    protected void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    //TODO re-request
                }
                break;
            }
        }
    }
}
