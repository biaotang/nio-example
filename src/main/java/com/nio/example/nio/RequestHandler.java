package com.nio.example.nio;

public class RequestHandler {

	public String handle(String request) {
		return "From NIOServer Hello " + request + ".\n";
	}
	
}
