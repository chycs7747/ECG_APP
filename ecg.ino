#include <SoftwareSerial.h>

int Tx = 6; //전송 보내는핀  8
int Rx = 7; //수신 받는핀
int count = 512; //테스트 횟수
SoftwareSerial bluetooth(Tx,Rx);
int tmp1, tmp2;
byte ecg_data[1024];




void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  bluetooth.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly:
  int a = bluetooth.read();
  if(a!=-1) {
    Serial.println(a);  
    count = 0;  
  }
  if(count<512) {
    if (bluetooth.available()) {
      Serial.write(bluetooth.read());
    }
    if (Serial.available()) {
      bluetooth.write(Serial.read());
    }

    tmp2 = analogRead(0);
 
    tmp1 = (tmp2 & 0xff00);
    tmp2 = (tmp2 & 0x00ff);

    ecg_data[count*2] = (tmp1>>8)+1;
    ecg_data[count*2+1] = tmp2+1;

    delay(20);
    count++;
  }
  else if(count == 512){
    bluetooth.write(ecg_data,1024);
    count++;
  }

}
