public class ComplexBranching {

    public int calculate(int a, int b, int c) {
        int result = 0;

        if (a > 0) {
            result += a;
        } else if (a < 0) {
            result -= a;
        } else {
            result = 1;
        }

        if (b > 0 && c > 0) {
            for (int i = 0; i < b; i++) {
                if (i % 2 == 0) {
                    result += i;
                } else {
                    result -= i;
                }
            }
        } else if (b > 0 || c > 0) {
            while (b > 0) {
                result += b--;
            }
        }

        switch (c) {
            case 1: result += 1; break;
            case 2: result += 2; break;
            default: result += 3;
        }

        return result;
    }
}
