package umn.ac.id.researchtracking;

public class PhoneState {
    private String radio;
    private int mcc;
    private int mnc;
    private String timestamp;

    public PhoneState(){};
    public PhoneState(String radio,int mcc,int mnc,String timestamp){
        this.radio = radio;
        this.mcc = mcc;
        this.mnc = mnc;
        this.timestamp = timestamp;
    }

    public int getMcc() {
        return mcc;
    }

    public int getMnc() {
        return mnc;
    }

    public String getRadio() {
        return radio;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setMcc(int mcc) {
        this.mcc = mcc;
    }

    public void setMnc(int mnc) {
        this.mnc = mnc;
    }

    public void setRadio(String radio) {
        this.radio = radio;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}


