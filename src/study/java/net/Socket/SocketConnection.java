package study.java.net.Socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;



public class SocketConnection {

    private String mAddress;
    private int nPort = 0;
    private MainFrame mFrame;
    public SocketConnection(MainFrame frame){
        mFrame = frame;
        mAddress = getLocalIP();
    }

    private void myLog(String name, String msg){
        mFrame.myLog(name, msg);
    }

    private Thread ServerThread;
    public void startServerSocket(int port){
        nPort = port;
        ServerThread = new Thread(ServerRunnable);
        ServerThread.start();
    }
    public void stopServerSocket(){
        if((null != ServerThread) && (!mServerSocket.isClosed())){
            stopClientSocket();
            ServerThread.interrupt();
            try {
                //the new Socket below is used to interrupt ServerSocket.accept() to avoid 
                //java.net.SocketException: Socket closed
                new Socket(mServerSocket.getInetAddress(), mServerSocket.getLocalPort()).close();
                mServerSocket.close();
                myLog("LOG", "ServerSocket Closed.");
            } catch (IOException ioe) {
                myLog("LOG", "Error when stop ServerSocket, As Below:");
                ioe.printStackTrace();
            }
        } else {
            myLog("LOG", "ServerSocket is Not Running.");            
        }
    }
    private ServerSocket mServerSocket;
    private Socket remoteSocket;
    private Map<String, Socket> socketPool = new HashMap<String,Socket>();
//    private boolean serverThreadFlag = false;
    private Runnable ServerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if((null == mAddress) || (0 == nPort)){
                    myLog("LOG", "ServerSocket address or port is not set.");
                    return;
                }
                myLog("LOG", "ServerSocket " + mAddress + ":" + nPort + " has started");
                mServerSocket = new ServerSocket(nPort);
                mServerSocket.setReuseAddress(true);
                while (!Thread.currentThread().isInterrupted()) {
                    remoteSocket = mServerSocket.accept();
                    String remoteSocketName = getSocketName(remoteSocket);
                    myLog("LOG", remoteSocketName + " is Connected.");
                    socketPool.put(remoteSocketName, remoteSocket);
                }
                startChatting(remoteSocket);
            } catch (IOException e) {
                myLog("LOG", "Exception In ServerRunnable IOException, As Below:");
                e.printStackTrace();
            }
        }
    };

    public boolean startClientSocket(String address, int port){
        try {
            remoteSocket = new Socket(address, port);
            try{
                remoteSocket.sendUrgentData(0xFF);
            }catch(Exception ex){
                myLog("startClientSocket Error", "ServerSocket " + address + ":" + port +" is NOT open.");
                return false;
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            myLog("startClientSocket Error", "UnknownHostException, As Below:");
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            myLog("startClientSocket Error", "IOException, As Below:");
            e.printStackTrace();
        }
        myLog("LOG", "Socket Connection to " + address + ":" + port + " Success.");
        startChatting(remoteSocket);
        return true;
    }

    public void stopClientSocket() {
        this.stopChatting();
        if ((null != remoteSocket) && (!remoteSocket.isClosed())) {
            try {
                remoteSocket.close();
                myLog("LOG", getSocketName(remoteSocket) + " has closed.");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                myLog("LOG", "Exception In stopClientSocket IOException, As Below:");
                e.printStackTrace();
            }
        }
    }

    private Socket destSocket;
    private String destName;
    private Thread ReceivingThread;
    private void startChatting(Socket socket){
        destSocket = socket;
        destName = getSocketName(socket);
        ReceivingThread = new Thread(ReceivingRunnable);
        ReceivingThread.start();
    }
    private void stopChatting(){
        if((null != ReceivingThread) && (ReceivingThread.isAlive())){
            ReceivingThread.interrupt();
            destSocket = null;
        }else{
            myLog("LOG", "Receiving Thread is Not Start.");
        }
    }
    private Runnable ReceivingRunnable = new Runnable() {
        @Override
        public void run() {
            BufferedReader input;
            try {
                input = getReader(destSocket);
                while (!Thread.currentThread().isInterrupted()) {
                    String messageStr = null;
                    messageStr = input.readLine();
                    if (messageStr != null) {
                        myLog(destName, messageStr);
                    } else {
//                        break;
                    }
                }
                input.close();
            } catch (IOException e) {
                myLog("ReceivingRunnable Error", "IOException, As Below:");
                e.printStackTrace();
            }
        }
    };

    public boolean sendMessage(String msg) {
        boolean isOK = false;
        try {
            if (destSocket == null) {
                myLog("sendMessage Error", "destSocket is null");
            } else if (destSocket.getOutputStream() == null) {
                myLog("sendMessage Error", "destSocket output stream is null.");
            } else {
                PrintWriter out = this.getWriter(destSocket);
                out.println(msg);
                out.flush();
                myLog("I say", msg);
                isOK = true;
            }
        } catch (UnknownHostException e) {
            myLog("sendMessage Error", "UnknownHostException, As Below:");
            e.printStackTrace();
        } catch (IOException e) {
            myLog("sendMessage Error", "IOException, As Below:");
            e.printStackTrace();
        } catch (Exception e) {
            myLog("sendMessage Error", "Exception, As Below:");
            e.printStackTrace();
        }
        return isOK;
    }

    private PrintWriter getWriter(Socket socket){// throws IOException
        PrintWriter pw = null;
        try {
            OutputStream stream = socket.getOutputStream();
            OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
            BufferedWriter bufferedWriter = new BufferedWriter(streamWriter);
            pw = new PrintWriter(bufferedWriter);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pw;
    }
    private BufferedReader getReader(Socket socket){// throws IOException
        BufferedReader bufferedReader = null;
        try {
            InputStream stream = socket.getInputStream();
            InputStreamReader streamReader = new InputStreamReader(stream);
            bufferedReader = new BufferedReader(streamReader);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bufferedReader;
    }

    private String getSocketName(Socket socket){
        String destName;
        String address = socket.getInetAddress().toString().substring(1);
        int port = socket.getPort();
        destName = address + ":" + port;
        return destName;
    }
    
    private String getLocalIP(){
        String address = null;
        try {
            for (Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces(); nifs.hasMoreElements();) {
                if(null != address){
                    break;
                }
                NetworkInterface nif = nifs.nextElement();
//                    myLog("name of network interface: " + nif.getName());
                for (Enumeration<InetAddress> iaenum = nif.getInetAddresses(); iaenum.hasMoreElements();) {
                    InetAddress interfaceAddress = iaenum.nextElement();
                      if (!interfaceAddress.isLoopbackAddress()) {
                          if (interfaceAddress instanceof Inet4Address) {
//                                  myLog(interfaceAddress.getHostName() + " -- " + interfaceAddress.getHostAddress());
                              address = interfaceAddress.getHostAddress();
                              break;
                          }
                      }
                }
            }
        } catch (SocketException se) {
            myLog("LOG", "Exception In getLocalIP SocketException, As Below:");
            se.printStackTrace();
        }
        return address;
    }
}