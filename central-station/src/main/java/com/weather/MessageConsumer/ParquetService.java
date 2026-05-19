package com.weather.MessageConsumer;
import com.weather.MessageConsumer.interfaces.IParquet;
import com.weather.model.WeatherMessage;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.hadoop.fs.Path;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.apache.hadoop.conf.Configuration;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParquetService implements IParquet {

    private static final Logger log = LoggerFactory.getLogger(ParquetService.class);
    private static final int BATCH_SIZE = 10_000;
    private static final long FLUSH_INTERVAL = 5; // I chose this interval to balance between the latency and demo in the discussion
    private static final Configuration HADOOP_CONF = new Configuration();
    private static final String baseDir = "/data/parquet" + Instant.now().getEpochSecond();
    private final Schema schema;
    private List<WeatherMessage> buffer = new ArrayList<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();


    public ParquetService() throws IOException {
        this.schema  = new Schema.Parser().parse(
                getClass().getResourceAsStream("/weather.avsc"));


        scheduler.scheduleAtFixedRate(this::flushIfNotEmpty,
                FLUSH_INTERVAL, FLUSH_INTERVAL, TimeUnit.MINUTES);

        // I added this to handle the edge case of the service shutdown with number of message less than the configured batch size
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            flushIfNotEmpty();
        }));
    }

    @Override
    public synchronized void process(WeatherMessage msg)  {
        buffer.add(msg);
        if (buffer.size() >= BATCH_SIZE) flush();
    }

    private synchronized void flushIfNotEmpty() {
        if (!buffer.isEmpty()) flush();
    }

    private void flush() {
        List<WeatherMessage> snapshot = swapBuffer();
        if (snapshot.isEmpty()) return;

        writeBatch(snapshot);
    }

    private synchronized List<WeatherMessage> swapBuffer() {
        List<WeatherMessage> snap = buffer;
        buffer = new ArrayList<>();
        return snap;
    }

    private void writeBatch(List<WeatherMessage> records) {
        // Partition path based on current date
        LocalDate today = LocalDate.now();
        String partitionPath = String.format("%s/year=%d/month=%02d/day=%02d",
                baseDir,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth());

        // create directories if they don't exist
        File dir = new File(partitionPath);
        if (!dir.mkdirs() && !dir.exists()) {
            log.error("Failed to create directory: {}", partitionPath);
            return;
        }

        // unique filename using current timestamp
        String filePath = partitionPath + "/data_" + Instant.now().getEpochSecond() + ".parquet";

        Path hadoopPath = new Path(filePath);

        try (ParquetWriter<GenericRecord> writer =
                     AvroParquetWriter.<GenericRecord>builder(HadoopOutputFile.fromPath(hadoopPath, HADOOP_CONF))
                             .withSchema(schema)
                             .withCompressionCodec(CompressionCodecName.SNAPPY).withRowGroupSize((long) ParquetWriter.DEFAULT_BLOCK_SIZE)
                             .withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
                             .build()) {

            for (WeatherMessage msg : records) {
                GenericRecord record = getGenericRecord(msg);
                writer.write(record);
            }

        } catch (IOException e) {
            log.error("Failed to write parquet batch of {} records to {}: {}",
                    records.size(), filePath, e.getMessage());
        }

    }

    private @NonNull GenericRecord getGenericRecord(WeatherMessage msg) {
        GenericRecord record = new GenericData.Record(schema);
        record.put("station_id",       msg.station_id);
        record.put("s_no",             msg.s_no);
        record.put("battery_status",   msg.battery_status);
        record.put("status_timestamp", msg.status_timestamp);
        record.put("humidity",         msg.weather.humidity);
        record.put("temperature",      msg.weather.temperature);
        record.put("wind_speed",       msg.weather.wind_speed);
        return record;
    }


}
