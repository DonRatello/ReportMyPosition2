package com.czernecki.marcin.reportmyposition;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    Button btnOn;
    Button btnOff;
    TextView txtStatus;
    TextView txtPositionLatitude;
    TextView txtPositionLongitude;
    EditText txtInterval;
    EditText txtContactNumber;
    GoogleApiClient googleApiClient;

    int interval;
    Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Initialize();
    }

    public void Initialize() {
        btnOn = (Button) findViewById(R.id.btnOn);
        btnOff = (Button) findViewById(R.id.btnOff);
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        txtPositionLatitude = (TextView) findViewById(R.id.txtPositionLatitude);
        txtPositionLongitude = (TextView) findViewById(R.id.txtPositionLongitude);
        txtInterval = (EditText) findViewById(R.id.txtInterval);
        txtContactNumber = (EditText) findViewById(R.id.txtContactNmbr);

        // Create an instance of GoogleAPIClient.
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    public void pickContact(View view) {
        Log.d("ReportMyPosition", "PickContact clicked");
    }

    public void btnOnClick(View view) {
        btnOn.setEnabled(false);
        btnOff.setEnabled(true);
        SetStatus("Initializing...");

        googleApiClient.connect();
    }

    public void btnOffClick(View view) {
        btnOff.setEnabled(false);
        btnOn.setEnabled(true);
        SetStatus("OFF");

        googleApiClient.disconnect();
    }

    public void SetStatus(String message) {
        Calendar time = Calendar.getInstance();
        String sTime = String.format("%02d", time.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", time.get(Calendar.MINUTE)) + ":" + String.format("%02d", time.get(Calendar.SECOND));
        txtStatus.setText(sTime + "   " + message);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (mLastLocation != null) {
            SetStatus("Position found");

            if (lastLocation != null) {
                if (mLastLocation.getLongitude() == lastLocation.getLongitude() && mLastLocation.getLatitude() == lastLocation.getLatitude()) {
                    SetStatus("Same as last");
                }
            }

            lastLocation = mLastLocation;

            txtPositionLongitude.setText(String.valueOf(mLastLocation.getLongitude()));
            txtPositionLatitude.setText(String.valueOf(mLastLocation.getLatitude()));


            SendSms(txtContactNumber.getText().toString(), BuildMessageForSms());

            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("ReportMyPosition", "Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public String BuildMessageForSms() {
        String googleLink = "http://www.google.com/maps/place/{lat},{lng}";
        googleLink = googleLink.replace("{lat}", String.valueOf(lastLocation.getLatitude())).replace("{lng}", String.valueOf(lastLocation.getLongitude()));
        Log.d("ReportMyPosition", "SMS string length: " + googleLink.length());
        return googleLink;
    }

    private void SendSms(String phoneNumber, String message)
    {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }
}
