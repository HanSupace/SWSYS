import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** JDBC alternative when psql is unavailable. Credentials come only from .env. */
public class RunSeedSql {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: RunSeedSql.java path/to/script.sql");
        Properties env = new Properties();
        try (var r = Files.newBufferedReader(Path.of(".env"), StandardCharsets.UTF_8)) { env.load(r); }
        Properties p = new Properties();
        p.setProperty("user", env.getProperty("DB_USER"));
        p.setProperty("password", env.getProperty("DB_PASSWORD"));
        p.setProperty("sslmode", "require"); p.setProperty("connectTimeout", "15");
        p.setProperty("socketTimeout", "60");
        String url = "jdbc:postgresql://" + env.getProperty("DB_HOST") + ":" + env.getProperty("DB_PORT", "5432") + "/" + env.getProperty("DB_NAME", "postgres");
        try (var db = DriverManager.getConnection(url, p); var s = db.createStatement()) {
            boolean result = s.execute(Files.readString(Path.of(args[0]), StandardCharsets.UTF_8));
            while (true) {
                if (result) try (var rs = s.getResultSet()) {
                    var md = rs.getMetaData();
                    while (rs.next()) {
                        var row = new ArrayList<String>();
                        for (int i=1; i<=md.getColumnCount(); i++) row.add(md.getColumnLabel(i)+"="+rs.getString(i));
                        System.out.println(String.join(" | ", row));
                    }
                }
                else if (s.getUpdateCount() == -1) break;
                result = s.getMoreResults();
            }
        } catch (SQLException e) {
            System.err.println("SQLState="+e.getSQLState()+" message="+e.getMessage());
            System.exit(1);
        }
    }
}
