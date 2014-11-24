int pin_red   = 6;
int pin_green = 9;
int pin_blue  = 5;

byte red1   = 64;
byte green1 = 64;
byte blue1  = 64;

float red2   = 0;
float green2 = 0;
float blue2  = 0;

int counter = 0;

void setup()  {
	Serial.begin(115200);
	Serial.setTimeout(20);

	pinMode(pin_red,   OUTPUT);
	pinMode(pin_green, OUTPUT);
	pinMode(pin_blue,  OUTPUT);

    analogWrite(pin_red,   0);
    analogWrite(pin_green, 0);
    analogWrite(pin_blue,  0);
}

void loop()  {
	char color[3] = {0, 0, 0};
	int num_bytes = 0;
	num_bytes = Serial.readBytes(color, 3);

	if(num_bytes == 3) {
		red1   = color[0];
		green1 = color[1];
		blue1  = color[2];
	}

	red2   += (red1   - red2  ) / 40;
	green2 += (green1 - green2) / 40;
	blue2  += (blue1  - blue2 ) / 40;

	analogWrite(pin_red,   red2  );
	analogWrite(pin_green, green2);
	analogWrite(pin_blue,  blue2 );

	counter++;

	if(counter == 10) {
		counter = 0;
		Serial.write(red1  );
		Serial.write(green1);
		Serial.write(blue1 );
	}
}
