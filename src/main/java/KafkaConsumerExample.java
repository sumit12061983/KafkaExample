import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerExample {
    private final static String TOPIC = "my-topic";
    private final static String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerExample.class);

    private static Consumer<Long, String> createConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                "KafkaExampleConsumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        // Create the consumer using props.
        final Consumer<Long, String> consumer =
                new KafkaConsumer<>(props);
        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(TOPIC));
        return consumer;
    }

    static void runConsumer() throws InterruptedException, Exception {
        final Consumer<Long, String> consumer = createConsumer();
        final MongoClient mongoClient = new MongoClient();
        final int giveUp = 100;   int noRecordsCount = 0;
        while (true) {
            final ConsumerRecords<Long, String> consumerRecords =
                    consumer.poll(1000);
            if (consumerRecords.count()==0) {
                noRecordsCount++;
                if (noRecordsCount > giveUp) break;
                else continue;
            }
            consumerRecords.forEach(record -> {
                LOGGER.info("Consumer Record:( {}, {}, {}, {})\n",
                        record.key(), record.value(),
                        record.partition(), record.offset());
//                System.out.printf("Consumer Record:(%d, %s, %d, %d)\n",
//                        record.key(), record.value(),
//                        record.partition(), record.offset());
                DBObject message = new BasicDBObject("_id", record.key().longValue())
                        .append("message", record.value())
                        .append("partition", record.partition())
                        .append("offset", record.offset());
                DB database = mongoClient.getDB("music");
                DBCollection collection = database.getCollection("KafkaRecords");
                collection.insert(message);
            });
            consumer.commitAsync();
        }
        mongoClient.close();
        consumer.close();
        LOGGER.info("DONE");
//        System.out.println("DONE");
    }

    public static void main(String... args) throws Exception {
        runConsumer();
    }
}
