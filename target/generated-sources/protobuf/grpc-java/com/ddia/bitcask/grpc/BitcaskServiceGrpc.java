package com.ddia.bitcask.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.62.2)",
    comments = "Source: bitcask.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class BitcaskServiceGrpc {

  private BitcaskServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "BitcaskService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.Empty,
      com.ddia.bitcask.grpc.KeyValueChunk> getGetAllKeysMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetAllKeys",
      requestType = com.ddia.bitcask.grpc.Empty.class,
      responseType = com.ddia.bitcask.grpc.KeyValueChunk.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.Empty,
      com.ddia.bitcask.grpc.KeyValueChunk> getGetAllKeysMethod() {
    io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.Empty, com.ddia.bitcask.grpc.KeyValueChunk> getGetAllKeysMethod;
    if ((getGetAllKeysMethod = BitcaskServiceGrpc.getGetAllKeysMethod) == null) {
      synchronized (BitcaskServiceGrpc.class) {
        if ((getGetAllKeysMethod = BitcaskServiceGrpc.getGetAllKeysMethod) == null) {
          BitcaskServiceGrpc.getGetAllKeysMethod = getGetAllKeysMethod =
              io.grpc.MethodDescriptor.<com.ddia.bitcask.grpc.Empty, com.ddia.bitcask.grpc.KeyValueChunk>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetAllKeys"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ddia.bitcask.grpc.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ddia.bitcask.grpc.KeyValueChunk.getDefaultInstance()))
              .setSchemaDescriptor(new BitcaskServiceMethodDescriptorSupplier("GetAllKeys"))
              .build();
        }
      }
    }
    return getGetAllKeysMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.KeyRequest,
      com.ddia.bitcask.grpc.ValueResponse> getGetKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetKey",
      requestType = com.ddia.bitcask.grpc.KeyRequest.class,
      responseType = com.ddia.bitcask.grpc.ValueResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.KeyRequest,
      com.ddia.bitcask.grpc.ValueResponse> getGetKeyMethod() {
    io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.KeyRequest, com.ddia.bitcask.grpc.ValueResponse> getGetKeyMethod;
    if ((getGetKeyMethod = BitcaskServiceGrpc.getGetKeyMethod) == null) {
      synchronized (BitcaskServiceGrpc.class) {
        if ((getGetKeyMethod = BitcaskServiceGrpc.getGetKeyMethod) == null) {
          BitcaskServiceGrpc.getGetKeyMethod = getGetKeyMethod =
              io.grpc.MethodDescriptor.<com.ddia.bitcask.grpc.KeyRequest, com.ddia.bitcask.grpc.ValueResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ddia.bitcask.grpc.KeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ddia.bitcask.grpc.ValueResponse.getDefaultInstance()))
              .setSchemaDescriptor(new BitcaskServiceMethodDescriptorSupplier("GetKey"))
              .build();
        }
      }
    }
    return getGetKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.PutRequest,
      com.ddia.bitcask.grpc.PutResponse> getPutMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Put",
      requestType = com.ddia.bitcask.grpc.PutRequest.class,
      responseType = com.ddia.bitcask.grpc.PutResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.PutRequest,
      com.ddia.bitcask.grpc.PutResponse> getPutMethod() {
    io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.PutRequest, com.ddia.bitcask.grpc.PutResponse> getPutMethod;
    if ((getPutMethod = BitcaskServiceGrpc.getPutMethod) == null) {
      synchronized (BitcaskServiceGrpc.class) {
        if ((getPutMethod = BitcaskServiceGrpc.getPutMethod) == null) {
          BitcaskServiceGrpc.getPutMethod = getPutMethod =
              io.grpc.MethodDescriptor.<com.ddia.bitcask.grpc.PutRequest, com.ddia.bitcask.grpc.PutResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Put"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ddia.bitcask.grpc.PutRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ddia.bitcask.grpc.PutResponse.getDefaultInstance()))
              .setSchemaDescriptor(new BitcaskServiceMethodDescriptorSupplier("Put"))
              .build();
        }
      }
    }
    return getPutMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.Empty,
      com.ddia.bitcask.grpc.MergeResponse> getMergeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Merge",
      requestType = com.ddia.bitcask.grpc.Empty.class,
      responseType = com.ddia.bitcask.grpc.MergeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.Empty,
      com.ddia.bitcask.grpc.MergeResponse> getMergeMethod() {
    io.grpc.MethodDescriptor<com.ddia.bitcask.grpc.Empty, com.ddia.bitcask.grpc.MergeResponse> getMergeMethod;
    if ((getMergeMethod = BitcaskServiceGrpc.getMergeMethod) == null) {
      synchronized (BitcaskServiceGrpc.class) {
        if ((getMergeMethod = BitcaskServiceGrpc.getMergeMethod) == null) {
          BitcaskServiceGrpc.getMergeMethod = getMergeMethod =
              io.grpc.MethodDescriptor.<com.ddia.bitcask.grpc.Empty, com.ddia.bitcask.grpc.MergeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Merge"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ddia.bitcask.grpc.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ddia.bitcask.grpc.MergeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new BitcaskServiceMethodDescriptorSupplier("Merge"))
              .build();
        }
      }
    }
    return getMergeMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static BitcaskServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BitcaskServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BitcaskServiceStub>() {
        @java.lang.Override
        public BitcaskServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BitcaskServiceStub(channel, callOptions);
        }
      };
    return BitcaskServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static BitcaskServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BitcaskServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BitcaskServiceBlockingStub>() {
        @java.lang.Override
        public BitcaskServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BitcaskServiceBlockingStub(channel, callOptions);
        }
      };
    return BitcaskServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static BitcaskServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<BitcaskServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<BitcaskServiceFutureStub>() {
        @java.lang.Override
        public BitcaskServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new BitcaskServiceFutureStub(channel, callOptions);
        }
      };
    return BitcaskServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void getAllKeys(com.ddia.bitcask.grpc.Empty request,
        io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.KeyValueChunk> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetAllKeysMethod(), responseObserver);
    }

    /**
     */
    default void getKey(com.ddia.bitcask.grpc.KeyRequest request,
        io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.ValueResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetKeyMethod(), responseObserver);
    }

    /**
     */
    default void put(com.ddia.bitcask.grpc.PutRequest request,
        io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.PutResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPutMethod(), responseObserver);
    }

    /**
     */
    default void merge(com.ddia.bitcask.grpc.Empty request,
        io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.MergeResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMergeMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service BitcaskService.
   */
  public static abstract class BitcaskServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return BitcaskServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service BitcaskService.
   */
  public static final class BitcaskServiceStub
      extends io.grpc.stub.AbstractAsyncStub<BitcaskServiceStub> {
    private BitcaskServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BitcaskServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BitcaskServiceStub(channel, callOptions);
    }

    /**
     */
    public void getAllKeys(com.ddia.bitcask.grpc.Empty request,
        io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.KeyValueChunk> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetAllKeysMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getKey(com.ddia.bitcask.grpc.KeyRequest request,
        io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.ValueResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void put(com.ddia.bitcask.grpc.PutRequest request,
        io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.PutResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPutMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void merge(com.ddia.bitcask.grpc.Empty request,
        io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.MergeResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMergeMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service BitcaskService.
   */
  public static final class BitcaskServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<BitcaskServiceBlockingStub> {
    private BitcaskServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BitcaskServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BitcaskServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public java.util.Iterator<com.ddia.bitcask.grpc.KeyValueChunk> getAllKeys(
        com.ddia.bitcask.grpc.Empty request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetAllKeysMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.ddia.bitcask.grpc.ValueResponse getKey(com.ddia.bitcask.grpc.KeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.ddia.bitcask.grpc.PutResponse put(com.ddia.bitcask.grpc.PutRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPutMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.ddia.bitcask.grpc.MergeResponse merge(com.ddia.bitcask.grpc.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMergeMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service BitcaskService.
   */
  public static final class BitcaskServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<BitcaskServiceFutureStub> {
    private BitcaskServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BitcaskServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new BitcaskServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ddia.bitcask.grpc.ValueResponse> getKey(
        com.ddia.bitcask.grpc.KeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ddia.bitcask.grpc.PutResponse> put(
        com.ddia.bitcask.grpc.PutRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPutMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ddia.bitcask.grpc.MergeResponse> merge(
        com.ddia.bitcask.grpc.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMergeMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_ALL_KEYS = 0;
  private static final int METHODID_GET_KEY = 1;
  private static final int METHODID_PUT = 2;
  private static final int METHODID_MERGE = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_ALL_KEYS:
          serviceImpl.getAllKeys((com.ddia.bitcask.grpc.Empty) request,
              (io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.KeyValueChunk>) responseObserver);
          break;
        case METHODID_GET_KEY:
          serviceImpl.getKey((com.ddia.bitcask.grpc.KeyRequest) request,
              (io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.ValueResponse>) responseObserver);
          break;
        case METHODID_PUT:
          serviceImpl.put((com.ddia.bitcask.grpc.PutRequest) request,
              (io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.PutResponse>) responseObserver);
          break;
        case METHODID_MERGE:
          serviceImpl.merge((com.ddia.bitcask.grpc.Empty) request,
              (io.grpc.stub.StreamObserver<com.ddia.bitcask.grpc.MergeResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetAllKeysMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.ddia.bitcask.grpc.Empty,
              com.ddia.bitcask.grpc.KeyValueChunk>(
                service, METHODID_GET_ALL_KEYS)))
        .addMethod(
          getGetKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ddia.bitcask.grpc.KeyRequest,
              com.ddia.bitcask.grpc.ValueResponse>(
                service, METHODID_GET_KEY)))
        .addMethod(
          getPutMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ddia.bitcask.grpc.PutRequest,
              com.ddia.bitcask.grpc.PutResponse>(
                service, METHODID_PUT)))
        .addMethod(
          getMergeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ddia.bitcask.grpc.Empty,
              com.ddia.bitcask.grpc.MergeResponse>(
                service, METHODID_MERGE)))
        .build();
  }

  private static abstract class BitcaskServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    BitcaskServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ddia.bitcask.grpc.BitcaskProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("BitcaskService");
    }
  }

  private static final class BitcaskServiceFileDescriptorSupplier
      extends BitcaskServiceBaseDescriptorSupplier {
    BitcaskServiceFileDescriptorSupplier() {}
  }

  private static final class BitcaskServiceMethodDescriptorSupplier
      extends BitcaskServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    BitcaskServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (BitcaskServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new BitcaskServiceFileDescriptorSupplier())
              .addMethod(getGetAllKeysMethod())
              .addMethod(getGetKeyMethod())
              .addMethod(getPutMethod())
              .addMethod(getMergeMethod())
              .build();
        }
      }
    }
    return result;
  }
}
