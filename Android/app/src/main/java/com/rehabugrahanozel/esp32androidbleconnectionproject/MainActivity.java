package com.rehabugrahanozel.esp32androidbleconnectionproject;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{
    public final int  REQUEST_ENABLE_BT =1;
    public final int SELECT_PAIRED_DEVICE = 2;
    public final int SELECT_DISCOVERED_DEVICE = 2;

    public static BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> arrayAdapter;
    private String macConnectedDevice;   //mac number of device

    //UI
    static TextView tvStatusMessage;

    //Threads
    static ConnectionThread connect;
    private static int selectedButtonPos;
    private static String btReceivedMessage;

    static ArrayAdapter<String> adapter;
    private static String [] spinnerItems = new String[]{"0(off)", "1(off)", "2(off)", "3(off)",
            "4(off)", "5(off)", "12(off)", "13(off)", "14(off)", "15(off)", "16(off)", "17(off)",
            "18(off)", "19(off)", "21(off)", "22(off)", "23(off)", "25(off)", "26(off)", "27(off)",
            "32(off)", "33(off)", "34(off)", "36(off)", "39(off)"};
    private static Button ledControlBtn;


    private static String ledStatus = "";

    static boolean isConnected = false;

    static boolean isRequestSent = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ledControlBtn = findViewById(R.id.button_Send);
        // UI
        tvStatusMessage = findViewById(R.id.statusMessage);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Spinner spinner = findViewById(R.id.spinner);
        adapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item,
                        spinnerItems);
        adapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // Bir öğe seçildi. kullanarak seçilen öğeyi geri alabilirsiniz.
                // getSelectedItem()

                Toast.makeText(getApplicationContext(),adapterView.getSelectedItem().toString(),Toast.LENGTH_LONG).show();
                selectedButtonPos = adapterView.getSelectedItemPosition();
                if (currentPinStatus().equals("on")) {
                    Log.e("Current Status", "on");
                    ledControlBtn.setBackgroundColor(Color.GREEN);
                } else {
                    Log.e("Current Status", "off");
                    ledControlBtn.setBackgroundColor(Color.RED);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Bir seçenek kaldırılırsa ne yapmalı
                // veya başka bir şey
            }
        });

        // Check for Bluetooth
        if(bluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // If it exists, enable
        if(!bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    //edit
//pair or select device
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show();
            }
            finish();
        }
        else if(requestCode == SELECT_PAIRED_DEVICE || requestCode == SELECT_DISCOVERED_DEVICE){
            if(resultCode == RESULT_OK){
                assert data != null;
                tvStatusMessage.setText(String.format("Selected: %s → %s",
                        data.getStringExtra("btDevName"), data.getStringExtra("btDevAddress")));
                macConnectedDevice = data.getStringExtra("btDevAddress");
                connect = new ConnectionThread(data.getStringExtra("btDevAddress"));

            }else{
                tvStatusMessage.setText(R.string.any_connected_devices);
            }
        }

        // MISSING TO CONFIGURE WHAT HAPPENS IF USER Deactivates BLUETOOTH
    }

    public void searchPairedDevices(View view){
        Intent searchPairedDevicesIntent = new Intent(this, PairedDevices.class);
        startActivityForResult(searchPairedDevicesIntent, SELECT_PAIRED_DEVICE);
    }

    public void discoverDevices(View view){
        Intent searchPairesDevicesIntent = new Intent(this, DiscoveredDevices.class);
        startActivityForResult(searchPairesDevicesIntent, SELECT_DISCOVERED_DEVICE);
    }


    public void enableVisibility(View view) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
        startActivity(discoverableIntent);
    }

    public void waitConnection(View view){
        connect = new ConnectionThread();
        connect.start();
    }

    public void connect(View view){
        Log.d("MAC", macConnectedDevice);
        connect = new ConnectionThread(macConnectedDevice);
        connect.start();
    }

    static public Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            byte[] data = bundle.getByteArray("data");
            btReceivedMessage = new String(data);
            String dataString = new String(data);

            if (dataString.equals("---N"))
                tvStatusMessage.setText("A connection error has occurred");
            else if (dataString.equals("---S"))
                tvStatusMessage.setText("Connected");
            else if (dataString.equals("---TO"))
                tvStatusMessage.setText("Connection Timeout");
            //else tvStatusMessage.setText(new String(data)); //BLUETOOTH MESSAGE RECEIPT
            Log.e("bluetooth message", dataString);
            if (dataString.equals("---S"))
                statusRequest();
            if (dataString.contains("initialStatus"))
                setSpinnerValues(13);
            if (dataString.contains("newStatus"))
                setSpinnerValues(9);
            if (isRequestSent) {
                if (dataString.equals("on")) {
                    updateUi("on)");
                    ledControlBtn.setBackgroundColor(Color.GREEN);
                } else if (dataString.equals("off")) {
                    updateUi("off)");
                    ledControlBtn.setBackgroundColor(Color.RED);
                }
            }
        }
    };
    public static void statusRequest() {
        connect.write("$$".getBytes());
    }
    public static void turnOnAllThePins(View v) {
        connect.write("&&".getBytes());
        ledControlBtn.setBackgroundColor(Color.GRAY);
    }
    public static void turnOffAllThePins(View v) {
        connect.write("%%".getBytes());
        ledControlBtn.setBackgroundColor(Color.GRAY);
    }
    public static void setSpinnerValues(int size) {
        ledStatus = btReceivedMessage;
        isConnected = true;
        ledStatus = btReceivedMessage;
        Log.e("led Status", ledStatus);
        for (int i = 0; i < spinnerItems.length; i++) {
            String temp = spinnerItems[i];
            String newString = "";

            for (int j = 0; j < temp.length(); j++) {
                if (temp.toCharArray()[j] == '(') {
                    if (ledStatus.toCharArray()[i + size] == '1') {
                        newString = newString + temp.toCharArray()[j];
                        newString = newString + "on)";
                        break;
                    } else {
                        newString = newString + temp.toCharArray()[j];
                        newString = newString + "off)";
                        break;
                    }
                } else {
                    newString = newString + temp.toCharArray()[j];
                }
            }
            spinnerItems[i] = newString;
            adapter.notifyDataSetChanged();
            if (currentPinStatus().equals("on")) {
                Log.e("Current Status", "on");
                ledControlBtn.setBackgroundColor(Color.GREEN);
            } else {
                Log.e("Current Status", "off");
                ledControlBtn.setBackgroundColor(Color.RED);
            }

            Log.e("new spinner item ", newString);
        }

    }
    public static String currentPinStatus(){
        String statusString = "";
        boolean flag = false;
        String itemForStatus = spinnerItems[selectedButtonPos];
        for (int i = 0; i < itemForStatus.length(); i++) {
            if (itemForStatus.toCharArray()[i] == ')') {
                break;
            }
            if (flag) {
                statusString = statusString + itemForStatus.toCharArray()[i];
            }
            if (itemForStatus.toCharArray()[i] == '(') {
                flag = true;
            }
        }
        return statusString;
    }

    public static void updateUi(String s) {
        String itemForStatus = spinnerItems[selectedButtonPos];
        Log.e("spinner item changed", "");
        for (int i = 0; i < spinnerItems.length; i++) {
            if (spinnerItems[i].equals(itemForStatus)) {

                String temp = spinnerItems[i];
                String newString = "";

                for (int j = 0; j < temp.length(); j++) {
                    if (temp.toCharArray()[j] == '(') {
                        newString = newString + temp.toCharArray()[j];
                        newString = newString + s;
                        break;
                    } else {
                        newString = newString + temp.toCharArray()[j];
                    }
                }
                spinnerItems[i] = newString;
                adapter.notifyDataSetChanged();
                if (currentPinStatus().equals("on")) {
                    Log.e("Current Status", "on");
                    ledControlBtn.setBackgroundColor(Color.GREEN);
                } else {
                    Log.e("Current Status", "off");
                    ledControlBtn.setBackgroundColor(Color.RED);
                }

                Log.e("new spinner item ", newString);
            }
        }
        isRequestSent = false;
    }
    public void changeLedStatus(View view) {
        if (isConnected){
            String itemForStatus = spinnerItems[selectedButtonPos];
            Log.e("bt message", btReceivedMessage);
            String statusString = currentPinStatus();
            Log.e("---------------------------", "button clicked");
            Log.e("status before ", statusString);
            if (statusString.equals("off")) {
                turnOnLight();
                isRequestSent = true;
                Log.e("turn on request send!", "");
            } else if (statusString.equals("on")) {
                turnOffLight();
                isRequestSent = true;
                Log.e("turn off request send!", "");
            }
        }
        Log.e("process status ","end");
    }

    public static void turnOffLight() {
        String itemForStatus = spinnerItems[selectedButtonPos];
        if (tvStatusMessage.getText().toString().equals("Connected")) {
            String messageString = "";
            for (int i = 0; i < itemForStatus.length(); i++){
                if(itemForStatus.toCharArray()[i] != '(') {
                    messageString = messageString + itemForStatus.toCharArray()[i];
                } else {
                    break;
                }
            }
            messageString = messageString + "-" + "0";
            byte[] data = messageString.getBytes();
            connect.write(data);
            ledControlBtn.setBackgroundColor(Color.GRAY);
        }
    }
    public static void turnOnLight() {
        String itemForStatus = spinnerItems[selectedButtonPos];
        if (tvStatusMessage.getText().toString().equals("Connected")) {
            String messageString = "";
            for (int i = 0; i < itemForStatus.length(); i++){
                if(itemForStatus.toCharArray()[i] != '(') {
                    messageString = messageString + itemForStatus.toCharArray()[i];
                } else {
                    break;
                }
            }
            messageString = messageString + "-" + "1";
            byte[] data = messageString.getBytes();
            connect.write(data);
            ledControlBtn.setBackgroundColor(Color.GRAY);
        }
    }





    //Menu
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

}