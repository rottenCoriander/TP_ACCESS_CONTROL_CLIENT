/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package access_control_client;

import java.util.EventListener;

/**
 *
 * @author Admin
 * Listener Interface
 */
public interface CardListener extends EventListener {
    void cardEvent(ReadCard event);

    
}
