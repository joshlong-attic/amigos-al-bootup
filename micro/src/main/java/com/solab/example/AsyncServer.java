package com.solab.example;

import com.solab.example.protos.MaxProto;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * @author Enrique Zamudio
 * Date: 2019-03-23 18:02
 */
public class AsyncServer {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    @Getter
    private final int port;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final Lookups ipLookup;
    private Channel channel;
    private boolean work;

    public AsyncServer(int port, int threads, Lookups handler) {
        this.port = port;
        bossGroup = new NioEventLoopGroup(threads);
        workerGroup = new NioEventLoopGroup(threads);
        ipLookup = handler;
        log.info("Created server on port {} with {} threads", port, threads);
    }

    @PostConstruct
    public void run() {
        if (work) {
            log.warn("called run() on server.{} already running", port);
            return;
        }
        try {
   			final ServerBootstrap sbs = new ServerBootstrap();
   			sbs.option(ChannelOption.SO_BACKLOG, 1024);
            sbs.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                        new LengthFieldBasedFrameDecoder(32*1024, 0,
                                                4, 0, 4),
                                        new ProtobufDecoder(MaxProto.Request.getDefaultInstance())
                            );
                            ch.pipeline().addLast(new ProtobufHandler(ipLookup));
                            ch.pipeline().addLast(
                                    new LengthFieldPrepender(4),
                                    new ProtobufEncoder()
                            );
                        }
                    });
            channel = sbs.bind(port).sync().channel();
            work = true;
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @PreDestroy
    public void shutdown() {
        work = false;
        if (channel != null) {
            channel.close();
        }
        bossGroup.shutdownGracefully(10, 10, TimeUnit.SECONDS);
        workerGroup.shutdownGracefully(10, 10, TimeUnit.SECONDS);
        log.info("SERVER {} SHUTDOWN", port);
    }

}
