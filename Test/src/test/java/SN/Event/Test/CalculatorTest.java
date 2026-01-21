package SN.Event.Test;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    Calculator calc = new Calculator();

    @Test
    void testAdd() {
        assertEquals(20, calc.add(10, 5));
        assertEquals(0, calc.add(-5, 5));
    }

    @Test
    void testSubtract() {
        assertEquals(5, calc.subtract(10, 5));
        assertEquals(-10, calc.subtract(-5, 5));
    }

    @Test
    void testMax() {
        assertEquals(10, calc.max(10, 5));
        assertEquals(5, calc.max(5, 5));
        assertEquals(20, calc.max(15, 20));
    }

    @Test
    void testDivide() {
        assertEquals(2, calc.divide(10, 5));
        assertEquals(-2, calc.divide(-10, 5));
    }

    @Test
    void testDivideByZero() {
        assertThrows(IllegalArgumentException.class, () -> calc.divide(10, 0));
    }

}
