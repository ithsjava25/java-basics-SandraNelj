//java -cp C:\Users\sandr\IdeaProjects\java-basics-SandraNelj\target\classes com.example.Main
//java -cp target/classes com.example.Main
package com.example;
import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

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

    // Metod medelpris
    private static double meanPrice(List<Elpris> priser) {
        //returnera medelpris i öre/kWh
        return priser.stream().mapToDouble(p -> p.sekPerKWh() * 100)
                .average()
                .orElse(0.0);
    }

    private static int findEarliestIndexOfMin(List<Elpris> priser) {
        double min = Double.POSITIVE_INFINITY;
        int index = -1;
        for (int i = 0; i < priser.size(); i++) {
            double v = priser.get(i).sekPerKWh();
            if (v < min) {
                min = v;
                index = i;
            }
        }
        return index;
    }

    private static int findEarliestIndexOfMax(List<Elpris> priser) {
        double max = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int i = 0; i < priser.size(); i++) {
            double v = priser.get(i).sekPerKWh();
            if (v > max) {
                max = v;
                index = i;
            }
        }
        return index;
    }

    // Metod hitta billigaste laddningsfönster
    private static void findChargingWindow(List<Elpris> priser, int timmar, int startIndex) {
        if (startIndex >= priser.size()) {
            System.out.println("Inga framtida timmar tillgängliga för laddning.");
            return;
        }
        if (priser.size() - startIndex < timmar) {
            System.out.println("Inte tillräckligt många timmar framåt för ett " + timmar + "h-fönster");
            return;
        }

        double minSum = Double.POSITIVE_INFINITY;
        int bestStart = -1;

        for (int i = startIndex; i <= priser.size() - timmar; i++) {
            double sum = 0.0;
            for (int j = i; j < i + timmar; j++) sum += priser.get(j).sekPerKWh();
            if (sum < minSum) {
                minSum = sum;
                bestStart = i;
            }
        }

        System.out.printf("Billigaste laddningsfönster (%dh):%n", timmar);
        for (int i = bestStart; i < bestStart + timmar; i++) {
            Elpris p = priser.get(i);
            System.out.printf("Tid: %s = %.0f öre/kWh%n",
                    p.timeStart().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
                    p.sekPerKWh() * 100.0);
        }
        System.out.printf("Medelpris för fönstret: %.0f öre/kWh%n", (minSum / timmar) * 100.0);
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
        Prisklass prisklass = null;
        if (!flags.containsKey("--zone")) {
            Scanner scanner = new Scanner(System.in);
            while (prisklass == null) {
                System.out.println("Ange elområde (SE1, SE2, SE3, SE4): ");
                String line = scanner.nextLine().trim().toUpperCase();
                try {
                    prisklass = Prisklass.valueOf(line);
                } catch (Exception e) {
                    System.out.println("Ogiltigt område - försök igen!");
                }
            }
        } else {
            try {
                prisklass = Prisklass.valueOf(flags.get("--zone").toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Ogiltigt område - ange SE1, SE2, SE3, SE4");
                return;
            }
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

        // Hämta priser för dagens datum samt morgondagen
        ElpriserAPI api = new ElpriserAPI();
        List<Elpris> combined = new ArrayList<>();
        List<Elpris> day1 = api.getPriser(date, prisklass);
        if (!day1.isEmpty()) combined.addAll(day1);

        List<Elpris> day2 = api.getPriser(date.plusDays(1), prisklass);
        if (!day2.isEmpty()) {
            System.out.println("Prisdata för " + date.plusDays(1) + " finns och inkluderas.");
            combined.addAll(day2);
        }
        if (combined.isEmpty()) {
            System.out.println("Inga priser hittades för " + date + " i " + prisklass + ".");
            return;
        }

        //Sortering
        List<Elpris> chronological = combined.stream()
                .sorted(Comparator.comparing(Elpris::timeStart))
                .collect(Collectors.toList());

        //Aktuell starttid
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime startThreshold = now.truncatedTo(ChronoUnit.HOURS);
        if (now.getMinute() != 0 || now.getSecond() != 0 || now.getNano() != 0) {
            startThreshold = startThreshold.plusHours(1);
        }
        int startIndex = 0;
        while (startIndex < chronological.size()
                && chronological.get(startIndex).timeStart().toInstant().isBefore(startThreshold.toInstant())) {
            startIndex++;
        }

        int required = 24;
        List<Elpris> window24 = chronological.subList(startIndex, Math.min(startIndex + required, chronological.size()));


        if (window24.isEmpty()) {
            System.out.println("Ingen prisdata efter nuvarande tidpunkt (" + startThreshold + ").");
            return;
        }


        double mean24 = meanPrice(window24);
        // Sortering
        if (flags.containsKey("--sorted")) {
            chronological = chronological.stream()
                    .sorted(Comparator.comparingDouble(Elpris::sekPerKWh).reversed())
                    .collect(Collectors.toList());
        } else {
            chronological = chronological.stream()
                    .sorted(Comparator.comparing(Elpris::timeStart))
                    .collect(Collectors.toList());
        }

        // Hitta billigaste och dyraste timme (tidigaste vid lika pris)
        int minIdx = findEarliestIndexOfMin(window24);
        int maxIdx = findEarliestIndexOfMax(window24);

        //Utskrift

        List<Elpris> displayList;
        if (flags.containsKey("--sorted")) {
            displayList = chronological.stream()
                    .sorted(Comparator.comparingDouble(Elpris::sekPerKWh).reversed())
                    .collect(Collectors.toList());
        } else {
            displayList = chronological; // kronologisk
        }


        System.out.println("Elpriser i " + prisklass + " för " + date + ":");
        displayList.forEach(p -> System.out.printf("Tid: %s = %.0f öre/kWh%n",
                p.timeStart().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), p.sekPerKWh() * 100.0));


        System.out.println();
        System.out.printf("Medelpris (nästa %d timmar): %.0f öre/kWh%n", window24.size(), mean24);


        if (minIdx >= 0) {
            Elpris pmin = window24.get(minIdx);
            System.out.printf("Billigaste timme: %s = %.0f öre/kWh%n",
                    pmin.timeStart().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), pmin.sekPerKWh() * 100.0);
        }
        if (maxIdx >= 0) {
            Elpris pmax = window24.get(maxIdx);
            System.out.printf("Dyraste timme: %s = %.0f öre/kWh%n",
                    pmax.timeStart().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), pmax.sekPerKWh() * 100.0);
        }


        // Charging
        if (flags.containsKey("--charging")) {
            String val = flags.get("--charging").toLowerCase().replace("h", "");
            try {
                int timmar = Integer.parseInt(val);
                if (timmar != 2 && timmar != 4 && timmar != 8) {
                    System.out.println("Ogiltigt värde för --charging. Använd 2h, 4h eller 8h.");
                } else {
                // findChargingWindow listar fönstret och startIndex anger varifrån vi får börja
                    findChargingWindow(chronological, timmar, startIndex);
                }
            } catch (NumberFormatException e) {
                System.out.println("Ogiltigt värde för --charging. Använd 2h, 4h eller 8h.");
            }
        }
    }
}
