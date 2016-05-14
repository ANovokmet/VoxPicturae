package hr.sound.filter;

public class SavitzkyGolayFilter implements Filter {

    @Override
    public void execute(short[] data){
        int length = data.length;
        for(int iteration=0; iteration<2; iteration++) {
            for (int index = 3; index < length-3 ; index++) {
                data[index] = (short)((-2*data[index - 3] + 3*data[index - 2] + 6*data[index - 1]
                        + 7*data[index] + 6*data[index + 1] + 3*data[index + 2]
                        + -2*data[index + 3]) / (-2 + 3 + 6 + 7 + 6 + 3 - 2));
            }
        }
    }
}
