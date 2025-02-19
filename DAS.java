import java.io.*;
import java.net.*;
import java.util.*;

public class DAS {
    static int port;
    static int number;
//    flag to check if it should be broadcasted or not
    static boolean isBroadcasted = false;
//    number of bytes in buffer(right now for integer)
    private static int arraySize = 4;

    public static void main(String... args) throws Exception {
//check if everything works as it should
        if (args.length != 2) {
            throw new Exception("Wrong number of arguments, try again: java DAS <port> <number>");
        }

        try {
            port = Integer.parseInt(args[0]);
            number = Integer.parseInt(args[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Wrong arguments");
        }

        try {
            DatagramSocket socket = new DatagramSocket(port);
            System.out.println("Port empty, changing mode: master");
            master(socket);
        } catch (SocketException e) {
            System.out.println("Port busy, changing mode: slave");
            slave();
        }
    }

    public static void master(DatagramSocket socket){
        List<Integer> intList = new ArrayList<>();
        intList.add(number);
        byte[] buffer = new byte[arraySize];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                System.out.println("Error receiving packet!");
            }
            int receivedNumber = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
            if(!isBroadcasted){
                try {
                    System.out.println("Packet received: "+receivedNumber);
                    byte[] ack = "ACK".getBytes();
                    DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, packet.getAddress(), packet.getPort());
                    socket.send(ackPacket);
                    System.out.println("Send ACK.");
                } catch (IOException e) {
                    System.err.println("Error receiving packet!: " + e.getMessage());
                }
            }
            //action based on received number
            switch (receivedNumber) {
                case 0:
                    int avg = (int) intList.stream().filter(e -> e != 0).mapToDouble(Integer::intValue).average().getAsDouble();
                    System.out.println("Average value: " + avg + ", starting broadcasting...\n");
                    broadcast(socket, avg);
                    break;
                case -1:
                    System.out.println("Number received: " + receivedNumber +", ending program...\n");
                    broadcast(socket, receivedNumber);
                    socket.close();
                    System.exit(0);
                    break;
                default:
                    if (!isBroadcasted) {
                        System.out.println("Number received: " +receivedNumber+", adding numbers to memory...\n");
                        intList.add(receivedNumber);
                    }
                    isBroadcasted = false;
                    break;
            }
        }
    }

    public static void slave() {
        try {
            int timeout = 2000;
            int ackSize = 3;
            DatagramSocket socket = new DatagramSocket(freePort());
            boolean ackReceived = false;

            //create buffers
            byte[] buffer = String.valueOf(number).getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), port);

            byte[] ackBuffer = new byte[ackSize];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            //send packets until master receives one
            while(!ackReceived){
                socket.send(packet);
                System.out.println("Send pakiet: " + number);
                try {
                    socket.setSoTimeout(timeout);
                    socket.receive(ackPacket);
                    String ackMessage = new String(ackPacket.getData(), 0, ackPacket.getLength());
                    if ("ACK".equals(ackMessage)) {
                        System.out.println("Confirmation received from master");
                        ackReceived = true;
                    } else {
                        System.out.println("Invalid message received: " + ackMessage);
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("No response, trying to resend...\n");
                }
            }
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //broadcasts number to your network
    public static void broadcast(DatagramSocket socket, Integer numberToBroadcast) {
        InetAddress broadcastAddress = null;
        try{
             broadcastAddress = getLocalAddress();
        }catch (UnknownHostException | SocketException e){
            System.out.println("Error in obtaining broadcast address "+ e);
        }
        byte[] buffer = String.valueOf(numberToBroadcast).getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, port);
            socket.setBroadcast(true);
            isBroadcasted = true;
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //returns the first empty port
    private static int freePort() {
        Random random = new Random();
        while (true) {
            int randomPort = random.nextInt(65536);
            try (DatagramSocket socket = new DatagramSocket(randomPort)) {
                socket.close();
                return randomPort;
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }
    //tries to get the first working broadcast address from your network
    public static InetAddress getLocalAddress() throws UnknownHostException, SocketException {
        for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast != null) {
                        return broadcast;
                    }
                }
            }
        }
        return InetAddress.getByName("255.255.255.255");
    }
}