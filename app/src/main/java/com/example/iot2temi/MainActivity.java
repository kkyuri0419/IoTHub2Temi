package com.example.iot2temi;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.charset.Charset;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledExecutorService;

public class MainActivity extends AppCompatActivity {

    TextView txtview;
    TextView tempShowtxt;
    TextView humShowtxt;
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

                            txtview.setText(msg);


                            Log.e(this.getClass().getName(),msg + "TTTTTTTTTTTTTTTTT");

//                            Object s = receivedEvent.getObject().;

                            int i = msg.indexOf(":");
                            int y = msg.indexOf("y");
                            String tempval = msg.substring(i+2,i+7);
                            String humval = msg.substring(y+3,y+9);


                            Log.e(this.getClass().getName(),tempval + "QQQQQQQQQQQQQQQ");
                            Log.e(this.getClass().getName(),humval + "RRRRRRRRRRRRRR");

                            tempShowtxt.setText(tempval);
                            humShowtxt.setText(humval);

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