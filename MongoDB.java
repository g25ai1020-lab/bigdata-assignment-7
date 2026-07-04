import static com.mongodb.client.model.Filters.eq;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class MongoDB {

    public static final String DATABASE_NAME = "mydb";

    public MongoClient mongoClient;
    public MongoDatabase db;

    public static void main(String[] args) throws Exception {

        MongoDB qmongo = new MongoDB();

        qmongo.connect();

        qmongo.load();

        qmongo.loadNest();

        System.out.println("================================");
        System.out.println("Query 1");
        System.out.println("================================");
        System.out.println(qmongo.query1(1));

        System.out.println("================================");
        System.out.println("Query 2");
        System.out.println("================================");
        System.out.println(qmongo.query2(1));

        System.out.println("================================");
        System.out.println("Query 2 Nested");
        System.out.println("================================");
        System.out.println(qmongo.query2Nest(1));

        System.out.println("================================");
        System.out.println("Query 3");
        System.out.println("================================");
        System.out.println(qmongo.query3());

        System.out.println("================================");
        System.out.println("Query 3 Nested");
        System.out.println("================================");
        System.out.println(qmongo.query3Nest());

        System.out.println("================================");
        System.out.println("Query 4");
        System.out.println("================================");
        System.out.println(toString(qmongo.query4()));

        System.out.println("================================");
        System.out.println("Query 4 Nested");
        System.out.println("================================");
        System.out.println(toString(qmongo.query4Nest()));
    }

    public MongoDatabase connect() {

        try {

            String url =
                "mongodb+srv://g25ai1020_db_user:Px7zy1tinWR4gbHC@bigdata0.nlkhzuh.mongodb.net/?retryWrites=true&w=majority";

            mongoClient = MongoClients.create(url);

            db = mongoClient.getDatabase(DATABASE_NAME);

            System.out.println("Connected Successfully");

        } catch (Exception e) {

            e.printStackTrace();
        }

        return db;
    }

    public void load() throws Exception {

        MongoCollection<Document> customerCol =
                db.getCollection("customer");

        MongoCollection<Document> ordersCol =
                db.getCollection("orders");

        customerCol.drop();
        ordersCol.drop();

        List<Document> customerDocs =
                new ArrayList<>();

        BufferedReader br =
                new BufferedReader(
                        new FileReader("data/customer.tbl"));

        String line;

        while ((line = br.readLine()) != null) {

            String[] c = line.split("\\|");

            if (c.length < 7)
                continue;

            Document doc = new Document();

            doc.append("custkey",
                    Integer.parseInt(c[0]));

            doc.append("name", c[1]);
            doc.append("address", c[2]);

            doc.append("nationkey",
                    Integer.parseInt(c[3]));

            doc.append("phone", c[4]);

            doc.append("acctbal",
                    Double.parseDouble(c[5]));

            doc.append("mktsegment", c[6]);

            customerDocs.add(doc);
        }

        br.close();

        if (!customerDocs.isEmpty()) {

            customerCol.insertMany(customerDocs);
        }

        List<Document> orderDocs = new ArrayList<>();

        br = new BufferedReader(
                new FileReader("data/orders.tbl"));

        while ((line = br.readLine()) != null) {

            String[] o = line.split("\\|");

            if (o.length < 5)
                continue;

            Document doc = new Document();

            doc.append("orderkey",
                    Integer.parseInt(o[0]));

            doc.append("custkey",
                    Integer.parseInt(o[1]));

            doc.append("orderstatus",
                    o[2]);

            doc.append("totalprice",
                    Double.parseDouble(o[3]));

            doc.append("orderdate",
                    o[4]);

            orderDocs.add(doc);
        }

        br.close();

        if (!orderDocs.isEmpty()) {

            ordersCol.insertMany(orderDocs);
        }

        System.out.println(
                "Customer and Orders loaded successfully.");
    }

    public void loadNest() throws Exception {

        MongoCollection<Document> customerCol =
                db.getCollection("customer");

        MongoCollection<Document> ordersCol =
                db.getCollection("orders");

        MongoCollection<Document> custOrdersCol =
                db.getCollection("custorders");

        custOrdersCol.drop();

        List<Document> nestedDocuments =
                new ArrayList<>();

        MongoCursor<Document> customers =
                customerCol.find().iterator();

        while (customers.hasNext()) {

            Document customer =
                    customers.next();

            int custkey =
                    customer.getInteger("custkey");

            List<Document> orders =
                    ordersCol.find(
                            eq("custkey", custkey))
                            .into(new ArrayList<>());

            Document nested =
                    new Document(customer);

            nested.append("orders",
                    orders);

            nestedDocuments.add(nested);
        }

        customers.close();

        if (!nestedDocuments.isEmpty()) {

            custOrdersCol.insertMany(
                    nestedDocuments);
        }

        System.out.println(
                "custorders collection created successfully.");
    }

    public String query1(int custkey) {

        MongoCollection<Document> col =
                db.getCollection("customer");

        Document doc =
                col.find(eq("custkey", custkey))
                        .first();

        if (doc == null) {

            return "Customer Not Found";
        }

        return doc.getString("name");
    }

    public String query2(int orderId) {

        MongoCollection<Document> col =
                db.getCollection("orders");

        Document doc =
                col.find(eq("orderkey", orderId))
                        .first();

        if (doc == null) {

            return "Order Not Found";
        }

        return doc.getString("orderdate");
    }

    public String query2Nest(int orderId) {

        MongoCollection<Document> col =
                db.getCollection("custorders");

        Document doc =
                col.find(eq("orders.orderkey",
                        orderId))
                        .first();

        if (doc == null) {

            return "Order Not Found";
        }

        List<Document> orders =
                (List<Document>) doc.get("orders");

        for (Document order : orders) {

            Integer oid =
                    order.getInteger("orderkey");

            if (oid != null &&
                    oid.intValue() == orderId) {

                return order.getString(
                        "orderdate");
            }
        }

        return "Order Not Found";
    }

    public long query3() {

        MongoCollection<Document> col =
                db.getCollection("orders");

        return col.countDocuments();
    }

    public long query3Nest() {

        MongoCollection<Document> col =
                db.getCollection("custorders");

        long total = 0;

        MongoCursor<Document> cursor =
                col.find().iterator();

        while (cursor.hasNext()) {

            Document doc =
                    cursor.next();

            List<Document> orders =
                    (List<Document>) doc.get("orders");

            if (orders != null) {

                total += orders.size();
            }
        }

        cursor.close();

        return total;
    }

    public MongoCursor<Document> query4() {

        MongoCollection<Document> col =
                db.getCollection("orders");

        List<Bson> pipeline =
                Arrays.asList(

                        new Document("$group",
                                new Document("_id",
                                        "$custkey")
                                        .append(
                                                "totalAmount",
                                                new Document(
                                                        "$sum",
                                                        "$totalprice"))),

                        new Document("$sort",
                                new Document(
                                        "totalAmount",
                                        -1)),

                        new Document("$limit",
                                5));

        AggregateIterable<Document> result =
                col.aggregate(pipeline);

        return result.iterator();
    }

    public MongoCursor<Document> query4Nest() {

        MongoCollection<Document> col =
                db.getCollection("custorders");

        List<Bson> pipeline =
                Arrays.asList(

                        new Document("$unwind",
                                "$orders"),

                        new Document("$group",
                                new Document("_id",
                                        "$custkey")
                                        .append(
                                                "name",
                                                new Document(
                                                        "$first",
                                                        "$name"))
                                        .append(
                                                "totalAmount",
                                                new Document(
                                                        "$sum",
                                                        "$orders.totalprice"))),

                        new Document("$sort",
                                new Document(
                                        "totalAmount",
                                        -1)),

                        new Document("$limit",
                                5));

        AggregateIterable<Document> result =
                col.aggregate(pipeline);

        return result.iterator();
    }

    public MongoDatabase getDb() {

        return db;
    }

    public static String toString(
            MongoCursor<Document> cursor) {

        StringBuilder sb =
                new StringBuilder();

        sb.append("Rows:\n");

        int count = 0;

        while (cursor.hasNext()) {

            Document doc =
                    cursor.next();

            sb.append(doc.toJson());
            sb.append("\n");

            count++;
        }

        cursor.close();

        sb.append("\nNumber of rows: ");
        sb.append(count);

        return sb.toString();
    }
}
