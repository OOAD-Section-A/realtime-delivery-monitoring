public class CenterDivSmokeTest {
    public static void main(String[] args) throws Exception {
        Class<?> facadeClass = Class.forName("facade.TransportFacade");
        Object facade = facadeClass.getDeclaredConstructor().newInstance();
        System.out.println("OK: Loaded " + facadeClass.getName() + " via " + facade.getClass().getClassLoader());

        String[] expectedMethods = {
                "getTransportService",
                "getAllCarriers",
                "optimizeRoute",
                "getAllTerritories",
                "reportVehicleHealth",
                "notifyRiderAvailable"
        };

        for (String name : expectedMethods) {
            boolean found = false;
            for (var method : facadeClass.getMethods()) {
                if (method.getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            System.out.println((found ? "OK" : "MISSING") + ": method name present -> " + name);
        }
    }
}
