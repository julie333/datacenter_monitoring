package com.example.datacenter_monitoring;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.example.datacenter_monitoring.util.Assert;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD_MS = 10000;
    // Bluetooth SIG registered 16-bit "UUIDs" have base UUID 0000xxxx-0000-1000-8000-00805f9b34fb
    private static final UUID DATACENTER_MONITOR_SERVICE_UUID =
            UUID.fromString("6b750001-006c-4f1b-8e32-a20d9d19aa13");
    private static final UUID TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID =
            UUID.fromString("6b750004-006c-4f1b-8e32-a20d9d19aa13"); // N
    private static final UUID HUMIDITY_MEASUREMENT_CHARACTERISTIC_UUID =
            UUID.fromString("6b750002-006c-4f1b-8e32-a20d9d19aa13"); // R
    private static final UUID HEATER_STATE_CHARACTERISTIC_UUID =
            UUID.fromString("6b750003-006c-4f1b-8e32-a20d9d19aa13"); // W

    private boolean mIsConnected = false;
    private BluetoothLeScanner mScanner;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private Handler mHandler = new Handler(Looper.getMainLooper());


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

    /* Bluetooth Connection */

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i(TAG, "onScanResult, result = " + result.getDevice().getAddress());
            mBluetoothDevice = result.getDevice();
            connect(); // TODO: move to button handler
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "onScanFailed, errorCode = " + errorCode);
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "onConnectionStateChange, STATE_CONNECTED\n\tgatt = " + gatt);
                mIsConnected = true;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "onConnectionStateChange, STATE_DISCONNECTED\n\tgatt = " + gatt);
                disconnect();
            }
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            Log.i(TAG, "onCharacteristicChanged, UUID = " +  characteristic.getUuid());
            UUID uuid = characteristic.getUuid();
            if (uuid.equals(HUMIDITY_MEASUREMENT_CHARACTERISTIC_UUID)) {
                //byte[] value = characteristic.getValue();
                int formatType = BluetoothGattCharacteristic.FORMAT_UINT8;
                int value = characteristic.getIntValue(formatType, 1);
                Log.i(TAG, "value = " + value);
            }

            if (uuid.equals(TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                //byte[] value = characteristic.getValue();
                int formatType = BluetoothGattCharacteristic.FORMAT_UINT8;
                int value = characteristic.getIntValue(formatType, 1);
                Log.i(TAG, "value = " + value);
            }
        }

        @Override
        public void onCharacteristicRead(
                final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status)
        {
            Log.i(TAG, "onCharacteristicRead, UUID = " +
                    characteristic.getUuid() + ", status = " + status);
            if (status == GATT_SUCCESS) {
                UUID uuid = characteristic.getUuid();
                if (uuid.equals(HUMIDITY_MEASUREMENT_CHARACTERISTIC_UUID)) {
                    byte[] value = characteristic.getValue();
                    Log.i(TAG, value.toString());
                }

                if (uuid.equals(TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                    byte[] value = characteristic.getValue();
                    Log.i(TAG, value.toString());
                }

            }
        }

        @Override
        public void onCharacteristicWrite(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            Log.i(TAG, "onCharacteristicWrite, UUID = " +
                    characteristic.getUuid() + ",\nstatus = " + status);
        }

        private void init (final BluetoothGatt gatt) {
            // TODO: implement a queue or use a 3rd party BLE library
            readCharacteristic(gatt, DATACENTER_MONITOR_SERVICE_UUID,
                    HUMIDITY_MEASUREMENT_CHARACTERISTIC_UUID, 100);

            readCharacteristic(gatt, DATACENTER_MONITOR_SERVICE_UUID,
                    TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID, 100);

            writeCharacteristic(gatt, DATACENTER_MONITOR_SERVICE_UUID,
                    HUMIDITY_MEASUREMENT_CHARACTERISTIC_UUID,
                    0, BluetoothGattCharacteristic.FORMAT_UINT16, 500);
            writeCharacteristic(gatt, DATACENTER_MONITOR_SERVICE_UUID,
                    TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID,
                    0, BluetoothGattCharacteristic.FORMAT_UINT16, 500);

            setCharacteristicNotification(gatt, DATACENTER_MONITOR_SERVICE_UUID,
                    HUMIDITY_MEASUREMENT_CHARACTERISTIC_UUID, 1000);
            setCharacteristicNotification(gatt, DATACENTER_MONITOR_SERVICE_UUID,
                    TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID, 1000);
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            Log.i(TAG, "Services discovered, status = " + status);
            if (status == GATT_SUCCESS) {
                init(gatt);
            }
        }
    };

    private void readCharacteristic(
            final BluetoothGatt gatt, final UUID serviceUuid, final UUID characteristicUuid, int delayMs)
    {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothGattService gattService = gatt.getService(serviceUuid);
                if (gattService != null) {
                    BluetoothGattCharacteristic characteristic =
                            gattService.getCharacteristic(characteristicUuid);
                    if (characteristic != null) {
                        gatt.readCharacteristic(characteristic);
                    }
                }
            }
        }, delayMs);
    }

    private void writeCharacteristic(
            final BluetoothGatt gatt, final UUID serviceUuid, final UUID characteristicUuid,
            final int value, final int formatType, int delayMs)
    {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                BluetoothGattService gattService = gatt.getService(serviceUuid);
                if (gattService != null) {
                    BluetoothGattCharacteristic characteristic =
                            gattService.getCharacteristic(characteristicUuid);
                    if (characteristic != null) {
                        characteristic.setValue(value, formatType, 0);
                        gatt.writeCharacteristic(characteristic);
                    }
                }
            }
        }, delayMs);
    }

    private void setCharacteristicNotification(
            final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int delayMs)
    {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                gatt.setCharacteristicNotification(characteristic, true);
                // 0x2902 org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
                UUID configUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(configUuid);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }, delayMs);
    }

    private void setCharacteristicNotification(
            final BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid, int delayMs)
    {
        setCharacteristicNotification(
                gatt, gatt.getService(serviceUuid).getCharacteristic(characteristicUuid), delayMs);
    }

    private boolean isLocationEnabled() {
        // Based on https://stackoverflow.com/questions/10311834
        boolean enabled;
        int locationMode = 0;
        try { // 19+
            locationMode = Settings.Secure.getInt(
                    this.getContentResolver(), Settings.Secure.LOCATION_MODE);
            enabled = (locationMode != Settings.Secure.LOCATION_MODE_OFF);
        } catch (Settings.SettingNotFoundException e) {
            enabled = false;
        }
        return enabled;
    }

    private void scan() {
        Assert.check(mHandler != null);
        Assert.check(mScanner != null);
        List<ScanFilter> filters = new ArrayList<>();
        //filters.add(new ScanFilter.Builder().setDeviceAddress("C9:1E:3F:18:61:9D").build());
        filters.add(new ScanFilter.Builder().setServiceUuid(
                new ParcelUuid(DATACENTER_MONITOR_SERVICE_UUID)).build()); // 21+
        ScanSettings settings = (new ScanSettings.Builder().setScanMode(
                ScanSettings.SCAN_MODE_LOW_LATENCY)).build();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "stop scan");
                mScanner.stopScan(mScanCallback);
            }
        }, SCAN_PERIOD_MS);
        Log.i(TAG, "start scan");
        mScanner.startScan(filters, settings, mScanCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        Log.i(TAG, "onCreate");
        boolean hasBle = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        // Or <uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
        if (hasBle) {
            Log.i(TAG, "BLE available");
            BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                Log.i(TAG, "BLE enabled");
                mScanner = bluetoothAdapter.getBluetoothLeScanner();
                String[] permissions = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                };
                int requestCode = 0;
                ActivityCompat.requestPermissions(MainActivity.this, permissions, requestCode);
            } else {
                Log.i(TAG, "BLE not enabled");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            Log.i(TAG, "BLE not available");
        }
    }

    @Override
    public void onRequestPermissionsResult (
            int requestCode, String[] permissions, int[] grantResults)
    {
        if (isLocationEnabled()) {
            scan(); // TODO: move to button handler
        } else {
            Log.i(TAG, "Location not enabled");
        }
    }

    public void connect() {
        Log.i(TAG, "connect");
        Assert.check(mBluetoothDevice != null);
        Assert.check(mIsConnected == false);
        // call from main thread required to make connectGatt work
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // work-around to prevent multiple calls to mGattCallback
                // based on http://stackoverflow.com/questions/33274009
                if (mBluetoothGatt == null) {
                    // see https://stackoverflow.com/questions/22214254
                    boolean autoConnect = false; // see onConnectionStateChange
                    Log.i(TAG, "mBluetoothDevice.connectGatt");
                    try {
                        mBluetoothGatt = mBluetoothDevice.connectGatt(
                                MainActivity.this, autoConnect, mGattCallback);
                        Log.i(TAG, "mBluetoothGatt = " + (mBluetoothGatt != null ?
                                mBluetoothGatt.toString() : "null"));
                    } catch (Exception e) {
                        Log.i(TAG, e.toString());
                    }
                } else {
                    Log.i(TAG, "mBluetoothGatt.connect");
                    mBluetoothGatt.connect();
                }
            }
        }, 1);
    }

    public void disconnect() {
        Log.i(TAG, "disconnect");
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mIsConnected = false;
    }
}
