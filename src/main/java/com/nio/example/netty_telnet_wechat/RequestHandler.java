package com.nio.example.netty_telnet_wechat;

import java.net.InetAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

public class RequestHandler extends SimpleChannelUpstreamHandler {
	
	static final ChannelGroup channels = new DefaultChannelGroup();
	
	static AtomicInteger count = new AtomicInteger(0);
	
	static Map<Integer, String> userMap = new ConcurrentHashMap<>();
	
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		e.getChannel().write("Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
		e.getChannel().write("It is " + new Date() + " now.\r\n");
		channels.add(e.getChannel());
		userMap.put(e.getChannel().getId(), "user_" + count.addAndGet(1));
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		String request = (String)e.getMessage();
		
		String response;
		boolean close = false;
		if (request.length() == 0) {
			response = "Please type something.\r\n";
		} else if ("bye".equals(request.toLowerCase())) {
			response = "Have a good day!\r\n";
			close = true;
		} else {
			response = String.format(userMap.get(e.getChannel().getId()) + " : %s\r\n", request);
		}
		for (Channel channel : channels) {
			ChannelFuture future = channel.write(response);
			
			//close the connection if client has sent 'bye'
			if (close) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
		}
		
	}
	
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		channels.remove(e.getChannel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		e.getCause().printStackTrace();
		e.getChannel().write("Get an exception ： " + e.getCause().getMessage());
		e.getChannel().close();
		channels.remove(e.getChannel());
	}
	
}
