package hr.sound;

import java.util.Date;

public class TrackElement {

    private Date timestamp;
    private short data[];

    private double pitch = -1;
    private double power = -1;

    private double max_frequency;

    //test features
    public int numofpeaks;
    public float gender;  // probability of 1 means male

    public TrackElement(short[] data, int length){
        this.data = new short[length];
        timestamp = new Date();
        copyArray(data, this.data, length);
    }

    private void copyArray(short[] src, short[] dest, int how_much){
        for(int i=0; i<how_much; i++){
            dest[i] = src[i];
        }
    }

    public short[] getData(){
        return this.data;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public double getMax_frequency() {
        return max_frequency;
    }

    public void setMax_frequency(double max_frequency) {
        this.max_frequency = max_frequency;
    }
}
