/*
HandMon
v3
        - Registers the radio signal level of serving and neighbour cells
        - Registers time, location and speed of  handset
        - Terminates by pressing the screen.
        - Stores all data as a single JSON file (v2)
        - POSTs data to remote elastic search
v3.1
        - Stores a single ES file, that can be BULK POSTed to elastic search using curl. Note //temp for swapping between v3 and v3.1
v3.2
        - 5G SA support
@Author: Navid Solhjoo, navid.solhjoo@bristol.ac.uk, August 2020
*/


package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
// import android.app.Activity;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.ToneGenerator;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telecom.TelecomManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthNr;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;

import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

// import com.example.myapplication.archive.R;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.common.ConnectionQuality;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.AnalyticsListener;
import com.androidnetworking.interfaces.ConnectionQualityChangeListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;


import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED;
import static android.telephony.PhoneStateListener.LISTEN_CELL_LOCATION;
import static com.example.myapplication.AppConstants.FILE_DURATION;
import static com.example.myapplication.AppConstants.ORCHESTRATOR_IP;
import static com.example.myapplication.AppConstants.ORCHESTRATOR_PORT;
import static java.text.DateFormat.getDateInstance;


public class SignalStrengthActivity extends AppCompatActivity {

    private static final String TAG = SignalStrengthActivity.class.getSimpleName();

    TextView cellIDNetworkType;
    TextView cellIDTextView;
    TextView cellMccTextView;
    TextView cellMncTextView;
    TextView cellPciTextView;
    TextView cellTacTextView;
    TextView cellCqiTextView;
    TextView cellRsrpTextView;
    TextView cellRsrqTextView;
    TextView cellRssiTextView;
    TextView cellRssnrTextView;
    TextView cellTaTextView;
    TextView cellDbmTextView;
    TextView cellOpTextView;
    TextView cellRATTextView;
    TextView cellBwTextView;
    TextView cellEarTextView;
    TextView cellStatusTextView;
    TextView cellAsuTextView;
    TextView cellLevelTextView;
    TextView cellNeighIDTextView;
    TextView cellNeighMccTextView;
    TextView cellNeighMncTextView;
    TextView cellNeighPciTextView;
    TextView cellNeighTacTextView;
    TextView cellNeighCqiTextView;
    TextView cellNeighRsrpTextView;
    TextView cellNeighRsrqTextView;
    TextView cellNeighRssiTextView;
    TextView cellNeighRssnrTextView;
    TextView cellNeighTaTextView;
    TextView cellNeighDbmTextView;
    TextView cellNeighOpTextView;
    TextView cellNeighBwTextView;
    TextView cellNeighEarTextView;
    TextView cellNeighStatusTextView;
    TextView cellNeighAsuTextView;
    TextView cellNeighLevelTextView;
    TextView fileNameTextView;
    TextView CountLoopTextView;
    TextView cellLatTextView;
    TextView cellLongTextView;
    TextView cellAltTextView;
    TextView cellAccTextView;
    TextView cellBearingTextView;
    TextView cellBearAccTextView;
    TextView cellBundleLocTextView;
    TextView cellTimeTextView;
    TextView cellelapsedRTView;
    TextView cellelapsedRTNanoView;
    TextView cellSpeedTextView;
    TextView cellSpeedAccTextView;
    TextView cellProviderTextView;
    TextView cellRAT;
    String[] result = {null};
    int networkType;
    List<CellInfo> cellInfoList;
    List<CellSignalStrength> cellSignalStrengthList;
    int handoverflag = 0, numFilesT = 1, numFilesES = 1, cellPCINR;
    int cellSig, cellID, cellPci, cellBw, cellEar, cellTac, cellDbm, cellCqi, cellRsrp, cellRsrq, cellRssi, cellRssnr, cellTa, cellStatus, cellAsu, cellssRsrpNR, cellcsiRsrpNR, cellBand[], cellLevel = 0, num_records = 0, num_records_ES = 0;
    String cellMcc, cellMnc, cellOp, cellNeighMcc, cellNeighMnc, cellNeighOp, provider, bundleLoc;
    int count_loop = 0, cellNeighSig, cellNeighID, cellNeighPci, cellNeighBw, cellNeighEar, cellNeighTac, cellNeighDbm, cellNeighCqi, cellNeighRsrp, cellNeighRsrq, cellNeighRssi, cellNeighRssnr, cellNeighTa, cellNeighStatus, cellNeighLevel, cellNeighAsu = 0, prevCellPci;
    double latitude, longitude, altitude, elapsedRTNano;
    float speed, speedAccuracy, accuracy, bearing, bearingAccuracy;
    long time, elapsedRT, prevElapsedRT, prevTime;
    TelephonyManager tm;
    private static Context context;
    SignalStrengthListener signalStrengthListener;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean isGPS = false;
    JSONObject jsonObject;
    JSONObject[] jsonObjects;
    JSONArray jsonArray;
    File file, filePath, filePathT, filePathES, fileT, fileES, file_rT;
    String file_r_name, file_r_nameT, file_r_nameES, RadioData, tempRadioDataT, RadioDataT, RadioDataES;
    String RadioDataTArray = "", RadioDataESArray = "";
    FileWriter fileWriter, fileWriterT, fileWriterES;
    FileOutputStream outputStream;
    BufferedWriter bufferedWriter, bufferedWriterT, bufferedWriterES;
    Calendar rightNow;
    long now_ms, prev_ms;
    int ri = 0;
    Date today;
    String current_time, time_ms_slice;
    SimpleDateFormat formatter;
    Thread Thread1 = null;
    SendHandover _sendHandover = null;
    String current_time_ISO8601;
    //TelephonyDisplayInfo telephonyDisplayInfo = null;
    String strRAT = "";
    CellSignalStrengthNr cellSignalStrengthNr = null;
    CellInfoNr cellInfoNr;

    // CellInfoNr nrCellInfo = new CellInfoNr();

    public SignalStrengthActivity() {
    }

    public SendHandover getSendHandover() {
        if (this._sendHandover == null) {
            this._sendHandover = new SendHandover(ORCHESTRATOR_IP, ORCHESTRATOR_PORT, this);
        }
        return this._sendHandover;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setup display
        this.setContentView(R.layout.signal_strength);
        cellRATTextView = findViewById(R.id.cellRATTextView);
        CountLoopTextView = findViewById(R.id.CountLoopTextView);
        fileNameTextView = findViewById(R.id.fileNameTextView);
        cellIDTextView = findViewById(R.id.cellIDTextView);
        cellMccTextView = findViewById(R.id.cellMccTextView);
        cellMncTextView = findViewById(R.id.cellMncTextView);
        cellPciTextView = findViewById(R.id.cellPciTextView);
        cellTacTextView = findViewById(R.id.cellTacTextView);
        cellOpTextView = findViewById(R.id.cellOpTextView);
        cellBwTextView = findViewById(R.id.cellBwTextView);
        cellEarTextView = findViewById(R.id.cellEarTextView);
        cellTaTextView = findViewById(R.id.cellTaTextView);
        cellDbmTextView = findViewById(R.id.cellDbmTextView);
        cellCqiTextView = findViewById(R.id.cellCqiTextView);
        cellRsrpTextView = findViewById(R.id.cellRsrpTextView);
        cellRsrqTextView = findViewById(R.id.cellRsrqTextView);
        cellRssiTextView = findViewById(R.id.cellRssiTextView);
        cellRssnrTextView = findViewById(R.id.cellRssnrTextView);
        cellAsuTextView = findViewById(R.id.cellAsuTextView);
        cellNeighIDTextView = findViewById(R.id.cellNeighIDTextView);
        cellNeighMccTextView = findViewById(R.id.cellNeighMccTextView);
        cellNeighMncTextView = findViewById(R.id.cellNeighMncTextView);
        cellNeighPciTextView = findViewById(R.id.cellNeighPciTextView);
        cellNeighTacTextView = findViewById(R.id.cellNeighTacTextView);
        cellNeighOpTextView = findViewById(R.id.cellNeighOpTextView);
        cellNeighBwTextView = findViewById(R.id.cellNeighBwTextView);
        cellNeighEarTextView = findViewById(R.id.cellNeighEarTextView);
        cellNeighTaTextView = findViewById(R.id.cellNeighTaTextView);
        cellNeighDbmTextView = findViewById(R.id.cellNeighDbmTextView);
        cellNeighCqiTextView = findViewById(R.id.cellNeighCqiTextView);
        cellNeighRsrpTextView = findViewById(R.id.cellNeighRsrpTextView);
        cellNeighRsrqTextView = findViewById(R.id.cellNeighRsrqTextView);
        cellNeighRssiTextView = findViewById(R.id.cellNeighRssiTextView);
        cellNeighRssnrTextView = findViewById(R.id.cellNeighRssnrTextView);
        cellNeighAsuTextView = findViewById(R.id.cellNeighAsuTextView);
        cellLatTextView = findViewById(R.id.cellLatTextView);
        cellLongTextView = findViewById(R.id.cellLongTextView);
        cellAltTextView = findViewById(R.id.cellAltTextView);
        cellAccTextView = findViewById(R.id.cellAccTextView);
        cellBearingTextView = findViewById(R.id.cellBearingTextView);
        cellBearAccTextView = findViewById(R.id.cellBearAccTextView);
        cellBundleLocTextView = findViewById(R.id.cellBundleLocTextView);
        cellTimeTextView = findViewById(R.id.cellTimeTextView);
        cellelapsedRTView = findViewById(R.id.cellelapsedRTView);
        cellelapsedRTNanoView = findViewById(R.id.cellelapsedRTNanoView);
        cellSpeedTextView = findViewById(R.id.cellSpeedTextView);
        cellSpeedAccTextView = findViewById(R.id.cellSpeedAccTextView);
        cellProviderTextView = findViewById(R.id.cellProviderTextView);
        num_records = 0;
        JSONArray jsonArray = new JSONArray();



        //start the signal strength listener
        signalStrengthListener = new SignalStrengthListener();
        ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).listen(signalStrengthListener, SignalStrengthListener.LISTEN_SIGNAL_STRENGTHS | LISTEN_CELL_LOCATION ); // | LISTEN_CALL_STATE | LISTEN_CELL_INFO | LISTEN_SERVICE_STATE);
        //((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).listen(signalStrengthListener, SignalStrengthListener.LISTEN_SIGNAL_STRENGTHS | LISTEN_CELL_LOCATION);
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        SignalStrengthActivity.context = getApplicationContext();

//permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 3);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 5);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)!= PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 6);


//location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10);// * 1000); // 10 seconds
        locationRequest.setFastestInterval(5); // * 1000); // 5 seconds

        new GpsUtils(this).turnGPSOn(new GpsUtils.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                // turn on GPS
                isGPS = isGPSEnable;
            }
        });

        locationCallback = new LocationCallback() {
			@RequiresApi(api = Build.VERSION_CODES.O)										 
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        longitude = location.getLongitude();
                        latitude = location.getLatitude();
                        altitude = location.getAltitude();
                        if (location.hasSpeedAccuracy() && location.hasSpeedAccuracy()) {
                            speed = location.getSpeed();
                            speedAccuracy = location.getSpeedAccuracyMetersPerSecond();
                        }
                        time = System.currentTimeMillis();
                        if (location.hasAccuracy()) {
                            accuracy = location.getAccuracy();
                        }
                        if (location.hasBearing()) {
                            bearing = location.getBearing();
                        }
                        if (location.hasBearingAccuracy()) {
                            bearingAccuracy = location.getBearingAccuracyDegrees();
                        }
                        prevElapsedRT = elapsedRT;
                        elapsedRT = location.getElapsedRealtimeNanos();
                        provider = location.getProvider();
                        cellLatTextView.setText(String.valueOf(latitude));
                        cellLongTextView.setText(String.valueOf(longitude));
                        cellAltTextView.setText(String.valueOf(altitude));
                        cellAccTextView.setText(String.valueOf(accuracy));
                        cellBearingTextView.setText(String.valueOf(bearing));
                        cellBearAccTextView.setText(String.valueOf(bearingAccuracy));
                        cellBundleLocTextView.setText(String.valueOf(bundleLoc));
                        cellTimeTextView.setText(String.valueOf(time));
                        cellelapsedRTView.setText(String.valueOf(elapsedRT));
                        cellelapsedRTNanoView.setText(String.valueOf(elapsedRTNano));
                        cellSpeedTextView.setText(String.valueOf(speed));
                        cellSpeedAccTextView.setText(String.valueOf(speedAccuracy));
                        cellProviderTextView.setText(String.valueOf(provider));
                    }
                }
            }
        };

        //get location
        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

        try {
            jsonObject = new JSONObject();
        } catch (Exception e) {
            throw new NullPointerException("Cannot allocate JSONObject");
        }
/*
        try {
            cellInfoList = tm.getAllCellInfo();
        } catch (Exception e) {
            Log.d("SignalStrength", "1: " + e);
        }
        try {
            networkType = tm.getDataNetworkType();

            for (CellInfo cellInfo : cellInfoList) {
                cellInfoList = tm.getAllCellInfo();
                if (cellInfo instanceof CellInfoNr) {
                    strRAT = "  - 5GNR";
                }
                if (cellInfo instanceof CellInfoLte) {

                    if (isNSAConnected(tm))
                    {
                        strRAT = " - NSA " + networkType;
                        if (cellInfo.isRegistered())
                        {
                            getCellInfoLTE(cellInfo);
                        }
                    } else
                    {
                        strRAT = " - LTE " + networkType;
                    }
                    if (cellInfo.isRegistered()) {
                        prevCellPci = cellPci;
                        getCellInfoLTE(cellInfo);
                    } else {
                        getNeighbourCellInfoLTE(cellInfo);
                        break;
                    }
                }
//                if (cellInfo instanceof CellInfoLte) {
//                    strRAT = " - LTE";
//                    if (cellInfo.isRegistered()) {
//                        getCellInfoLTE(cellInfo);
//                    } else {
//                        getNeighbourCellInfoLTE(cellInfo);
//                        break;
//                    }
//                }
            }
        } catch (Exception e) {
            Log.d("SignalStrength", "2: " + e);
        }
*/


        formatter = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss", Locale.ENGLISH);
        today = new Date();
        current_time = formatter.format(today);

        ZonedDateTime nowISO8601 = ZonedDateTime.now();
        current_time_ISO8601 = nowISO8601.format(DateTimeFormatter.ISO_INSTANT);

        //create main data file on handset
        filePath = context.getExternalFilesDir(null);
        file_r_name = "/radio_" + current_time + ".txt";
        fileNameTextView.setText("radio_" + current_time);

        File[] filesInDir;

        //temp directory

        filePathT = new File(filePath + File.separator + "Temp");
        filesInDir = filePathT.listFiles();

        if (filesInDir == null) {
            boolean bool = filePathT.mkdir();  //whether it exists or not.
        } else {
            for (File value : filesInDir) {
                value.delete();
            }
        }

        // elastic search file directory
        filePathES = new File(filePath + File.separator + "ES");
        filesInDir = filePathES.listFiles();

        if (filesInDir == null) {
            boolean bool = filePathES.mkdir();  //whether it exists or not.
        } else {
            for (File value : filesInDir) {
                value.delete();
            }
        }

        try {
            file = new File(context.getExternalFilesDir(null), file_r_name);
            fileWriter = new FileWriter(file, true);
            bufferedWriter = new BufferedWriter(fileWriter);
        } catch (IOException e) {
            Log.d("Cannot create file", "4: " + e);
        }

        InitNetworking();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (signalStrengthListener != null) {
                tm.listen(signalStrengthListener, SignalStrengthListener.LISTEN_NONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onDestroy() {
        super.onDestroy();
        try {
            if (signalStrengthListener != null) {
                tm.listen(signalStrengthListener, SignalStrengthListener.LISTEN_NONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                }
            }
//            default: {
//                if (!(grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED))
//                {
//                    Toast.makeText(getApplicationContext(),
//                            "Permission not granted", Toast.LENGTH_LONG).show();
//                }
//                }
        }
    }


    public void closeApp(View view) {
        try {
            RadioData = RadioData + ']';
            bufferedWriter.write(RadioData);
            bufferedWriter.close();
            bufferedWriterT.write(RadioDataT);
            bufferedWriterT.close();
//temp for single ES file
            RadioDataES = RadioDataES + '\n';  //temp
            bufferedWriterES.write(RadioDataES);
            bufferedWriterES.close();

        } catch (IOException e) {
            Log.d("Cannot write data", "4: " + e);
        }
        finish();

    }

    public void InitNetworking() {
        AndroidNetworking.initialize(getApplicationContext());
        AndroidNetworking.enableLogging();
        AndroidNetworking.setConnectionQualityChangeListener(new ConnectionQualityChangeListener() {
            @Override
            public void onChange(ConnectionQuality currentConnectionQuality, int currentBandwidth) {
                Log.d("onChange: currentConnectionQuality : ", " currentConnectionQuality +  currentBandwidth : ");
            }
        });
    }

    public void getNeighbourCellInfoNR(CellInfo cellInfo) {
        CellInfoNr nrCellInfo = (CellInfoNr) cellInfo;
        CellIdentityNr cellIdentityNr = (CellIdentityNr) nrCellInfo.getCellIdentity();
        //       cellSignalStrengthNr = (CellSignalStrengthNr) nrCellInfo.getCellSignalStrength();
        //       cellMcc = cellIdentityNr.getMccString();
        //       cellMnc = cellIdentityNr.getMncString();
        cellNeighPci = cellIdentityNr.getPci();
        //       cellTac = cellIdentityNr.getTac();
        //       cellEar = cellIdentityNr.getNrarfcn();
    }

    public void getCellInfoNR(CellInfo cellInfo) {
        CellInfoNr nrCellInfo = (CellInfoNr) cellInfo;
        CellIdentityNr cellIdentityNr = (CellIdentityNr) nrCellInfo.getCellIdentity();
        //   cellSignalStrengthNr = (CellSignalStrengthNr) nrCellInfo.getCellSignalStrength();
        cellMcc = cellIdentityNr.getMccString();
        cellMnc = cellIdentityNr.getMncString();
        cellPci = cellIdentityNr.getPci();
        cellTac = cellIdentityNr.getTac();
        cellEar = cellIdentityNr.getNrarfcn();
        //   cellRsrp = cellSignalStrengthNr.getSsRsrp();
        //   cellRsrp = cellSignalStrengthNr.getCsiRsrp();
        cellID = -99;
        cellOp = "Ignore";
        cellBw = -99;
        cellTa = -99;
        cellDbm = -99;
        cellCqi = -99;
        cellRssnr = -99;
        cellAsu = -99;
        cellLevel = -99;
    }

    public void getCellInfoLTE(CellInfo cellInfo) {
        cellID = ((CellInfoLte) cellInfo).getCellIdentity().getCi();
        cellMcc = ((CellInfoLte) cellInfo).getCellIdentity().getMccString();
        cellMnc = ((CellInfoLte) cellInfo).getCellIdentity().getMncString();
        cellPci = ((CellInfoLte) cellInfo).getCellIdentity().getPci();
        cellTac = ((CellInfoLte) cellInfo).getCellIdentity().getTac();
        cellOp = ((CellInfoLte) cellInfo).getCellIdentity().getMobileNetworkOperator();
        cellBw = ((CellInfoLte) cellInfo).getCellIdentity().getBandwidth();
        cellEar = ((CellInfoLte) cellInfo).getCellIdentity().getEarfcn();
        cellTa = ((CellInfoLte) cellInfo).getCellSignalStrength().getTimingAdvance();
        cellDbm = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
        cellCqi = ((CellInfoLte) cellInfo).getCellSignalStrength().getCqi();
        cellRsrp = ((CellInfoLte) cellInfo).getCellSignalStrength().getRsrp();
        cellRsrq = ((CellInfoLte) cellInfo).getCellSignalStrength().getRsrq();
        cellRssnr = ((CellInfoLte) cellInfo).getCellSignalStrength().getRssnr();
        cellAsu = ((CellInfoLte) cellInfo).getCellSignalStrength().getAsuLevel();
        cellLevel = ((CellInfoLte) cellInfo).getCellSignalStrength().getLevel();

//        cellBand = ((CellInfoLte) cellInfo).getCellIdentity().getBands();

    }

    public void getNeighbourCellInfoLTE(CellInfo cellInfo) {
        cellNeighID = ((CellInfoLte) cellInfo).getCellIdentity().getCi();
        cellNeighMcc = ((CellInfoLte) cellInfo).getCellIdentity().getMccString();
        cellNeighMnc = ((CellInfoLte) cellInfo).getCellIdentity().getMncString();
        cellNeighPci = ((CellInfoLte) cellInfo).getCellIdentity().getPci();
        cellNeighTac = ((CellInfoLte) cellInfo).getCellIdentity().getTac();
        cellNeighOp = ((CellInfoLte) cellInfo).getCellIdentity().getMobileNetworkOperator();
        cellNeighBw = ((CellInfoLte) cellInfo).getCellIdentity().getBandwidth();
        cellNeighEar = ((CellInfoLte) cellInfo).getCellIdentity().getEarfcn();
        cellNeighTa = ((CellInfoLte) cellInfo).getCellSignalStrength().getTimingAdvance();
        cellNeighDbm = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
        cellNeighCqi = ((CellInfoLte) cellInfo).getCellSignalStrength().getCqi();
        cellNeighRsrp = ((CellInfoLte) cellInfo).getCellSignalStrength().getRsrp();
        cellNeighRsrq = ((CellInfoLte) cellInfo).getCellSignalStrength().getRsrq();
        cellNeighRssnr = ((CellInfoLte) cellInfo).getCellSignalStrength().getRssnr();
        cellNeighAsu = ((CellInfoLte) cellInfo).getCellSignalStrength().getAsuLevel();
        cellNeighLevel = ((CellInfoLte) cellInfo).getCellSignalStrength().getLevel();
    }

    public boolean isNSAConnected(TelephonyManager telephonyManager) {
        try {
            Object obj = Class.forName(telephonyManager.getClass().getName())
                    .getDeclaredMethod("getServiceState", new Class[0]).invoke(telephonyManager, new Object[0]);
            // try extracting from string
            String serviceState = obj.toString();
            boolean is5gActive = serviceState.contains("nrState=CONNECTED") ||
                    serviceState.contains("nsaState=5") ||
                    (serviceState.contains("EnDc=true") &&
                            serviceState.contains("5G Allocated=true"));
            if (is5gActive) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    private class SignalStrengthListener extends PhoneStateListener {


        public void onCellInfoChanged (List<CellInfo> cellInfo)
        {
            int t = 9;
        }
        public void onCellLocationChanged (CellLocation location)
        {
            int j = 9;
        }

        @Override
        public void onSignalStrengthsChanged(android.telephony.SignalStrength signalStrength) {


//            if ((ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) +
//                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) +
//                    ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) +
//                    ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) +
//                    ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)+
//                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE))
//                    != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
//                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 3);
//                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
//                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 5);
//                requestPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 6);
//            }

            ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).listen(signalStrengthListener, SignalStrengthListener.LISTEN_SIGNAL_STRENGTHS | LISTEN_CELL_LOCATION ); // | LISTEN_CALL_STATE | LISTEN_CELL_INFO | LISTEN_SERVICE_STATE);
            tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            TelephonyManager.CellInfoCallback cellInfoCallback = new TelephonyManager.CellInfoCallback() {
                @Override
                public void onCellInfo(List<CellInfo> cellInfo) {

                }
            };
            tm.requestCellInfoUpdate(context.getMainExecutor(), cellInfoCallback);

            try {
                networkType = tm.getDataNetworkType();
                cellInfoList = tm.getAllCellInfo();
                PersistableBundle pb = tm.getCarrierConfig();
                cellSignalStrengthList = signalStrength.getCellSignalStrengths();
                for (CellInfo cellInfo : cellInfoList) {

                    if (cellInfo instanceof CellInfoNr) {  // 5G SA
                        strRAT = "  - 5G-SA " + networkType;
                        for (CellSignalStrength cellSignalStrengthNr: cellSignalStrengthList) {
                            if (cellSignalStrengthNr instanceof CellSignalStrengthNr) {
                                if (cellInfo.isRegistered()) {
                                    prevCellPci = cellPci;
                                    getCellInfoNR(cellInfo);
                                    cellRsrp = ((CellSignalStrengthNr) cellSignalStrengthNr).getSsRsrp();
                                    cellRsrq = ((CellSignalStrengthNr) cellSignalStrengthNr).getSsRsrq();
                                } else {
                                    getNeighbourCellInfoNR(cellInfo);
                                    cellNeighRsrp = ((CellSignalStrengthNr) cellSignalStrengthNr).getSsRsrp();
                                    break;
                                }
                            }
                        }
                    }  //5G SA

                    if (cellInfo instanceof CellInfoLte) {  //LTE
                        if (isNSAConnected(tm))
                        {
                            strRAT = " - 5G-NSA " + networkType;
                            if (cellInfo.isRegistered())
                            {
                                getCellInfoLTE(cellInfo);
                            }
                        } else
                        {
                            strRAT = " - LTE " + networkType;
                        }
                        if (cellInfo.isRegistered()) {
                            prevCellPci = cellPci;
                            getCellInfoLTE(cellInfo);
                        } else {
                            getNeighbourCellInfoLTE(cellInfo);
                            break;
                        }
                    }  //LTE
                }
            } catch (Exception e) {
                Log.d("SignalStrength", "3: " + e);
            }
            //print values on screen
            //Android 10 +


            //Android 11
            //boolean technology = NetworkCapabilities.hasCapability(NET_CAPABILITY_NOT_METERED)
            //           CellInfoNr cellInfoNr = null;
//            int connectionStatus = cellInfoNr.getCellConnectionStatus();
            //           CellIdentityNr cellIdentityNr = null;


            count_loop++;
            //           cellIDNetworkType.setText(String.valueOf(networkType));
            cellRATTextView.setText(String.valueOf(strRAT));
            cellIDTextView.setText(String.valueOf(cellID));
            cellMccTextView.setText(String.valueOf(cellMcc));
            cellMncTextView.setText(String.valueOf(cellMnc));
            cellPciTextView.setText(String.valueOf(cellPci));
            cellOpTextView.setText(String.valueOf(cellOp));
            cellBwTextView.setText(String.valueOf(cellBw));
            cellEarTextView.setText(String.valueOf(cellEar));
            cellTacTextView.setText(String.valueOf(cellTac));
            cellTaTextView.setText(String.valueOf(cellTa));
            cellCqiTextView.setText(String.valueOf(cellCqi));
            cellDbmTextView.setText(String.valueOf(cellDbm));
            cellRsrpTextView.setText(String.valueOf(cellRsrp));
            cellRsrqTextView.setText(String.valueOf(cellRsrq));
//            cellRssnrTextView.setText(String.valueOf(cellRssnr));
            cellAsuTextView.setText(String.valueOf(cellAsu));
            cellNeighIDTextView.setText(String.valueOf(cellNeighID));
            cellNeighMccTextView.setText(String.valueOf(cellNeighMcc));
            cellNeighMncTextView.setText(String.valueOf(cellNeighMnc));
            cellNeighPciTextView.setText(String.valueOf(cellNeighPci));
            cellNeighOpTextView.setText(String.valueOf(cellNeighOp));
            cellNeighBwTextView.setText(String.valueOf(cellNeighBw));
            cellNeighEarTextView.setText(String.valueOf(cellNeighEar));
            cellNeighTacTextView.setText(String.valueOf(cellNeighTac));
            cellNeighTaTextView.setText(String.valueOf(cellNeighTa));
            cellNeighCqiTextView.setText(String.valueOf(cellNeighCqi));
            cellNeighDbmTextView.setText(String.valueOf(cellNeighDbm));
            cellNeighRsrpTextView.setText(String.valueOf(cellNeighRsrp));
            cellNeighRsrqTextView.setText(String.valueOf(cellNeighRsrq));
            cellNeighRssnrTextView.setText(String.valueOf(cellNeighRssnr));
            cellNeighAsuTextView.setText(String.valueOf(cellAsu));
            CountLoopTextView.setText(String.valueOf(count_loop));


            super.onSignalStrengthsChanged(signalStrength);
            //     super.onDisplayInfoChanged(telephonyDisplayInfo);

            try {
                formatter = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SS", Locale.ENGLISH);
                today = new Date();
                current_time = formatter.format(today);
                String time_ms = Long.toString(time);
                prevTime = time;

                if (time_ms.length() > 10) {
                    time_ms_slice = time_ms.substring(6, 13);
                }
                handoverflag = 0;
                jsonObject.put("Row", num_records);
                jsonObject.put("Time", current_time);
                jsonObject.put("TimeMS", time_ms_slice);
                jsonObject.put("SCellID", cellID);
                jsonObject.put("NCellID", cellNeighID);
                jsonObject.put("SPLMN", cellMcc + cellMnc);
                jsonObject.put("NPLMN", cellNeighMcc + cellNeighMnc);
                jsonObject.put("SPCI", cellPci);
                //Handover event
                if (cellPci != prevCellPci) {
                    handoverflag = 1;
                    getSendHandover().exec(current_time_ISO8601, prevCellPci, cellPci);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {  // toast message on main thread
                        @Override
                        public void run() {
                            Context context = getApplicationContext();
                            CharSequence text = "Handover to " + cellPci;
                            int duration = Toast.LENGTH_LONG;
                            Toast.makeText(context, text, duration).show();
                        }
                    });
                }
                jsonObject.put("NPCI", cellNeighPci);
                jsonObject.put("Handover", handoverflag);
                jsonObject.put("SBW", cellBw);
                jsonObject.put("NBW", cellNeighBw);
                jsonObject.put("SEARFCN", cellEar);
                jsonObject.put("NEARFCN", cellNeighEar);
                jsonObject.put("STAC", cellTac);
                jsonObject.put("NTAC", cellNeighTac);
                jsonObject.put("STA", cellTa);
                jsonObject.put("NTA", cellNeighTa);
                jsonObject.put("SCQI", cellCqi);
                jsonObject.put("NCQI", cellNeighCqi);
                jsonObject.put("SDbm", cellDbm);
                jsonObject.put("NDbm", cellNeighDbm);
                jsonObject.put("SRSRP", cellRsrp);
                jsonObject.put("NRSRP", cellNeighRsrp);
                jsonObject.put("SRSRQ", cellRsrq);
                jsonObject.put("NRSRQ", cellNeighRsrq);
                jsonObject.put("SRSSNR", cellRssnr);
                jsonObject.put("NRSSNR", cellNeighRssnr);
                jsonObject.put("SASU", cellAsu);
                jsonObject.put("NASU", cellNeighAsu);
                jsonObject.put("Lat", latitude);
                jsonObject.put("Long", longitude);
                jsonObject.put("Alt", altitude);
                jsonObject.put("Acc", accuracy);
                jsonObject.put("Bear", bearing);
                jsonObject.put("BearAcc", bearingAccuracy);
                jsonObject.put("ElapsedRT", elapsedRT);
                jsonObject.put("Speed", speed);
                jsonObject.put("SpeedAcc", speedAccuracy);   // ES
                RadioData = jsonObject.toString();
                RadioDataES = RadioData;
                jsonArray = jsonObject.toJSONArray(jsonObject.names());
                jsonArray.put(jsonObject);
                if (num_records > 0) {
                    RadioData = ',' + RadioData;
                }
                else
                {
                    RadioData = '[' + RadioData;
                }

//temp for single ES file
                if (num_records_ES > 0) {
                    RadioDataES = '\n' + "{\"index\":{}}" + '\n' + RadioDataES;
                }else {
                    RadioDataES = "{\"index\":{}}"+ '\n' + RadioDataES;
                    num_records_ES++;
                }

////temp removed for single ES file
//                if (num_records_ES > 0) {
//                    RadioDataESArray = RadioDataESArray + '\n' + "{\"index\":{}}" + '\n' + RadioDataES;
//                }else {
//                    RadioDataESArray = "{\"index\":{}}"+ '\n' + RadioDataES;
//                    num_records_ES++;
//                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            //Post JSON record to ES
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Context context = getApplicationContext();
                    handoverflag = 1;
                    CharSequence texts = "Sending ";
                    createRecordES(jsonObject);
                    int duration = Toast.LENGTH_LONG;
                    //Toast.makeText(context, texts, duration).show();
                }
            });

            RadioDataTArray = RadioDataTArray + RadioData;
            num_records++;

            rightNow = Calendar.getInstance();
            now_ms = rightNow.getTimeInMillis();
            file_r_nameT = "/radio_temp_" + numFilesT + ".txt";
            file_r_nameES = "/radio_es_" + numFilesES + ".txt";

            if (prev_ms == 0)
            {
                prev_ms = now_ms;
                try {
                    fileT = new File(context.getExternalFilesDir(null) + File.separator + "Temp", file_r_nameT);
                    fileWriterT = new FileWriter(fileT, true);
                    bufferedWriterT = new BufferedWriter(fileWriterT);
                } catch (IOException e) {
                    Log.d("Cannot create file", "4: " + e);
                }
                try {
                    fileES = new File(context.getExternalFilesDir(null) + File.separator + "ES", file_r_nameES);
                    fileWriterES = new FileWriter(fileES, true);
                    bufferedWriterES = new BufferedWriter(fileWriterES);
                } catch (IOException e) {
                    Log.d("Cannot create file", "4: " + e);
                }
            }
            if (now_ms > prev_ms + FILE_DURATION) {
                try {
                    if (RadioDataTArray.charAt(0) == ',') {
                        StringBuilder myString = new StringBuilder(RadioDataTArray);
                        myString.setCharAt(0, '[');
                        RadioDataTArray = myString.toString();
                    }

                    bufferedWriterT.write(RadioDataTArray+"]");      //1. Write the Temp file every FILE_DURATION(seconds)
                    bufferedWriterT.close();
//temp removed for single ES file
//                    RadioDataESArray = RadioDataESArray + '\n';
//                    bufferedWriterES.write(RadioDataESArray);          //2. Write the raw elastic search file every FILE_DURATION(seconds)
//                    bufferedWriterES.close();
//                    num_records_ES=0;

                    RadioDataTArray = "";
                    prev_ms = rightNow.getTimeInMillis();

                    file_r_nameT = "/radio_temp_" + numFilesT + ".txt";
                    numFilesT++;
                    fileT = new File(context.getExternalFilesDir(null) + File.separator + "Temp", file_r_nameT);
                    fileWriterT = new FileWriter(fileT, true);
                    bufferedWriterT = new BufferedWriter(fileWriterT);

                    RadioDataESArray = "";
                    //temp Removed for single ES file
//                    file_r_nameES = "/radio_es_" + numFilesES + ".txt";
//                    numFilesES++;
//                    fileES = new File(context.getExternalFilesDir(null) + File.separator + "ES", file_r_nameES);
//                    fileWriterES = new FileWriter(fileES, true);
//                    bufferedWriterES = new BufferedWriter(fileWriterES);

                } catch (IOException e) {
                    Log.d("Cannot write data", "4: " + e);
                }
            }
            try {
                bufferedWriter.write(RadioData);                          //3. Write the main data file
                bufferedWriterES.write(RadioDataES);     //temp for single ES file
            } catch (IOException e) {
                Log.d("Cannot write data", "4: " + e);
            }
            ri++;
        }


        // POST JSON records to Elastic Search
        public void createRecordES(JSONObject jsonObject) {

            if ((ActivityCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE) +
                    ActivityCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE))
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 4);
                requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, 5);
            }

            AndroidNetworking.post(ApiEndPoint.BASE_URL_JSON + ApiEndPoint.POST_CREATE_AN_USER)
                    .setContentType("application/json")
                    .addJSONObjectBody(jsonObject)
                    //  .addJSONArrayBody(jsonArrRecord)
                    .setTag(this)
                    .setPriority(Priority.LOW)
                    .build()
                    .setAnalyticsListener(new AnalyticsListener() {
                        @Override
                        public void onReceived(long timeTakenInMillis, long bytesSent, long bytesReceived, boolean isFromCache) {
                            Log.d(TAG, " 1timeTakenInMillis : " + timeTakenInMillis);
                            Log.d(TAG, " 2bytesSent : " + bytesSent);
                            Log.d(TAG, " 3bytesReceived : " + bytesReceived);
                            Log.d(TAG, " 4isFromCache : " + isFromCache);
                        }
                    })

                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, "onResponse object : " + response.toString());
                            Log.d(TAG, "onResponse isMainThread : " + String.valueOf(Looper.myLooper() == Looper.getMainLooper()));
                        }

                        @Override
                        public void onError(ANError error) {
                            if (error.getErrorCode() != 0) {
                                // received ANError from server
                                Log.d(TAG, "onError errorCode : " + error.getErrorCode());
                                Log.d(TAG, "onError errorBody : " + error.getErrorBody());
                                Log.d(TAG, "onError errorDetail : " + error.getErrorDetail());
                            } else {
                                // error.getErrorDetail() : connectionError, parseError, requestCancelledError
                                Log.d(TAG, "onError errorDetail : " + error.getErrorDetail());
                            }
                        }
                    });
        }


    }
}

