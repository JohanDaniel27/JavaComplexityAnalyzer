public class OrderService {

    private User user;
    private Database database;

    public void process(User user) {

        database = new Database();
        database.save(user);
    }
}