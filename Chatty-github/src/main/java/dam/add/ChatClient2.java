package dam.add;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient2 {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Nombre: ");
        String name = sc.nextLine();

        Socket tcpSocket = new Socket("localhost", 5000);
        DatagramSocket udpSocket = new DatagramSocket();
        InetAddress serverAddr = InetAddress.getByName("localhost");

        // Hilo para recibir TCP
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()))) {
                String msg;
                while ((msg = in.readLine()) != null) System.out.println(msg);
            } catch (Exception e) {}
        }).start();

        // Hilo para recibir UDP
        new Thread(() -> {
            try {
                byte[] buf = new byte[1024];
                while (true) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    udpSocket.receive(p);
                    System.out.println(new String(p.getData(), 0, p.getLength()).trim());
                }
            } catch (Exception e) {}
        }).start();

        System.out.println("Escribe 'u:mensaje' para UDP o solo el mensaje para TCP:");
        while (sc.hasNextLine()) {
            String raw = sc.nextLine();
            if (raw.startsWith("u:")) {
                byte[] b = (name + ": " + raw.substring(2)).getBytes();
                udpSocket.send(new DatagramPacket(b, b.length, serverAddr, 5000));
            } else {
                PrintWriter out = new PrintWriter(tcpSocket.getOutputStream(), true);
                out.println(name + ": " + raw);
            }
        }
    }
}