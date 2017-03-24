package com.example.hloek.ical2;

import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/*
Importer inn biblioteket til biweekly og icalender.
 */
import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.DateTimeStamp;


public class MainActivity extends AppCompatActivity {
    /*
    month: måneden som er nå
    today: datoen idag
    eventMonth: måneden til det aktuelle eventet
    eventDay: dagen til det aktuelle eventet
    eventHour: timen til det aktuelle eventet
    eventMin: minuttene til det aktuelle eventet.
    lectureUsername: brukernavnet til 1024 timeplangenerator - må settes før man kalled funksjonen
    filename: navnet på filen som lastes ned, må være unik for hver timeplan - kan ha flere timeplaner
     */
    private Date datemaker = new Date();
    private int month = datemaker.getMonth();
    private int today = datemaker.getDate();
    private int eventMonth;
    private int eventHour;
    private int eventMin;
    private int eventDay;


    private int nextEventMonth;
    private int nextEventHour;
    private int nextEventMin;
    private int nextEventDay;
    private DateTimeStamp nextEventTimeStamp;

    private String lectureUsername;
    private String filename;

    // Mappen hvor timeplanene blir lagret
    private final String LECTURE_FILE_PATH = "/lecture";

    private List<VEvent> events;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickNextEvent(View view) {
        generateLecturePlan();
        countDownToNextStory();
        //getTimeTableFromEventAndSetNextEventTime();
        //getMillisecondsForNextEvent();
    }

    // Henter ned timeplanen fra 1024 og lager en sortert liste over alle eventene i kalenderen.
    public void generateLecturePlan(){
        setLectureUsername("test");
        generateFileName();

        file_download("https://ntnu.1024.no/2017/var/" + getLectureUsername() + "/ical/forelesninger/");

        /*
        Hjelpe-textview for utvikling
         */
        TextView tv = (TextView) findViewById(R.id.displayNextEvent);

        try {

            ICalendar ical; // ical object
            //File f = new File(this.getExternalFilesDir("/folder/" + getFilename()).toString());
            File f = new File(this.getExternalFilesDir(LECTURE_FILE_PATH + "/" + getFilename()).toString());

            InputStream in_s = new FileInputStream(f);

            ical = (ICalendar) Biweekly.parse(in_s).first(); // parser inputstremen over i et ical object.
            in_s.close(); // lukker inputstreamen

            /*
            Lager en liste over alle eventene i ical objectet og sorterer dem etter dato.
             */
            events = new ArrayList<VEvent>(ical.getEvents());

            Collections.sort(events, new Comparator<VEvent>() {
                @Override
                public int compare(VEvent event1, VEvent event2) {
                    return event1.getDateStart().getValue().compareTo(event2.getDateStart().getValue());
                }
            });

            /*
            Itterer over alle eventene - når et gitt event skjer på dags dato finner den første eventet den dagen,
            henter ut start tidspunktet og går ut av loopen.
             */
            for(int i = 0; i < events.size(); i++){
                getTimeTableFromEvent(i);

                // Finner første forelesning for idag
                if(getEventMonth() == getMonth() && getEventDay() == getToday()){
                    // Her printers det bare ut noe, men her kan det endres til
                    tv.setText(events.get(i).getSummary().getValue() + ": " + getEventHour() + ":" + getEventMin() + "\n" + "Er personen i tide: " + isPersonOnTime() +
                    "\n" + "Username: " + getLectureUsername());
                    break;
                }
            }
        } catch (IOException e) {e.printStackTrace();}
    }

    /*
    Skal hente ut første event imorgen og sette tiden det gjenstår før den inntreffer- ikke ferdig
     */

    public void getTimeTableFromEventAndSetNextEventTime(){
        for (int k = 0; k < events.size(); k++){
            String eventDate = convertStringMonthToIntMonth(k);
            setNextEventMonth(Integer.valueOf(eventDate.substring(0,2)));
            setNextEventDay(Integer.valueOf(eventDate.substring(3,5)));
            setNextEventHour(Integer.valueOf(eventDate.substring(6,8)));
            setNextEventMin(Integer.valueOf(eventDate.substring(9,11)));

            if(getNextEventMonth() == getMonth() && getNextEventDay() == getToday() + 1){
                TextView tv = (TextView) findViewById(R.id.displayTimefornextevent);
                tv.setText(String.valueOf(getEventDay())
                        + " : " + String.valueOf(getEventHour())
                        + " : " + String.valueOf(getEventMin())
                        + "\n"
                        + " : " + String.valueOf(getNextEventDay())
                        + " : " + String.valueOf(getNextEventHour())
                        + " : " + String.valueOf(getNextEventMin()));
                System.out.println(events.get(k).getDateTimeStamp().toString());
                nextEventTimeStamp = events.get(k).getDateTimeStamp();
                break;
            }
        }
    }

    /*
    Gir ut antall millisekunder til neste gang du får et kapittel
     */
    public int getMillisecondsForNextEvent(){
        Date d = new Date();
        long t = d.getTime() - nextEventTimeStamp.getValue().getTime();
        System.out.println(nextEventTimeStamp.getValue().getTime() + " - " + d.getTime() + " = " + t);

        return 0;
    }


    /*
    Henter ut informasjon om eventet i kalenderen
     */
    public void getTimeTableFromEvent(int i){
        String eventDate = convertStringMonthToIntMonth(i);

        setEventMonth(Integer.valueOf(eventDate.substring(0,2)));
        setEventDay(Integer.valueOf(eventDate.substring(3,5)));
        setEventHour(Integer.valueOf(eventDate.substring(6,8)));
        setEventMin(Integer.valueOf(eventDate.substring(9,11)));


    }


    /*
    Endrer formatet til stringen fra kalender filen, den gir ut dato som String og ikke Int
     */
    public String convertStringMonthToIntMonth(int i){
        return events.get(i).getDateStart().getValue().toString().substring(4, 19)
                .replace("Jan", "01").replace("Feb", "02").replace("Mar", "03")
                .replace("Apr", "04").replace("Mai", "05").replace("Jun", "06")
                .replace("Jul", "07").replace("Aug", "08").replace("Sep", "09")
                .replace("Oct", "10").replace("Nov", "11").replace("Des", "12");
    }

    /*
    Countdown til neste hisotire - ikke ferdig
     */
    public void countDownToNextStory(){




        new CountDownTimer(30000, 1000){
            TextView tv = (TextView) findViewById(R.id.countDown);

            @Override
            public void onTick(long millisUntilFinished) {
                tv.setText("Sekunder igjen: " + millisUntilFinished/1000);
            }

            @Override
            public void onFinish() {
                tv.setText("Story ready!");
            }
        }.start();

    }

    //Download file from url
    public void file_download(String URL) {

        //Check if directory excists
        File direct = new File(Environment.getExternalStorageDirectory()
                + LECTURE_FILE_PATH);

        if (!direct.exists()) {
            direct.mkdirs();
        }

        //Create a DownloadManager for downloading file
        DownloadManager mgr = (DownloadManager) this.getSystemService(this.DOWNLOAD_SERVICE);

        //Parse URL to DownloadManager
        Uri downloadUri = Uri.parse(URL);
        DownloadManager.Request request = new DownloadManager.Request(
                downloadUri);

        //Set download settings
        request.setAllowedOverRoaming(false)
                .setVisibleInDownloadsUi(false)
                .setNotificationVisibility(2)
                .setDestinationInExternalFilesDir(this, LECTURE_FILE_PATH, getFilename());

        //Download
        mgr.enqueue(request);

        //Check download status
        while (true){
            //Get download status
            DownloadManager.Query query = new DownloadManager.Query();
            Cursor cursor = mgr.query(query);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = cursor.getInt(columnIndex);

            //Do actions depending on status
            if (DownloadManager.STATUS_SUCCESSFUL == status){
                //CONTINUE
                break;
            }
            else if (DownloadManager.STATUS_FAILED == status){
                //TRY AGAIN
            }
            else if (DownloadManager.STATUS_PAUSED == status){
                //Get user to turn on wifi/cellular
            }

        }

    }

    /*
    Funksjonen som gir true eller false avhengig av om personen er i tide eller ikke, må kjøreres etter at man har hentet ned kalenderen
     */
    public boolean isPersonOnTime(){
        return ((getEventHour() == getHoursPlussGMT()) && (getEventMin() >= 0 && getEventMin() <=15));
    }

    /*
    Getters and setters
     */

    public int getHoursPlussGMT(){
        return datemaker.getHours();
    }

    public int getMonth() {
        return month;
    }

    public int getToday() {
        return today;
    }

    public int getEventHour() {
        return eventHour;
    }

    public void setEventHour(int eventHour) {
        this.eventHour = eventHour;
    }

    public int getEventMin() {
        return eventMin;
    }

    public void setEventMin(int eventMin) {
        this.eventMin = eventMin;
    }

    public void setEventMonth(int eventMonth){
        this.eventMonth = eventMonth;
    }

    public int getEventMonth(){
        return this.eventMonth;
    }

    public void setEventDay(int eventDay){
        this.eventDay = eventDay;
    }

    public int getEventDay(){
        return this.eventDay;
    }

    public String getFilename() {
        return filename;
    }

    public void generateFileName() {
        this.filename =  "lecture_" + getLectureUsername() + ".ics";
    }

    public String getLectureUsername() {
        return lectureUsername;
    }

    public void setLectureUsername(String lectureUsername) {
        this.lectureUsername = lectureUsername;
    }

    public List<VEvent> getEvents() {
        return events;
    }

    public int getNextEventMonth() {
        return nextEventMonth;
    }

    public void setNextEventMonth(int nextEventMonth) {
        this.nextEventMonth = nextEventMonth;
    }

    public int getNextEventHour() {
        return nextEventHour;
    }

    public void setNextEventHour(int nextEventHour) {
        this.nextEventHour = nextEventHour;
    }

    public int getNextEventMin() {
        return nextEventMin;
    }

    public void setNextEventMin(int nextEventMin) {
        this.nextEventMin = nextEventMin;
    }

    public int getNextEventDay() {
        return nextEventDay;
    }

    public void setNextEventDay(int nextEventDay) {
        this.nextEventDay = nextEventDay;
    }


}
