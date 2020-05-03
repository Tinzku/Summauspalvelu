
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

/**
*@var int[] portList: Lista porteista, joita SocketSum kuuntelee
*@var ArrayList<Integer> valueList: Lista portien arvoista jotka soketit saavat, jokisella portilla on omakohta listalla
*@var ArrayList<Thread> socketPortList: Lista porteista jotka luodaan portListin mukaan.
*@var int counter: Pitää listaa kuinkamonta kertaa soketti on saanut luvun käsiteltäväksi, poislukien arvon nolla.
*@var ConnectTCP ct: Avustaja jolle on jaettu osa toteutuksesta.
*
*@implements Runnable
*/

public class CounterServer implements Runnable {

    private int[] portList;
    private final ArrayList<Integer> valueList = new ArrayList<>();
    private final ArrayList<Thread> socketPortList = new ArrayList<>();
    private boolean running;
    private int counter = 0;
    private final ConnectTCP ct = new ConnectTCP();

    public static void main(String[] args) {
        CounterServer counterServer = new CounterServer();
        counterServer.run();
    }
    
    /**
    * UDP -vaihdon jälkeen aloitetaan TCP-yhteys ja luodaan sekä
    * Input- ja OutputStreamit. 
    * Boolean "running" voidaan testata pitääkö ohjelman vielä suorittaa, 
    * jos palvelimelta Y saadaan "case 0", voidaan lopettaa suoritus.
    */
    
    public void run() {
        Socket socket = new Socket();
        ObjectOutputStream oOut = null;
        ObjectInputStream oIn = null;

        try {
            socket = ct.makeTCP();
            System.out.println("My port: " + socket.getPort());

            OutputStream oS = socket.getOutputStream();
            InputStream iS = socket.getInputStream();
            oOut = new ObjectOutputStream(oS);
            oIn = new ObjectInputStream(iS);

            portList = ct.ConnectList(socket, oIn, oOut);
            running = ct.runningStatus();
            setAndStart(socket, oOut);
            running = true;
            while (running) {
                asker(socket, oIn, oOut);
            }
            socket.close();
        } catch (Exception e) {
        }
    }//run
    
    /**
    * Lisää "socketPortList"-taulukkoon uuden SocketSum-olion ja sille portin, sekä
    * lisää sen "valueList"-ArrayListiin. Sen jälkeen kyseinen säie käynnistetään. 
    * Alempi for-loop printtaa portListin tietoja.
    * 
    * @throws IOException
    */

    public void setAndStart(Socket socket, ObjectOutputStream oOut) throws IOException {
        
        //Luonti ja lisäys listaan 
        for (int i = 0; i < portList.length; i++) {
            socketPortList.add(new SocketSum(i, portList[i]));
            valueList.add(i, 0);
            //Käynnistys
            socketPortList.get(i).start();
        }
        // Printataan portin tiedot portLististä.
        for (int k = 0; k < portList.length; k++) {
            oOut.writeInt(portList[k]);
            oOut.flush();
        }
    }//setAndSend
    
    /**
    * getSum(): Laskee jokaisen valueList listan soketin saamien summat yhteen ja paluttaa saadun tuloksen.
    * getBiggest(): Antaa valueListan suurimman arvon ja plauttaa sen.
    * getCount(): Palauttaa counter-arvon.
    */
    
    /**
    * @return sum
    * @pre true
    * @RESULT == (sum)
    */
    
    public int getSum() {
        int sum = 0;
        synchronized (valueList) {
            for (Integer i : valueList) {
                sum = sum + i;
            }
            return sum;
        }
    }//getSum
    
    /**
    * @return valueList.indexOf(Collections.max(valueList)) + 1 
    * @pre true
    * @RESULT == (valueList.indexOf(Collections.max(valueList)) + 1)
    */

    public int getBiggest() {
        synchronized (valueList) {
            return valueList.indexOf(Collections.max(valueList)) + 1;
        }
    }//getBiggest
    
    /**
    * @return counter
    * @pre true
    * @RESULT == (counter)
    */

    public int getCount() {
        return counter;
    }//getCount
    
		/**
    * @var int port: arvo port saadaan alustuksessa.
    * @var int address: arvo address saadaan alustuksessa.
    * @var ServerSocket serverSocket: Oletettu ServerSocket joka luodaan saadun portin mukaan.
    * @var boolean running: Pitää ohjlmaa päälä siihen asti kunnes sen arvo muuttu falseksi.
    */
    
    public class SocketSum extends Thread {

        private final int port;
        private final int address;
        private ServerSocket serverSocket;
        private boolean running = true;
        
	//Alustetaan SocketSum
        public SocketSum(int address, int port) throws IOException {
            this.port = port;
            this.address = address;
        }//SocketSum
				
        //Luodaan serverSocket ja ryhdytään lukemaan int-arvoja "while"-loopissa.
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                serverSocket.setSoTimeout(5000);
                Socket socket = serverSocket.accept();
                System.out.println("Socket number:" + address + " Connect port:" + port);
                OutputStream oS = socket.getOutputStream();
                InputStream iS = socket.getInputStream();
                ObjectOutputStream oOn = new ObjectOutputStream(oS);
                ObjectInputStream oIn = new ObjectInputStream(iS);
                int value = 0;
                int tmp = 0;

                while (running) {

                    value = oIn.readInt();
                    if (value == 0) {
                        break;
                    }
                    tmp = valueList.get(address);
                    value += tmp;
                    valueList.set(address, value);
                    counter++;
                }
                socket.close();
            } catch (IOException e) {
                try {
                    join(1000);
                } catch (InterruptedException ex) {
                    System.out.println(address + " Broken ");
                }
            }
        }//run
    }//SocketSum
    
    /**
    * Suorittaa kyselyyn vastaamiseen. Vaatii toimiakseen socketin, 
    * ObjectInputStream ja ObjectOutputStream.
    *
    * Palvelin Y voi kysyä askerilta kolmenlaista tietoa:
    * Case 1: Tähän mennessä välitettyjen lukujen summma
    * Case 2: Tähän mennessä sokettie suurinta arvoa.
    * Case 3: Tähän mennessä kaikkien välitettyjen lukujen kokonaislukumäärä.
    * Case 0: Saa tiedon, että Y on siirtänyt tarvittavat luvut.
    * Default: Sammutta askerin jos saadaan muutakuin hyväksyty kysely arvo.
    *
    * Jos X vastaanottaa jonkun muun luvun kuin 1, 2, 3 tai 0, se vastaa Y:lle luvulla "-1".
    *
    * @throws IOException
    */
    
    private void asker(Socket socket, ObjectInputStream oIn, ObjectOutputStream oOut) throws IOException {
        while (running) {
            //Kyselyä hoidetaan numero arvoilla 0 ja 3 väliltä.
            int cases = oIn.readInt();
            try {
                switch (cases) {

                    //Sammutta askerin.
                    case 0:

                        running = false;
                        for (Thread saie : socketPortList) {
                            saie.join();
                        }
                        System.out.println("Case:" + cases + " DONE! Quitting...");
                        oOut.flush();
                        break;

                    //Vastaa summaus kyselyyn
                    case 1:
                        System.out.println(valueList.toString());
                        System.out.println("Case:" + cases + "	answer:" + getSum());
                        oOut.writeInt(getSum());
                        oOut.flush();
                        break;

                    //Vastaa suurimman arvon kyselyyn
                    case 2:
                        System.out.println(valueList.toString());
                        System.out.println("Case:" + cases + " answer:" + getBiggest());
                        oOut.writeInt(getBiggest());
                        oOut.flush();
                        break;
                        
                    //Vastaa lukujen sen hetkisen kokonaismäärän
                    case 3:

                        System.out.println(valueList.toString());
                        System.out.println("Case:" + cases + " answer:" + getCount());
                        oOut.writeInt(getCount());
                        oOut.flush();
                        break;
                        
                    //Oletus arvoinen toiminta, jos saadaan muu kuin hyväksytty kyselyn arvo.
                    default:

                        running = false;
                        System.out.println("Case:" + cases + " close...");
                        oOut.writeInt(-1);
                        oOut.flush();
                        break;
                }
            } catch (IOException | InterruptedException e) {
                e.toString();
            }
        }
        oOut.flush();
        oOut.close();
        oIn.close();
    }//asker
}//CounterServer