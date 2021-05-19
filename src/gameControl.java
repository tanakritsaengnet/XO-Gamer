
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class gameControl implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(gameControl.class.getName());
    private String ip = "localhost";
    private int port = 3000;
    private Socket socket;
    private ServerSocket serverSocket;
    private BufferedReader br;
    private PrintWriter pw;
    private Thread thread;
    private FrameGame FrameGame = new FrameGame();

    private boolean isWon;
    private String whoWin = "";
    private final String startString = "Start Game";
    private final String waitingString = "Waiting for another player";
    private final String unableToCommunicateWithOpponentString = "Unable to communicate with opponent.";
    private final String wonString = " won!";
    private final String enemyWonString = " Opponent won!";
    private final String tieString = "Game ended in a tie.";
    private boolean isFristRun = true;
    private boolean isCreateRoom = false;
    public String[] positions = new String[9];
    private int[][] wins = new int[][]{{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};

    public boolean myTurn = false;
    public boolean isO = false;
    public boolean isX = false;
    public ImageIcon imageO;
    public ImageIcon imageX;

    public DataOutputStream dos;
    public DataInputStream dis;

    private Sender sender;
    private Receiver receiver;
    private Thread Tsender;
    private Thread Treceiver;

    public gameControl() {
        try {
            imageO = new ImageIcon(ImageIO.read(getClass().getResourceAsStream("res/redCircle.png")));
            imageX = new ImageIcon(ImageIO.read(getClass().getResourceAsStream("res/redX.png")));
        } catch (IOException ex) {
            Logger.getLogger(gameControl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setStartGame(String roomName, String Myname) {
        FrameGame.setNameRoom(roomName);
        FrameGame.setTitle(roomName);
        if (isCreateRoom) {
            FrameGame.setNamePlayer1(Myname);
        } else {
            FrameGame.setNamePlayer2(Myname);
        }
    }

    public void createRoom(String roomName, String Myname) {
        isCreateRoom = true;
        setStartGame(roomName, Myname);
        initializeServer();
        checkJoin();
    }

    public void joinRoom(String ip, String MyName) {
        boolean isComplete = connect(ip);
        clientSent(MyName);
    }

    private boolean connect(String ip) {
        try {
            if (ip != null) {
                this.ip = ip;
            }
            socket = new Socket(this.ip, port);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(socket.getOutputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to connect to the address: {0}:{1} | Starting a server", new Object[]{ip, port});
            return false;
        }
        if (isFristRun) {
            LOGGER.log(Level.INFO, "Successfully connected to the server.");
        }
        return true;
    }

    private void initializeServer() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
        }
        if (isFristRun) {
            LOGGER.log(Level.INFO, "Successfully Create server.");
        }
    }

    private void checkJoin() {
        boolean start = false;
        LOGGER.log(Level.INFO, waitingString);
        while (!start) {
            try {
                socket = serverSocket.accept();
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw = new PrintWriter(socket.getOutputStream());

                String msg = br.readLine();
                pw.println(FrameGame.getNameRoomAndPlayer1());
                pw.flush();
                FrameGame.setNamePlayer2(msg);
                start = true;
                isFristRun = false;
                isX = true;
                myTurn = true;

                socket.close();
                br.close();
                pw.close();
            } catch (Exception e) {

                LOGGER.log(Level.WARNING, e.getMessage());
            }
        }
        if (start) {
            FrameGame.setControl(this);
            FrameGame.setVisible(true);
            LOGGER.log(Level.INFO, startString);
        }
        thread = new Thread(this);
        thread.start();
    }

    private void clientSent(String MyName) {
        try {
            pw.println(MyName);
            pw.flush();

            String msg = br.readLine();
            LOGGER.log(Level.INFO, msg);
            String[] msgs = msg.split(":");
            setStartGame(msgs[0], MyName);
            FrameGame.setNamePlayer1(msgs[1]);
            isFristRun = false;
            isO = true;
            myTurn = false;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage());
        }
        if (!isFristRun) {
            FrameGame.setControl(this);
            FrameGame.setVisible(true);
            LOGGER.log(Level.INFO, startString);
        }
        thread = new Thread(this);
        thread.start();
    }

    public int oldPosition;

    private void listenForServerRequest() {
        Socket socket = null;
        try {
            socket = serverSocket.accept();
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            System.out.println("CLIENT HAS REQUESTED TO JOIN, AND WE HAVE ACCEPTED");
            myTurn = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendData(int i) {
        //System.out.println(i);
        sender = new Sender(dos);
        Tsender = new Thread(sender);
        sender.setData(i);
        Tsender.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiverData() {
        receiver = new Receiver(dis);
        Treceiver = new Thread(receiver);
        Treceiver.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void checkWin() {
        for (int[] win : wins) {
            if ("O".equals(positions[win[0]]) && "O".equals(positions[win[1]]) && "O".equals(positions[win[2]])) {
                isWon = true;
                whoWin = "O";
            }
            if ("X".equals(positions[win[0]]) && "X".equals(positions[win[1]]) && "X".equals(positions[win[2]])) {
                isWon = true;
                whoWin = "X";
            }
            if (isWon) {
                if (isX) {
                    if (whoWin.equals("X")) {
                        JOptionPane.showMessageDialog(FrameGame, FrameGame.getNamePlayer1() + wonString);
                        myTurn = false;
                    } else {
                        JOptionPane.showMessageDialog(FrameGame, FrameGame.getNamePlayer2() + enemyWonString);
                        myTurn = true;
                    }
                } else {
                    if (whoWin.equals("O")) {
                        JOptionPane.showMessageDialog(FrameGame, FrameGame.getNamePlayer2() + wonString);
                        myTurn = true;
                    } else {
                        JOptionPane.showMessageDialog(FrameGame, FrameGame.getNamePlayer1() + enemyWonString);
                        myTurn = false;
                    }
                }
                whoWin = "";
                isWon = false;
                positions = new String[9];
                FrameGame.resetGame();
            }
        }
    }

    @Override
    public void run() {
        if (isX) {
            listenForServerRequest();
        } else {
            connect(null);
        }
        while (true) {
            checkWin();
            if (myTurn) {
                int position = FrameGame.getData();
                //System.out.println(position);
                sendData(position);
                boolean isComplete = FrameGame.checkData(position);
                //System.out.println(isComplete);
                if (isComplete) {
                    myTurn = !myTurn;
                }
            } else {
                receiverData();
                if (FrameGame.setData(receiver.getPosition())) {
                    myTurn = !myTurn;
                }
            }
        }
    }
}
