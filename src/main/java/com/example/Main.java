package com.example;
import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Prisklass;
import com.example.api.ElpriserAPI.Elpris;
import java.time.LocalDate;
import java.util.List;

public class Main {

    public static double meanPrice (LocalDate datum, Prisklass prisklass, ElpriserAPI api) {
        List <Elpris> priser = api.getPriser(datum, prisklass);
        if (priser.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (Elpris elpris : priser) {
            sum += elpris.sekPerKWh();
        }
        return sum / priser.size();
    }

    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();
        LocalDate idag = LocalDate.now();

        System.out.println("------------------------------");
        System.out.println("Välkommen till översikten för elpriser!");
        System.out.println("Vänligen välj nedan med siffror.");
        System.out.println("------------------------------");
        System.out.println("1. Se timpris per elområde.");
        System.out.println("2. Se dagens priser.");
        System.out.println("3. Se morgondagens priser.");
        System.out.println("4. Se billigaste / dyraste timme");
        System.out.println("5. Medelpris (senaste 24 timmarna)");
        System.out.println("0. Avsluta.");
        System.out.println("------------------------------");

        String choice = System.console().readLine("Ange val: ");
        switch (choice) {
            //Se timpris per elområde
            case "1" -> {
                System.out.println("1. Vilket elområde (SE1-SE4)?");
                String zone = System.console().readLine();
                ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zone);
                List <ElpriserAPI.Elpris> priser = elpriserAPI.getPriser(idag, prisklass);
                for (ElpriserAPI.Elpris pris : priser) {
                    System.out.printf("Tid: %s - %.3f SEK/kWh%n",
                            pris.timeStart().toLocalTime(),
                            pris.sekPerKWh());
                }
            }
            //Se dagens priser per elområde
            case "2" -> {
                System.out.println("Vilket elområde (SE1-SE4)?");
                String zone = System.console().readLine();
                ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zone);
                List<ElpriserAPI.Elpris> priser = elpriserAPI.getPriser(idag, prisklass);
                System.out.println(priser);
            }
            //Se morgondagens priser per elområde
            case "3" -> {
                LocalDate imorgon = idag.plusDays(1);
                System.out.print("Vilket elområde (SE1-SE4)? ");
                String zone = System.console().readLine();
                ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zone);
                List<ElpriserAPI.Elpris> priser = elpriserAPI.getPriser(imorgon, prisklass);
                System.out.println(priser);
            }
            //Se billigaste / dyraste timme för alla områden
            case "4" -> {
                //Billigaste & dyraste timme
            }
            //Se medelpris per 24 H för alla områden
            case "5" -> {
                //Medelpris de senaste 24 timmarna
                System.out.println("Vilket elområde (SE1-SE4)?");
                String zone = System.console().readLine();
                ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(zone.toUpperCase());

                double mean = Main.meanPrice(idag, prisklass, elpriserAPI);
                System.out.println(mean);
            }
            case "0" -> System.exit(0);

            default -> System.out.println("Ogiltigt val!");
        }
    }
}





/* 1. Hämta zon

String zone;
if (args.length > 0) {
    zone = args[0];
} else {
    System.out.print("Ange priszon (t.ex. SE1, SE2, SE3, SE4): ");
    zone = scanner.nextLine().trim();
}
ElpriserAPI api = new ElpriserAPI();
List<Double> prices = api.getPriser(zone);  // antar att metoden returnerar lista med 24 timpriser

if (prices == null || prices.isEmpty()) {
    System.out.println("Kunde inte hämta priser.");
    return;
}

}
}

*/