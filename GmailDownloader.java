/*
 * GmailDownloader.java
 * (c) staktrace systems, 2009.
 * This code is BSD-licensed.
 */

import java.util.*;
import java.io.*;
import java.net.*;
import javax.net.*;
import javax.net.ssl.*;

public class GmailDownloader {
    public static void main( String[] args ) throws Exception {
        boolean delete = false;
        int start = 1;
        int end = -1;
        if (args.length > 0) {
            int argix = 0;
            if (args[ argix ].equals( "-d" )) {
                delete = true;
                argix++;
            }
            try {
                start = Integer.parseInt( args[ argix++ ] );
                if (args.length > argix) {
                    end = Integer.parseInt( args[ argix ] );
                } else if (delete) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException nfe) {
                System.out.println( "Usage: java GmailDownloader [start [end]]" );
                System.out.println();
                System.out.println( "   start and end are integers indicating the range of messages to" );
                System.out.println( "   be downloaded. For an initial download just leave them blank, or" );
                System.out.println( "   specify start as 1 and end as the number of messages to download." );
                System.out.println( "   To resume a previously-interrupted or partial download, specify" );
                System.out.println( "   the start value for where you left off. Optionally specify the" );
                System.out.println( "   end value to limit the number of messages downloaded." );
                System.out.println();
                System.out.println( "      java GmailDownloader -d <start> <end>" );
                System.out.println();
                System.out.println( "   Deletes the messages in the given range. Usually you want to only" );
                System.out.println( "   delete messages you have already downloaded. Note that once messages" );
                System.out.println( "   are deleted, the messages after that are renumbered so that there are" );
                System.out.println( "   no holes in the numbering. So start=1 end=1000 will delete the first" );
                System.out.println( "   1000 messages, and message 1001 will get renumbered down to 1." );
                return;
            }
        }

        Console console = System.console();
        String username = console.readLine( "Enter username: " );
        String password = new String( console.readPassword( "Enter password: " ) );

        SocketFactory sf = SSLSocketFactory.getDefault();
        Socket socket = sf.createSocket( "imap.gmail.com", 993 );
        InputStream in = socket.getInputStream();
        BufferedReader br = new BufferedReader( new InputStreamReader( in ) );

        OutputStream out = socket.getOutputStream();
        PrintWriter pw = new PrintWriter( out );

        pw.print( "A LOGIN " + username + " " + password + "\r\n" ); pw.flush();
        while (! br.readLine().startsWith( "A OK" ));

        pw.print( "B SELECT \"[Gmail]/All Mail\"\r\n" ); pw.flush();
        for (String s = br.readLine(); !s.startsWith( "B OK" ); s = br.readLine()) {
            if (! s.endsWith( " EXISTS" )) {
                continue;
            }
            StringTokenizer st = new StringTokenizer( s );
            st.nextToken();
            if (end < 0) {
                end = Integer.parseInt( st.nextToken() );
                System.out.println( "Found " + end + " messages" );
            } else {
                System.out.println( "Found " + st.nextToken() + " messages" );
            }
        }

        if (delete) {
            System.out.println( "Deleting messages from [" + start + "] to [" + end + "]" );
            pw.print( "ZZ COPY " + start + ":" + end + " \"[Gmail]/Trash\"\r\n" ); pw.flush();
            for (String s = br.readLine(); !s.startsWith( "ZZ OK" ); s = br.readLine()) {
                if (! s.endsWith( " EXISTS" )) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer( s );
                st.nextToken();
                System.out.println( "Down to " + st.nextToken() + " messages" );
            }
        } else {
            System.out.println( "Downloading messages from [" + start + "] to [" + end + "]" );
            for (int i = start; i <= end; i++) {
                System.out.println( "Downloading message " + i );
                pw.print( "ZZ FETCH " + i + " RFC822\r\n" ); pw.flush();
                br.readLine();
                String fn = i + ".msg";
                while (fn.length() < 9) {
                    fn = "0" + fn;
                }
                PrintWriter file = new PrintWriter( new File( fn ) );
                outer: while (true) {
                    String s = br.readLine();
                    while (s.endsWith( ")" )) {
                        String t = br.readLine();
                        if (t.startsWith( "ZZ OK" )) {
                            if (s.length() > 1) {
                                file.println( s.substring( 0, s.length() - 1 ) );
                            }
                            break outer;
                        }
                        file.println( s );
                        s = t;
                    }
                    file.println( s );
                }
                file.close();
            }
        }

        pw.print( "C LOGOUT\r\n" ); pw.flush();

        in.close();
        out.close();
        socket.close();
    }
}
