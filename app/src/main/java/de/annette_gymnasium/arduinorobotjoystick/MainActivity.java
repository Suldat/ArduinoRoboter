package de.annette_gymnasium.arduinorobotjoystick;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.bluetooth.*;
import android.widget.Toast;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

import java.io.IOException;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityClass";

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
    DecimalFormat dfOffset = new DecimalFormat("0.00", symbols);

    //BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
    Button btnBluetooth;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    // Well known SPP UUID
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Insert your server's MAC address
    private static String address = "98:D3:31:40:21:44";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "In onCreate()");

        dfOffset.setRoundingMode(RoundingMode.HALF_UP);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialisierung der Views
        final TextView textView = (TextView) findViewById(R.id.textView);
        Joystick joystick = (Joystick) findViewById(R.id.joystick);
        Button buttonAufnehmen = (Button) findViewById(R.id.buttonAufnehmen);
        final Button buttonStick = (Button) findViewById(R.id.buttonStick);
        final GradientDrawable bgShape = (GradientDrawable) buttonStick.getBackground().getCurrent();
        btnBluetooth = (Button) findViewById(R.id.buttonBluetooth);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        /*
        String status = "";

        if(bluetooth == null) {
            new AlertDialog.Builder(this).setTitle("Nicht kompatibel")
                    .setMessage("Dein Handy unterstützt kein Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                           System.exit(0);
                        }
                    }).setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            if(bluetooth.isEnabled()) {
                String myDeviceAdress = bluetooth.getAddress();
                String myDeviceName = bluetooth.getName();
                status = myDeviceName + " : " +myDeviceAdress;
            } else {
                status = "Bluetooth ist nicht eingeschaltet";
            }
        }

        Toast.makeText(this, status, Toast.LENGTH_LONG).show();
        */
        //der Listener vom Joystick
        joystick.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {
                textView.setText("onDown");
            }

            @Override
            public void onDrag(float degreesExact, float offsetExact) {

                int degrees = Math.round(degreesExact);
                float offset = Float.parseFloat(dfOffset.format((double) offsetExact));


                textView.setText("onDrag. degrees: " +degrees +"; offset: " +offset);
                //sendData(degrees +";" +offset +"#");
            }

            @Override
            public void onUp() {
                textView.setText("onUp");
            }
        });

        buttonAufnehmen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bgShape.setColor(Color.parseColor("#E53935"));

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bgShape.setColor(Color.parseColor("#80CBC4"));
                    }
                },1000);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...In onResume - Attempting client connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting to Remote...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection established and data link opened...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Creating Socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void errorExit(String title, String message){
        Toast msg = Toast.makeText(getBaseContext(),
                title + " - " + message, Toast.LENGTH_SHORT);
        msg.show();
        finish();
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on

        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Schwerwiegender Fehler", "Bluetooth wird nicht unterstützt. App wird beendet");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ist eingeschaltet...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Daten werden gesendet: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
            msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
        }
    }
}
