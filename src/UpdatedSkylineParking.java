import java.util.Scanner;

public class UpdatedSkylineParking {

  static final String GARAGE_NAME = "Skyline Parking Garage";

  static final int LEVELS = 3;
  static final int SPOTS_PER_LEVEL = 8;
  static final int CAPACITY = LEVELS * SPOTS_PER_LEVEL;

  static final char[] LEVEL_LETTERS = { 'A', 'B', 'C' };

  static final double ENTRY_FEE = 1.00;
  static final double HOURLY_RATE = 2.50;
  static final double DAILY_MAX = 25.00;
  static final double[] LEVEL_SURCHARGE = { 1.00, 0.50, 0.00 };

  static final int MAX_PARK_MINUTES = 20 * 60;

  static final int[] NEAREST_SPOT_ORDER = { 0, 4, 1, 5, 2, 6, 3, 7 };

  static boolean[][] occupied;

  static Car[] parked;
  static int parkedCount = 0;

  static boolean hasBooked = false;

  static class Car {
    String plate;
    int level;
    int spot;
    int minutesParked;

    Car(String plate, int minutesParked) {
      this.plate = normalizePlate(plate);
      this.minutesParked = minutesParked;
      this.level = -1;
      this.spot = -1;
    }

    String location() {
      if (level < 0) {
        return "unassigned";
      }
      return "" + LEVEL_LETTERS[level] + (spot + 1);
    }
  }

  static String normalizePlate(String raw) {
    if (raw == null) {
      return "";
    }
    return raw.trim().toUpperCase();
  }

  static String money(double amount) {
    return "$" + String.format("%.2f", amount);
  }

  static int freeOnLevel(int level) {
    int free = 0;
    for (int sp = 0; sp < occupied[level].length; sp++) {
      if (!occupied[level][sp]) {
        free++;
      }
    }
    return free;
  }

  static int totalFree() {
    int free = 0;
    for (int lvl = 0; lvl < LEVELS; lvl++) {
      for (int sp = 0; sp < SPOTS_PER_LEVEL; sp++) {
        if (!occupied[lvl][sp]) {
          free++;
        }
      }
    }
    return free;
  }

  static boolean garageFull() {
    return totalFree() == 0;
  }

  static int[] findFreeSpotOnLevel(int level) {
    for (int sp : NEAREST_SPOT_ORDER) {
      if (occupied[level][sp]) {
        continue;
      }
      return new int[] { level, sp };
    }
    return null;
  }

  static int[] findNearestFreeSpot() {
    for (int lvl = 0; lvl < LEVELS; lvl++) {
      int[] slot = findFreeSpotOnLevel(lvl);
      if (slot != null) {
        return slot;
      }
    }
    return null;
  }

  static Car park(String plate, int minutesParked, int preferredLevel) {
    if (isParked(plate)) {
      return null;
    }
    int[] slot;
    switch (preferredLevel) {
      case 0:
      case 1:
      case 2:
        slot = findFreeSpotOnLevel(preferredLevel);
        break;
      default:
        slot = findNearestFreeSpot();
        break;
    }
    if (slot == null) {
      return null;
    }
    int lvl = slot[0];
    int sp = slot[1];
    if (lvl < 0 || lvl >= LEVELS || sp < 0 || sp >= SPOTS_PER_LEVEL) {
      return null;
    }
    occupied[lvl][sp] = true;
    Car car = new Car(plate, minutesParked);
    car.level = lvl;
    car.spot = sp;
    parked[parkedCount] = car;
    parkedCount++;
    return car;
  }

  static Car[] activeCars() {
    Car[] result = new Car[parkedCount];
    for (int i = 0; i < parkedCount; i++) {
      result[i] = parked[i];
    }
    return result;
  }

  static Car findByPlate(String plate) {
    String target = normalizePlate(plate);
    for (Car c : activeCars()) {
      if (c.plate.equals(target)) {
        return c;
      }
    }
    return null;
  }

  static boolean isParked(String plate) {
    return findByPlate(plate) != null;
  }

  static double[] feeBreakdown(int level, int minutesParked) {
    int hours = minutesParked / 60;
    int mins = minutesParked % 60;
    int billedHours = hours + (mins > 0 ? 1 : 0);
    if (billedHours < 1) {
      billedHours = 1;
    }
    double entry = ENTRY_FEE;
    double hourly = HOURLY_RATE * billedHours;
    double surcharge = LEVEL_SURCHARGE[level] * billedHours;
    double subtotal = entry + hourly + surcharge;
    double total = subtotal;
    if (total > DAILY_MAX) {
      total = DAILY_MAX;
    }
    return new double[] { billedHours, entry, hourly, surcharge, subtotal, total };
  }

  static final int LABEL_WIDTH = 40;

  static String ticketRow(String label, String value) {
    StringBuilder sb = new StringBuilder();
    sb.append("  ").append(label);
    for (int i = label.length(); i < LABEL_WIDTH; i++) {
      sb.append(' ');
    }
    sb.append(value);
    return sb.toString();
  }

  static void printTicket(Car car) {
    double[] b = feeBreakdown(car.level, car.minutesParked);
    int billedHours = (int) b[0];
    double entry = b[1];
    double hourly = b[2];
    double surcharge = b[3];
    double subtotal = b[4];
    double total = b[5];
    int hours = car.minutesParked / 60;
    int mins = car.minutesParked % 60;
    String hrText = billedHours + (billedHours == 1 ? " hour" : " hours");
    String billedWord = billedHours == 1 ? "hour" : "hours";
    StringBuilder sb = new StringBuilder();
    sb.append("* * * * * * * * * * 🎟️ YOUR TICKET 🎟️ * * * * * * * * * *\n\n");
    sb.append(ticketRow("Number Plate", car.plate)).append("\n");
    sb.append(ticketRow("Spot Assigned", car.location() + " (Level " + LEVEL_LETTERS[car.level] + ")")).append("\n");
    sb.append(ticketRow("Parked", hours + " hours " + mins + " mins. (Billed " + billedHours + " " + billedWord + ")")).append("\n");
    sb.append(ticketRow("Entry Fee", money(entry))).append("\n");
    sb.append(ticketRow("Hourly (" + hrText + " x " + money(HOURLY_RATE) + ")", money(hourly))).append("\n");
    sb.append(ticketRow("Level " + LEVEL_LETTERS[car.level] + " Surcharge (" + hrText + " x " + money(LEVEL_SURCHARGE[car.level]) + ")", money(surcharge))).append("\n");
    sb.append(ticketRow("Subtotal", money(subtotal))).append("\n");
    if (subtotal > DAILY_MAX) {
      sb.append(ticketRow("Daily cap applied", money(DAILY_MAX))).append("\n");
    }
    sb.append(ticketRow("TOTAL", money(total)));
    sb.append("\n\n");
    sb.append("😊 Thank you for parking with us!\n");
    sb.append("\n* * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");

    System.out.println(sb.toString());
  }

  static String spotLabel(int level, int spot) {
    return "" + LEVEL_LETTERS[level] + (spot + 1);
  }

  static void printGarageMap() {
    StringBuilder sb = new StringBuilder();
    for (int lvl = LEVELS - 1; lvl >= 0; lvl--) {
      appendSpotsRow(sb, lvl, SPOTS_PER_LEVEL / 2, SPOTS_PER_LEVEL);
      appendSpotsRow(sb, lvl, 0, SPOTS_PER_LEVEL / 2);
      if (lvl > 0) {
        sb.append("\n");
      }
    }
    System.out.println(sb.toString());
  }

  static void appendSpotsRow(StringBuilder sb, int lvl, int from, int to) {
    for (int sp = from; sp < to; sp++) {
      String label = occupied[lvl][sp] ? "🚗" : spotLabel(lvl, sp);
      sb.append(String.format("%-6s", label));
    }
    sb.append("\n");
  }

  static void openGarage() {
    occupied = new boolean[LEVELS][SPOTS_PER_LEVEL];
    parked = new Car[CAPACITY];
    parkedCount = 0;
    occupied[0][0] = true;
    occupied[0][1] = true;
    occupied[1][2] = true;
  }

  static int parseDurationMinutes(String raw) {
    String cleaned = raw.replace(" ", "");
    if (!cleaned.contains(":")) {
      return -1;
    }
    String[] parts = cleaned.split(":");
    if (parts.length != 2) {
      return -1;
    }
    try {
      int hh = Integer.parseInt(parts[0]);
      int mm = Integer.parseInt(parts[1]);
      if (hh < 0 || mm < 0 || mm > 59) {
        return -1;
      }
      return (hh * 60) + mm;
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  static boolean isValidLevelChoice(String choice) {
    if (choice.equals("D")) {
      return true;
    }
    for (int lvl = 0; lvl < LEVELS; lvl++) {
      if (choice.length() == 1 && choice.charAt(0) == LEVEL_LETTERS[lvl] && freeOnLevel(lvl) > 0) {
        return true;
      }
    }
    return false;
  }

  static int levelChoiceToIndex(String choice) {
    if (choice.equals("D")) {
      return -1;
    }
    for (int lvl = 0; lvl < LEVELS; lvl++) {
      if (choice.charAt(0) == LEVEL_LETTERS[lvl]) {
        return lvl;
      }
    }
    return -1;
  }

  static Scanner scanner = new Scanner(System.in);

  static String ask(String promptText) {
    System.out.print(promptText);
    return scanner.nextLine();
  }

  public static void main(String[] args) {
    System.out.println(ask("Type something: "));
  }
}