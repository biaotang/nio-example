package com.nio.example.netty_telnet_wechat;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.ssl.SslContext;
import org.jboss.netty.handler.ssl.util.SelfSignedCertificate;

public class NettyServer {

	static final boolean SSL = System.getProperty("ssl") != null;
	static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));
	
	public static void main(String[] args) throws Exception {
		//Configure SSL
		final SslContext sslCtx;
		if (SSL) {
			SelfSignedCertificate ssc = new SelfSignedCertificate();
			sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
		} else {
			sslCtx = null;
		}
		
		//Configure the server
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		
		//Enable TCP_NODELAY to handle pipelined requests without latency
		bootstrap.setOption("child.tcpNoDelay", true);
		
		//Set up the event pipeline factory
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				
				if (sslCtx != null) {
					pipeline.addLast("ssl", sslCtx.newHandler());
				}
				
				//Add the text line codec combination first
				pipeline.addLast("framer", new DelimiterBasedFrameDecoder(
						8192, Delimiters.lineDelimiter()));
				pipeline.addLast("decoder", new StringDecoder());
				pipeline.addLast("encoder", new StringEncoder());
				
				//add then business logic
				pipeline.addLast("handler", new RequestHandler());
				return pipeline;
			}
		});
		
		//Bind and start to accept incoming connections
		bootstrap.bind(new InetSocketAddress(PORT));
		System.out.println("Netty has started，listening  on port ：" + PORT);
	}
	
}
