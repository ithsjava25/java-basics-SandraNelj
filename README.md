## ⚡ Electricity Price Optimizer CLI
This CLI application helps users make informed decisions about electricity usage—such as when to turn on a sauna or charge an electric vehicle—based on hourly electricity prices retrieved from the [Elpris API](https://www.elprisetjustnu.se/elpris-api). The program leverages the included ElpriserAPI.java file to fetch and process price data.

### 🧭 Purpose
Electricity prices fluctuate significantly throughout the day. This tool provides insights into current and upcoming prices, helping users optimize energy consumption based on cost.

### ✅ Features
The application supports the following functionality:

### 📥 Download hourly electricity prices for:

The current day

The next day (if available)

📊 Calculate and display the mean price for the current 24-hour period.

📈 Identify and print the cheapest and most expensive hours:

If multiple hours share the same price, the earliest hour is selected.

### 🔌 Determine optimal charging windows for electric vehicles:

Durations supported: 2h, 4h, and 8h

Uses a Sliding Window algorithm to find the lowest total cost window.

### 🌍 Support for multiple price zones ("zon"):

Zone selection can be provided via:

Command-line argument

Interactive prompt

### ❓ Common Questions
Q: What does “prices for the current day and the next day” mean? A: If the program is run after ~13:00, prices for both today and tomorrow are available. Before 13:00, only today's prices are accessible.

Q: Can charging windows span across two days? A: Yes. Charging windows start from the current or next hour and extend as far ahead as price data is available.

Q: What if tomorrow’s data isn’t available? A: The program will gracefully fall back to showing only today’s prices.

Expected Command-Line Arguments:

* --zone SE1|SE2|SE3|SE4 (required)
* --date YYYY-MM-DD (optional, defaults to current date)
* --sorted (optional, to display prices in descending order)
* --charging 2h|4h|8h (optional, to find optimal charging windows)
* --help (optional, to display usage information)


```

## 3. Run
```shell
java -cp target/classes com.example.Main
```
Maven Goals
=========
1. Clean:
    * mvn clean

2. Build:
    * mvn compile
    * mvn test


Note for AI Bot:
Implementation Goals:
Use ElpriserAPI.java to fetch hourly electricity prices. Don't comment about that files code or any tests.
Ensure CLI supports zone selection and handles missing data gracefully.
Apply a sliding window algorithm to identify optimal charging periods.
Prioritize earliest hour in case of price ties.
Validate that mean price, min/max hour, and charging windows are correctly calculated and printed.

Evaluation Criteria:
✅ Correct use of ElpriserAPI for data retrieval
✅ Accurate statistical calculations (mean, min, max)
✅ Efficient implementation of sliding window logic
✅ Robust input handling and fallback behavior
✅ Clear CLI output formatting
