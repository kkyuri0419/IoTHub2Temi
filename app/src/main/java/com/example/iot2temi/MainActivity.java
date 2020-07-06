package com.example.iot2temi;

import androidx.appcompat.app.AppCompatActivity;
import pl.pawelkleczkowski.customgauge.CustomGauge;

import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.microsoft.azure.eventhubs.ConnectionStringBuilder;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.EventHubException;
import com.microsoft.azure.eventhubs.EventPosition;
import com.microsoft.azure.eventhubs.EventHubRuntimeInformation;
import com.microsoft.azure.eventhubs.PartitionReceiver;

import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.nio.charset.Charset;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledExecutorService;

public class MainActivity extends AppCompatActivity {

    TextView txtview;
    TextView tempShowtxt;
    TextView humShowtxt;
    TextView luxShowtxt;
    CustomGauge gauge1;
    CustomGauge gauge2;
    CustomGauge gauge3;
    ProgressBar progress1;

    boolean isFistVal = true;

    int lasttemp;
    int lasthum;
    int lastlux;
    int inttemp;
    int inthum;
    int intlux;
    int difftemp=0;
    int diffhum=0;
    int difflux=0;

//    Button btnStart;
//    Button btnStop;

    // az iot hub show --query properties.eventHubEndpoints.events.endpoint --name {your IoT Hub name}
    private static final String eventHubsCompatibleEndpoint = "sb://iothub-ns-xdk2iot-3630662-a0007a5c12.servicebus.windows.net/";

    // az iot hub show --query properties.eventHubEndpoints.events.path --name {your IoT Hub name}
    private static final String eventHubsCompatiblePath = "xdk2iot";

    // az iot hub policy show --name service --query primaryKey --hub-name {your IoT Hub name}
    private static final String iotHubSasKey = "jnALouW45kFUOsnRx2bhfevWeBt4SNbNJUYtrf5dGGA=";
    private static final String iotHubSasKeyName = "service";

    // Track all the PartitionReciever instances created.
    private static ArrayList<PartitionReceiver> receivers = new ArrayList<PartitionReceiver>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtview = findViewById(R.id.txtview);
        tempShowtxt = findViewById(R.id.tempShowtxt);
        humShowtxt = findViewById(R.id.humShowtxt);
        luxShowtxt = findViewById(R.id.luxShowtxt);
        gauge1 = findViewById(R.id.gauge1);
        gauge2 = findViewById(R.id.gauge2);
        gauge3 = findViewById(R.id.gauge3);
        progress1 = findViewById(R.id.progress1);
//        btnStart = findViewById(R.id.btnStart);
//        btnStop = findViewById(R.id.btnStop);
        BasicConfigurator.configure();
        start();
    }


    // Asynchronously create a PartitionReceiver for a partition and then start
    // reading any messages sent from the simulated client.
    public void receiveMessages(EventHubClient ehClient, String partitionId)
            throws EventHubException, ExecutionException, InterruptedException {

//        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final ScheduledExecutorService scheduledexecutorService = Executors.newSingleThreadScheduledExecutor();


        // Create the receiver using the default consumer group.
        // For the purposes of this sample, read only messages sent since
        // the time the receiver is created. Typically, you don't want to skip any messages.
        ehClient.createReceiver(EventHubClient.DEFAULT_CONSUMER_GROUP_NAME, partitionId,
                EventPosition.fromEnqueuedTime(Instant.now())).thenAcceptAsync(receiver -> {
            System.out.println(String.format("Starting receive loop on partition: %s", partitionId));
            System.out.println(String.format("Reading messages sent since: %s", Instant.now().toString()));

            receivers.add(receiver);

            while (true) {
                try {
                    // Check for EventData - this methods times out if there is nothing to retrieve.
                    Iterable<EventData> receivedEvents = receiver.receiveSync(100);

                    // If there is data in the batch, process it.
                    if (receivedEvents != null) {
                        for (EventData receivedEvent : receivedEvents) {
                            String msg = new String(receivedEvent.getBytes(), Charset.defaultCharset());
                            String msg2 = new String(receivedEvent.getBytes(), Charset.defaultCharset());

                            // display the message
                            txtview.setText(msg);

                            Log.e(this.getClass().getName(),msg + "TTTTTTTTTTTTTTTTT");

                            int i = msg.indexOf("p");
                            int y = msg.indexOf("y");
                            int x = msg.indexOf("x");
                            String tempval = msg.substring(i+4,i+9);
                            String humval = msg.substring(y+4,y+6);
                            String luxval = msg.substring(x+4,x+6);

                            Log.e(this.getClass().getName(),tempval + "QQQQQQQQQQQQQQQ");
                            Log.e(this.getClass().getName(),humval + "RRRRRRRRRRRRRR");
                            Log.e(this.getClass().getName(),luxval + "CCCCCCCCCCCCCC");

                            tempShowtxt.setText(tempval);
                            humShowtxt.setText(humval);
                            luxShowtxt.setText(luxval);

                            inttemp = Double.valueOf(tempval).intValue();
                            inthum = (Double.valueOf(humval).intValue());
                            intlux = (Double.valueOf(luxval).intValue());

                            if (isFistVal){
                                gauge1.setValue(inttemp);
                                gauge2.setValue(inthum);
                                gauge3.setValue(intlux);
                                Log.e(this.getClass().getName(),"true");
                                isFistVal = false;
                                lasthum = inthum;
                                lasttemp = inttemp;
                                lastlux = intlux;
                            }else{
                                difftemp = inttemp - lasttemp;
                                Log.e(this.getClass().getName(), "lasttemp : " +(String.valueOf(lasttemp)));
                                Log.e(this.getClass().getName(), "difftemp : " +(String.valueOf(difftemp)));
                                diffhum = inthum - lasthum;
                                Log.e(this.getClass().getName(), "lasthum : " +(String.valueOf(lasthum)));
                                Log.e(this.getClass().getName(),"diffhum : " +(String.valueOf(diffhum)));
                                difflux = intlux - lastlux;
                                Log.e(this.getClass().getName(), "lastlux : " +(String.valueOf(lastlux)));
                                Log.e(this.getClass().getName(),"difflux : " +(String.valueOf(difflux)));

                                if (difflux < 0){
                                    difflux *= -1;
                                    Runnable LuxDrunnable = new LuxDecRunnable(inthum, lasthum, diffhum);
                                    new Thread(LuxDrunnable).start();

                                }else if (difflux > 0){
                                    Runnable LuxIncRunnable = new LuxIncRunnable(inttemp, lasttemp, difftemp);
                                    new Thread(LuxIncRunnable).start();

                                }else if (difflux == 0){
                                    lastlux = intlux;
                                    Log.e(this.getClass().getName(),"SAME LUX");
                                }

                                if (difftemp < 0){
                                    difftemp *= -1;
                                    Runnable tempDrunnable = new TempDecRunnable(inthum, lasthum, diffhum);
                                    new Thread(tempDrunnable).start();

                                }else if (difftemp > 0){
                                    Runnable tempIrunnable = new TempIncRunnable(inttemp, lasttemp, difftemp);
                                    new Thread(tempIrunnable).start();

                                }else if (difftemp == 0){
                                    lasttemp = inttemp;
                                    Log.e(this.getClass().getName(),"SAME TEMP");
                                }

                                if (diffhum < 0){
                                    diffhum *= -1;
                                    Runnable humDrunnable = new HumDecRunnable(inthum, lasthum, diffhum);
                                    new Thread(humDrunnable).start();

                                }else if (diffhum > 0){
                                    Runnable HumIncRunnable = new HumIncRunnable(inttemp, lasttemp, difftemp);
                                    new Thread(HumIncRunnable).start();

                                }else if (diffhum == 0){
                                    lasthum = inthum;
                                    Log.e(this.getClass().getName(),"SAME HUM");
                                }
                            }

                            System.out.println(String.format("Telemetry received:\n %s",
                                    new String(receivedEvent.getBytes(), Charset.defaultCharset())));
                            System.out.println(String.format("Application properties (set by device):\n%s", receivedEvent.getProperties().toString()));
                            System.out.println(String.format("System properties (set by IoT Hub):\n%s\n", receivedEvent.getSystemProperties().toString()));
                        }
                    }
                } catch (EventHubException e) {
                    System.out.println("Error reading EventData");
                }
            }
        }, scheduledexecutorService);

    }

    public void start() {

        try {

            final ConnectionStringBuilder connStr = new ConnectionStringBuilder()
                    .setEndpoint(new URI(eventHubsCompatibleEndpoint))
                    .setEventHubName(eventHubsCompatiblePath)
                    .setSasKeyName(iotHubSasKeyName)
                    .setSasKey(iotHubSasKey);

            System.out.println(connStr.toString());
            Log.e(this.getClass().getName(),connStr.toString() + "AAAAAAAAAAAAAAAAAAAA");

            // Create an EventHubClient instance to connect to the
            // IoT Hub Event Hubs-compatible endpoint.
            final ScheduledExecutorService scheduledexecutorService = Executors.newSingleThreadScheduledExecutor();

            final EventHubClient ehClient = EventHubClient.createSync(connStr.toString(), scheduledexecutorService);

            // Use the EventHubRunTimeInformation to find out how many partitions
            // there are on the hub.
            final EventHubRuntimeInformation eventHubInfo = ehClient.getRuntimeInformation().get();

            // Create a PartitionReciever for each partition on the hub.
            for (String partitionId : eventHubInfo.getPartitionIds()) {
                receiveMessages(ehClient, partitionId);
            }

        } catch (ExecutionException|InterruptedException|IOException|URISyntaxException|EventHubException e){
            System.err.println("Exception : " + e);
        }
    }

    public class TempIncRunnable implements Runnable {

        public TempIncRunnable(int pres, int last, int diff){
        }
        @Override
        public void run() {

            for (int i = 1; i <= difftemp; i++){
                gauge1.setValue(lasttemp+=1);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                Log.e(this.getClass().getName(),"Thread Success11111");
            }
            lasttemp = inttemp;
        }
    }

    public class TempDecRunnable implements Runnable {

        public TempDecRunnable(int pres, int last, int diff){

        }
        @Override
        public void run() {

            for (int i = 1; i <= difftemp; i++){
                gauge1.setValue(lasttemp-=1);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                Log.e(this.getClass().getName(),"Thread Success22222");
            }
            lasttemp = inttemp;
        }
    }

    public class HumIncRunnable implements Runnable {

        public HumIncRunnable(int pres, int last, int diff){

        }
        @Override
        public void run() {

            for (int i = 1; i <= diffhum; i++) {
                gauge2.setValue(lasthum+=1);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                Log.e(this.getClass().getName(),"Thread Success33333");
                lasthum = inthum;
            }

        }
    }

    public class HumDecRunnable implements Runnable {

        public HumDecRunnable(int pres, int last, int diff){

        }
        @Override
        public void run() {

            for (int i = 1; i <= diffhum; i++){
                gauge2.setValue(lasthum-=1);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                Log.e(this.getClass().getName(),"Thread Success444444");
            }
            lasthum = inthum;
        }
    }

    public class LuxIncRunnable implements Runnable {

        public LuxIncRunnable(int pres, int last, int diff){

        }
        @Override
        public void run() {

            for (int i = 1; i <= difflux; i++) {
                gauge3.setValue(lastlux+=1);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                Log.e(this.getClass().getName(),"Thread Success33333");
                lastlux = intlux;
            }

        }
    }

    public class LuxDecRunnable implements Runnable {

        public LuxDecRunnable(int pres, int last, int diff){

        }
        @Override
        public void run() {

            for (int i = 1; i <= difflux; i++){
                gauge3.setValue(lastlux-=1);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                Log.e(this.getClass().getName(),"Thread Success444444");
            }
            lastlux = intlux;
        }
    }
}

//        public void btnStopOnClick() throws EventHubException, ExecutionException, InterruptedException, IOException, URISyntaxException{
//
//            final ConnectionStringBuilder connStr = new ConnectionStringBuilder()
//                    .setEndpoint(new URI(eventHubsCompatibleEndpoint))
//                    .setEventHubName(eventHubsCompatiblePath)
//                    .setSasKeyName(iotHubSasKeyName)
//                    .setSasKey(iotHubSasKey);
//
//            final ExecutorService executorService = Executors.newSingleThreadExecutor();
//            final EventHubClient ehClient = EventHubClient.createSync(connStr.toString(), (ScheduledExecutorService) executorService);
//
//            // Shut down cleanly.
////            System.out.println("Press ENTER to exit.");
////            System.in.read();
//            System.out.println("Shutting down...");
//            for (PartitionReceiver receiver : receivers) {
//                receiver.closeSync();
//            }
//            ehClient.closeSync();
//            executorService.shutdown();
//            System.exit(0);
//        }