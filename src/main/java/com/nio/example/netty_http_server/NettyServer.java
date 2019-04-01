package com.nio.example.netty_http_server;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
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
		bootstrap.setPipelineFactory(new HttpServerPipelineFactory(sslCtx));
		
		//Bind and start to accept incoming connections
		bootstrap.bind(new InetSocketAddress(PORT));
		System.out.println("Netty http server has started，listening  on port ：" + PORT);
	}
}
