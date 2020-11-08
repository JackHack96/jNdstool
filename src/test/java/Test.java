import nitro.ROM;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

public class Test {
    public static void main(String[] args){
        try {
            Instant start, finish;
            long timeDifference;

            System.out.println("Extracting " + args[0] + " to " + args[1] + "...");
            start = Instant.now();
            ROM.extractROM(Paths.get(args[0]), Paths.get(args[1]));
            finish = Instant.now();
            timeDifference = Duration.between(start, finish).toMillis();
            System.out.println("Extraction took " + timeDifference + "ms");

            System.out.println("\nRepacking " + args[1] + " in lol.nds");
            start = Instant.now();
            ROM.buildROM(Paths.get(args[1]), Paths.get("/home/matteo/lol.nds"));
            finish = Instant.now();
            timeDifference = Duration.between(start, finish).toMillis();
            System.out.println("Repacking took " + timeDifference + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}