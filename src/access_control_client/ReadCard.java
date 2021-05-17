/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package access_control_client;

import java.util.EventObject;

/**
 *
 * @author Admin
 */
public class ReadCard extends EventObject{
    private Boolean cardDetect = false;
    public void setCardDetect(Boolean b){
        this.cardDetect = b;
    }
    public Boolean getCardDetect(){
        return this.cardDetect;
    }
    public ReadCard(Object source) {
        super(source);
    }
    
}
