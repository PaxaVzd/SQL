import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    public static void main(String[] args) {
        readTxtFile("user.txt");
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Звязок встановлено!");
            createLotteryTable(connection);
            Scanner scanner = new Scanner(System.in);
            boolean exit = false;
            while (!exit) {
                System.out.println("Меню:");
                System.out.println("1. Показати всі результати лотереї");
                System.out.println("2. Додати новий результат лотереї");
                System.out.println("3. Видалити результат лотереї");
                System.out.println("4. Вийти з програми");
                System.out.print("Виберіть опцію: ");

                try {
                    int choice = scanner.nextInt();
                    scanner.nextLine();
                    switch (choice) {
                        case 1:
                            showAllResults(connection);
                            break;
                        case 2:
                            addNewResult(connection, scanner);
                            break;
                        case 3:
                            deleteResult(connection, scanner);
                            break;
                        case 4:
                            exit = true;
                            break;
                        default:
                            System.out.println("Некоректний вибір опції. Спробуйте ще раз.");
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Введіть число зі списку можливих дій.");
                    scanner.next();
                } catch (SQLException e) {
                    System.err.println("Помилка бази даних:");
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            System.err.println("Не вдалося підключитися:");
            e.printStackTrace();
        }
    }

    private static void createLotteryTable(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS lottery_results (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "draw_date DATE," +
                "winning_numbers VARCHAR(255)," +
                "jackpot_amount DECIMAL(10, 2)" +
                ")";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableSQL);
        }
    }

    private static void showAllResults(Connection connection) throws SQLException {
        String selectQuery = "SELECT * FROM lottery_results";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(selectQuery)) {
            System.out.println("Результати лотереї:");
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                Date drawDate = resultSet.getDate("draw_date");
                String winningNumbers = resultSet.getString("winning_numbers");
                double jackpotAmount = resultSet.getDouble("jackpot_amount");
                System.out.println("ID: " + id + ", Дата розіграшу: " + drawDate +
                        ", Виграшні номери: " + winningNumbers + ", Джекпот: " + jackpotAmount);
            }
        }
    }

    private static void addNewResult(Connection connection, Scanner scanner) throws SQLException {
        try {
            String drawDate = null;
            while (drawDate == null || !drawDate.matches("\\d{1,2}-\\d{1,2}-\\d{4}")) {
                System.out.print("Введіть дату розіграшу (день-місяць-рік): ");
                drawDate = scanner.nextLine();
                if (!drawDate.matches("\\d{1,2}-\\d{1,2}-\\d{4}")) {
                    System.out.println("Неправильний формат дати. Введіть у форматі день-місяць-рік.");
                }
            }

            String winningNumbers = null;
            while (winningNumbers == null || !winningNumbers.matches("\\d+( \\d+)*")) {
                System.out.print("Введіть виграшні номери (цифри і пробіли): ");
                winningNumbers = scanner.nextLine();
                if (!winningNumbers.matches("\\d+( \\d+)*")) {
                    System.out.println("Неправильний формат виграшних номерів. Введіть лише цифри та пробіли.");
                }
            }

            double jackpotAmount = 0;
            boolean validAmount = false;
            while (!validAmount) {
                System.out.print("Введіть суму джекпоту: ");
                if (scanner.hasNextDouble()) {
                    jackpotAmount = scanner.nextDouble();
                    validAmount = true;
                } else {
                    System.out.println("Введіть числове значення.");
                    scanner.next();
                }
            }

            String insertQuery = "INSERT INTO lottery_results (draw_date, winning_numbers, jackpot_amount) VALUES (?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                preparedStatement.setDate(1, Date.valueOf(drawDate));
                preparedStatement.setString(2, winningNumbers);
                preparedStatement.setDouble(3, jackpotAmount);
                int rowsAffected = preparedStatement.executeUpdate();
                System.out.println(rowsAffected + " рядків було додано.");
            }
        } catch (SQLException e) {
            System.err.println("Помилка бази даних:");
            e.printStackTrace();
        }
    }

    private static void deleteResult(Connection connection, Scanner scanner) throws SQLException {
        System.out.print("Введіть ID результату, який потрібно видалити: ");
        int id = scanner.nextInt();
        String deleteQuery = "DELETE FROM lottery_results WHERE id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
            preparedStatement.setInt(1, id);
            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println(rowsAffected + " рядків було видалено.");
        }
    }

    private static void readTxtFile(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    switch (key) {
                        case "DB_URL":
                            DB_URL = value;
                            break;
                        case "DB_USER":
                            DB_USER = value;
                            break;
                        case "DB_PASSWORD":
                            DB_PASSWORD = value;
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
