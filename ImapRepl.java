/*
 * ImapRepl.java
 * (c) staktrace systems, 2010.
 * This code is BSD-licensed.
 */

import java.util.*;
import java.io.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;

public class ImapRepl {
    private final String _host;
    private final int _port;

    private Socket _socket;
    private InputStream _socketIn;
    private BufferedReader _imapServer;
    private OutputStream _socketOut;
    private PrintWriter _server;
    private int _commandCounter;

    public ImapRepl( String host, int port ) {
        _host = host;
        _port = port;
    }

    void connect() throws IOException {
        SocketFactory sf = SSLSocketFactory.getDefault();
        _socket = sf.createSocket( _host, _port );
        _socketIn = _socket.getInputStream();
        _imapServer = new BufferedReader( new InputStreamReader( _socketIn ) );
        _socketOut = _socket.getOutputStream();
        _server = new PrintWriter( _socketOut );
        _commandCounter = 1;
    }

    void disconnect() throws IOException {
        _socketIn.close();
        _socketOut.close();
        _socket.close();

        _server = null;
        _socketOut = null;
        _imapServer = null;
        _socketIn = null;
        _socket = null;
    }

    void logIn( String username, String password ) throws IOException {
        imapSend( "LOGIN " + username + " " + password );
    }

    void logOut() throws IOException {
        imapSend( "LOGOUT" );
    }

    private void imapSend( String command ) throws IOException {
        String ident = "A" + (_commandCounter++);
        _server.print( ident + " " + command + "\r\n" );
        _server.flush();

        String s;
        for (s = _imapServer.readLine(); ! s.startsWith( ident ); s = _imapServer.readLine()) {
            System.out.println( s );
        }
        System.out.println( '*' + s.substring( ident.length() ) );
    }

    public static void usage( PrintStream out ) {
        out.println( "Usage: java ImapRepl <host> [port]" );
        out.println( "       This will open a secure socket to the given host/port and prompt you for credentials." );
        out.println( "       The credentials will be used to authenticate the IMAP connection, and then you will" );
        out.println( "       be allowed to manually enter commands via a command-line that will be sent to the IMAP" );
        out.println( "       server." );
        out.println( "       Hit CTRL+D (or end-of-file input) to terminate." );
        out.println();
    }

    public static void main( String[] args ) throws Exception {
        if (args.length < 1) {
            usage( System.out );
            return;
        }

        String host = args[0];
        int port = 993;
        if (args.length > 1) {
            try {
                port = Integer.parseInt( args[1] );
            } catch (NumberFormatException nfe) {
                usage( System.out );
                return;
            }
        }

        Console console = System.console();
        String username = console.readLine( "Enter username: " );
        String password = new String( console.readPassword( "Enter password: " ) );

        System.out.println( "Connecting..." );
        ImapRepl repl = new ImapRepl( host, port );
        repl.connect();
        System.out.println( "Logging in..." );
        repl.logIn( username, password );
        System.out.println( "Ready for input..." );
        for (String input = console.readLine( "> " ); input != null; input = console.readLine( "> " )) {
            repl.imapSend( input );
        }
        repl.logOut();
        repl.disconnect();
    }
}
