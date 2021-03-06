package study.java.net.Socket;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.json.JSONObject;


@SuppressWarnings("serial")
public class MainFrame extends JFrame implements ActionListener {

	private JMenuBar menuBar;
    private JMenu serverMenu;
    private JMenuItem startSocketServerMI, stopServerMI, openChatWindow, chatWindowList;

	private Container container;
	private JTextArea historyTextArea;
	private JScrollPane scorllTextArea;

	private Calendar calendar;

	private MainFrame instance;
	private  int WIDTH = 500, HEIGHT = 300; 
	private Dimension screenSize =Toolkit.getDefaultToolkit().getScreenSize();
	private SocketConnection socketConn;
	private int PORT = 7777;

	public MainFrame() {
		this.setTitle("用户界面");
		instance = this;
		menuBar = new JMenuBar();
		container = this.getContentPane();
		
		serverMenu = new JMenu("服务器设置");
        openChatWindow = new JMenuItem("打开客户端窗口");
        chatWindowList = new JMenuItem("客户端窗口列表");
        startSocketServerMI = new JMenuItem("开启ServerSocket");
        stopServerMI = new JMenuItem("关闭ServerSocket");
        openChatWindow.addActionListener(this);
        chatWindowList.addActionListener(this);
        startSocketServerMI.addActionListener(this);
        stopServerMI.addActionListener(this);
        serverMenu.add(openChatWindow);
        serverMenu.add(chatWindowList);
        serverMenu.add(startSocketServerMI);
        serverMenu.add(stopServerMI);
		menuBar.add(serverMenu);
		
		setJMenuBar(menuBar);

		historyTextArea = new JTextArea();
		historyTextArea.setEditable(false);
		historyTextArea.setBackground(Color.lightGray);
		scorllTextArea = new JScrollPane(historyTextArea);
		scorllTextArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 5, 3));

		container.add(scorllTextArea, BorderLayout.CENTER);

		calendar = Calendar.getInstance();
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
//			    myLog("退出用户界面。。。");
				System.exit(0);
			}
		});

        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setLocation((screenSize.width - WIDTH)/2, (screenSize.height - HEIGHT)/2);
		socketConn = new SocketConnection(this);
//        eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
	}
    
//    private EventQueue eventQueue = null;
//	public void myLog(String name, String msg){
//        eventQueue.postEvent( new LOGAWTEvent( this, name, msg));
//	}
//    @Override
//    protected void processEvent(AWTEvent e) {
//        // TODO Auto-generated method stub
//        if ( e instanceof LOGAWTEvent )
//        {
//            LOGAWTEvent ev = (LOGAWTEvent) e;
//            processLog(ev.getName(), ev.getMessage());
//        }else{
//            super.processEvent(e);
//        }
//    }

    private JDialog mDialog;
    private JButton confirmBtn, cancelBtn;
    private JTextField hostField, portField;
    private void initDialog(){
        JPanel mPanel = new JPanel();
        GridLayout mLayout = new GridLayout(0,2);
        mLayout.setHgap(10);
        mLayout.setVgap(10);
        JLabel hostLabel = new JLabel("IP地址：");
        JLabel portLabel = new JLabel("端口号：");

        hostField = new JTextField();
        hostField.setColumns(16);
        portField = new JTextField();
        portField.setColumns(16);
        
        hostLabel.setLabelFor(hostField);
        portLabel.setLabelFor(portField);
        
        confirmBtn = new JButton("确定");
        cancelBtn = new JButton("取消");
        confirmBtn.addActionListener(this);
        cancelBtn.addActionListener(this);

        mPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//        mPanel.setPreferredSize(new Dimension(400, 0));
        mPanel.setLayout(mLayout);
        mPanel.add(hostLabel);
        mPanel.add(hostField);
        mPanel.add(portLabel);
        mPanel.add(portField);
        mPanel.add(confirmBtn);
        mPanel.add(cancelBtn);
        
        mDialog = new JDialog(this,true);
        mDialog.setTitle("输入IP和端口号");
        mDialog.getContentPane().add(mPanel);
        mDialog.pack();   
    }

    private void showDialog(){
        if(null == mDialog){
            initDialog();
        }
        mDialog.setVisible(true);   
    }
    private void hideDialog(){
        mDialog.setVisible(false);
    }
    
    public void myLog(String name, String msg){
//        int hour, minute, second;
//        calendar.setTimeInMillis(System.currentTimeMillis());
//        hour = calendar.get(Calendar.HOUR_OF_DAY);
//        minute = calendar.get(Calendar.MINUTE);
//        second = calendar.get(Calendar.SECOND);
//        "（" + hour + ":" + minute + ":" + second + "）："
        String content = name + ": " + msg;
        historyTextArea.append(content + "\n");
        System.out.println(content);
    }
    public void processMsgObj(JSONObject msgObj){
//        System.out.println("MainFrame.processMsgObj: " + msgObj.toString());
        if(msgObj.has("content")){
            String address = msgObj.getString("address");
            ChatWindow chatWindow = getChatWindow(address);
            if(!chatWindow.isVisible()){
                chatWindow.open();
            }
            JSONObject contentObj = new JSONObject(msgObj.getString("content"));
            chatWindow.myLog(address, contentObj.getString("message"));
        }else{
            myLog("processMsgObj Error", "received an unrecognized packet.");            
        }
    }
    public boolean sendMessage(String address, JSONObject msgObj){
        System.out.println("Send to " + address + ":" + PORT + ". Origin Message: " + msgObj.toString());
        return socketConn.sendMessage(address, PORT, msgObj);
    }

    private Map<String, ChatWindow> chatWindowManager = new HashMap<String, ChatWindow>();
    public ChatWindow getChatWindow(String address){
        ChatWindow clientWindow;
        if(chatWindowManager.containsKey(address)){
            clientWindow = chatWindowManager.get(address);
        }else{
            clientWindow = new ChatWindow(this, address, 500, 650);
            chatWindowManager.put(address, clientWindow);
        }
        return clientWindow;
    }
    public void showChatWindowList(){
        String address;
        Iterator<String> iter = chatWindowManager.keySet().iterator();
        while (iter.hasNext()) {
            address = iter.next();
            this.myLog("ChatWindow", address);
        }
    }
	
	// private SimpleHttpServer httpServer;
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		JComponent item = (JComponent) e.getSource();
        if (item.equals(startSocketServerMI)) {
            socketConn.startServerSocket(PORT);
        }else
		if(item.equals(stopServerMI)){
		    socketConn.stopServerSocket();
		}else
	    if(item.equals(openChatWindow)){
            showDialog();
	    }else
        if(item.equals(chatWindowList)){
            showChatWindowList();
        }else	        
        if(item.equals(confirmBtn)){
            String remoteAddress = "192.168.5.176";//hostField.getText();
            int serverPort;
            String portStr = "7777";//portField.getText();
            if((null != remoteAddress) && (null != portStr)){
                serverPort = Integer.parseInt(portStr);
//              myLog("You want to connet to server " + destAddress + ": " + destPort);
                portField.setText("");
                portField.setText("");
            }
            ChatWindow chatWindow = getChatWindow(remoteAddress);
            if(null != chatWindow){
                this.hideDialog();
                chatWindow.open();
            }
        }else
        if(item.equals(cancelBtn)){
            portField.setText("");
            portField.setText("");
            this.hideDialog();      
        }        
	}


	public static void main(String[] args){
        MainFrame mainFrame = new MainFrame();
        mainFrame.pack();
        mainFrame.setVisible(true);
	}
}


@SuppressWarnings("serial")
class LOGAWTEvent extends AWTEvent {
    public static final int EVENT_ID = AWTEvent.RESERVED_ID_MAX + 1;
    private String mName;
    private String mMessage;

    LOGAWTEvent(Object target, String name, String message) {
        super(target, EVENT_ID);
        mName = name;
        mMessage = message;
    }

    public String getName() {
        return mName;
    }

    public String getMessage() {
        return mMessage;
    }
}