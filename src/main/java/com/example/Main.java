//java -cp C:\Users\sandr\IdeaProjects\java-basics-SandraNelj\target\classes com.example.Main
//java -cp target/classes com.example.Main
package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;
import java.time.LocalDate;
import java.util.*;


/**
 * Main-program för att hämta elpriser, skriva ut statistik och beräkna laddningsfönster.
 */
public class Main {

    public static void main(String[] args) {
        Map<String, String> flags = parseArgs(args);

        // Hantera --help
        if (flags.containsKey("--help")) {
            printHelp();
            return;
        }

        // Hantera zon
        if (!flags.containsKey("--zone")) {
            System.out.println("Du måste ange en zon med --zone SE1|SE2|SE3|SE4");
            return;
        }
        Prisklass prisklass;
        try {
            prisklass = Prisklass.valueOf(flags.get("--zone"));
        } catch (IllegalArgumentException e) {
            System.out.println("Ogiltig zon. Tillåtna värden är SE1, SE2, SE3 eller SE4.");
            return;
        }

        // Hantera datum (default = idag)
        LocalDate date;
        try {
            date = flags.containsKey("--date")
                    ? LocalDate.parse(flags.get("--date"))
                    : LocalDate.now();
        } catch (Exception e) {
            System.out.println("Ogiltigt datum, Använd formatet YYYY-MM-DD.");
            return;
        }

        ElpriserAPI api = new ElpriserAPI();

        // Hämta dagens priser
        List<Elpris> priser = new ArrayList<>(api.getPriser(date, prisklass));
            priser.addAll(api.getPriser(date.plusDays(1), prisklass));
        if (priser.isEmpty()) {
            System.out.println("Inga priser tillgängliga.");
            return;
        }

        // Sortera om användaren vill
        if (flags.containsKey("--sorted")) {
            priser.sort(Comparator.comparingDouble(Elpris::sekPerKWh).reversed());
        } else {
            priser.sort(Comparator.comparing(Elpris::timeStart));
        }

        // --- Utskrift av alla priser ---
        if (flags.containsKey("--sorted")) {
            for (Elpris pris : priser) {
                int start = pris.timeStart().getHour();
                int end = pris.timeEnd().getHour();
                System.out.printf("%02d-%02d %.2f öre%n",
                        start, end, pris.sekPerKWh() * 100.0);
            }
        } else {
            Map<Integer, List<Elpris>> perTimme = new TreeMap<>();
            for (Elpris pris : priser) {
                int timme = pris.timeStart().getHour();
                perTimme.computeIfAbsent(timme, _ -> new ArrayList<>()).add(pris);
            }

            for (var entry : perTimme.entrySet()) {
                int timme = entry.getKey();
                List<Elpris> kvart = entry.getValue();

                double snittSek = kvart.stream()
                        .mapToDouble(Elpris::sekPerKWh)
                        .average()
                        .orElse(0.0);

                System.out.printf("%02d-%02d %.2f öre%n",
                        timme,
                        (timme + 1) % 24,
                        snittSek * 100.0);
            }
        }

        // --- Medelpris ---
        double medelpris = priser.stream()
                .mapToDouble(Elpris::sekPerKWh)
                .average()
                .orElse(0.0);

        System.out.printf("Medelpris: %.2f SEK/kWh%n", medelpris);
        System.out.printf("Medelpris: %.2f öre%n", medelpris * 100);

        // --- Billigaste och dyraste timme ---
        Elpris billigast = priser.stream().min(Comparator.comparingDouble(Elpris::sekPerKWh)).orElseThrow();
        Elpris dyrast = priser.stream().max(Comparator.comparingDouble(Elpris::sekPerKWh)).orElseThrow();

        System.out.printf("lägsta pris: %02d:00–%02d:00 → %.2f SEK/kWh%n",
                billigast.timeStart().getHour(),
                billigast.timeEnd().getHour(),
                billigast.sekPerKWh());

        System.out.printf("högsta pris: %02d:00–%02d:00 → %.2f SEK/kWh%n",
                dyrast.timeStart().getHour(),
                dyrast.timeEnd().getHour(),
                dyrast.sekPerKWh());

        // --- Optimal laddning ---
        if (flags.containsKey("--charging")) {
            String dur = flags.get("--charging");
            int timmar = switch (dur) {
                case "2h" -> 2;
                case "4h" -> 4;
                case "8h" -> 8;
                default -> throw new IllegalArgumentException("Endast 2h, 4h, 8h är tillåtna.");
            };

            List<Elpris> window = findBestWindow(priser, timmar);
            double snitt = window.stream().mapToDouble(Elpris::sekPerKWh).average().orElse(0.0);

            double snittOre = snitt * 100.0;

            System.out.printf("Påbörja laddning kl %02d:00%n", window.getFirst().timeStart().getHour());
            System.out.printf("Medelpris för fönster: %.2f öre%n", snittOre);
        }
    }

    // Sliding Window för att hitta bästa laddningsfönstret
    private static List<Elpris> findBestWindow(List<Elpris> priser, int timmar) {
        double minSum = Double.MAX_VALUE;
        int startIndex = 0;
        for (int i = 0; i <= priser.size() - timmar; i++) {
            double sum = 0;
            for (int j = 0; j < timmar; j++) {
                sum += priser.get(i + j).sekPerKWh();
            }
            if (sum < minSum) {
                minSum = sum;
                startIndex = i;
            }
        }
        return priser.subList(startIndex, startIndex + timmar);
    }

    // Enkel parser för argument
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    map.put(arg, args[i + 1]);
                    i++;
                } else {
                    map.put(arg, "true");
                }
            }
        }
        return map;
    }

    // Hjälptext
    private static void printHelp() {
        System.out.println("""
                Användning: java -jar app.jar --zone SE1|SE2|SE3|SE4 [options]
                Options:
                  --date YYYY-MM-DD    Ange datum (default = idag)
                  --sorted             Sortera priser fallande
                  --charging 2h|4h|8h  Hitta billigaste laddningsfönstret
                  --help               Visa denna hjälp
                """);
    }
}

