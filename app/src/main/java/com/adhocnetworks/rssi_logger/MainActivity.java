package com.adhocnetworks.rssi_logger;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Permission variables
    public static final int PERMISSION_REQUEST_CODE = 101;
    private final ArrayList<Integer> RSSI_list = new ArrayList<Integer>(); // Keeps track of the RSSI values
    private RSSIScanner scanner;
    // Keeps track of the amount of samples
    private int numSamples = 0;
    private boolean hasPerms = false; // Check whether RSSI preview can start
    private String saveDescription = ""; // Custom description by the user
    private boolean measureIsOn = false; // Measurement has started or not
    // WiFi variables
    private WifiManager wifiManager;
    private WifiInfo wifiInfo;
    // Component variables
    private TextView sampleCount, values, previewValue;
    private Button buttonStart, buttonClear, buttonSave;

    private int getWiFiRSSI() {
        wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getRssi();
    }

    private void addRSSIMeasurement(int rssi) {
        RSSI_list.add(rssi);
        numSamples++;

        sampleCount.setText("Amount of samples: " + numSamples);
        values.setText(String.valueOf(rssi));
    }

    /**
     * The RSSI value which is not logged, only serves as preview
     * @param rssi the integer value received from the WiFi manager
     */
    private void updateRSSIPreview(int rssi) {
        previewValue.setText(String.valueOf(rssi));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create textviews
        sampleCount = (TextView) findViewById(R.id.textView_SampleCount);
        values = (TextView) findViewById(R.id.textView_RSSI);
        previewValue = (TextView) findViewById(R.id.textView_RSSI_preview);

        // Create buttons
        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonClear = (Button) findViewById(R.id.buttonClear);
        buttonSave = (Button) findViewById(R.id.buttonSave);

        // Set WiFi manager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Create click listeners
        buttonStart.setOnClickListener(this);
        buttonClear.setOnClickListener(this);
        buttonSave.setOnClickListener(this);

        // Create recurring measurement + update
        scanner = new RSSIScanner(this, () -> {
            int RSSI = getWiFiRSSI();
            if (measureIsOn) {
                this.addRSSIMeasurement(RSSI);
            }
            this.updateRSSIPreview(RSSI);
            Log.d("Runnable", "RSSI measured: " + RSSI);
        });

        // Check if permission has been granted already, if not requests for permission
        hasPerms = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasPerms) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }

        // Needs WiFi access which is given before app boots
        updateRSSIPreview(getWiFiRSSI());
    }

    @Override
    protected void onResume() {
        super.onResume();
        scanner.startScanning(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanner.stopScanning(this);
    }

    // Requests for permission to write to external storage (to store the RSSI values)
    // If the user does not give permission, the app can not function and will exit
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // If request is cancelled, the result array is empty.
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "Permission has been granted");
            } else {
                finish();
                System.exit(0);
            }
        }
    }

    // A writer that will save the data to a .txt file that can be opened on a computer
    private void writeToFile(String data, String filename) {
        try {
            File file = new File(this.getExternalFilesDir(null), filename + ".txt");
            FileOutputStream fileOutput = new FileOutputStream(file);
            OutputStreamWriter os = new OutputStreamWriter(fileOutput);
            os.write(data);
            os.flush();
            os.close();
            Toast.makeText(this, "Data is saved as: " + filename + ".txt", Toast.LENGTH_LONG).show();
            Log.d("Data", data);
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    // onClick events
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonStart: { // Turn measuring on or off
                if (!measureIsOn) {
                    Log.d("Runnable", "Runnable started");
                    buttonStart.setText("Stop Measuring");
                    buttonSave.setEnabled(false);
                    buttonClear.setEnabled(false);
                    measureIsOn = true;
                } else {
                    Log.d("Runnable", "Runnable stopped");
                    buttonStart.setText("Start Measuring");
                    buttonSave.setEnabled(true);
                    buttonClear.setEnabled(true);
                    measureIsOn = false;
                }
                break;
            }
            case R.id.buttonClear: { // Clear data
                Log.d("Button", "Data cleared");
                RSSI_list.clear();
                numSamples = 0;
                sampleCount.setText("Amount of samples: " + numSamples);
                values.setText(String.valueOf(0));
                break;
            }
            case R.id.buttonSave: { // Save data
                Log.d("Runnable", "Save button clicked");

                // Creates a prompt for the user to write the description
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Input filename description");

                // Set up the input
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", (dialog, which) -> {
                    saveDescription = input.getText().toString();
                    DateFormat df = new SimpleDateFormat("yyMMdd-HHmm");
                    writeToFile(RSSI_list.toString(), df.format(new Date()) + "_" + saveDescription);
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

                builder.show();

                break;
            }
            default:
                break;
        }
    }
}
