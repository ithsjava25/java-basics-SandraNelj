package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    private static void printHelp() {
        System.out.println("""
            Usage: java Main --zone SE1|SE2|SE3|SE4 [options]
            
            Options:
              --zone <SE1|SE2|SE3|SE4>   (required) Elområde
              --date YYYY-MM-DD          (optional) Datum, standard är dagens datum
              --sorted                   (optional) Sorterar priser fallande (dyrast först)
              --charging 2h|4h|8h        (optional) Hittar billigaste laddningsfönster
              --help                     (optional) Visar denna hjälptext
            """);
    }

    // Medelpris
    private static double meanPrice(List<Elpris> priser) {
        return priser.stream().mapToDouble(p -> p.sekPerKWh() * 100).average().orElse(0.0);
    }

    // Hitta billigaste laddningsfönster
    private static void findChargingWindow(List<Elpris> priser, int timmar) {
        if (priser.size() < timmar) {
            System.out.println("Inte tillräckligt många timmar med data.");
            return;
        }

        double minSum = Double.MAX_VALUE;
        int startIndex = 0;

        for (int i = 0; i <= priser.size() - timmar; i++) {
            double sum = 0;
            for (int j = i; j < i + timmar; j++) {
                sum += priser.get(j).sekPerKWh();
            }
            if (sum < minSum) {
                minSum = sum;
                startIndex = i;
            }
        }

        System.out.printf("Billigaste laddningsfönster (%dh):%n", timmar);
        for (int i = startIndex; i < startIndex + timmar; i++) {
            Elpris p = priser.get(i);
            System.out.printf("Tid: %s = %.0f öre/kWh%n",
                    p.timeStart().toLocalTime(), p.sekPerKWh() * 100);
        }
        System.out.printf("Medelpris: %.0f öre/kWh%n", minSum / timmar);
    }

    public static void main(String[] args) {
        if (args.length == 0 || Arrays.asList(args).contains("--help")) {
            printHelp();
            return;
        }

        Map<String, String> flags = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    flags.put(args[i], args[i + 1]);
                } else {
                    flags.put(args[i], "true");
                }
            }
        }

        // Kontrollera zone
        if (!flags.containsKey("--zone")) {
            System.out.println("Fel: --zone är obligatoriskt.");
            printHelp();
            return;
        }

        Prisklass prisklass;
        try {
            prisklass = Prisklass.valueOf(flags.get("--zone").toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Ogiltigt elområde. Använd SE1, SE2, SE3 eller SE4.");
            return;
        }

        // Datum (default = idag)
        LocalDate date = LocalDate.now();
        if (flags.containsKey("--date")) {
            try {
                date = LocalDate.parse(flags.get("--date"));
            } catch (Exception e) {
                System.out.println("Ogiltigt datumformat. Använd YYYY-MM-DD.");
                return;
            }
        }

        // Hämta priser
        ElpriserAPI api = new ElpriserAPI();
        List<Elpris> priser = api.getPriser(date, prisklass);

        if (priser.isEmpty()) {
            System.out.println("Inga priser hittades för " + date + " i " + prisklass + ".");
            return;
        }

        // Sortering
        if (flags.containsKey("--sorted")) {
            priser = priser.stream()
                    .sorted(Comparator.comparingDouble(Elpris::sekPerKWh).reversed())
                    .collect(Collectors.toList());
        } else {
            priser = priser.stream()
                    .sorted(Comparator.comparing(Elpris::timeStart))
                    .collect(Collectors.toList());
        }

        // Utskrift
        System.out.println("Elpriser i " + prisklass + " för " + date + ":");
        priser.forEach(p -> System.out.printf("Tid: %s = %.0f öre/kWh%n",
                p.timeStart().toLocalTime(), p.sekPerKWh() * 100));

        // Statistik
        double mean = meanPrice(priser);
        double min = priser.stream().mapToDouble(p -> p.sekPerKWh() * 100).min().orElse(0.0);
        double max = priser.stream().mapToDouble(p -> p.sekPerKWh() * 100).max().orElse(0.0);

        System.out.printf("%nMedelpris: %.0f öre/kWh%n", mean);
        System.out.printf("Lägsta timpris: %.0f öre/kWh%n", min);
        System.out.printf("Högsta timpris: %.0f öre/kWh%n", max);

        // Charging
        if (flags.containsKey("--charging")) {
            String val = flags.get("--charging").toLowerCase().replace("h", "");
            try {
                int timmar = Integer.parseInt(val);
                findChargingWindow(priser, timmar);
            } catch (NumberFormatException e) {
                System.out.println("Ogiltigt värde för --charging. Använd 2h, 4h eller 8h.");
            }
        }
    }
}
