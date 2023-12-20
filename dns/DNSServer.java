package dns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Class representing a DNS Server.
 *
 * @version 1.0
 */
public class DNSServer {
    
    /**
     * DNS uses port UDP port 53 for the server
     */
    final private int PORT = 53;

    /**
     * set the maximum packet size to be 512 bytes for DNS messages
     */
    final private int MAX_SIZE = 512;

    /**
     * this server will handle requests for a single zone/domain
     */
    private DNSZone zone;

    // TODO: add class variable for the cache
    private DNSCache cache;

    // TODO: add class variable to track pending queries
    private HashMap<String, DatagramPacket> pendingQueries = new HashMap<>();

    /**
     * all queries sent from this server will go to a single "upstream" server
     */
    private InetAddress nextServer;
    private int nextServerPort;

    /**
     * Required constructor that simply prints out some messages about the server.
     *
     * @param zone a DNSZone object that has already been constructed
     */
    public DNSServer(DNSZone zone) {
        this.zone = zone;

        cache = new DNSCache();
    
        try {
            nextServer = InetAddress.getByName("127.0.0.53");
        } catch(UnknownHostException e) {
            System.out.println("Should never get here.");
            System.exit(0);
        }
        nextServerPort = 53;

        System.out.printf("Starting server on port %d%n", PORT);
    }

    /**
     * handle one incoming DNS query message
     * TODO: complete me!
     *
     * @param   query   the DNS query message
     * @return          a DatagramPacket object with the response message
     */
    private DatagramPacket handleQuery(DNSMessage query) {
        System.out.println("Query received from " + query.getPacket().getSocketAddress());
        System.out.println(query);

        boolean inZone = true;
        var records = zone.getRecords(query.getQuestionName(), query.getQuestionType(), query.getQuestionClass());

        if (records.isEmpty()) {
            inZone = false;
            records = cache.getRecords(query.getQuestionName());
        }

        if (records.size() != 0) {
            var reply = new DNSMessage(query, records, inZone);

            System.out.println("Reply to " + query.getPacket().getSocketAddress());
            System.out.println(reply);

            return new DatagramPacket(reply.getData(), reply.getDataLength(), query.getPacket().getSocketAddress());
        }

        System.out.println("Forwarding query to next server: " + nextServer.getHostAddress() + ":" + nextServerPort);
        System.out.println("Forwarded Query: " + query);
        
        pendingQueries.put(query.getQuestionName(), query.getPacket());
        byte[] queryData = query.getData();
        DatagramPacket forwardPacket = new DatagramPacket(queryData, queryData.length, nextServer, nextServerPort);
        
        return forwardPacket;
    }

    /**
     * handle one incoming DNS reply message
     * TODO: complete me!
     *
     * @param   reply   the incoming reply message
     * @return          a DatagramPacket object with the response message
     */
    private DatagramPacket handleReply(DNSMessage reply) {
        System.out.println("Reply received from " + reply.getPacket().getSocketAddress());
        System.out.println(reply);
        
        DatagramPacket packet = pendingQueries.get(reply.getQuestionName());
        
        reply.getAnswers().forEach(record -> cache.addRecord(record.getName(), record));
        pendingQueries.remove(reply.getQuestionName());
        
        System.out.println("Processed reply: " + reply);
        byte[] responseData = reply.getData();
        
        return new DatagramPacket(responseData, responseData.length, packet.getAddress(), packet.getPort());
    }


    /**
     * handle one DNS message
     *
     * @param   incomingPkt the UDP packet containing the incoming DNS message
     * @return              a UDP packet containing the DNS response
     */
    private DatagramPacket handleMessage(DatagramPacket incomingPkt) {
        cache.cleanup();

        DNSMessage message = new DNSMessage(incomingPkt);

        if (message.isQuery()) 
            return handleQuery(message);
        else 
            return handleReply(message);
        
    }
    /**
     * Open a socket to receive UDP packets and handle those packets
     */
    public void run() {
        // open the socket, ensure it will close when the try block finishes
        try (
            // listen on localhost only
            var sock = new DatagramSocket(PORT, InetAddress.getLoopbackAddress());
        ) {
            // keep reading packets one at a time, forever
            while(true) {
                // packet to store the incoming message
                var in_packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);

                // blocking call, read one packet
                sock.receive(in_packet);

                // handle this packet
                var out_packet = handleMessage(in_packet);

                // only send a response if there were no errors
                if (out_packet != null) {
                    sock.send(out_packet);
                }
            }
        } catch(IOException e) {
            // Have to catch IOexceptions for most socket calls
            System.out.println("Network error!");
        }
    }

    /**
     * Server starting point
     *
     * @param args should contain a single value, the filename of the zone file
     */
    public static void main(String[] args) {
        // must have exactly a single command line argument
        if(args.length != 1) {
            System.out.println("Usage: sudo java dns.DNSServer zone_file");
            System.exit(0);
        }

        // make the zone, which will exit() if the file is invalid in any way
        var zone = new DNSZone(args[0]);

        // make the server object then start listening for DNS requests
        var server = new DNSServer(zone);
        server.run();
    }
}