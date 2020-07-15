package umn.ac.id.researchtracking;

public class PhoneState {
    private String radio;
    private int mcc;
    private int mnc;
    private String timestamp;
    private String user_id;

    public PhoneState(){};
    public PhoneState(String radio,int mcc,int mnc,String timestamp,String user_id){
        this.radio = radio;
        this.mcc = mcc;
        this.mnc = mnc;
        this.timestamp = timestamp;
        this.user_id = user_id;
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

    public String getUser_id() {
        return user_id;
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

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
}


