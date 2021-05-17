/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package access_control_client;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.util.CommandArgumentParser;
import static com.pi4j.wiringpi.Gpio.delay;
import com.pi4j.wiringpi.SoftTone;

/**
 *
 * @author Admin
 */
public class Beep extends Thread{
    
    public void run(/*int frequncy, int delay*/){

      SoftTone.softToneCreate(26);
      SoftTone.softToneWrite(26, 1200);
        delay(100);
        SoftTone.softToneWrite(26, 0);
    }
    
}
