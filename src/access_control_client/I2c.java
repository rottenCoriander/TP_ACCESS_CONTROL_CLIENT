/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package access_control_client;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import java.io.IOException;

/**
 *
 * @author Admin
 */
public class I2c {
   // public final static int SLAVE_ADDRESS; // See RPi_I2C.ino. Must be in sync.
    private static boolean verbose = "true".equals(System.getProperty("i2c.verbose", "false"));
    
    private I2CBus bus;
    private I2CDevice device;
    /*
    public I2c() throws I2CFactory.UnsupportedBusNumberException {
		this(SLAVE_ADDRESS);
	}*/
    public I2c(int address) throws I2CFactory.UnsupportedBusNumberException {
		try {
			// Get i2c bus
			bus = I2CFactory.getInstance(I2CBus.BUS_1); // Depends onthe RasPI version
			if (verbose) {
				System.out.println("Connected to bus. OK.");
			}

			// Get device itself
			device = bus.getDevice(address);
			if (verbose) {
				System.out.println("Connected to device. OK.");
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
    
    public void close() {
		try {
			this.bus.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
    
    public int read()
					throws Exception {
		int r = device.read();
		return r;
	}

	public void write(byte b)
					throws Exception {
		device.write(b);
	}

	private static void delay(float d) // d in seconds.
	{
		try {
			Thread.sleep((long) (d * 1_000));
		} catch (Exception ex) {
		}
	}
    
}
