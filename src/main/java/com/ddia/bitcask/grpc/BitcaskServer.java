package com.ddia.bitcask.grpc;

import com.ddia.bitcask.Impl.BitcaskFactory;
import com.ddia.bitcask.enums.SyncConfig;
import com.ddia.bitcask.interfaces.Bitcask;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class BitcaskServer {

  private static final int PORT = 50051;
  private Server server;
  private final Bitcask bitcask;

  public BitcaskServer(Bitcask bitcask) {
    this.bitcask = bitcask;
  }

  public void start() throws IOException {
    server =
        ServerBuilder.forPort(PORT).addService(new BitcaskServiceImpl(bitcask)).build().start();
    System.out.println("Bitcask gRPC Server started, listening on port " + PORT);

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  // this is called when jvm is being closed
                  try {
                    BitcaskServer.this.stop();
                  } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                  }
                  System.err.println("Server shut down successfully");
                }));
  }

  public void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }

    try {
      if (bitcask != null) {
        bitcask.close();
        System.out.println("Bitcask engine closed safely.");
      }
    } catch (IOException e) {
      System.err.println("Error closing Bitcask engine: " + e.getMessage());
    }
  }

  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    String dbDirectory = "/home/omar/Documents/compaction_test_db/";
    Bitcask engine = BitcaskFactory.getInstance(dbDirectory, SyncConfig.NONE);

    final BitcaskServer server = new BitcaskServer(engine);
    server.start();
    server.blockUntilShutdown();
  }

  // gRPC Service Implementation

  private static class BitcaskServiceImpl extends BitcaskServiceGrpc.BitcaskServiceImplBase {
    private final Bitcask bitcask;

    BitcaskServiceImpl(Bitcask bitcask) {
      this.bitcask = bitcask;
    }

    @Override
    public void getAllKeys(Empty request, StreamObserver<KeyValueChunk> responseObserver) {

      final int MAX_BATCH_SIZE_IN_BYTES = 8 * 1024 * 1024;
      ServerCallStreamObserver<KeyValueChunk> serverObserver =
          (ServerCallStreamObserver<KeyValueChunk>) responseObserver;
      KeyValueChunk.Builder chunkBuilder = KeyValueChunk.newBuilder();
      int currentBatchSize = 0;

      try {
        BlockingQueue<Map.Entry<String, String>> queue = bitcask.getAll();

        while (true) {
          Map.Entry<String, String> entry = queue.take();

          // 3. Stop if we hit the End Pill
          if (entry == Bitcask.END) {
            break;
          }

          KeyValuePair pair =
              KeyValuePair.newBuilder().setKey(entry.getKey()).setValue(entry.getValue()).build();

          chunkBuilder.addPairs(pair);
          currentBatchSize += entry.getKey().length() + entry.getValue().length();

          if (currentBatchSize >= MAX_BATCH_SIZE_IN_BYTES) {
            while (!serverObserver.isReady()) {
              Thread.sleep(100);
            }
            serverObserver.onNext(chunkBuilder.build());
            chunkBuilder.clear();
            currentBatchSize = 0;
          }
        }

        //  Send the final batch
        if (currentBatchSize > 0) {
          while (!serverObserver.isReady()) {
            Thread.sleep(100);
          }
          responseObserver.onNext(chunkBuilder.build());
        }

        responseObserver.onCompleted();

      } catch (Exception e) {
        responseObserver.onError(
            Status.INTERNAL
                .withDescription("Streaming error: " + e.getMessage())
                .asRuntimeException());
      }
    }

    @Override
    public void getKey(KeyRequest request, StreamObserver<ValueResponse> responseObserver) {
      try {
        String value = bitcask.get(request.getKey());

        ValueResponse response =
            ValueResponse.newBuilder()
                .setValue(value != null ? value : "")
                .setFound(value != null)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
      } catch (IOException e) {
        responseObserver.onError(
            Status.INTERNAL
                .withDescription("Disk read error: " + e.getMessage())
                .asRuntimeException());
      } catch (Exception e) {
        responseObserver.onError(
            Status.INTERNAL
                .withDescription("Unknown error: " + e.getMessage())
                .asRuntimeException());
      }
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
      try {
        bitcask.put(request.getKey(), request.getValue());

        PutResponse response =
            PutResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Successfully saved key: " + request.getKey())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
      } catch (IOException e) {
        responseObserver.onError(
            Status.INTERNAL
                .withDescription("Disk write error: " + e.getMessage())
                .asRuntimeException());
      }
    }

    @Override
    public void merge(Empty request, StreamObserver<MergeResponse> responseObserver) {
      try {
        bitcask.merge();

        MergeResponse response =
            MergeResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Compaction/Merge completed successfully.")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
      } catch (IOException e) {
        responseObserver.onError(
            Status.INTERNAL
                .withDescription("Merge operation failed: " + e.getMessage())
                .asRuntimeException());
      }
    }
  }
}
