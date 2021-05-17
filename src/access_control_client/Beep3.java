/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package access_control_client;

import static com.pi4j.wiringpi.Gpio.delay;
import com.pi4j.wiringpi.SoftTone;

/**
 *
 * @author Admin
 */
public class Beep3 extends Thread{
    public void run(/*int frequncy, int delay*/){

      SoftTone.softToneCreate(26);
      SoftTone.softToneWrite(26, 4820);
        delay(75);
        SoftTone.softToneWrite(26, 0);
    }
}
