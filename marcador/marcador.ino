// sparkfun pro micro 5V/16MHz

#include "SPI.h"
#include "pratipo_WS2801.h"
#include "alphabet710.h"

/*****************************************************************************
Example sketch for driving Pratipo WS2801 pixels!


  Designed specifically to work with the Pratipo RGB Pixels!
  12mm Bullet shape ----> https://www.Pratipo.com/products/322
  12mm Flat shape   ----> https://www.Pratipo.com/products/738
  36mm Square shape ----> https://www.Pratipo.com/products/683

  These pixels use SPI to transmit the color data, and have built in
  high speed PWM drivers for 24 bit color per pixel
  2 pins are required to interface

  Pratipo invests time and resources providing this open source code, 
  please support Pratipo and open-source hardware by purchasing 
  products from Pratipo!

  Written by David Kavanagh (dkavanagh@gmail.com).  
  BSD license, all text above must be included in any redistribution

*****************************************************************************/

// Choose which 2 pins you will use for output.
// Can be any valid output pins.
// The colors of the wires may be totally different so
// BE SURE TO CHECK YOUR PIXELS TO SEE WHICH WIRES TO USE!
uint8_t dataPin  = 6;    // Yellow wire on Pratipo Pixels
uint8_t clockPin = 5;    // Green wire on Pratipo Pixels

// Don't forget to connect the ground wire to Arduino ground,
// and the +5V wire to a +5V supply

// Set the first variable to the NUMBER of pixels in a row and
// the second value to number of pixels in a column.
Pratipo_WS2801 strip = Pratipo_WS2801((uint16_t)280, dataPin, clockPin);

char text[] = {'0', '0','0','0'};

char modeA = 1;
char modeB = 1;

long prev;
int interval = 500;
int vel = 5;
int cycle = 0;

void setup() {
  Serial.begin(9600);
  Serial1.begin(9600);
  
  strip.begin();
  strip.show(); // all off
  
  prev = millis();
}

void loop() {         
 //TXLED1; delay(500); 
 //TXLED0; delay(500); 
 //Serial.println("*");
 //Serial.println(str);
 
 String s; // Allocate some space for the string
 if(Serial1.available() > 0)
 {
   s = Serial1.readStringUntil('\n');
   Serial.print("got "); Serial.print(s);
   
   if (s[0] == 'A')
     modeA = s[1]-48;
   else if (s[0] == 'B')
     modeB = s[1]-48 ;
   else{
     for (int i=0; i<4; i++){
       text[i] = s[i]-48;
     }  
   }
 }
 
 if (millis()-prev > interval){
   prev = millis();
   Text(cycle++);
 }
 
}

void Text(int cyc){
 
  char mode = 1;
  
  for (int i=0; i<4; i++){
    char caracter = text[i];
    uint32_t color;
    
    int seed = 0;
    int offset = 0 ;
    
    if(i==0)
      mode = modeA;
    if(i==2)
      mode = modeB;
      
    if (mode == 1) // random per-char color
      color = Wheel( random(64)+i*64 );
     
    for (int x=0; x<7; x++){
      
      if (mode == 3)
        color = Wheel(((7*i+x)*vel + cyc) % 255); // scrolling rainbow
      
      for (int y=0; y<10; y++){
        
        if( 1<<(6-x) & pgm_read_byte_near(font710 + 10*caracter + y) ){

          if (mode == 2) // random per-pixel random
            color = Wheel( (random(64)+i*64) );
        
          strip.setPixelColor710(x+i*7, 9-y, color);
        } else {
          strip.setPixelColor710(x+i*7, 9-y, Color(0,0,0));
        }
      } 
    }
  }
  strip.show();
}


void black() {
  for (int i=0; i < 280; i++) {
     strip.setPixelColor(i, 0, 0, 0);
  }
  strip.show();
}

/* Helper functions */

// Create a 24 bit color value from R,G,B
uint32_t Color(byte r, byte g, byte b)
{
  uint32_t c;
  c = r;
  c <<= 8;
  c |= g;
  c <<= 8;
  c |= b;
  return c;
}

//Input a value 0 to 255 to get a color value.
//The colours are a transition r - g -b - back to r
uint32_t Wheel(byte WheelPos)
{
  if (WheelPos < 85) {
   return Color(WheelPos * 3, 255 - WheelPos * 3, 0);
  } else if (WheelPos < 170) {
   WheelPos -= 85;
   return Color(255 - WheelPos * 3, 0, WheelPos * 3);
  } else {
   WheelPos -= 170; 
   return Color(0, WheelPos * 3, 255 - WheelPos * 3);
  }
}
