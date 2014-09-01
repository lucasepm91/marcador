/*
Android App for the WSD-Marcador project
Barcelona 2014
Idensitat+Pratipo

Code strongly based on
http://digitalhacksblog.blogspot.com.es/2012/05/arduino-to-android-turning-led-on-and.html
adapted by Pratipo
check github.com/pratipo for more info
*/

package com.pratipo.wsd.wsd;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;


import java.io.IOException;
import java.io.OutputStream;

import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.util.Log;
import android.content.Intent;

import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


public class main extends Activity {

    private static final String TAG = "main";

    private TextView stateText;
    private Button pairButton;
    private NumberPicker pickerA, pickerB;
    private RadioGroup rg1, rg2;
    private RadioButton rA1, rA2, rA3, rB1, rB2, rB3;


    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private Set<BluetoothDevice>pairedDevices;

    // standard SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // bluetooth device MAC address
    private static String address = "00:14:03:18:40:61"; // "Azul2" device




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        stateText = (TextView) findViewById(R.id.textState);
        stateText.setText("not connected");

        pairButton = (Button) findViewById(R.id.pairButton);

        pairButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connect();
            }
        });

        pickerA = (NumberPicker) findViewById(R.id.pickerA);
        pickerA.setMinValue(0);
        pickerA.setMaxValue(99);
        pickerA.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        pickerB = (NumberPicker) findViewById(R.id.pickerB);
        pickerB.setMinValue(0);
        pickerB.setMaxValue(99);
        pickerB.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        NumberPicker.OnScrollListener changeMgmt = new NumberPicker.OnScrollListener() {
            @Override
            public void onScrollStateChange(NumberPicker numberPicker, int scrollState) {
                if (scrollState == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {

                    String data = String.format("%2s", String.valueOf(((NumberPicker) findViewById(R.id.pickerA)).getValue()) ).replace(' ', '0')
                            + String.format("%2s", String.valueOf(((NumberPicker) findViewById(R.id.pickerB)).getValue()) ).replace(' ', '0');

                    sendData(data);
                }
            }
        };

        pickerA.setOnScrollListener(changeMgmt);
        pickerB.setOnScrollListener(changeMgmt);

        rA1 = (RadioButton)findViewById(R.id.radioA1);
        rA1.setChecked(true);

        rB1 = (RadioButton)findViewById(R.id.radioB1);
        rB1.setChecked(true);

        RadioGroup.OnCheckedChangeListener change = new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                String code = new String();
                if ( group.getId()== R.id.radioGroupA) {
                    code = "A";
                    switch (checkedId) {
                        case R.id.radioA1:
                            code += 1; break;
                        case R.id.radioA2:
                            code += 2; break;
                        case R.id.radioA3:
                            code += 3; break;
                    }
                }
                else if ( group.getId()== R.id.radioGroupB) {
                    code = "B";
                    switch (checkedId) {
                        case R.id.radioB1:
                            code += 1; break;
                        case R.id.radioB2:
                            code += 2; break;
                        case R.id.radioB3:
                            code += 3; break;
                    }
                }
                Toast.makeText(getApplicationContext(), code, Toast.LENGTH_SHORT).show();
                //code = "";
                sendData(code);
            }
        };
        rg1 = (RadioGroup) findViewById(R.id.radioGroupA);
        rg1.setOnCheckedChangeListener(change);

        rg2 = (RadioGroup) findViewById(R.id.radioGroupB);
        rg2.setOnCheckedChangeListener(change);

        //-----

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister broadcast listeners
    }

    private void sendData(String message) {
        if (btSocket != null && btSocket.isConnected()) {
            byte[] msgBuffer = message.getBytes();

            Log.d(TAG, "...Sending data: " + message + "...");

            try {
                outStream.write(msgBuffer);
            } catch (IOException e) {
                String msg = ".\n\nTried writting on outStream. Check MAC / SPP UUID: \n\n";
                errorExit("Fatal Error", msg);
            }
        }
    }

    private void connect() {
        Log.d(TAG, "CONNECT()");
        Log.d(TAG, "getting device from mac adress...");
        // pointer to the remote node using it's mac address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // create the connection
        try {
            Log.d(TAG, "creating socket...");
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "socket creation attempt failed: " + e.getMessage() + ".");
        }

        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "connecting to Remote...");
        try {
            btSocket.connect();
            Log.d(TAG, "connection established and data link opened...");
        } catch (IOException e) {
            try {
                btSocket.close();
                Log.d(TAG, "connection NOT stablished...");
            } catch (IOException e2) {
                errorExit("Fatal Error", "unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        if(btSocket.isConnected()) {
            try {
                stateText.setText("connected");
                outStream = btSocket.getOutputStream();
                Log.d(TAG, "outstream created...");
                pairButton.setEnabled(false);
            } catch (IOException e) {
                errorExit("Fatal Error", "output stream creation failed:" + e.getMessage() + ".");
            }
        }
        else
            Log.d(TAG, "NOT CONNECTED...");
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "ONPAUSE()");
        if (outStream != null) {
            try {
                outStream.flush();
                Log.d(TAG, "stream flushed");
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try {
            btSocket.close();
            Log.d(TAG, "socket closed");
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "ONRESUME()");
        //connect();
        pairButton.setEnabled(true);
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on

        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth is enabled...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}