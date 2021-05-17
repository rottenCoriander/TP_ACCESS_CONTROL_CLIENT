/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package access_control_client;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Admin
 */
public class Authentication implements Runnable{
    
    ReadCard rc;
    Boolean loop = true;
    String saa;
    
    //Database
    Connection conn = null;
    Statement st = null;
    ResultSet rs = null;
    String query;
    String update;
    String UID;
    String sqldata = null;
    String readdata;
    String DB_NAME;
    int DB_STATUS;
    int DB_GROUPID;
    String DB_UID;
    String DB_WELLCOME_MESSAGE;

    //RC522
    RaspRC522 rc522;
    int back_len[]=new int[1];
    String MA;
    int i,status;
    String strUID;
    byte sector=14,block=1;
    byte[] key=new byte[]{(byte)0x01,(byte)0x04,(byte)0x20,(byte)0x20,(byte)0x01,(byte)0x22};
    int states = 0;
    public String uid;
    
   
    
    
    @Override
    public void run() {
        loop = true;
        
        while(loop){
            
        if(CardDetected()){
        Prism.cardDetector.setReadCard(true);
        loop = false;
            }
         
        }
        
    }
    
    public void init() throws ClassNotFoundException{
        Class.forName("com.mysql.cj.jdbc.Driver");
    }
    
    public void connectDatabase() throws SQLException{
        conn = DriverManager.getConnection("jdbc:mysql://172.16.1.240:3306/access_control", "Admin" , "!Himbeere");
        st= conn.createStatement();
    }
    
    public Boolean DBisClosed() throws SQLException{
        return conn.isClosed();
    }
    
     public boolean CardDetected(){
        Boolean b=false;
        rc522=new RaspRC522();
        if(rc522.Request(RaspRC522.PICC_REQIDL, back_len) == rc522.MI_OK){
         
        //Anticoll
        b = true;
            }
        rc522.AntiColl(RFID_READIN.tagid);
       
        
        return b;
    }
    
    
    public boolean Authenticate(){
        
        Boolean b=false;
        strUID= Convert.bytesToHex(RFID_READIN.tagid);
        //System.out.println(strUID);
        //System.out.println("Card Read UID:" + tagid[0] + "," + tagid[1] + "," + tagid[2] + "," + tagid[3]);
       /* System.out.println("Card Read UID:" + strUID.substring(0,2) + "," +
                strUID.substring(2,4) + "," +
                strUID.substring(4,6) + "," +
                strUID.substring(6,8));*/
        uid = strUID.substring(0,2) + strUID.substring(2,4)+strUID.substring(4,6)+strUID.substring(6,8);
        UID =strUID.substring(0,2) + strUID.substring(2,4) +strUID.substring(4,6) +strUID.substring(6,8);
        int size=rc522.Select_Tag(RFID_READIN.tagid);
       // System.out.println("Size="+size);
        
        status = rc522.Auth_Card(RaspRC522.PICC_AUTHENT1A, sector,block, key, RFID_READIN.tagid);
        if(status != RaspRC522.MI_OK)
        {
           // System.out.println("Authenticate error");
            //return;
        }else{
            //System.out.println("Authenticated");
            b = true;
        }
        
        return b;
    }
    
    public boolean readData(){
        Boolean b = false;
        byte data[]=new byte[16];
        query = "SELECT * FROM access_control.card where uid = \""+UID+"\";";
        try {
            rs = st.executeQuery(query);
            rs.next();
            sqldata = rs.getString("data");
            DB_UID = rs.getString("UID");
            DB_STATUS = rs.getInt("STATUS");
            DB_GROUPID = rs.getInt("GROUPID");
            DB_WELLCOME_MESSAGE = rs.getString("WELLCOME_MESSAGE");
        } catch (SQLException ex) {
            Logger.getLogger(Authentication.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        status=rc522.Read(sector,block,data);
        readdata= Convert.bytesToHex(data);
        
        
        System.out.println(data[6]);
        System.out.println(data[7]);
        System.out.println(data[8]);
        System.out.println(data[9]);
        
        //status=rc522.Read(sector,(byte)3,data);
        System.out.println(readdata);
        
        if(sqldata != null & readdata != null){
            if(sqldata.equals(readdata)){
            try {
                DB_NAME = rs.getString("name");
            } catch (SQLException ex) {
                Logger.getLogger(Authentication.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("資料核對結果吻合，准予通行");
            System.out.println("Data Matched , Access Granted!");
            System.out.println("持卡人:"+DB_NAME);
            System.out.println("Passed For:"+DB_NAME);
            System.out.println("----------------------------");
            b = true;
        }else{
            //System.out.println("Data NOT Matched, Access Denied");
            b = false;
            }
        }else{
            b = false;
        }
        
        return b;
    }
    
    public boolean checkCardEnable(){
       
        if(DB_STATUS == 1){
            return true;
        }else{
            return false;
        }
    }
    public boolean checkGroup(){
      
        if(DB_GROUPID == 0 | DB_GROUPID == 1 | DB_GROUPID == 2){
           return true; 
        }else{
        return false;
    }
}
    
    public String getDB_NAME(){
        return DB_NAME;
    }
    
    public void uploadRecord(int access,String reason){
        Date date = new Date();
        switch(access){
            case 0:
                update = "INSERT Into access_control.records Values (NULL,"+"\""+UID+"\""+","+"\""+DB_NAME+"\""+","+Integer.toString(DB_GROUPID)+","+DB_STATUS+",1,"+"\""+date.toString()+"\""+","+reason+",1);";
                break;
            case 1:
                update = "INSERT Into access_control.records Values (NULL,"+"\""+UID+"\""+",NULL,NULL,NULL,0,"+"\""+date.toString()+"\""+","+"\""+reason+"\""+",1);";
                break;
            case 2:
                update = "INSERT Into access_control.records Values (NULL,"+"\""+UID+"\""+","+"\""+DB_NAME+"\""+","+Integer.toString(DB_GROUPID)+","+DB_STATUS+",0,"+"\""+date.toString()+"\""+","+"\""+reason+"\""+",1);";
                break;
            case 3:
                update = "INSERT Into access_control.records Values (NULL,"+"\""+UID+"\""+","+"\""+DB_NAME+"\""+","+Integer.toString(DB_GROUPID)+","+DB_STATUS+",0,"+"\""+date.toString()+"\""+","+"\""+reason+"\""+",1);";
                break;
            default :
                update = "INSERT Into access_control.records Values (NULL,"+"\""+UID+"\""+","+"\""+DB_NAME+"\""+","+Integer.toString(DB_GROUPID)+","+DB_STATUS+",0,"+"\""+date.toString()+"\""+","+"\""+reason+"\""+",1);";
                break;
        
        }
        
        
        try {
            //rs = st.executeQuery(query);
            st.executeUpdate(update);
        } catch (SQLException ex) {
            Logger.getLogger(Authentication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
