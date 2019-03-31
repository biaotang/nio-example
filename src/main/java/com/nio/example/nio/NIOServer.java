package com.nio.example.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NIOServer {

	public static void main(String[] args) throws IOException {
		//打开服务端套接字通道
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		//兼容bio
		serverChannel.configureBlocking(false); //adjust bio mode
		//将通道的套接字绑定到本地地址，并配置套接字以侦听连接
		serverChannel.bind(new InetSocketAddress(9999));
		System.out.println("NIOServer has started, listening on port:" + serverChannel.getLocalAddress());
		
		//打开一个selector
		Selector selector =Selector.open();
		//将服务端通道注册到selector选择器上， 并返回SelectionKey
		//SelectionKey可以理解为是服务端和客户端的channel
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		//缓冲区
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		RequestHandler requestHandler = new RequestHandler();
		
		while (true) {
			//唯一一个会阻塞的地方，所有的等待都汇集到这里
			//返回已准备好进行I / O操作的通道数量
			int select = selector.select();
			if (select == 0) {
				continue;
			}
			
			//如果selector有channel的话，迭代SelectionKey
			Set<SelectionKey> selectionKeys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = selectionKeys.iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				//当客户端已经连接上来
				if (key.isAcceptable()) {
					ServerSocketChannel channel = (ServerSocketChannel) key.channel();
					SocketChannel clientChannel = channel.accept();
					System.out.println("Connection from " + clientChannel.getRemoteAddress());
					clientChannel.configureBlocking(false);
					//让服务端给客户端执行读取操作，需要给clientChannel状态注册成readable或writable
					clientChannel.register(selector, SelectionKey.OP_READ);
				}
				
				if (key.isReadable()) {
					SocketChannel channel = (SocketChannel)key.channel();
					channel.read(buffer);
					String request = new String(buffer.array()).trim();
					buffer.clear();
					System.out.println(String.format("From %s : %s", channel.getRemoteAddress(), request));
					String response = requestHandler.handle(request);
					channel.write(ByteBuffer.wrap(response.getBytes()));
				}
				iterator.remove();
			}
		}
	}
}
