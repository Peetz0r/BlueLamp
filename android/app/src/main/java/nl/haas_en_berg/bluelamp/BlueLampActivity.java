package nl.haas_en_berg.bluelamp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class BlueLampActivity extends Activity {

    private static final String TAG = "BlueLampActivity";
    private static final String PREFS_NAME = "BlueLampPreferences";
    private static final int REQUEST_ENABLE_BT = 1;
    private Spinner spinner;
    private ToggleButton toggle;
    private SharedPreferences settings;
    private String address;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private ColorPicker picker;
    private SaturationBar saturationBar;
    private ValueBar valueBar;
    private InputStream btInputStream;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        toggle = (ToggleButton) findViewById(R.id.toggle);

        picker = (ColorPicker) findViewById(R.id.picker);
        picker.addSaturationBar((SaturationBar) findViewById(R.id.saturationbar));
        picker.addValueBar((ValueBar) findViewById(R.id.valuebar));

    }

    public void updateColor(View view) {
        if (btSocket != null) {
            int color = picker.getColor();

            try {
                btSocket.getOutputStream().write(Color.red(color));
                btSocket.getOutputStream().write(Color.green(color));
                btSocket.getOutputStream().write(Color.blue(color));
                picker.setOldCenterColor(color);
            } catch (IOException e) {
                Log.e(TAG, "error sending data: " + e.getMessage());
                Log.i(TAG, "closing streams");
                try {
                    btSocket.getInputStream().close();
                    btSocket.getOutputStream().close();
                } catch (IOException e1) {
                    Log.e(TAG, "cannot close streams");
                }
                Log.i(TAG, "closing socket");
                try {
                    btSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "cannot close socket");
                }
                toggle.setChecked(false);
            }
            Log.i(TAG, "User tapped OK. Red: " + String.valueOf(Color.red(color)) + "\tGreen: " + String.valueOf(Color.green(color)) + "\tBlue: " + String.valueOf(Color.blue(color)));
        }
    }


    public void performStart(View view) {
        if (toggle.isChecked()) {
            toggle.setChecked(false);
            HashMap<String, String> map = (HashMap<String, String>) spinner.getSelectedItem();
            if (map != null) {
                toggle.setEnabled(false);
                address = map.get("address");
                Log.i(TAG, "Connecting to " + address);
                new BTConnectTask().execute();
            }
        } else {
            if (btSocket != null) {
                Log.i(TAG, "closing streams");
                try {
                    btSocket.getInputStream().close();
                    btSocket.getOutputStream().close();
                } catch (IOException e1) {
                    Log.e(TAG, "cannot close streams");
                }
                Log.i(TAG, "closing socket");
                try {
                    btSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG, "cannot close socket");
                }
                toggle.setChecked(false);
            }
        }
    }

    private class BTConnectTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("address", address);
            editor.commit();

            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "socket create failed: " + e.getMessage() + ".");
                return false;
            }

            btAdapter.cancelDiscovery();

            try {
                btSocket.connect();
                Log.i(TAG, "Connection ok");
            } catch (IOException e) {
                try {
                    btSocket.close();
                    Log.e(TAG, "connection failure: " + e.getMessage() + ".");
                    return false;
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close socket during connection failure: " + e2.getMessage() + ".");
                    return false;
                }
            }

            try {
                btInputStream = btSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "unable to obtain input stream: " + e.getMessage() + ".");
            }

            handler.post(checkColor);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            toggle.setChecked(result);
            toggle.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        populateSpinner();
        toggle.performClick();

        super.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        Button ok = (Button) findViewById(R.id.ok);
        ok.getLayoutParams().width = (int) Math.round(picker.getWidth() * 0.4);
        ok.getLayoutParams().height = (int) Math.round(picker.getHeight() * 0.4);
        ok.setShadowLayer((float) 3.5 * getResources().getDisplayMetrics().density, 0, 0, Color.BLACK);
    }

    private Runnable checkColor = new Runnable() {
        @Override
        public void run() {
            new AsyncTask<Boolean, Boolean, Boolean>() {
                public Integer result;

                @Override
                protected Boolean doInBackground(Boolean... booleans) {
                    if (btSocket != null && btInputStream != null) {
                        try {
                            byte[] color = new byte[3];
                            int num_bytes = btInputStream.read(color);
                            if (num_bytes == 3) {
                                Log.d(TAG, "Got three bytes. Red: " + String.valueOf(color[0] & 0xff) + "\tGreen: " + String.valueOf(color[1] & 0xff) + "\tBlue: " + String.valueOf(color[2] & 0xff));
                                result = Color.rgb(color[0] & 0xff, color[1] & 0xff, color[2] & 0xff);
                            } else {
                                Log.d(TAG, "could not read exactly 3 bytes during checkColor.");
                            }
                            return true;
                        } catch (IOException e) {
                            Log.e(TAG, "unable to read from socket during checkColor: " + e.getMessage() + ".");
                            try {
                                btSocket.close();
                            } catch (IOException e2) {
                                Log.e(TAG, "unable to close socket during connection failure: " + e2.getMessage() + ".");
                            }
                        }
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    if (success) {
                        if (result != null) {
                            picker.setOldCenterColor(result);
                        }
                        handler.postDelayed(checkColor, 100);
                    }
                    toggle.setChecked(success);
                    super.onPostExecute(success);
                }
            }.execute();

        }
    };

    @Override
    protected void onPause() {
        toggle.setChecked(false);
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "unable to close socket during onPause: " + e.getMessage() + ".");
            }
        }

        super.onPause();
    }

    private void populateSpinner() {
        spinner = (Spinner) findViewById(R.id.spinner1);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            Log.e(TAG, "Bluetooth not supported");
            Log.i(TAG, "exiting");
            return;
        } else {
            if (btAdapter.isEnabled()) {
                Log.i(TAG, "Bluetooth ON");
            } else {
                Log.i(TAG, "Bluetooth OFF");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return;
            }
        }

        String address = settings.getString("address", "");
        int position = 0;

        ArrayList<Map<String, String>> spinnerArray = new ArrayList<Map<String, String>>();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Map<String, String> data = new HashMap<String, String>(2);
                data.put("name", device.getName());
                data.put("address", device.getAddress());
                if (device.getAddress().equals(address)) {
                    position = spinnerArray.size();
                }
                spinnerArray.add(data);
            }
        }
        SimpleAdapter adapter = new SimpleAdapter(this, spinnerArray, android.R.layout.simple_list_item_2, new String[]{"name", "address"}, new int[]{android.R.id.text1, android.R.id.text2});
        spinner.setAdapter(adapter);
        spinner.setSelection(position);
    }
}
