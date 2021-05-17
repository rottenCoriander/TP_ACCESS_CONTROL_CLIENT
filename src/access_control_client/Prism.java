/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package access_control_client;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CFactory;
import java.awt.Color;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Admin
 */
public class Prism extends javax.swing.JFrame implements Runnable{

    /**
     * Creates new form Prism
     */
    
       
     //GPIO
     GpioController gpio;
     GpioPinDigitalInput pin0;              //Up
     GpioPinDigitalInput pin1;              //Down
     GpioPinDigitalInput pin2;              //DoorBell
     static GpioPinDigitalOutput pin27;     //Button LED
     GpioPinListenerDigital pin0Trigger;
     GpioPinListenerDigital pin1Trigger;
     GpioPinListenerDigital pin2Trigger;
     Pin pin;
     Timer t;
     
     I2c i2c;
    
     static boolean passed = false;
     
     Boolean DBOnline;  
     
     static CardDetector cardDetector;
     Authentication auth;     
     
    int initErrorCode;
    
    Thread beep = new Beep();
    Thread beep2 = new Beep2();
    Thread beep3 = new Beep3();
    Thread beepDenied = new BeepDenied();
    
    Boolean Authenticating=false;
    
    Calendar cal;
    SimpleDateFormat sdf;
    
    public Prism() {
       initComponents();
       initGpio();
       initProgress.setVisible(false);
       unlockBt.setVisible(false);
       unlockBt.setEnabled(false);
       initI2c();     
       Thread clock = new Thread(this);
       clock.start();
    }
    
    public void initGpio(){
        consoleWriteln("Create GPIO Object");
        gpio = GpioFactory.getInstance();
        //OUTPUT
        consoleWriteln("Configure Output");
        pin27 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_27);
        //INPUT
         consoleWriteln("Configure Input");
        pin0 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_00, PinPullResistance.PULL_DOWN);
        pin1 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_DOWN);
        pin2 = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
       //Input Listener
       //GreenButton
       pin0Trigger = new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if(passed){
                    if(event.getState() == PinState.HIGH){
                        try {
                            i2c.write((byte)0x01);
                        } catch (Exception ex) {
                            Logger.getLogger(Prism.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
       pin0.addListener(pin0Trigger);
       //RedButton
       pin1Trigger = new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if(passed){
                    if(event.getState() == PinState.HIGH){
                        try {
                            i2c.write((byte)0x02);
                        } catch (Exception ex) {
                            Logger.getLogger(Prism.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
       pin1.addListener(pin1Trigger);
       //DoorBell
       pin2Trigger = new GpioPinListenerDigital() {
            @Override
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                if(Authenticating == false){
                    statusPanel.setBackground(Color.ORANGE);
                    statusZh.setText("呼叫中，請稍候 Calling, Please Wait.");
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Prism.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    systemReady();
                }
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
       pin2.addListener(pin2Trigger);
       
       
    }
    
    public void initI2c(){
         consoleWriteln("Initializing I2C Databus");
         try {
             consoleWriteln("Create i2c Object Address 0x04");
             i2c = new I2c(0x04);
         } catch (I2CFactory.UnsupportedBusNumberException ex) {
             Logger.getLogger(Prism.class.getName()).log(Level.SEVERE, null, ex);
         }
         
         
    }
    
    public void intiDB() throws ClassNotFoundException, SQLException{
        auth.init();
        auth.connectDatabase();
    }
    
    public void systemInit() throws ClassNotFoundException, SQLException{
        cardDetector = new CardDetector();
        auth = new Authentication();
        initProgress.setVisible(true);
        initProgress.setValue(0);
        statusPanel.setBackground(Color.YELLOW);
        actionZh.setText("系統啟動中 System Initializing...");
        consoleWriteln("System Initailizing");
        initProgress.setValue(5);
        statusZh.setText("載入驅動程式 Loading Driver...");
        consoleWriteln("Load Driver and authenticate object");
        auth.init();
        initProgress.setValue(33);
        statusZh.setText("連接資料庫 Connecting Database...");
        consoleWriteln("Connect to Database");
        auth.connectDatabase();
        
        if(auth.DBisClosed()){
            statusZh.setText("無法連接資料庫 Could not Connect to Database");
            consoleWriteln("Fail to Connect to Database");
            DBOnline = false;
            initProgress.setValue(50);
            
            statusZh.setText("啟動RFID模組 Initializing RFID Module...");
            consoleWriteln("Initializing RFID Module");
            initRFID();
            initProgress.setValue(100);
            systemReadyOffline();
            
        }else{
            statusZh.setText("已連接到資料庫 Database Connected.");
            consoleWriteln("Database Connected");
            DBOnline = true;
            initProgress.setValue(66);
           
            statusZh.setText("啟動RFID模組 Initializing RFID Module");
            consoleWriteln("Initializing RFID Module");
            initRFID();
            initProgress.setValue(100);
            systemReady();
        }
        
    }
    
    public void systemReady(){
        initProgress.setVisible(false);
        statusPanel.setBackground(Color.LIGHT_GRAY);
        actionZh.setText("請刷卡或按鈴");
        actionEn.setText("Place a Card or Ring the Bell.");
        statusZh.setText("系統就緒 Ready.");
        consoleWriteln("System Ready.");
        
    }
    public void checkAndReconnectDatabse(){
         while(true){
             try {
                 if(auth.DBisClosed()){
                     consoleWriteln("Database Not Connected, Connecting");
                     auth.connectDatabase();
                     consoleWriteln("Database Connected");
                 }else{
                     break;
                 }
             } catch (SQLException ex) {
                 Logger.getLogger(Prism.class.getName()).log(Level.SEVERE, null, ex);
             }
         }
    }
    public void systemReadyOffline(){
        initProgress.setVisible(false);
        statusPanel.setBackground(Color.YELLOW);
        actionZh.setText("請刷卡或按鈴");
        actionEn.setText("Place a Card or Ring the Bell.");
        statusZh.setText("離線模式 Offline Mode.");
    }
    public void systemReadyWithError(int ErrCode){
        switch(ErrCode){
            case 0:
                
                break;
            case 1:
                break;
            default:
                break;
    }
        
    }
    
    public void initRFID(){
    cardDetector.setReadCard(false);
    cardDetector.addListener(new CardListener() {
        @Override
        public void cardEvent(ReadCard event) {
            if(event.getCardDetect()){
                beep.run();
                statusPanel.setBackground(Color.YELLOW);
                statusZh.setText("卡片讀取中 Reading Card...");
                actionZh.setText("驗證中...");
                actionEn.setText("Verifying...");
                Authenticating = true;
                consoleWriteln("Card Detected.");
                cardAuthentication();
                
               }else{
                
               }
          //  throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    });
    startCardDetect();
    }
    
    public void startCardDetect(){
        new Thread(auth, "card").start();
    }
    
    public void cardAuthentication(){
        statusZh.setText("卡片驗證中 Verifying...");
        if(auth.Authenticate()){
            statusPanel.setBackground(Color.GREEN);
            statusZh.setText("驗證成功 Verification Successful.");
            beep2.run();
            statusZh.setText("資料核對中 Matching Data...");
            statusPanel.setBackground(Color.YELLOW);
            if(auth.readData()){
                if(auth.checkGroup()){
                    if(auth.checkCardEnable()){
                        ACCESS_GRANTED();
                    }else{
                        ACCESS_DENIED(3);
                    }
                }else{
                    ACCESS_DENIED(2);
                }                            
            }else{
               ACCESS_DENIED(1);
            }
        }else{
            ACCESS_DENIED(0);
        }
        
    }
    
    public void ACCESS_DENIED(int deniedCode){
        statusPanel.setBackground(Color.RED);
        beepDenied.run();
        statusZh.setText("存取被拒 , Access DENIED.");
        switch(deniedCode){
            case 0:
                actionZh.setText("驗證失敗");
                actionEn.setText("Verification Faild.");
                auth.uploadRecord(1, "Verification Faild");
                consoleWriteln("Access Denied : Code 1");
                break;                
            case 1:
                actionZh.setText("資料核對結果不吻合");
                actionEn.setText("Data Not Matched.");
                auth.uploadRecord(2, "Data Not Matched");
                consoleWriteln("Access Denied : Code 2");
                break;
            case 2:
                actionZh.setText("權限不足");
                actionEn.setText("Insufficient permissions.");
                auth.uploadRecord(3, "Insufficient permissions");
                consoleWriteln("Access Denied : Code 3");
                break;
            case 3:
                actionZh.setText("卡片已停用");
                actionEn.setText("Card has been Disabled.");
                auth.uploadRecord(4, "Card Disabled");
                consoleWriteln("Access Denied : Code 4");
                break;
            default:
                break;
        }
        try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Prism.class.getName()).log(Level.SEVERE, null, ex);
            }
            systemReady();
            startCardDetect();
            Authenticating = false;
    }
    
    public void ACCESS_GRANTED(){
        statusPanel.setBackground(Color.GREEN);
        beep3.run();
        statusZh.setText("准予通行 Access Granted!");
        actionZh.setText(auth.getDB_NAME());
        consoleWriteln("Access Granted");
        
        if(auth.DB_GROUPID == 0){
             unlockBt.setVisible(true);
             unlockBt.setEnabled(true);
             statusPanel.setBackground(Color.PINK);
        }
        if(auth.DB_UID.equals("9C9C0DC5")){
    
        }
        if(auth.DB_WELLCOME_MESSAGE == null){
                actionEn.setText("");
            }else{
                actionEn.setText(auth.DB_WELLCOME_MESSAGE);
            }
            auth.uploadRecord(0, null);
            pin27.setState(PinState.HIGH);
            passed = true;
             try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Prism.class.getName()).log(Level.SEVERE, null, ex);
            }
            pin27.setState(PinState.LOW);
            passed = false;
            unlockBt.setVisible(false);
            unlockBt.setEnabled(false);
            systemReady();
            startCardDetect();
            Authenticating = false;
    } 
    
    public void consoleWriteln(String message){
        cal  = Calendar.getInstance();
        
        sdf = new SimpleDateFormat("EEE, d MMM yyyy, z hh:mm:ss aa");
        Date dat = cal.getTime();
        String time12 = sdf.format(dat);
        
        System.out.println(time12 + " "+message);
    }
    


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        timeLabel = new javax.swing.JLabel();
        actionZh = new javax.swing.JLabel();
        statusPanel = new javax.swing.JPanel();
        statusZh = new javax.swing.JLabel();
        actionEn = new javax.swing.JLabel();
        unlockBt = new javax.swing.JButton();
        initProgress = new javax.swing.JProgressBar();

        javax.swing.GroupLayout jLayeredPane1Layout = new javax.swing.GroupLayout(jLayeredPane1);
        jLayeredPane1.setLayout(jLayeredPane1Layout);
        jLayeredPane1Layout.setHorizontalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jLayeredPane1Layout.setVerticalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);
        setPreferredSize(new java.awt.Dimension(800, 480));

        timeLabel.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        timeLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        timeLabel.setText("#TIME##");

        actionZh.setFont(new java.awt.Font("Microsoft JhengHei", 1, 60)); // NOI18N
        actionZh.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        actionZh.setText("系統停止");

        statusZh.setFont(new java.awt.Font("Microsoft JhengHei", 0, 36)); // NOI18N
        statusZh.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        statusZh.setText("######");
        statusZh.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusZh, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(statusZh)
                .addContainerGap(21, Short.MAX_VALUE))
        );

        actionEn.setFont(new java.awt.Font("Microsoft JhengHei", 0, 44)); // NOI18N
        actionEn.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        actionEn.setText("System Halt.");

        unlockBt.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        unlockBt.setText("UNLOCK");
        unlockBt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unlockBtActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(actionZh, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(actionEn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(unlockBt, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(157, 157, 157)
                        .addComponent(initProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(284, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(timeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 650, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(89, 89, 89)
                .addComponent(actionZh)
                .addGap(27, 27, 27)
                .addComponent(actionEn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(initProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(41, 41, 41)
                        .addComponent(timeLabel))
                    .addComponent(unlockBt, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void unlockBtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unlockBtActionPerformed
         try {
             // TODO add your handling code here:
             i2c.write((byte)0x03);
         } catch (Exception ex) {
             Logger.getLogger(Prism.class.getName()).log(Level.SEVERE, null, ex);
         }
    }//GEN-LAST:event_unlockBtActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Prism.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Prism.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Prism.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Prism.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

    
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                //new Prism().setVisible(true);
                Prism prism = new Prism();
                prism.setVisible(true);

                try {
                    prism.systemInit();
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Prism.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SQLException ex) {
                    Logger.getLogger(Prism.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel actionEn;
    private javax.swing.JLabel actionZh;
    private javax.swing.JProgressBar initProgress;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JLabel statusZh;
    private javax.swing.JLabel timeLabel;
    private javax.swing.JButton unlockBt;
    // End of variables declaration//GEN-END:variables

    @Override
    public void run() {
        Calendar cal;
        while(true){
        cal  = Calendar.getInstance();
        
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy, z hh:mm:ss aa");
        Date dat = cal.getTime();
        String time12 = sdf.format(dat);
        
        timeLabel.setText(time12);
        
        }
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
