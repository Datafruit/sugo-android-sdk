package io.sugo.android.java_websocket.server;

import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

import io.sugo.android.java_websocket.WebSocketAdapter;
import io.sugo.android.java_websocket.WebSocketImpl;
import io.sugo.android.java_websocket.drafts.Draft;

public class DefaultWebSocketServerFactory implements WebSocketServer.WebSocketServerFactory {
	@Override
	public WebSocketImpl createWebSocket(WebSocketAdapter a, Draft d, Socket s ) {
		return new WebSocketImpl( a, d );
	}
	@Override
	public WebSocketImpl createWebSocket(WebSocketAdapter a, List<Draft> d, Socket s ) {
		return new WebSocketImpl( a, d );
	}
	@Override
	public SocketChannel wrapChannel(SocketChannel channel, SelectionKey key ) {
		return (SocketChannel) channel;
	}
}