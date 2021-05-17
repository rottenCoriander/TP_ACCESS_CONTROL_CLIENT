/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package access_control_client;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Admin
 */
public class CardDetector {
    private ConcurrentLinkedQueue<CardListener> listeners;
    boolean val;
    
    //addListener
    public void addListener(CardListener cardListener){
        if(listeners == null){
            listeners = new ConcurrentLinkedQueue();
        }
        listeners.add(cardListener);
    }
    
    public void removeListener(CardListener cardListener){
        if(listeners == null)
            return;
        listeners.remove(cardListener);
    }
    
    public Boolean getReadCard(){
       // ReadCard event = new ReadCard(this);
       return val;
        
    }
    
    protected void setReadCard(Boolean cardState){
        if(listeners == null)
            return;
        val = cardState;
        ReadCard event = new ReadCard(this);
        event.setCardDetect(cardState);
        notifyListeners(event);
    }
    
    private void notifyListeners(ReadCard event){
        for(CardListener cardListener:listeners)
            cardListener.cardEvent(event);
    }
    
    
    
}
