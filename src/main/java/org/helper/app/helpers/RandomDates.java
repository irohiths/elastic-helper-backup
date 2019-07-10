package org.helper.app.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

public class RandomDates {
//    public static void main(String[] args) {
//        for (int i = 0; i < 10; i++) {
//            LocalDate randomDate = createRandomDate(1900, 2000);
//            System.out.println(randomDate);
//        }
//    }

    public static int createRandomIntBetween(int start, int end) {
        return start + (int) Math.round(Math.random() * (end - start));
    }

    public static String createRandomDate(int startYear, int endYear) {
        try{
            int day = createRandomIntBetween(1, 28);
            int month = createRandomIntBetween(1, 12);
            int year = createRandomIntBetween(startYear, endYear);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date convertedCurrentDate = sdf.parse(year+"-"+month+"-"+day);
            String dateString = sdf.format(convertedCurrentDate);
            return dateString;
        } catch (ParseException e) {
            System.out.println("Caught ParseException in RandomDate.createRandomDate() : " + e.getStackTrace());
        }
        return "2019-01-01";
    }
}
