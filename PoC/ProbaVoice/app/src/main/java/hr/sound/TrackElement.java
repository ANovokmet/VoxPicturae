package hr.sound;

public class TrackElement {

    private short data[];

    public TrackElement(short[] data, int length){
        this.data = new short[length];
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
}
