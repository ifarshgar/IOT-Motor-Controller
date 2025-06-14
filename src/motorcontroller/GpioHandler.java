
package motorcontroller;

import com.pi4j.component.lcd.impl.GpioLcdDisplay;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GpioHandler {
    public static int OptoCounter =0;
    public static double speedcms;
    public static float wRadius;
    public static int CycleForce=0;
    public static boolean MotorStatus = false;
    public static long startTime  = 0 ;
    public static long CurrentTime = 0 ;
    public static double Pdx = 0;//public dx
    
    GpioController gpio ;
    GpioPinDigitalOutput ENA , IN1 , IN2 ;//motor
    GpioPinDigitalInput Opto;
    //LCD 
    final GpioLcdDisplay lcd;
    public final  int LCD_ROWS = 2; // No of rows 1/2
    public final  int LCD_ROW_1 = 0;
    public final  int LCD_ROW_2 = 1;
    public final  int LCD_COLUMNS = 16; //No of columns 
    
    //classMethod
    public GpioHandler(){
        OptoCounter=0;
        speedcms=0;
        gpio = GpioFactory.getInstance();
        //MOTOR DRIVER
        ENA   = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "ENA", PinState.LOW);//#11
        IN1   = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "IN1", PinState.LOW);//#13
        IN2   = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "IN2", PinState.LOW);//#15
        //turn off motor 
        ENA.low();
        IN1.low();
        IN2.low();
        //LCD  
        lcd = new GpioLcdDisplay(LCD_ROWS,    // number of row supported by LCD
                                                    LCD_COLUMNS,       // number of columns supported by LCD
                                                    RaspiPin.GPIO_25,  // LCD RS pin
                                                    RaspiPin.GPIO_24,  // LCD strobe pin //e
                                                    RaspiPin.GPIO_23,  // LCD data bit D4
                                                    RaspiPin.GPIO_22,  // LCD data bit D5
                                                    RaspiPin.GPIO_21,  // LCD data bit D6
                                                    RaspiPin.GPIO_14); // LCD data bit D7
        lcd.clear();
        lcd.write(LCD_ROW_1, "speed : " + "00.00");
        lcd.write(LCD_ROW_2, "counter : " + "0000");
        
        //OPTO COUNTER
        Opto = gpio.provisionDigitalInputPin(RaspiPin.GPIO_06, PinPullResistance.PULL_DOWN);
        Opto.addListener((GpioPinListenerDigital) (GpioPinDigitalStateChangeEvent event) -> {
            if(event.getState() == PinState.HIGH){
                int numberCycle = ++GpioHandler.OptoCounter;//count rpm
                //calc speed
                if(GpioHandler.wRadius<=0) GpioHandler.wRadius=1;
                double dx=((2*Math.PI)* GpioHandler.wRadius) * numberCycle;
                GpioHandler.Pdx = dx;
                GpioHandler.CurrentTime  = System.nanoTime();
                double dt = (GpioHandler.CurrentTime - GpioHandler.startTime)/1_000_000_000.0;
                //                  System.out.println("dx " + dx + " dt " + dt + " count : " + numberCycle );
                GpioHandler.speedcms = dx/dt;
                //                  System.out.println("GpioHandler.speedcms" + GpioHandler.speedcms);
                //show speed on LCD 16*2
                lcd.clear();
                lcd.write(LCD_ROW_1, "speed : " + GpioHandler.speedcms);
                lcd.write(LCD_ROW_2, "cunter : " + numberCycle);
                try {//take a break for LCD refresh
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GpioHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                //Make The motor stop
                if(numberCycle >= GpioHandler.CycleForce && GpioHandler.CycleForce!=0){
                    GpioHandler.startTime=0;
                    CurrentTime=0;
                    ENA.low();
                    IN1.low();
                    IN2.low();
                }
            }
        }); //event for counting   
    }
    //functions
    public void RunMotor(boolean forward){
        GpioHandler.startTime = System.nanoTime();
        GpioHandler.OptoCounter=0;
        MotorStatus=true;
        if(forward){
            ENA.high();
            IN1.high();
            IN2.low();
        }else{
            ENA.high();
            IN1.low();
            IN2.high();
        }
    }
    public void StopMotor(){
        GpioHandler.startTime=0;
        CurrentTime=0;
        MotorStatus=false;
        ENA.low();
        IN1.low();
        IN2.low();
    }
    public void ClearLCD(){
        lcd.clear();
        lcd.write(LCD_ROW_1, "speed : " + GpioHandler.speedcms);
        lcd.write(LCD_ROW_2, "fcunter : " + GpioHandler.CycleForce);
    }
}
