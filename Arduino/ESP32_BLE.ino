// this header is needed for Bluetooth Serial -> works ONLY on ESP32
#include "BluetoothSerial.h" 

// init Class:
BluetoothSerial ESP_BT; 

// init PINs: assign any pin on ESP32
int pins[] = {0,1,2,3,4,5,12,13,14,15,16,17,18,19,21,22,23,25,26,27,32,33,34,36,39};

void setup() {
  Serial.begin(9600);

  ESP_BT.begin("ESP32_Control"); //Name of the Bluetooth interface -> will show up on your phone
  
  for(int i = 0; i< sizeof(pins); i++) { // setting all the pins
    pinMode (pins[i], OUTPUT);
  }

  while (!ESP_BT.available()) {}
}

void loop() {
  // -------------------- Receive Bluetooth signal ----------------------
  if (ESP_BT.available()) 
  {
    String incomingString;
    
    while(ESP_BT.available()) {//Read what we receive
      int temp =  ESP_BT.read();
      incomingString = incomingString + (char)temp;
    }

    // split received message according to "-" 
    bool flag = true;
    String buttonId = "";
    String buttonVal = "";
    for(int i = 0; i < incomingString.length(); i++) {
      char temp = incomingString.charAt(i);
      if (temp == '-') {
          flag = false;
      } else if (flag) {
        buttonId += temp;
      } else if (!flag) {
        buttonVal += temp;
      }
    }
    
    if(buttonId.equals("$$")) { // when triggered callback initial status of the pin
      Serial.println("Initial Status were sent!");
      String initialStatus = createStatusString();
      callback(initialStatus);
    } else if(buttonId.equals("&&")) { // when triggered, turn on all pins and callbackthe new pin status
      Serial.print("all pins are turning on");
      setAllPins(1);
      String newStatus = createStatusString();
      callback(newStatus);
    } else if(buttonId.equals("%%")) { // when triggered, turn off all pins and callbackthe new pin status
      Serial.print("all pins are turning off");
      setAllPins(0);
      String newStatus = createStatusString();
      callback(newStatus);
    } else { // trigger a pin according to its pin id and pin value
      buttonTrigger(buttonId, buttonVal);
    }
  }
}

void setAllPins(int value) { //sets all pins to desired value
  for(int i = 0; i< sizeof(pins); i++) {
    digitalWrite (pins[i], value);
  }
}
String createStatusString() { //creates a status string according to all pin values
  String newStatus = "";
  for(int i = 0; i< sizeof(pins); i++) {
    digitalRead(pins[i]) ? newStatus += "1" : newStatus += "0";
  }
  newStatus = "newStatus" + newStatus;
  Serial.println(newStatus);
  return newStatus;
}
void buttonTrigger(String buttonId, String buttonVal) { // triggers a specific button with spesific value 
  Serial.print("Pin ");
  Serial.print(buttonId);
  Serial.print(":");
  int buttonIdInt = buttonId.toInt();
  if(buttonVal.equals("1")){
    Serial.println(1);
    digitalWrite(buttonIdInt, 1);
    if(digitalRead(buttonIdInt)) {
      callback("on");
    }else{
      callback("Something is wrong!");
    }
  } else {
    Serial.println(0);
    digitalWrite(buttonIdInt, 0);
    if(!digitalRead(buttonIdInt)) {
      callback("off");
    }else{
      callback("Something is wrong!");
    }
  }
}
void callback(String a) { // callback function a bluetooth request
  Serial.println("Callback");
  uint8_t buf[a.length()];
  memcpy(buf, a.c_str(), a.length());
  ESP_BT.write(buf, a.length());
}