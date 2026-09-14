public class TestCode {

    private int number;

    public int calculate(int a, int b) {

        if (a > b && b > 0) {
            return a;
        } else if (a == b) {
            return 0;
        }

        for (int i = 0; i < 5; i++) {
            number++;
        }

        return b;
    }

    public void printMessage() {
        System.out.println("Hello");
    }
}