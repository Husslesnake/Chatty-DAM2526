package dam.add;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 5000;
    private static List<SocketChannel> tcpClients = new ArrayList<>();
    private static Set<SocketAddress> udpClients = new HashSet<>();

    public static void main(String[] args) throws Exception {
        Selector selector = Selector.open();

        ServerSocketChannel tcpChannel = ServerSocketChannel.open();
        tcpChannel.bind(new InetSocketAddress(PORT));
        tcpChannel.configureBlocking(false);
        tcpChannel.register(selector, SelectionKey.OP_ACCEPT);

        DatagramChannel udpChannel = DatagramChannel.open();
        udpChannel.bind(new InetSocketAddress(PORT));
        udpChannel.configureBlocking(false);
        udpChannel.register(selector, SelectionKey.OP_READ);

        while (true) {
            selector.select();
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();

                if (key.isAcceptable()) {
                    SocketChannel sc = tcpChannel.accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.OP_READ);
                    tcpClients.add(sc);
                } else if (key.isReadable()) {
                    if (key.channel() instanceof SocketChannel) {
                        broadcastTCP((SocketChannel) key.channel());
                    } else {
                        broadcastUDP((DatagramChannel) key.channel());
                    }
                }
            }
        }
    }

    private static void broadcastTCP(SocketChannel source) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        if (source.read(buffer) <= 0) {
            tcpClients.remove(source);
            source.close();
            return;
        }
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        String msg = "[TCP] " + new String(data).trim();

        for (SocketChannel client : tcpClients) {
            client.write(ByteBuffer.wrap((msg + "\n").getBytes()));
        }
    }

    private static void broadcastUDP(DatagramChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        SocketAddress sender = channel.receive(buffer);
        udpClients.add(sender);
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        String msg = "[UDP] " + new String(data).trim();

        for (SocketAddress client : udpClients) {
            channel.send(ByteBuffer.wrap((msg + "\n").getBytes()), client);
        }
    }
}