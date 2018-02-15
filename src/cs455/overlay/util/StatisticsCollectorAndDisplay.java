package cs455.overlay.util;

import cs455.overlay.wireformats.OverlayNodeReportsTrafficSummary;

import java.util.ArrayList;

public class StatisticsCollectorAndDisplay {
    private ArrayList<OverlayNodeReportsTrafficSummary> trafficSummaries;

    public StatisticsCollectorAndDisplay() {
        this.trafficSummaries = new ArrayList<>();
    }

    public synchronized void addTrafficSummary(OverlayNodeReportsTrafficSummary trafficSummary) {
        trafficSummaries.add(trafficSummary);
    }

    public synchronized void clearTrafficSummaries() {
        trafficSummaries.clear();
    }

    public void printTrafficSummary() {
        // Cumulative totals across all nodes
        int totalPacketsSnt = 0;
        int totalPacketsRcvd = 0;
        int totalPacketsRelayed = 0;
        long totalPacketsSntSummation = 0;
        long totalPacketsRcvdSummation = 0;

        // Print out the table header
        System.out.printf ("%-20s %-20s %-20s %-20s %-20s %-20s\n", "Node", "Packets Sent", "Packets Received", "Packets Relayed", "Sum Values Sent", "Sum Values Received");

        // Print the summaries for each node
        for (OverlayNodeReportsTrafficSummary trafficSummary : trafficSummaries) {
            int nodeID = trafficSummary.getID();
            int packetsSnt = trafficSummary.getTotalPacketsSent();
            int packetsRcvd = trafficSummary.getTotalPacketsRcvd();
            int packetsRelayed = trafficSummary.getTotalPacketsRelayed();
            long packetsSntSummation = trafficSummary.getSendSummation();
            long packetsRcvdSummation = trafficSummary.getRcvSummation();

            String nodeIDStr = "Node " + nodeID;
            System.out.printf ("%-20s %-20d %-20d %-20d %-20d %-20d\n", nodeIDStr,
                    packetsSnt, packetsRcvd, packetsRelayed, packetsSntSummation, packetsRcvdSummation);

            totalPacketsSnt += packetsSnt;
            totalPacketsRcvd += packetsRcvd;
            totalPacketsRelayed += packetsRelayed;
            totalPacketsSntSummation += packetsSntSummation;
            totalPacketsRcvdSummation += packetsRcvdSummation;
        }

        // Print the cumulative totals
        System.out.printf ("%-20s %-20d %-20d %-20d %-20d %-20d\n", "Sum",
                totalPacketsSnt, totalPacketsRcvd, totalPacketsRelayed, totalPacketsSntSummation, totalPacketsRcvdSummation);

        // Clear the traffic summaries so printing only correspods to the stats of a particular run
        // Allows us to get correct numbers for successive runs
        clearTrafficSummaries();
    }
}
