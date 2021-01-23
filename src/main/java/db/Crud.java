package db;

public class Crud {
    public static void main(String[] args) {
        SqlDb.connect();
        SqlDb.crudDb();
        SqlDb.close();
    }
}
