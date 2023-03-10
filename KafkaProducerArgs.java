package kafka;

import input.Input;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;
import java.util.Random;

public class KafkaProducerArgs {

    public static void main(String[] args) throws Exception {


        // args[0]:inputTopic args[1]:inputTopicGroupID args[2]:datasetPath args[3]:workers args[4]:sourcePartition

        // Assign topicName to string variable
        String topicName = args[0];

        // Create instance for properties to access producer configs
        // Else properties with ProducerConfig
        Properties props = new Properties();

        // Assign localhost id
        props.put("bootstrap.servers", "localhost:9092");
        
        // Assign group_id
        props.put("group.id",args[1]);

        // Set acknowledgements for producer requests.
        // The acks config controls the criteria under which requests are considered complete.
        // The "all" setting we have specified will result in blocking on the full commit of the record, the slowest but most durable setting.
        props.put("acks", "all");

        // If the request fails, the producer can automatically retry
        props.put("retries", 0);

        // Kafka uses an asynchronous publish/subscribe model.
        // The producer consists of a pool of buffer space that holds records that haven't yet been transmitted to the server.Turning these records into requests and transmitting them to the cluster.
        // The send() method is asynchronous.
        // When called it adds the record to a buffer of pending record sends and immediately returns.
        // This allows the producer to batch together individual records for efficiency.

        // Specify buffer size in config
        // The producer maintains buffers of unsent records for each partition. These buffers are of a size specified by the batch.size config
        // Controls how many bytes of data to collect before sending messages to the Kafka broker.
        // Set this as high as possible, without exceeding available memory.
        // The default value is 16384.
        props.put("batch.size", 16384);

        // Reduce the number of requests less than 0
        // linger.ms sets the maximum time to buffer data in asynchronous mode
        // By default, the producer does not wait. It sends the buffer any time data is available.
        // E.g: Instead of sending immediately, you can set linger.ms to 5 and send more messages in one batch.
        // This would reduce the number of requests sent, but would add up to 5 milliseconds of latency to records sent, even if the load on the system does not warrant the delay.
        props.put("linger.ms", 0);

        // The buffer.memory controls the total amount of memory available to the producer for buffering.
        props.put("buffer.memory", 33554432);

        // key-serializer -> integer
        props.put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");

        // value-serializer -> string
        // props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "serde.InputSerializer");

        // Producer
        Producer<Integer, Input> producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);

        // Read the file line by line and send to topic topicName
        BufferedReader br = new BufferedReader(new FileReader(args[2]));
        String line = br.readLine();

        
        int total_count=0;
        while (line != null && !line.equals("EOF")) {

            // Skip the schema of dataset
            if( total_count == 0 ){
                total_count++;
                line = br.readLine();
                continue;
            }

            // Strips off all non-ASCII characters
            line = line.replaceAll("[^\\x00-\\x7F]", "");
            // Erases all the ASCII control characters
            line = line.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
            // Skip empty lines
            if(line.equals("")) {
                line = br.readLine();
                continue;
            }

            // Print the line
            if(total_count % 5000 == 0){ System.out.println("Process 5000 tuples"); }

            // ProducerRecord (string topic, k key, v value)
            // Topic => a topic to assign record.
            // Key => key for the record(included on topic).
            // Value => record contents

            // ProducerRecord (string topic, v value)
            // ProducerRecord<Integer, String> record = new ProducerRecord<>(topicName,line);

            int key = new Random().nextInt(Integer.parseInt(args[3]));
            Input input = new Input(line,String.valueOf(key),System.currentTimeMillis());
            
            // ProducerRecord<Integer, Input> record = new ProducerRecord<>(topicName,key,key,input);
            ProducerRecord<Integer, Input> record = new ProducerRecord<>(topicName,new Random().nextInt(Integer.parseInt(args[4])),key,input);
            // ProducerRecord<Integer, Input> record = new ProducerRecord<>(topicName,input);

            ProducerRecord<Integer, String> record = new ProducerRecord<>(topicName,num_partition, line);


            // ProducerRecord(String topic, Integer partition, K key, V value)
            // ProducerRecord<Integer, String> record = new ProducerRecord<>(topicName,new Random().nextInt(4), new Random().nextInt(4)+1,line);

            // Send the record and get the metadata
            producer.send(record).get();

            // Read the next line
            line = br.readLine();

            total_count++;
        }

        // Write EOF to all partitions
        /*for(int i=0;i<Integer.parseInt(args[4]);i++){

            // ProducerRecord(String topic, Integer partition, K key, V value)
            Input input = new Input("EOF",String.valueOf(i),System.currentTimeMillis());
            ProducerRecord<Integer, Input> record = new ProducerRecord<>(topicName,i,i,input);
            // ProducerRecord<Integer, Input> record = new ProducerRecord<>(topicName,input);

            // Send the record and get the metadata
            RecordMetadata metadata = producer.send(record).get();
            System.out.printf("Record(key=%d value=%s) " + "Meta(partition=%d, offset=%d)\n", record.key(), record.value(), metadata.partition(), metadata.offset());
        }*/

        // String as input => Write EOF to all partitions
        /* for(int i=0;i<Integer.parseInt(args[4]);i++){

            ProducerRecord<Integer, String> record = new ProducerRecord<>(topicName,i,i,"EOF");

            // Send the record and get the metadata
            RecordMetadata metadata = producer.send(record).get();
            System.out.printf("Record(key=%d value=%s) " + "Meta(partition=%d, offset=%d)\n", record.key(), record.value(), metadata.partition(), metadata.offset());
        }*/

        // Write EOF to all partitions for all workers
        for(int i=0;i<Integer.parseInt(args[4]);i++){

            for(int j=0;j<Integer.parseInt(args[3]);j++){

                // ProducerRecord(String topic, Integer partition, K key, V value)
                Input input = new Input("EOF",String.valueOf(j),System.currentTimeMillis());
                ProducerRecord<Integer, Input> record = new ProducerRecord<>(topicName,i,j,input);

                // Send the record and get the metadata
                RecordMetadata metadata = producer.send(record).get();
                System.out.printf("Record(key=%d value=%s) " + "Meta(partition=%d, offset=%d)\n", record.key(), record.value(), metadata.partition(), metadata.offset());
            }
        }

        System.out.println("Total count : "+total_count);

        // Close BufferedReader and Producer
        br.close();
        producer.close();
    }
}
