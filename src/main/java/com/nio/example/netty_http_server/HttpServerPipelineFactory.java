package com.nio.example.netty_http_server;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.ssl.SslContext;

public class HttpServerPipelineFactory implements ChannelPipelineFactory {

	private final SslContext sslCtx;
	
	public HttpServerPipelineFactory(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = new DefaultChannelPipeline();
		
		if (sslCtx != null) {
			pipeline.addLast("ssl", sslCtx.newHandler());
		}
		pipeline.addLast("decoder", new HttpRequestDecoder());
		//Uncomment the following line if you don't want to handle HttpChunks
		//pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
		pipeline.addLast("encoder", new HttpResponseEncoder());
		//Remove the following line if you don't want automatic content compression
		pipeline.addLast("deflater", new HttpContentCompressor());
		pipeline.addLast("handler", new HttpServerHandler());
		return pipeline;
	}

}
