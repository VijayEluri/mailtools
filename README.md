mailtools
=========
3 simple steps to download all of your Gmail:

1. Download the code:

  $ git clone https://github.com/staktrace/mailtools

2. Build the code:

  $ javac *.java

3. Download your mail:

  $ java GmailDownloader
  (enter username and password at the prompts)

This will download all of your mail and store it in RFC822-encoded files, starting at 00001.msg and counting up.
You can abort the download at any time by hitting ctrl+c, and resume it later from where you left off by running it with the -s option:

  $ java GmailDownloader -s &lt;index&gt;

where &lt;index&gt; is the last one that got downloaded.

For non-Gmail hosts that use regular IMAP, steps 1 and 2 are the same; but modify step 3 to specify the host and port:

  $ java ImapDownloader mail.dreamhost.com 993

Run "java GmailDownloader --help" or "java ImapDownloader --help" for more help on the commands you can use.
