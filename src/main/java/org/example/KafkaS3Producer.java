package org.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Properties;

/**
 * Hello world!
 *
 */
public class KafkaS3Producer
{
    private static final String BUCKET_NAME = "news-article-storage-mjnewsgen";
    private static final String TOPIC = "news_topic";
    private static final String KAFKA_BROKER = "localhost:9093";

    public static void main(String[] args) throws Exception {
        // Kafka Producer Config
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // AWS S3 Client
        S3Client s3 = S3Client.builder()
                .region(Region.of("ap-south-1"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        ListObjectsV2Response listResponse = s3.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(BUCKET_NAME)
                .prefix("articles/")
                .build());

        for (S3Object s3Object : listResponse.contents()) {
            String key = s3Object.key();

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .build();

            BufferedReader reader = new BufferedReader(new InputStreamReader(s3.getObject(getRequest)));
            StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }

            String articleHtml = contentBuilder.toString();
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, articleHtml);
            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.println("✅ Sent to Kafka: " + key);
                } else {
                    exception.printStackTrace();
                }
            });

            // Optional: sleep between sends
            Thread.sleep(500);
        }

        producer.flush();
        producer.close();
        System.out.println("✅ All S3 articles sent to Kafka.");
    }
}
