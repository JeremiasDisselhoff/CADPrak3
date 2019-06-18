package com.example.cadprak3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.sql.Time;
import java.text.DecimalFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    //GUI Elemente
    EditText editTimeToArrive;
    TextView txtPosInfo;
    TextView txtTimeInfo;
    TextView txtStatusInfo;
    TextView txtSensorInfo;

    //Sensoren
    SensorManager sensorManager;
    LocationManager locationManager;

    //Runnable
    Runnable runnable;
    Handler handler;

    DecimalFormat decimalFormat;

    int timeToArrive;
    int initPosFlag = 0;
    int sensorFlag = 0;
    int timeToGo;
    float distance;
    Location initLocation;
    Location finalLocation;
    Location hsLocation;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //GUI Elemente
        editTimeToArrive = findViewById(R.id.editTimeToArrive);
        txtPosInfo = findViewById(R.id.txtPosInfo);
        txtTimeInfo = findViewById(R.id.txtTimeInfo);
        txtStatusInfo = findViewById(R.id.txtStatusInfo);
        txtSensorInfo = findViewById(R.id.txtSensorInfo);

        decimalFormat = new DecimalFormat("00.00");

        //Location der Hochschule
        hsLocation = new Location("UNAVAILABLE");
        hsLocation.setLatitude(51.447561);
        hsLocation.setLongitude(7.270792);


        //############################SensorManager/Listener##############################
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        final SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                txtSensorInfo.setText("Sensor info: X: " + decimalFormat.format(event.values[0]) + " Y: " + decimalFormat.format(event.values[1]) + " Z: " + decimalFormat.format(event.values[2]));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };


        //############################LocationManager/Listener############################
        //Abfrage der Berechtigung
        if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        final LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(initPosFlag==0) {
                    txtPosInfo.setText("Position info: \nInitPos = \tlat: " + location.getLatitude() + "\n\t\t\t\t\t\t\t\t\tlong: " + location.getLongitude());
                    initLocation = location;
                    initPosFlag = 1;
                }
                if(initPosFlag==2) {
                    txtPosInfo.setText("Position info: \nFinalPos = \tlat: " + location.getLatitude() + "\n\t\t\t\t\t\t\t\t\tlong: " + location.getLongitude());
                    finalLocation = location;
                    initPosFlag = 3;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //############################Runnable############################################
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                timeToArrive = getTimeDiff(editTimeToArrive.getText().toString());
                txtTimeInfo.setText("Time info: Ankunft in " + timeToArrive + " Minuten");

                //mehr als eine Stunde bis Vorlesungsbeginn
                if(timeToArrive>60) txtStatusInfo.setText("Status info: Du hast noch Zeit");

                //getting init Position bei einer Stunde vor Beginn
                if(timeToArrive<=60 && timeToArrive>0 &&initPosFlag == 0){
                    txtStatusInfo.setText("Status info: Hole initiale Position");
                    if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                    }
                }

                //Berrechnung der Entfernung zur UNI nach initialer Position
                if(initPosFlag == 1){
                    locationManager.removeUpdates(locationListener);
                    distance = initLocation.distanceTo(hsLocation);
                    txtStatusInfo.setText("Status info: Du musst in " + Math.round(timeToArrive-(distance/1000)) + " Minuten los");
                    timeToGo = Math.round(timeToArrive-(distance/1000));
                    initPosFlag = 2;
                }

                //Anzeigen in wievielen Minuten man sich auf den Weg machen muss
                if(timeToGo>0 && timeToArrive<60){
                    timeToGo = Math.round(timeToArrive-(distance/1000));
                    txtStatusInfo.setText("Status info: Du musst in " + timeToGo + " Minuten los");
                }

                //Anzeigen dass man sich auf den Weg machen juss
                if(timeToGo<=0 && timeToArrive<60 && timeToArrive >=0){
                    txtStatusInfo.setText("Status info: Bruder muss los");
                    if(sensorFlag==0){
                        sensorManager.registerListener(sensorEventListener,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_FASTEST);
                        sensorFlag = 1;
                    }

                    //hier das Sensor Zeug
                }

                //Holen der finalen Position
                if(timeToArrive<0 && initPosFlag == 2){
                    txtStatusInfo.setText("Status info: Hole finale Position");
                    if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                    }

                    if(sensorFlag==1){
                        sensorManager.unregisterListener(sensorEventListener);
                        sensorFlag = 0;
                    }
                }

                //Überprüfen ob man da ist
                if(initPosFlag == 3){
                    locationManager.removeUpdates(locationListener);
                    distance = finalLocation.distanceTo(hsLocation);
                    if(distance<500){
                        txtStatusInfo.setText("Status info: Heyyy du bist da!!");
                    }else txtStatusInfo.setText("Status info: Heyyy du bist nicht da!!");
                    initPosFlag = 0;
                }

                //Morgen gehts weiter
                if(timeToArrive<-60)txtStatusInfo.setText("Status info: Morgen...");


                handler.postDelayed(runnable, 15000);
            }
        };
        handler.post(runnable);
    }
    private int getTimeDiff(String endTime){
        int endHours = Character.getNumericValue(endTime.charAt(0))*10 + Character.getNumericValue(endTime.charAt(1));
        int endMinutes = Character.getNumericValue(endTime.charAt(3))*10 + Character.getNumericValue(endTime.charAt(4)) + (endHours*60);

        int startHours = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int startMinutes = Calendar.getInstance().get(Calendar.MINUTE) + (startHours*60);

        int timeDiff = endMinutes - startMinutes;
        //if (timeDiff < 0) timeDiff += 24*60;

        return timeDiff;
    }
}
