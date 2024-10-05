import swiftbot.*; //list of all my imports
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DoesMySwiftBotWork {
    static SwiftBotAPI swiftBot; 
    private static List<String> colour_array = new ArrayList<>(); //list of all my global variables
    private static String Traffic_Colour = "";
    private static long startTime = 0;
    private static double time = 0;
    private static String frequent = "";
    private static int num_times = 0;
    
    public static void main(String[] args) throws InterruptedException {
        swiftBot = new SwiftBotAPI();
        System.out.println("PRESS BUTTON A TO START PROGRAM");
        swiftBot.enableButton(Button.A, () -> {
            System.out.println("Button A HAS BEEN PRESSED");
            swiftBot.disableButton(Button.A);
            long startTime = System.currentTimeMillis();
            IDLE_STATE();
        });
    } 
    
        
    public static void IDLE_STATE() { //idle state exactly shown in flowchart, pieces the code together
    	swiftBot.disableAllButtons();
        System.out.println("SWIFTBOT IS SWITCHING TO IDLE STATE");
        int[] yellowLight = {255, 0, 255};
        swiftBot.fillUnderlights(yellowLight);
        RUNBOTHATTHESAMETIME();
        System.out.println("LIGHT DETECT");
        Camera();
        COLOUR_MATRIX();
        if (Traffic_Colour == "r") {
            Red();
            colour_array.add("r");
        } else if (Traffic_Colour == "b") {
            Blue();
            colour_array.add("b"); //adds detected colours to colour array
        } else {
            Green();
            colour_array.add("g");
        }
        int multiple = colour_array.size();
        System.out.println(multiple);
        if (multiple % 3 == 0) {
    	   System.out.println("Would you like to end the program (Y/X)?");
           swiftBot.enableButton(Button.Y, () -> {
               System.out.println("Button Y HAS BEEN PRESSED");
               swiftBot.disableButton(Button.Y);
               END();
           });
           swiftBot.enableButton(Button.X, () -> {
               System.out.println("Button X HAS BEEN PRESSED");
               swiftBot.disableButton(Button.X);
               IDLE_STATE();
           });
        } else {
        	IDLE_STATE();
        }
    }
    
    public static void END() {
        long endTime = System.currentTimeMillis(); //end is made as a seperate method from main, due to button errors
        time = ((endTime - startTime)/1000);
        System.out.println("PRESS B TO DISPLAY THE JOURNEY LOG, PRESS A TO TERMINATE THE PROGRAM");
        swiftBot.enableButton(Button.B, () -> {
            System.out.println("Button B HAS BEEN PRESSED");
            swiftBot.disableButton(Button.B);
            LOG();
            SAVE_LOG(time);
            System.out.println("LOG SUCCESSFULLY SAVED TO SYSTEM, PROGRAM ENDED");
            System.exit(0);
        });
        swiftBot.enableButton(Button.A, () -> {
            System.out.println("Button A HAS BEEN PRESSED");
            swiftBot.disableButton(Button.A);
            SAVE_LOG(time);
            System.out.println("LOG SUCCESSFULLY SAVED TO SYSTEM, PROGRAM ENDED");
            System.exit(0);
        });
    }

    
    public static void RUNBOTHATTHESAMETIME() {
        Thread moveThread = new Thread(() -> swiftBot.startMove(100, 100));
        Thread ultrasoundThread = new Thread(() -> {
            Ultrasound();
        });

        moveThread.start();
        ultrasoundThread.start(); //runs ultrasound and swiftbot movement at same time

        try {
            moveThread.join();
            ultrasoundThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    
    public static void Ultrasound(){
    	boolean shouldStop = false;
    	while (shouldStop = false) {
            double ultrasoundValue = swiftBot.useUltrasound();
            if (ultrasoundValue <= 5) { //if ultrasound value is <5, light is detected
                shouldStop = true; //loop breaks and the swiftbot stops
                swiftBot.stopMove();
            }
            try {
                Thread.sleep(1000); // Adjust sleep time as needed
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void Camera(){
        try{
            BufferedImage image = swiftBot.takeStill(ImageSize.SQUARE_144x144);
            
            if(image == null){
                System.out.println("ERROR: Image is null");
                System.exit(5);
            }
            else{
                //saves coloured images to the swiftbot in a filepath
                ImageIO.write(image, "png", new File("/home/pi/colourImage.png"));

                System.out.println("PICTURE TAKEN SUCCESSFULLY");
            }
        }
        catch (Exception e){
            System.out.println("ERROR WHILE TAKING PICTURE...TRYING AGAIN");
            Camera(); //recrusion, itll call itself and try again
        }
    }
       
    public static void COLOUR_MATRIX() {
        String imagePath = "/home/pi/colourImage.png"; //path where the colored imaged gets saved
        try {
            File file = new File(imagePath); //gets the image from the filepath and stores it as file
            BufferedImage image = ImageIO.read(file); //read the file
            int[][][] rgbMatrix = new int[image.getWidth()][image.getHeight()][3]; //gets the height and width and stores it as rgb matrix
            for (int y = 0; y < image.getHeight(); y++) { //for each y row, get every x pixel
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y); //determine the rgb value
                    rgbMatrix[x][y][0] = (rgb >> 16) & 0xFF; // red 
                    rgbMatrix[x][y][1] = (rgb >> 8) & 0xFF;  // green
                    rgbMatrix[x][y][2] = rgb & 0xFF;         // bue
                }     //stores the rgb value at of each pixel in every row, until its done
            }
            Traffic_Colour = DETECT_COLOUR(rgbMatrix); //takes rgb matrix over to the detect colour method
        } catch (IOException e) {
            System.err.println("ERROR WHILE READING.TRYING AGAIN");
            COLOUR_MATRIX(); //recursion
        }
    }

    public static String DETECT_COLOUR(int[][][] colour_matrix) {
        int total_pixels = colour_matrix.length * colour_matrix[0].length;
        int redSum = 0, greenSum = 0, blueSum = 0; //counters for rgb
        for (int[][] row : colour_matrix) { 
            for (int[] pixel : row) { //for each pixel in every row in the colour matrix
                redSum += pixel[0]; //count if its red green or blue
                greenSum += pixel[1];
                blueSum += pixel[2];
            }
        }
        int avgRed = redSum / total_pixels; //average number of every coloured pixel
        int avgGreen = greenSum / total_pixels;
        int avgBlue = blueSum / total_pixels;
        if (avgRed > avgGreen && avgRed > avgBlue) {  //same as the flowchart, if statement that returns the value and stores it as traffic colour
            return "r";
        } else if (avgGreen > avgRed && avgGreen > avgBlue) {
            return "g";
        } else {
            return "b";
        }
    }

    public static void SAVE_LOG(double x) {
        String filePath = "/home/pi/Documents/log.txt"; //filepath of the log file
        int num1 = colour_array.size();
        double num2 = x; //time value
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(String.valueOf(num1)); //ADDS HOW MANY TRAFFIC LIGHT ENCOUNTRED TO FILE
            writer.write(String.valueOf(num2)); //ADD TIME TO FILE
            writer.write(String.valueOf(frequent)); //adds most frequent color
            writer.write(String.valueOf(num_times)); //adds number of times
            System.out.println("Data has been written to the file.");
        } catch (IOException e) {
            System.err.println("Error writing to the file");
            SAVE_LOG(x);
        }
    }

    public static void LOG() {
        int len = colour_array.size();
        int r_count = 0;
        int g_count = 0;
        int b_count = 0;

        for (String x : colour_array) { //for each element in the colour_array
            if (x.equals("r")) {
                r_count++;
            } else if (x.equals("g")) { //counters for each light detected in the array
                g_count++;
            } else if (x.equals("b")) {
                b_count++;
            }
        }

        if (r_count > g_count && r_count > b_count) { //compares the count to find the most frequent colour
            frequent = "red";
            num_times = r_count;
        } else if (g_count > r_count && g_count > b_count) {
            frequent = "green";
            num_times = g_count;
        } else if (b_count > r_count && b_count > g_count) {
            frequent = "blue";
            num_times = b_count;
        }

        System.out.println("System_Log"); //outputs the system log
        System.out.println("The number of times a light was encountered was " + len);
        System.out.println("\nThe duration was "+ time);
        System.out.println("\nMost frequent traffic light: " + frequent);
        System.out.println("\nNumber of occurrences for the most frequent light: " + num_times);
    }

    public static void Green() {
        System.out.println("GREEN LIGHT DETECTED");
        int[] greenLight = {0, 0, 255}; //NOTE THE VALUES FOR GREEN AND BLUE ARE RBG NOT RGB
        swiftBot.fillUnderlights(greenLight);
        swiftBot.move(50, 50, 2000);
        swiftBot.stopMove();
    }

    public static void Red() {
        System.out.println("RED LIGHT DETECTED");
        int[] redLight = {255, 0, 0};
        swiftBot.fillUnderlights(redLight);
        swiftBot.move(0, 0, 500);
    }

    public static void Blue() {
        System.out.println("BLUE LIGHT DETECTED");
        int[] blueLight = {0, 255, 0}; //value is rbg not rgb
        swiftBot.move(0, 0, 500);
        swiftBot.fillUnderlights(blueLight);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        	System.out.println("ERROR, retrying");
        	Blue();
        }
        swiftBot.disableUnderlights();
        swiftBot.move(100, 0, 2000); //turns left, moves for bit then retraces backwards
        swiftBot.move(50, 50, 2000);
        int leftwheelv = -50;
        int rightwheelv = -50;
        swiftBot.move(leftwheelv, rightwheelv, 2000);
        leftwheelv = -100;
        rightwheelv = 0;
        swiftBot.move(leftwheelv, rightwheelv, 2000);
    }
}