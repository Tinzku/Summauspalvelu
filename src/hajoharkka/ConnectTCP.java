
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 *
 * ConnectTCP käytetään luomaan yhteys Y palvelimelle ja luomaan porttilista.
 *
 * @var boolean running: Tarkistaa onko ohjelma käynnissä.
 *
 */

public class ConnectTCP {

    private boolean running = true;

    /**
     * Luo portti listan Y palvelusta saadusta luvusta. Portien luvut luodaan
     * randomisti ja porttien lukumäärä vaihtelee 2-10 Jos saatu luku ei ole
     * 2-10 väliltä. Lähetetään Y plavelimelle arvo -1 ja asetetaan
     * "running"-arvoksi false
     *
     * @return int[]
     * @throws Exception
     */
    
    protected int[] ConnectList(Socket socket, ObjectInputStream oIn, ObjectOutputStream oOut) throws Exception {
        int[] portList;
        int t;
        try {
            t = oIn.readInt();
            if (t >= 2 || t <= 10) {
                portList = new int[t];
                for (int i = 0; i < t; i++) {
                    portList[i] = (int) (1025 + (Math.random() * 64510));
                }
                System.out.println("ConnectList made " + t + " port number");
                return portList;
            } // if

        } catch (IOException e) {
            oOut.writeInt(-1);
            oOut.flush();
            running = false;
        }
        return portList = new int[1];
    }

    /**
     * Palauttaa booleanina "running"-arvon
     *
     * @return boolean
     * @RESULT == (boolean)
     */
    
    public boolean runningStatus() {
        return running;
    }

    /**
     * Muodostaa TCP-yhteyden palvelimeen Y.
     *
     * @return socket
     * @pre true
     * @RESULT == (socket)
     *
     * @throws IOException
     */
    
    protected Socket makeTCP() throws IOException {
        int portNo = 13370;
        int counter = 0;
        ServerSocket serverSocket = new ServerSocket(portNo);
        Socket socket = new Socket();
        while (counter < 5) {
            try {
                SendUDP();
                serverSocket.setSoTimeout(5000);
                socket = serverSocket.accept();
                serverSocket.close();
                System.out.println("TCP online");
                break;

            } catch (IOException e) {
                counter++;
                System.out.println("Connect fail try:" + counter);
            }
        }
        return socket;
    }//makeTCP

    /**
     * Muodostaa UDP-yhteyden palvelimeen Y ja lähettää paketin, joka sisältää
     * X:n portin
     *
     * @throws IOException
     */
    
    private void SendUDP() throws IOException {
        int portNo = 13370;
        String port = Integer.toString(portNo);
        DatagramSocket socketUDP = new DatagramSocket();
        byte[] data = port.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 3126);
        socketUDP.send(packet);
        socketUDP.close();
        System.out.println("UDP sended");
    }//SendUDP
}