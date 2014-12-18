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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;



public class SocketConnection {

    private String mAddress;
    private int nPort = 0;
    private MainFrame mFrame;
    public SocketConnection(MainFrame frame){
        mFrame = frame;
        mAddress = getLocalIP();
    }

    private void myLog(String name, String msg){
//        mFrame.myLog(name, msg);
        String content = name + ": " + msg;
        System.out.println(content);
    }

    private Thread ServerThread;
    public void startServerSocket(int port){
        nPort = port;
        startServerRunnable = true;
        ServerThread = new Thread(ServerRunnable);
        ServerThread.start();
    }
    public void stopServerSocket(){
        if((null != ServerThread) && (!mServerSocket.isClosed())){
//            stopClientSocket();
            ServerThread.interrupt();
            startServerRunnable = false;
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
    private boolean startServerRunnable = true;
//    private Map<String, Socket> socketPool = new HashMap<String,Socket>();
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
                while (!Thread.currentThread().isInterrupted() && startServerRunnable) {
                    remoteSocket = mServerSocket.accept();
                    myLog("address", getSocketAddress(remoteSocket));
                    myLog("port", ""+getSocketPort(remoteSocket));
                    JSONObject mMsgObj = getAndWrapMessage(remoteSocket);
                    if(mMsgObj != null){
//                        replyMessage(remoteSocket, mMsgObj);
//                        mFrame.processMsgObj(mMsgObj);
                    }
                }
            } catch (IOException e) {
                myLog("LOG", "Exception In ServerRunnable IOException, As Below:");
                e.printStackTrace();
            }
        }
    };
    private JSONObject getAndWrapMessage(Socket socket){
        BufferedReader input;
        JSONObject mMsgObj = new JSONObject();
        mMsgObj.put("address", this.getSocketAddress(socket));
        mMsgObj.put("port", this.getSocketPort(socket));
        try {
            input = getReader(socket);
            String message = input.readLine();
            JSONObject msgObjFromRemote = new JSONObject(message);
            if(msgObjFromRemote.has("type") && (msgObjFromRemote.getString("type").equals("SentEnFirst"))){
                mMsgObj.put("content", msgObjFromRemote.getString("content"));                
                myLog("getAndWrapMessage, Origin", message);
                myLog("getAndWrapMessage, Wrapped", mMsgObj.toString());
            }else{
                myLog("getAndWrapMessage", "Ignore Message, " + message);
                mMsgObj = null;
            }
            input.close();
        } catch (IOException e) {
            myLog("getMessage Exception", "IOException, As Below:");
            e.printStackTrace();
        }
        return mMsgObj;
    }
    private void replyMessage(Socket socket, JSONObject mMsgObj){
        JSONObject getContentObj = new JSONObject(mMsgObj.getString("content"));
        JSONObject replyContentObj = new JSONObject();
        replyContentObj.put("type", "Reply");
        replyContentObj.put("from", mAddress);
        replyContentObj.put("to", getContentObj.getString("from"));
        replyContentObj.put("message", this.stringMD5(getContentObj.toString()));
        replyContentObj.put("time", System.currentTimeMillis());
        JSONObject replyObj = new JSONObject();
        replyObj.put("type", "Reply");
        replyObj.put("content", replyContentObj.toString());
        myLog("replyMessage", replyObj.toString());
        PrintWriter out = this.getWriter(socket);
        if(null != out){
            out.println(replyObj.toString());
            out.flush();
            out.close();
        }
    }

    private Socket connectToServerSocket(String address, int port){
        Socket serverSocket = null;
        try {
            serverSocket = new Socket(address, port);
            try{
                serverSocket.sendUrgentData(0xFF);
            }catch(Exception ex){
                myLog("connectToServerSocket Exception", "ServerSocket " + address + ":" + port +" is NOT open.");
                return null;
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            myLog("connectToServerSocket Exception", "UnknownHostException, As Below:");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            myLog("connectToServerSocket Exception", "IOException, As Below:");
            e.printStackTrace();
            return null;
        }
        return serverSocket;        
    }    
    public boolean sendMessage(String address, int port, JSONObject msgObj) {
        boolean isOK = false;
        msgObj.put("from", this.mAddress);
        msgObj.put("uuid", this.mAddress);
        JSONObject objToSend = new JSONObject();
        objToSend.put("type", "SentEnFirst");
        objToSend.put("content", msgObj.toString());
        String strToSend = objToSend.toString();
        System.out.println("Sending Message to Socket: " + address + ":" + port + ", " + strToSend);
        Socket socket = connectToServerSocket(address, port);
        if(null != socket){
            PrintWriter out = this.getWriter(socket);
            if(null != out){
                out.println(strToSend);
                out.flush();
//                if(waitAndCheckReply(socket, msgObj.toString())){
//                    isOK = true;
//                }else{
//                    isOK = false;
//                }
                isOK = true;
            }else{
                isOK = false;
            }
        } else {
            isOK = false;
        }
        if(isOK){
            try {
                socket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                isOK = false;
                e.printStackTrace();
            }
        }
        return isOK;
    }
    private boolean waitAndCheckReply(Socket socket, String msgToSend){
        boolean isOK = false;
        //三秒没有反馈，关闭BufferReader。
        Timer mTimer = new Timer();
        mTimer.schedule(new CloseSocketTimerTask(socket), 3000);
        BufferedReader input = getReader(socket);
        try {
            if(null != input){
                JSONObject msgObj = new JSONObject(input.readLine());
                mTimer.cancel();
                System.out.println("Reply Message From Socket: " + msgObj.toString());
//                System.out.println(stringMD5(msgToSend));
                input.close();
                if("Reply".equals(msgObj.getString("type"))){
                    if(msgObj.has("content")){
                        JSONObject contentObj = new JSONObject(msgObj.getString("content"));
                        if(contentObj.has("message")){
                            if(contentObj.getString("message").equals(stringMD5(msgToSend))){
                                isOK = true;
                                System.out.println("isOK.");
                            }else{
                                System.out.println("contentObj.message" + "MD5 of Message is not the same");
                                System.out.println("contentObj.message: " + contentObj.getString("message"));
                                System.out.println("msgToSend:" + stringMD5(msgToSend));
                                System.out.println("msgToSend:" + byteMD5(msgToSend.getBytes()));
                            }                            
                        }else{
                            System.out.println("waitAndCheckReply:" + "Not message key exist in contentObj.");
                            System.out.println("contentObj:" + contentObj.toString());
                        }
                    }else{
                        System.out.println("waitAndCheckReply:" + "Not content key exist in msgObj.");
                    }
                }
            } else {
                myLog("waitAndCheckReply Exception", "BufferReader of Socket is null.");                
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            myLog("waitAndCheckReply Exception", "UnknownHostException, As Below:");
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            myLog("waitAndCheckReply Exception", "IOException, As Below:");
            e.printStackTrace();
        }
        return isOK;        
    }
    
    class CloseSocketTimerTask extends TimerTask {
        private Socket mSocket;
        public CloseSocketTimerTask(Socket socket){
            this.mSocket = socket;
        }            
        @Override
        public void run() {
            try {
                mSocket.close();
                myLog("TimerTask CloseSocket", "Close Socket.");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public String stringMD5(String input) {
        try {
            // 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            // 输入的字符串转换成字节数组
            byte[] inputByteArray = input.getBytes();
            // inputByteArray是输入字符串转换得到的字节数组
            messageDigest.update(inputByteArray);
            // 转换并返回结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();
            // 字符数组转换成字符串返回
            return byteArrayToHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    private String byteMD5(byte[] inputByteArray) {
        try {
            // 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            // inputByteArray是输入字符串转换得到的字节数组
            messageDigest.update(inputByteArray);
            // 转换并返回结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();
            // 字符数组转换成字符串返回
            return byteArrayToHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    public String byteArrayToHex(byte[] byteArray) {
        // 首先初始化一个字符数组，用来存放每个16进制字符
        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
        char[] resultCharArray = new char[byteArray.length * 2];
        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        // 字符数组组合成字符串返回
        return new String(resultCharArray);
    }

    private PrintWriter getWriter(Socket socket){
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
    private BufferedReader getReader(Socket socket){
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

    private String getSocketAddress(Socket socket){
        String address = socket.getInetAddress().toString().substring(1);
        return address;
    }

    private int getSocketPort(Socket socket){
        int port = socket.getPort();
        return port;
    }
    
    private String getSocketName(Socket socket){
        String destName;
        destName = getSocketAddress(socket) + ":" + getSocketPort(socket);
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
            myLog("getLocalIP Exception", "SocketException, As Below:");
            se.printStackTrace();
        }
        return address;
    }
}