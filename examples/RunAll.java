import java.lang.reflect.Method;

/**
 * Runs every example in order.
 *
 * <p>Mock transport by default. Set INOVIO_LIVE=1 with credentials to run the
 * same code against the real gateway.
 */
public class RunAll {
    public static void main(String[] args) throws Exception {
        String[] examples = {
            "Example01TestAvailability", "Example02TestAuth", "Example03Sale",
            "Example04Authorize", "Example05Capture", "Example06CaptureLineItem",
            "Example07Reverse", "Example08ReverseCapture", "Example09Refund",
            "Example10ForceCredit", "Example11Status", "Example12UpdateOrder",
            "Example13Tokenize", "Example14TimeoutRecovery",
        };
        System.out.printf("Running %d examples against %s%n%n", examples.length,
            Harness.LIVE ? "the LIVE gateway" : "a mock transport");

        int failed = 0;
        for (String name : examples) {
            String title = name.replaceAll("^Example\\d\\d", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
            System.out.println("── " + title.trim());
            try {
                Method m = Class.forName(name).getMethod("main", String[].class);
                m.invoke(null, (Object) new String[0]);
            } catch (Exception e) {
                failed++;
                Throwable cause = e.getCause() == null ? e : e.getCause();
                System.out.printf("  ✗ %s: %s%n",
                    cause.getClass().getSimpleName(), cause.getMessage());
            }
            System.out.println();
        }
        System.out.println(failed == 0
            ? "✅ all " + examples.length + " examples ran"
            : "❌ " + failed + " of " + examples.length + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }
}
