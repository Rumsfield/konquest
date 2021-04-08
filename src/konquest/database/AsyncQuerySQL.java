package konquest.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

public class AsyncQuerySQL implements Callable<ResultSet> {
    private DatabaseConnection connection;
    private String query;

    AsyncQuerySQL(DatabaseConnection connection, String query) {
        this.connection = connection;
        this.query = query;
    }

    public ResultSet call() {
        try {
            PreparedStatement statement = connection.prepare(query);
            ResultSet result = statement.executeQuery();

            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
