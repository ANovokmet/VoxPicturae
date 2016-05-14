package hr.image;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Ante on 29.3.2016..
 */
public class PictureFileManager {
    public static String createFileName() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hhmmss_ddMMyyyy");
        return simpleDateFormat.format(new Date());
    }
}
