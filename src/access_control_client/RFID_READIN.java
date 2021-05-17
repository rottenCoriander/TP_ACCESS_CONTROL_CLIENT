/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package access_control_client;

/**
 *
 * @author Admin
 */
public interface RFID_READIN {
    public byte tagid[]=new byte[5];
    public boolean prog = true;
    public int state = 0;
    public final String uid="00000000";
    public boolean passed = true;
}
