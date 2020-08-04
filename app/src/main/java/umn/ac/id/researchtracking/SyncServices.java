package umn.ac.id.researchtracking;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.widget.Toast.LENGTH_SHORT;

public class SyncServices extends Service {
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
    JSONArray JSONCell;
    JSONObject JSONCellTemp;
    JSONObject JSONPhoneState;
    int CellTotal;
    Cell[] ObjectCell;
    String UID;
    long Timestamp;

    public SyncServices() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("CUSTOM SERVICE", "onBind: Service Bind");
        return null;
    }

    public void onCreate() {
        super.onCreate();
        Log.i("CUSTOMSERVICE","onCreate: CustomService");
        //Initialize Variable And Layout Elements
        telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        JSONFinal = new JSONObject();
        JSONCell = new JSONArray();
        JSONCellTemp = new JSONObject();
        JSONPhoneState = new JSONObject();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        cellInfo = telephonyManager.getAllCellInfo();
        get_phone_info(); //get phone side info
        get_cell_info(); //get all cell/tower info

        try {
            JSONFinal.put("radio", PhoneType);
            JSONFinal.put("mcc", mcc);
            JSONFinal.put("mnc", mnc);
            JSONFinal.put("timestamp", Timestamp);
            JSONFinal.put("user_id", "1");
            JSONFinal.put("cells", JSONCell);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            Gson gson = new Gson();
            String JSONSend = gson.toJson(JSONFinal);

            URL url = new URL("http://tracking-research.herokuapp.com/collector"); //in the real code, there is an ip and a port
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept","application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes(JSONFinal.toString());
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

    }
    public void onDestroy(){
        super.onDestroy();
        Log.i("CUSTOMSERVICE","onDestroy: Service Destroyed");
    }

    public void get_phone_info() {
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


        NetworkOperator = telephonyManager.getNetworkOperator();

        mcc = Integer.parseInt(NetworkOperator.substring(0, 3));
        mnc = Integer.parseInt(NetworkOperator.substring(3));
        Timestamp = new Date().getTime();

        data += "\nNetwork Type = " + PhoneType;
        data += "\nNetwork Operator = "+NetworkOperator;
        data += "\nMCC = "+ mcc;
        data += "\nMNC = "+mnc;
        data += "\nTimestamp = "+ Timestamp;
        data += "\n\n----------------------\n\n";

    }

    public void get_cell_info(){//get cell info

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
                JSONCellTemp.put("cell_id",cellID);
                JSONCellTemp.put("rss",dbm);
                JSONCell.put(JSONCellTemp);
                ObjectCell[i] = new Cell(cellID,dbm);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
