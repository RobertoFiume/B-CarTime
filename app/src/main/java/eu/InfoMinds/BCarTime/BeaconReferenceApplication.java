package eu.InfoMinds.BCarTime;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import eu.InfoMinds.BCarTime.R;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.altbeacon.beacon.startup.BootstrapNotifier;

import java.time.DateTimeException;
import java.util.Collection;
import java.util.Date;

/**
 * Created by @Roberto Fiume 17-3-2020.
 */
public class BeaconReferenceApplication extends Application implements BootstrapNotifier , RangeNotifier{
    private static final String TAG = "BeaconReferenceApp";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private boolean haveDetectedBeaconsSinceBoot = false;
    private MonitoringActivity monitoringActivity = null;
    private String cumulativeLog = "";
    private BeaconManager beaconManager;
    private boolean hasEnterInRegion = false;
    private static final String CHANNEL_ID = "BCARTIME_ID";
    private static final String CHANNEL_NOTIFICAION_NAME = "BCARTIME NOTIFICATION";
    private static final String CHANNEL_NOTIFICAION_DESC = "B-CarTime notification";
    private static final int NOTIFICATION_COLOR = 0x3497D9;
    private static final String  ARUBA_BEACON_UUID = "4152554e-f99b-4a3b-86d0-947070693a78";
    private static final String  ACS_BEACON_UUID = "e2c56db5-dffb-48d2-b060-d0f5a71096e0";
    private static Context appContext;

    public void onCreate() {
        super.onCreate();

        appContext = getApplicationContext();
        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);

        // By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
        // find a different type of beacon, you must specify the byte layout for that beacon's
        // advertisement with a line like below.  The example shows how to find a beacon with the
        // same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb.  To find the proper
        // layout expression for other beacon types, do a web search for "setBeaconLayout"
        // including the quotes.
        //
        //beaconManager.getBeaconParsers().clear();
        //beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        //Parse IBeacon structure
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        beaconManager.setDebug(true);


        // Uncomment the code below to use a foreground service to scan for beacons. This unlocks
        // the ability to continually scan for long periods of time in the background on Andorid 8+
        // in exchange for showing an icon at the top of the screen and a always-on notification to
        // communicate to users that your app is using resources in the background.
        //
        createNotificationChannelid();
        buildNotification();

        // For the above foreground scanning service to be useful, you need to disable
        // JobScheduler-based scans (used on Android 8+) and set a fast background scan
        // cycle that would otherwise be disallowed by the operating system.
        //
        beaconManager.setEnableScheduledScanJobs(false);
        beaconManager.setBackgroundBetweenScanPeriod(0);
        beaconManager.setBackgroundScanPeriod(1100);


        Log.d(TAG, "setting up background monitoring for beacons and power saving");
        // wake up the app when a beacon is seen
       //Region region = new Region(ARUBA_BEACON_UUID, null, null, null);
        Region region = new Region(ACS_BEACON_UUID, null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);


        // simply constructing this class and holding a reference to it in your custom Application
        // class will automatically cause the BeaconLibrary to save battery whenever the application
        // is not visible.  This reduces bluetooth power usage by about 60%
        backgroundPowerSaver = new BackgroundPowerSaver(this);

        // If you wish to test beacon detection in the Android Emulator, you can use code like this:
        // BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
        // ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();
    }

    public void disableMonitoring() {
        if (regionBootstrap != null) {
            regionBootstrap.disable();
            regionBootstrap = null;
        }
    }
    public void enableMonitoring() {
        ////Region region = new Region(ARUBA_BEACON_UUID, null, null, null);
        Region region = new Region(ACS_BEACON_UUID, null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);
    }

    @Override
    public void didEnterRegion(Region region) {
        // In this example, this class sends a notification to the user whenever a Beacon
        // matching a Region (defined above) are first seen.
        Log.d(TAG, "did enter region.");
        if (!haveDetectedBeaconsSinceBoot) {
            Log.d(TAG, "auto launching MainActivity");

            // The very first time since boot that we detect an beacon, we launch the
            // MainActivity
            Intent intent = new Intent(this, MonitoringActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Important:  make sure to add android:launchMode="singleInstance" in the manifest
            // to keep multiple copies of this activity from getting created if the user has
            // already manually launched the app.
            this.startActivity(intent);
            haveDetectedBeaconsSinceBoot = true;

            sendNotification(getNLS(R.string.enter_car));
        } else {
            sendNotification(getNLS(R.string.enter_car));

            if (monitoringActivity != null) {
                // If the Monitoring Activity is visible, we log info about the beacons we have
                // seen on its display
                Log.d(TAG, "I see a beacon again" );
            } else {
                // If we have already seen beacons before, but the monitoring activity is not in
                // the foreground, we send a notification to the user on subsequent detections.
                Log.d(TAG, "foreground notification.");
           }
        }
    }

    @Override
    public void didExitRegion(Region region) {
        if (hasEnterInRegion) {
            stopTimeAttendance(new Date());
            sendNotification(getNLS(R.string.exit_car));

            Log.d(TAG, "Exit region.");
        }

        hasEnterInRegion = false;
        if (monitoringActivity != null) {
            monitoringActivity.populateListView();
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.d(TAG," Current region state is: " + (state == 1 ? "INSIDE" : "OUTSIDE ("+state+")"));
        //RF: attiva la determinazione della distanza (proximity)
        try {
            beaconManager.startRangingBeaconsInRegion(region);
            beaconManager.addRangeNotifier(this);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        for (Beacon beacon: beacons) {
            // This beacon is immediate (< 0.5 meters)
            if (beacon.getDistance() <= 1.5) {
                if (! hasEnterInRegion) {
                   if (region.getUniqueId().toString() != ACS_BEACON_UUID)
                        return;

                    String meterDistance = String.format("%.2f",beacon.getDistance());

                    hasEnterInRegion = true;
                    startTimeAttendance(new Date());
                    sendNotification(getNLS(R.string.enter_car) + " - " +  getNLS(R.string.distance) + ": " + meterDistance  + " " + getNLS(R.string.meters));
                }

                if (monitoringActivity != null) {
                    monitoringActivity.populateListView();
                }
            }
            else if(beacon.getDistance() < 3.0) {
                // This beacon is near (0.5 to 3 meters)
            }
            else {
                // This beacon is far (> 3 meters)
            }
        }
    }

    private void createNotificationChannelid() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NOTIFICAION_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_NOTIFICAION_DESC);
            channel.enableLights(true);
            channel.setLightColor(NOTIFICATION_COLOR);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void buildNotification() {
        Intent intent = new Intent(this, MonitoringActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this,CHANNEL_ID);
        }
        else {
            builder = new Notification.Builder(this);
        }

        builder
           .setSmallIcon(R.drawable.ic_notification)
           .setColor(NOTIFICATION_COLOR)
           .setContentTitle(getNLS(R.string.notification_car))
           .setContentIntent(pendingIntent);

        beaconManager.enableForegroundServiceScanning(builder.build(), 456);
    }

    private void sendNotification(String message) {
        Intent intent = new Intent(this, MonitoringActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this,CHANNEL_ID);
        }
        else {
            builder = new Notification.Builder(this);
        }

        builder
           .setContentTitle(getNLS(R.string.notification_car_title))
           .setContentText(message)
           .setSmallIcon(R.drawable.ic_notification)
           .setColor(NOTIFICATION_COLOR)
           .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }
    
    private String getNLS(int id) {
       Resources res = getResources();
       return res.getString(id);
    }

    private void startTimeAttendance(Date date) {
        /* try {
            DatabaseHelper database = new DatabaseHelper(this);

            database.updateTimeAttendance(date); //RF: Close time attendance open
            database.insertTimeAttendance(date);
        } catch (DateTimeException e) {
            Log.d(TAG, "Error on start time attendance: " + e.getMessage());
        }*/
        setTimeAttendance(date);
    }

    private void stopTimeAttendance(Date date) {
        /*try {
            DatabaseHelper database = new DatabaseHelper(this);

            database.updateTimeAttendance(date);
        } catch (DateTimeException e) {
            Log.d(TAG, "Error on stop time attendance: " + e.getMessage());
        }*/
        setTimeAttendance(date);
    }

    private void setTimeAttendance(Date date) {
        try {
            DatabaseHelper database = new DatabaseHelper(this);

            database.setTimeAttendance(date);
        } catch (DateTimeException e) {
            Log.d(TAG, "Error on set time attendance: " + e.getMessage());
        }
    }

    public void setMonitoringActivity(MonitoringActivity activity) {
        this.monitoringActivity = activity;
    }

    public String getLog() {
        return cumulativeLog;
    }
}
