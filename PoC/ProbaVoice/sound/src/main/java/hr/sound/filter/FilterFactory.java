package hr.sound.filter;

public class FilterFactory {

    /**
     * Returns default signal filter. Currently Savitzky-Golay filter.
     * @return
     */
    public static Filter getDefaultFilter(){
        return new SavitzkyGolayFilter();
    }

    /*public static Filter getFilter(String name){

    }*/
}
