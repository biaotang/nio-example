package com.nio.example.netty_http_server;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunkTrailer;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;

public class HttpServerHandler extends SimpleChannelUpstreamHandler {

	private HttpRequest request;
	private boolean readingChunks;
	/** Buffer that stores the response content **/
	private final StringBuilder buf = new StringBuilder();
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (!readingChunks) {
			HttpRequest request = this.request = (HttpRequest) e.getMessage();
			
			if (HttpHeaders.is100ContinueExpected(request)) {
				send100Continue(e);
			}
			
			buf.setLength(0);
			buf.append("WELCOME TO THE WILD WEB SERVER\r\n");
			buf.append("===============================\r\n");
			
			buf.append("VERSION: " + request.getProtocolVersion() + "\r\n");
			buf.append("HOST: " + request.headers().get("HOST") + "\r\n");
			buf.append("REQUEST_URI: " + request.getUri() + "\r\n");
			
			for (Map.Entry<String, String> h : request.headers()) {
				buf.append("HEADER: " + h.getKey() + " = " + h.getValue() + "\r\n");
			}
			buf.append("\r\n");
			
			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
			Map<String, List<String>> params = queryStringDecoder.getParameters();
			if (!params.isEmpty()) {
				for (Entry<String, List<String>> p : params.entrySet()) {
					String key = p.getKey();
					List<String> vals = p.getValue();
					for (String val : vals) {
						buf.append("PARAM: " + key + " = " + val + "\r\n");
					}
				}
				buf.append("\r\n");
			}
			
			if (request.isChunked()) {
				readingChunks = true;
			} else {
				ChannelBuffer content = request.getContent();
				if (content.readable()) {
					buf.append("CONTENT: " + content.toString(CharsetUtil.UTF_8) + "\r\n");
				}
				writeResponse(e);
			}
		} else {
			HttpChunk chunk = (HttpChunk) e.getMessage();
			if (chunk.isLast()) {
				readingChunks = false;
				buf.append("END OF CONTENT\r\n");
				
				HttpChunkTrailer trailer = (HttpChunkTrailer) chunk;
				if (!trailer.trailingHeaders().names().isEmpty()) {
					buf.append("\r\n");
					for (String name : trailer.trailingHeaders().names()) {
						for (String value : trailer.trailingHeaders().getAll(name)) {
							buf.append("TRAILING HEADER: " + name + " = " + value + "\r\n");
						}
					}
					buf.append("\r\n");
				}
				
				writeResponse(e);
			} else {
				buf.append("CHUNK: " + chunk.getContent().toString(CharsetUtil.UTF_8) + "\r\n");
			}
		}
	}
	
	private void writeResponse(MessageEvent e) {
		//Decide whether to close the connection or not
		boolean keepAlive = request.headers().contains("KEEP_ALIVE");
		
		//Build the response object
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.setContent(ChannelBuffers.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));
		response.headers().set("Content-Type", "text/plain;charset=UTF-8");
		
		if (keepAlive) {
			//Add 'Content-Length' header only for a keep-alive connection
			response.headers().set("Content-Length", response.getContent().readableBytes());
			//Add keep alive header as per:
			// - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
			response.headers().set("Connection", HttpHeaders.Values.KEEP_ALIVE);
		}
		
		// Encode the cookie
		String cookieString = request.headers().get("COOKIE");
		if (cookieString != null) {
			CookieDecoder cookieDecoder = new CookieDecoder();
			Set<Cookie> cookies = cookieDecoder.decode(cookieString);
			if (!cookies.isEmpty()) {
				//Reset the cookies if necessary
				CookieEncoder cookieEncoder = new CookieEncoder(true);
				for (Cookie cookie : cookies) {
					cookieEncoder.addCookie(cookie);
					response.headers().add("SET_COOKIE", cookieEncoder.encode());
				}
			}
		} else {
			//Browser sent no cookie. Add some
			CookieEncoder cookieEncoder = new CookieEncoder(true);
			cookieEncoder.addCookie("key1", "value1");
			response.headers().add("SET_COOKIE", cookieEncoder.encode());
			cookieEncoder.addCookie("key2", "value2");
			response.headers().add("SET_COOKIE", cookieEncoder.encode());
		}
		
		//write the response
		ChannelFuture future = e.getChannel().write(response);
		
		//Close the non-keep-alive connection after the write operation is done
		if (!keepAlive) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	public static void send100Continue(MessageEvent e) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		e.getChannel().write(response);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		// TODO Auto-generated method stub
		super.exceptionCaught(ctx, e);
	}
	
}
