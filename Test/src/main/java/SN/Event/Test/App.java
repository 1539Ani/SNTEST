package SN.Event.Test;

public class App {

    public static void main(String[] args) {
        Calculator calc = new Calculator();

        int a = 10;
        int b = 5;

        System.out.println("Add: " + calc.add(a, b));
        System.out.println("Subtract: " + calc.subtract(a, b));
        System.out.println("Max: " + calc.max(a, b));
        System.out.println("Divide: " + calc.divide(a, b));

    }
}
