package konquest.database;

public class AsyncUpdateSQL implements Runnable {
    private DatabaseConnection connection;
    private String query;

    public AsyncUpdateSQL(DatabaseConnection connection, String query) {
        this.connection = connection;
        this.query = query;
    }

    public void run() {
        connection.executeUpdate(query);
    }
}
