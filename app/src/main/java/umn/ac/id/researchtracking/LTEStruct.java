package umn.ac.id.researchtracking;

import android.content.Context;

public class LTEStruct
{
    public static final int UNKNOWN = Integer.MAX_VALUE;   //Default value for unknown fields

    public boolean isRegistered;
    public long timeStamp;
    public int MCC;
    public int MNC;
    public int CID;
    public int PCI;
    public int TAC;

    public int SS;
    public int RSRP;
    public int RSRQ;
    public int RSSNR;
    public int CQI;
    public int tAdvance;

    Context mContext;

    //Public constructor
    public LTEStruct(Context context)
    {
        mContext = context; //not used at the moment but possibly for future function
    }

    public void parse(String inTest)
    {
        //get isRegistered
        int index = inTest.indexOf("mRegistered=") + ("mRegistered=").length();
        if(inTest.substring(index,index + 3).contains("YES"))
            isRegistered = true;
        else
            isRegistered = false;

        //getTimestamp
        timeStamp = getValue(inTest,"mTimeStamp=", "ns");

        //get Cell Identity paramters
        MCC = (int) getValue(inTest,"mMcc=", " ");      //get Mcc
        MNC = (int) getValue(inTest,"mMnc=", " ");      //get MNC
        CID = (int) getValue(inTest,"mCi=", " ");       //get CID
        PCI = (int) getValue(inTest,"mPci="," ");       //get PCI
        TAC = (int) getValue(inTest,"mTac=","}");       //get TAC

        //get RF related parameters
        SS = (int) getValue(inTest," ss="," ");         //get SS
        RSRP = (int)getValue(inTest,"rsrp=", " ");      //get RSRP
        RSRQ = (int)getValue(inTest,"rsrq=", " ");      //get RSRQ
        RSSNR = (int)getValue(inTest,"rssnr=", " ");    //get RSSNR
        CQI = (int)getValue(inTest," cqi=", " ");       //get CQI
        tAdvance = (int)getValue(inTest," ta=", "}");   //get timing advance
    }

    //internal function to help with parsing of raw LTE strings
    private long getValue(String fullS, String startS, String stopS)
    {
        int index = fullS.indexOf(startS) + (startS).length();
        int endIndex = fullS.indexOf(stopS,index);

        return Long.parseLong(fullS.substring(index,endIndex).trim());
    }

}