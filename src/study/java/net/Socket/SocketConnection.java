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

    private void logToConsole(String name, String msg){
//        mFrame.myLog(name, msg);
        String content = name + ": " + msg;
        System.out.println(content);
    }
    private void logToMainFrame(String name, String msg){
        mFrame.myLog(name, msg);        
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
                logToMainFrame("LOG", "ServerSocket Closed.");
            } catch (IOException ioe) {
                logToMainFrame("LOG", "Error when stop ServerSocket, As Below:");
                ioe.printStackTrace();
            }
        } else {
            logToMainFrame("LOG", "ServerSocket is Not Running.");            
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
                    logToMainFrame("LOG", "ServerSocket address or port is not set.");
                    return;
                }
                logToMainFrame("LOG", "ServerSocket " + mAddress + ":" + nPort + " has started");
                mServerSocket = new ServerSocket(nPort);
                mServerSocket.setReuseAddress(true);
                while (!Thread.currentThread().isInterrupted() && startServerRunnable) {
                    remoteSocket = mServerSocket.accept();
                    String rAddress = getSocketAddress(remoteSocket);
                    int rPort = getSocketPort(remoteSocket);
                    logToConsole("ServerSocket", rAddress + ":" + rPort + "Connected.");
//                    logToConsole("address", rAddress);
//                    logToConsole("port", ""+rPort);
                    JSONObject mMsgObj = getAndWrapMessage(remoteSocket);
                    if(mMsgObj != null){
                        replyMessage(remoteSocket, mMsgObj);
                        mFrame.processMsgObj(mMsgObj);
                    }
                    remoteSocket.close();
                }
            } catch (IOException e) {
                logToMainFrame("LOG", "Exception In ServerRunnable IOException, As Below:");
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
                logToConsole("getAndWrapMessage, Origin", message);
                logToConsole("getAndWrapMessage, Wrapped", mMsgObj.toString());
            }else{
                logToConsole("getAndWrapMessage", "Ignore Message, " + message);
                mMsgObj = null;
            }
//            input.close();
        } catch (IOException e) {
            logToConsole("getMessage Exception", "IOException, As Below:");
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
        //注意此处必须使用mMsgObj.getString("content")，而不能使用getContentObj.toString
        replyContentObj.put("message", this.stringMD5(mMsgObj.getString("content")));
//        logToConsole("replyMessage Content:", mMsgObj.getString("content"));
//        logToConsole("replyMessage MD5:", this.stringMD5(mMsgObj.getString("content")));
        replyContentObj.put("time", System.currentTimeMillis());
        JSONObject replyObj = new JSONObject();
        replyObj.put("type", "Reply");
        replyObj.put("content", replyContentObj.toString());
        logToConsole("replyMessage", replyObj.toString());
        PrintWriter out = this.getWriter(socket);
        if(null != out){
            out.println(replyObj.toString());
            out.flush();
        }
    }

    private Socket connectToServerSocket(String address, int port){
        Socket serverSocket = null;
        try {
            serverSocket = new Socket(address, port);
            try{
                serverSocket.sendUrgentData(0xFF);
            }catch(Exception ex){
                logToConsole("connectToServerSocket Exception", "ServerSocket " + address + ":" + port +" is NOT open.");
                return null;
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            logToConsole("connectToServerSocket Exception", "UnknownHostException, As Below:");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logToConsole("connectToServerSocket Exception", "IOException, As Below:");
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
        logToConsole("sendMessage", "Send to " + address + ":" + port + ". Wrapped Message: " + strToSend);
        Socket socket = connectToServerSocket(address, port);
        if(null != socket){
            PrintWriter out = this.getWriter(socket);
            if(null != out){
                out.println(strToSend);
                out.flush();
                if(waitAndCheckReply(socket, msgObj.toString())){
                    isOK = true;
                }else{
                    isOK = false;
                }
//                isOK = true;
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
                this.logToConsole("waitAndCheckReply", "Reply Message From Socket: " + msgObj.toString());
                input.close();
                if("Reply".equals(msgObj.getString("type"))){
                    if(msgObj.has("content")){
                        JSONObject contentObj = new JSONObject(msgObj.getString("content"));
                        if(contentObj.has("message")){
                            if(contentObj.getString("message").equals(stringMD5(msgToSend))){
                                isOK = true;
                                this.logToConsole("waitAndCheckReply", "isOK.");
                            }else{
                                this.logToConsole("waitAndCheckReply", "MD5 of Message Content is not the same");
                                this.logToConsole("waitAndCheckReply",  contentObj.getString("message"));
                                this.logToConsole("waitAndCheckReply", "msgToSend: " + stringMD5(msgToSend));
                            }                            
                        }else{
                            this.logToConsole("waitAndCheckReply", "Key 'message' does not exist in contentObj.");
                            this.logToConsole("waitAndCheckReply", "contentObj:" + contentObj.toString());
                        }
                    }else{
                        this.logToConsole("waitAndCheckReply", "key 'content' does not exist in msgObj.");
                    }
                }
            } else {
                logToConsole("waitAndCheckReply Exception", "BufferReader of Socket is null.");                
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            logToConsole("waitAndCheckReply Exception", "UnknownHostException, As Below:");
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logToConsole("waitAndCheckReply Exception", "IOException, As Below:");
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
                logToConsole("TimerTask CloseSocket", "Close Socket.");
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
//                    logToConsole("name of network interface: " + nif.getName());
                for (Enumeration<InetAddress> iaenum = nif.getInetAddresses(); iaenum.hasMoreElements();) {
                    InetAddress interfaceAddress = iaenum.nextElement();
                      if (!interfaceAddress.isLoopbackAddress()) {
                          if (interfaceAddress instanceof Inet4Address) {
//                                  logToConsole(interfaceAddress.getHostName() + " -- " + interfaceAddress.getHostAddress());
                              address = interfaceAddress.getHostAddress();
                              break;
                          }
                      }
                }
            }
        } catch (SocketException se) {
            logToConsole("getLocalIP Exception", "SocketException, As Below:");
            se.printStackTrace();
        }
        return address;
    }
}