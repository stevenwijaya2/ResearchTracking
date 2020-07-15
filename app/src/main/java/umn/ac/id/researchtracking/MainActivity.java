package umn.ac.id.researchtracking;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends Activity {
    private LocationManager mLocationManager;
    private static final int MY_PERMISSIONS_REQUEST_ALL_PERMISSION = 200;
    String data = "Data : \n"; // variable to show data on screen (append later to screen)
    TelephonyManager telephonyManager;
    List<CellInfo> cellInfo;
    String PhoneType = "";
    String NetworkOperator;
    String cellID;
    double Latitude;
    double Longitude;
    int dbm;
    int mcc;
    int mnc;
    boolean GotLocation;
    JSONObject JSONFinal;
    JSONObject JSONCell;
    JSONObject JSONCellTemp;
    JSONObject JSONPhoneState;
    TextView tv;
    int CellTotal;
    Cell[] ObjectCell;

    String currentTime = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss", Locale.getDefault()).format(new Date());
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    String UID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ask_Location_permission(); //ask permission for read location
        ask_phoneState_permission(); //ask permission for read phone state

        //Initialize Variable
        telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        NetworkOperator = telephonyManager.getNetworkOperator();
        mcc = Integer.parseInt(NetworkOperator.substring(0, 3));
        mnc = Integer.parseInt(NetworkOperator.substring(3));
        JSONFinal = new JSONObject();
        JSONCell = new JSONObject();
        JSONCellTemp = new JSONObject();
        JSONPhoneState = new JSONObject();
        tv = findViewById(R.id.textView);

        //send back to Login when there's no active session
        if(mAuth.getCurrentUser() == null){
            Intent gotoLogin = new Intent(this,Login.class);
            startActivityForResult(gotoLogin, 1);
        }
        else {
            UID = mAuth.getCurrentUser().getUid();
        }

        //check login again, required by getAllCellInfo
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ask_Location_permission(); // ask permission for fine location & coarse location
        }
        cellInfo = telephonyManager.getAllCellInfo();
        get_phone_info(); //get phone side info
        get_cell_info(); //get all cell/tower info

//        Display one by one cell info (used for dumping)
//        Log.i("Info display", data);
//        for (CellInfo s : cellInfo){
//            Log.d("My array list content: ", s.toString());
//        }
//        Log.d("main", String.valueOf(cellInfo.listIterator()));//display everything.

        tv.setText(data); //Append data to TextView / display

        //set JSON that ready to upload
        try {
            JSONFinal.put("cell",JSONCell);
            JSONFinal.put("radio",PhoneType);
            JSONFinal.put("mcc",mcc);
            JSONFinal.put("mnc",mnc);
            JSONFinal.put("timestamp",currentTime);
            JSONFinal.put("user_id",mAuth.getCurrentUser().getUid());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("JSONFinal", String.valueOf(JSONFinal));
        UploadJSONtoFirebase();

    }
    public void ask_Location_permission() {

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this , Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ALL_PERMISSION);
        }

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE );
        boolean statusOfGPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!statusOfGPS){
            Toast.makeText(this, "We Cant Fully Operated without Location", LENGTH_SHORT).show();
            Intent intentAskGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intentAskGPS);
        }
        if(statusOfGPS){
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Latitude = location.getLatitude();
                    Longitude = location.getLongitude();
                    //Log.d("location", String.valueOf(location.getLatitude()));
                    GotLocation = true;

                }

                @Override
                public void onProviderDisabled(String provider) {
                    Log.d("Latitude","disable");
                }

                @Override
                public void onProviderEnabled(String provider) {
                    Log.d("Latitude","enable");
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    Log.d("Latitude","status");
                }
            });
        }

    }
    public void ask_phoneState_permission() {
        // Ask Permission for first timeManifest.permission.RECORD_AUDIO

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "We Need Your Permission to Fully Operating", LENGTH_SHORT).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_ALL_PERMISSION);
            }
        } else {
            // Permission has already been granted
        }
    }
    public void get_phone_info(){
        int phoneType = telephonyManager.getPhoneType();
            switch (phoneType) {
                case (TelephonyManager.PHONE_TYPE_CDMA):
                    PhoneType = "CDMA";
                    break;
                case (TelephonyManager.PHONE_TYPE_GSM):
                    PhoneType = "GSM";
                    break;
                case (TelephonyManager.PHONE_TYPE_NONE):
                    PhoneType = "NONE";
                    break;
            }
            // true or false for roaming or not


            data += "\nNetwork Type = " + PhoneType;
            data += "\nNetwork Operator = "+NetworkOperator;
            data += "\nMCC = "+ mcc;
            data += "\nMNC = "+mnc;
            data += "\nTimestamp = "+currentTime;
            data += "\n\n----------------------\n\n";

    }
    public void get_cell_info(){//get cell info
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ask_Location_permission();
            return;
        }
        CellTotal = cellInfo.size();
        ObjectCell = new Cell[CellTotal];
        for (int i = 0; i < cellInfo.size(); ++i) {
            try {
                CellInfo info = cellInfo.get(i);
//              Only show registered site
              //if(info.isRegistered()){
                if (info instanceof CellInfoGsm) //if GSM connection
                {
                    CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                    CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                    data += "Site_" + i + "\r\n";
                    data += "Registered: " + info.isRegistered() + "\r\n";
                    data += "cellID: " + identityGsm.getCid() + "\r\n";
                    data += "dBm: " + gsm.getDbm() + "\r\n\r\n";
                    cellID = String.valueOf(identityGsm.getCid());
                    dbm = gsm.getDbm();

                } else if (info instanceof CellInfoLte)  //if LTE connection
                {
                    CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                    CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                    data += "Site_" + i + "\r\n";
                    data += "Registered: " + info.isRegistered() + "\r\n";
                    data += "cellID: " + identityLte.getCi() + "\r\n";
                    data += "dBm: " + lte.getDbm() + "\r\n\r\n";
                    cellID = String.valueOf(identityLte.getCi());
                    dbm = lte.getDbm();

                } else if (info instanceof CellInfoWcdma)  //if wcdma connection
                {
                    CellSignalStrengthWcdma wcdmaS = ((CellInfoWcdma) info).getCellSignalStrength();
                    CellIdentityWcdma wcdmaid = ((CellInfoWcdma) info).getCellIdentity();
                    data += "Site_" + i + "\r\n";
                    data += "Registered: " + info.isRegistered() + "\r\n";
                    data += "cellID: " + wcdmaid.getCid() + "\r\n";
                    data += "dBm: " + wcdmaS.getDbm() + "\r\n\r\n";
                    cellID = String.valueOf(wcdmaid.getCid());
                    dbm = wcdmaS.getDbm();
                }
              //}
//              End of Only show registered site

            } catch (Exception ex) {
                Log.i("neighboring error 2: ", ex.getMessage());
            }
            try {
                JSONCellTemp.put("CellID",cellID);
                JSONCellTemp.put("Dbm",dbm);
                JSONCell.put("Cell_"+i,JSONCellTemp);
                ObjectCell[i] = new Cell(cellID,dbm);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    public void UploadJSONtoFirebase(){
        Gson GSON = new Gson();
        PhoneState phoneState = GSON.fromJson(String.valueOf(JSONFinal),PhoneState.class);
        Log.d("JSON Phone State", String.valueOf(JSONPhoneState));
        Log.d("UID", String.valueOf(UID));
        mDatabase.getReference("data").child(UID).setValue(phoneState);
        for(int x = 0;x<CellTotal;x++){
            Log.i("CELL ID ", ObjectCell[x].getCellid());
            mDatabase.getReference("data").child(UID).child("cells").child(String.valueOf(x)).setValue(ObjectCell[x]);
        }
    }
}
