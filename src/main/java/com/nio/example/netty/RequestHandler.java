package com.nio.example.netty;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class RequestHandler extends SimpleChannelUpstreamHandler {

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		String request = (String)e.getMessage();
		e.getChannel().write(String.format("from %s : %s", e.getRemoteAddress(), request));
	}
	
}
