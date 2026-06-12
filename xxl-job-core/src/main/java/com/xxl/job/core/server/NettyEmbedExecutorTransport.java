package com.xxl.job.core.server;

import com.xxl.job.core.openapi.ExecutorBiz;
import com.xxl.job.core.openapi.impl.ExecutorBizImpl;
import com.xxl.job.core.thread.ExecutorRegistryThread;
import com.xxl.tool.json.GsonTool;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class NettyEmbedExecutorTransport implements ExecutorTransport {
    private static final Logger logger = LoggerFactory.getLogger(NettyEmbedExecutorTransport.class);

    private ExecutorBiz executorBiz;
    private ExecutorTransportDispatcher dispatcher;
    private Thread thread;

    @Override
    public String type() {
        return "NETTY_EMBED";
    }

    @Override
    public void start(final String address, final int port, final String appname, final String accessToken) {
        executorBiz = new ExecutorBizImpl();
        dispatcher = new ExecutorTransportDispatcher(executorBiz, accessToken);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                ThreadPoolExecutor bizThreadPool = new ThreadPoolExecutor(
                        0,
                        200,
                        60L,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(2000),
                        new ThreadFactory() {
                            @Override
                            public Thread newThread(Runnable r) {
                                return new Thread(r, "xxl-job, EmbedServer bizThreadPool-" + r.hashCode());
                            }
                        },
                        new RejectedExecutionHandler() {
                            @Override
                            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                                throw new RuntimeException("xxl-job, EmbedServer bizThreadPool is EXHAUSTED!");
                            }
                        });
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel channel) {
                                    channel.pipeline()
                                            .addLast(new IdleStateHandler(0, 0, 30 * 3, TimeUnit.SECONDS))
                                            .addLast(new HttpServerCodec())
                                            .addLast(new HttpObjectAggregator(5 * 1024 * 1024))
                                            .addLast(new NettyEmbedHttpServerHandler(dispatcher, bizThreadPool));
                                }
                            })
                            .childOption(ChannelOption.SO_KEEPALIVE, true);

                    ChannelFuture future = bootstrap.bind(port).sync();
                    logger.info(">>>>>>>>>>> xxl-job remoting server start success, transport:{}, port:{}",
                            type(), port);

                    startRegistry(appname, registryValue(address));
                    future.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    logger.info(">>>>>>>>>>> xxl-job remoting server stop.");
                } catch (Throwable e) {
                    logger.error(">>>>>>>>>>> xxl-job remoting server error.", e);
                } finally {
                    try {
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.setName("xxl-job, EmbedServer");
        thread.start();
    }

    @Override
    public void stop() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        stopRegistry();
        logger.info(">>>>>>>>>>> xxl-job remoting server destroy success.");
    }

    private void startRegistry(final String appname, final String registryValue) {
        ExecutorRegistryThread.getInstance().start(appname, registryValue);
    }

    private void stopRegistry() {
        ExecutorRegistryThread.getInstance().toStop();
    }

    public static class NettyEmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private static final Logger logger = LoggerFactory.getLogger(NettyEmbedHttpServerHandler.class);

        private final ExecutorTransportDispatcher dispatcher;
        private final ThreadPoolExecutor bizThreadPool;

        public NettyEmbedHttpServerHandler(ExecutorTransportDispatcher dispatcher, ThreadPoolExecutor bizThreadPool) {
            this.dispatcher = dispatcher;
            this.bizThreadPool = bizThreadPool;
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) {
            final String requestData = msg.content().toString(CharsetUtil.UTF_8);
            final String uri = msg.uri();
            final HttpMethod httpMethod = msg.method();
            final boolean keepAlive = HttpUtil.isKeepAlive(msg);
            final String accessTokenReq = msg.headers().get(dispatcher.getAccessTokenHeaderName());

            bizThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    Object responseObj = dispatcher.dispatch(httpMethod.name(), uri, requestData, accessTokenReq);
                    String responseJson = GsonTool.toJson(responseObj);
                    writeResponse(ctx, keepAlive, responseJson);
                }
            });
        }

        private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.writeAndFlush(response);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error(">>>>>>>>>>> xxl-job provider netty_http server caught exception", cause);
            ctx.close();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.channel().close();
                logger.debug(">>>>>>>>>>> xxl-job provider netty_http server close an idle channel.");
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }
}
