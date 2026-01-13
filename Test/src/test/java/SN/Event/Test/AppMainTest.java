package SN.Event.Test;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class AppMainTest {

    @Test
    void testMainOutput() {
        // Capture System.out
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

//        // Call the main method
        App.main(new String[]{});

        // Expected output from App.main
        String expectedOutput = "Add: 15" + System.lineSeparator() +
                                "Subtract: 5" + System.lineSeparator() +
                                "Max: 10" + System.lineSeparator() +
                                "Divide: 2" + System.lineSeparator();

        assertEquals(expectedOutput, outContent.toString());

        // Restore System.out
        System.setOut(originalOut);
        
    }
}
