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

public class ImapRepl extends ImapBase {
    public ImapRepl( String host, int port ) {
        super( host, port );
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
        ImapRepl repl = new ImapRepl( host, port );

        System.out.println( "Connecting..." );
        repl.connect();

        System.out.println( "Logging in..." );
        if (repl.logIn( console )) {
            System.out.println( "Ready for input..." );
            for (String input = console.readLine( "> " ); input != null; input = console.readLine( "> " )) {
                repl.imapDump( input );
            }
            repl.logOut();
        }

        repl.disconnect();
    }
}
