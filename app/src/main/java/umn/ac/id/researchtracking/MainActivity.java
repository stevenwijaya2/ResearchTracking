package umn.ac.id.researchtracking;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends Activity {
    //define variable first (before onCreate) so aother method can access too later [global variable]
    private LocationManager mLocationManager;
    private static final int MY_PERMISSIONS_REQUEST_ALL_PERMISSION = 200;
    String data = "Data : \n"; // variable to show info on screen (append later to screen)
    TelephonyManager telephonyManager;
    List<CellInfo> cellInfo;
    String PhoneType;
    String NetworkOperator;
    String cellID;
    double Latitude;
    double Longitude;
    int dbm;
    int mcc;
    int mnc;
    boolean GotLocation;
    JSONObject JSONFinal;
    JSONArray JSONCell;
    JSONObject JSONCellTemp;
    TextView tv;
    int CellTotal;
    Boolean AutoSynctoServer = true;
    String UID;
    long Timestamp;
    Button btn_on_off;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //declare shared preferences to saved UID on device
        //if device didn't have UID yet, apps gonna generated UID for device and saved it on preferences
        //so, UID on 1 devices is consistent everytime user open the apps
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if(sharedPreferences.getString("UID", "") != ""){
            //check if there saved UID on device , so the apps did not generate new UID
            Log.i("pref","UID Already exist");
            UID = sharedPreferences.getString("UID", "");//set saved uid from preferences to UID variable
            Log.i("pref",sharedPreferences.getString("UID", ""));
        }
        else {
            //conditions where device didn't have UID
            //maybe it is the first time apps launched on device
            //apps generate UID and saved it on shared preferences
            //so apps can used it later and make UID is consistent on 1 devices
            Log.i("pref","NO UID Found on device");
            UID = UUID.randomUUID().toString();
            editor.putString("UID", UID);
            editor.commit();
            Log.i("pref",sharedPreferences.getString("UID", ""));
        }

        setContentView(R.layout.activity_main);
        ask_Location_permission(); //ask permission for read location
        ask_phoneState_permission(); //ask permission for read phone state

        //Initialize Layout Elements
        tv = findViewById(R.id.textView); //text view to show data on screen
        btn_on_off = findViewById(R.id.btn_on_off); //button to turn on/off autosync to server
        btn_on_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AutoSynctoServer = !AutoSynctoServer; //reverse the value of autosync, make true -> false or false -> true
                Toast.makeText(getApplication(), "Auto Sync data to server : " +AutoSynctoServer, LENGTH_SHORT).show();
            }
        });
        setRepeatingAsyncTask();//send data to server countinuously , depend on timer
    }

    private void setRepeatingAsyncTask() {
        //always update info on screen and send data to server based on set timer
        final Handler handler = new Handler();
        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    public void run() {
                        if(AutoSynctoServer) {
                            try {
                                //check location permission again, required by getAllCellInfo (mandatory by android studio)
                                if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    ask_Location_permission(); // ask permission for fine location & coarse location
                                }
                                //prepare multiple JSONObject and JSONArray to save all of the info
                                //but later, only JSONFinal sent to server (append the other to JSONFinal later)
                                JSONCellTemp = new JSONObject(); //contain single cell info. and append itself to JSONCell later
                                JSONCell = new JSONArray(); //contain multiple cell info (array of JSONObject)
                                JSONFinal = new JSONObject(); //json final that sent to server later (contain phonestate and multiple cell info)
                                data =""; //data to show on screen later
                                telephonyManager = (TelephonyManager) getApplication().getSystemService(Context.TELEPHONY_SERVICE);//access telephony manager that contain all phone/signal related info
                                cellInfo = telephonyManager.getAllCellInfo();//get cell info that connected to device (return list<CellInfo> Object)
                                Timestamp = Instant.now().getEpochSecond();//get current unix timestamp


                                get_phone_info(); //get phone side info
                                get_cell_info(); //get all cell/tower info

//                                //Display one by one cell info (used for dumping. can deleted later)
//                                Log.i("Info display", data);
//                                for (CellInfo s : cellInfo) {
//                                    Log.d("My array list content: ", s.toString());
//                                }
//                                Log.d("main", String.valueOf(cellInfo.listIterator()));//display everything.

                                tv.setText(data); //Append data to TextView (phonescreen)

                                //set JSON that ready to upload
                                //append everything to JSONFinal
                                try {
                                    JSONFinal.put("radio", PhoneType);
                                    JSONFinal.put("mcc", mcc);
                                    JSONFinal.put("mnc", mnc);
                                    JSONFinal.put("timestamp", Timestamp);
                                    JSONFinal.put("user_id", UID);
                                    JSONFinal.put("lat", Latitude);
                                    JSONFinal.put("lon", Longitude);
                                    JSONFinal.put("cells", JSONCell);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                //send data to server using AsyncTask
                                //because send data to server cant executed on main thread
                                //so AsyncTask create another thread to sent data to server
                                AsyncT PostToServer = new AsyncT();
                                PostToServer.execute();

                            } catch (Exception e) {
                                // error, do something
                            }
                        }
                    }
                });
            }
        };

        timer.schedule(task, 0, 1*1000);  // interval of two minute
    }

    public void ask_Location_permission() {
        //ask location permission
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ALL_PERMISSION);
        }
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        boolean statusOfGPS = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER); //get is location access is enabled or not

        if (!statusOfGPS) {
            // if apps did'not get access to location services
            // open location services setting.user need to enable the location
            Toast.makeText(this, "We Cant Fully Operated without Location", LENGTH_SHORT).show();
            Intent intentAskGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intentAskGPS);//open location setting on devices
        }
        if (statusOfGPS) {
            //condition where apps get location access
            //get latitude and longitude of device
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    //everytime device location change even only a little bit
                    //saved the latitude and lonitude on variable
                    //to append on json later
                    //latitude and longitude variable values changes continously
                    //even maybe json only sent to server on every 3 minutes
                    Latitude = location.getLatitude();
                    Longitude = location.getLongitude();
                    Log.d("location", String.valueOf(location.getLatitude()));
                    Log.d("location", String.valueOf(location.getLongitude()));
                    GotLocation = true; //tell that we got the location.
                    // because on android sometime there is delay to get location (approx. 5-10 s)
                }
                @Override
                public void onProviderDisabled(String provider) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }
            });
        }

    }

    public void ask_phoneState_permission() {
        // ask read phone state permission

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

    public void get_phone_info() {
        //get info from phone state

        int phoneType = telephonyManager.getPhoneType(); //get phone type info
        switch (phoneType) {
            //decide phone type are GSM,CDMA, or None (no info)
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


        NetworkOperator = telephonyManager.getNetworkOperator(); //NetworkOperator contains numeric name (MCC+MNC) of current registered operator.
        mcc = Integer.parseInt(NetworkOperator.substring(0, 3)); // get MCC [Mobile Country Codes]from NetworkOperator returned value
        mnc = Integer.parseInt(NetworkOperator.substring(3)); // get MNC [Mobile Network Code] from NetworkOperator returned value

        //append phone info data to variable 'data' to show on textview screen later
        data += "\nNetwork Type = " + PhoneType;
        data += "\nNetwork Operator = "+NetworkOperator;
        data += "\nMCC = "+ mcc;
        data += "\nMNC = "+mnc;
        data += "\nTimestamp = "+ Timestamp;
        data += "\n\n----------------------\n\n";
    }

    public void get_cell_info(){
        //get info about nearest cell
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //ask permission once again, because it needed by CellInfo (mandatory by android studio}
            ask_Location_permission();
            return;
        }
        CellTotal = cellInfo.size(); //get how many cell that connect to devices
        for (int i = 0; i < cellInfo.size(); ++i) {
            try {
                //there are 3 kind of cell tower (GSM,CDME,LTE)
                //and every each of that have different way to access rss(dbm) and cell id
                // so we using if to handle it suitable to each kind
                CellInfo info = cellInfo.get(i);
                if (info instanceof CellInfoGsm) //if GSM connection
                {
                    CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                    CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                    cellID = String.valueOf(identityGsm.getCid());//get cell id
                    dbm = gsm.getDbm();//get rss or signal strength or dbm

                    //append cell info to variable 'data' to show on screen later
                    data += "Site_" + i + "\r\n";
                    data += "Registered: " + info.isRegistered() + "\r\n";
                    data += "cellID: " + cellID + "\r\n";
                    data += "dBm: " + dbm + "\r\n\r\n";
                } else if (info instanceof CellInfoLte)  //if LTE connection
                {
                    CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                    CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                    cellID = String.valueOf(identityLte.getCi());//get cell id
                    dbm = lte.getDbm();//get rss or signal strength or dbm

                    //append cell info to variable 'data' to show on screen later
                    data += "Site_" + i + "\r\n";
                    data += "Registered: " + info.isRegistered() + "\r\n";
                    data += "cellID: " + cellID + "\r\n";
                    data += "dBm: " + dbm + "\r\n\r\n";


                } else if (info instanceof CellInfoWcdma)  //if wcdma connection
                {
                    CellSignalStrengthWcdma wcdmaS = ((CellInfoWcdma) info).getCellSignalStrength();
                    CellIdentityWcdma wcdmaid = ((CellInfoWcdma) info).getCellIdentity();
                    cellID = String.valueOf(wcdmaid.getCid());//get cell id
                    dbm = wcdmaS.getDbm(); //get rss or signal strength or dbm

                    //append cell info to variable 'data' to show on screen later
                    data += "Site_" + i + "\r\n";
                    data += "Registered: " + info.isRegistered() + "\r\n";
                    data += "cellID: " + wcdmaid.getCid() + "\r\n";
                    data += "dBm: " + wcdmaS.getDbm() + "\r\n\r\n";

                }

            } catch (Exception ex) {
                Log.i("neighboring error 2: ", ex.getMessage());
            }
            try {
                //append cell id and dbm on single tower to JSONCellTemp
                JSONCellTemp.put("cell_id",cellID);
                JSONCellTemp.put("rss",dbm);

                //append JSONCellTemp to JSONCell
                //JSONCell = array of object that contain multiple cell info
                JSONCell.put(JSONCellTemp);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public class AsyncT extends AsyncTask<JSONObject,JSONObject,JSONObject> {
        @Override
        @Nullable
        protected JSONObject doInBackground(JSONObject... objects) {
            //AsyncTask that got job to send data to server
            //create a new thread to send data to server
            //android prohibited send data on main thread
            try {
                URL url = new URL("http://tracking-research.herokuapp.com/collector"); //url server
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept","application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(JSONFinal.toString());//send JSONFinal to server
                conn.connect();
                os.flush();
                os.close();

                Log.i("BACKEND", String.valueOf(conn.getResponseCode()));
                Log.i("BACKEND" , conn.getResponseMessage());
                Log.i("BACKEND" , JSONFinal.toString());

                conn.disconnect();
            }
            catch (Exception e){
                e.printStackTrace();
                //Log.i("BACKEND" , e.printStackTrace());
            }
            return null;
        }
    }


}
