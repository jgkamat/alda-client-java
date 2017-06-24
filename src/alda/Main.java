package alda;

import java.io.File;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParameterException;

import alda.repl.AldaRepl;

public class Main {

  public static class FileConverter implements IStringConverter<File> {
    @Override
    public File convert(String value) {
      return new File(value);
    }
  }

  private static class GlobalOptions {
    @Parameter(names = {"--alda-fingerprint"},
               description = "Used to identify this as an Alda process",
               hidden = true)
    public boolean aldaFingerprint = false;

    @Parameter(names = {"-h", "--help"},
               help = true,
               description = "Print this help text")
    public boolean help = false;

    @Parameter(names = {"-v", "--verbose"},
               description = "Enable verbose output")
    public boolean verbose = false;

    @Parameter(names = {"-q", "--quiet"},
               description = "Disable non-error messages")
    public boolean quiet = false;

    @Parameter(names = {"-H", "--host"},
               description = "The hostname of the Alda server")
    public String host = "localhost";

    @Parameter(names = {"-p", "--port"},
               description = "The port of the Alda server/worker")
    public int port = 27713;

    @Parameter(names = {"-t", "--timeout"},
               description = "The number of seconds to wait for a server to start before giving up.")
    public int timeout = 30;

    @Parameter(names = {"-w", "--workers"},
               description = "The number of worker processes to start")
    public int numberOfWorkers = 2;
  }

  private static class AldaCommand {
      @Parameter(names = {"--help", "-h"},
                 help = true,
                 hidden = true,
                 description = "Print this help text")
      public boolean help = false;
  }

  @Parameters(commandDescription = "Start an Alda server in the foreground.",
              hidden = true)
  private static class CommandServer extends AldaCommand {}

  @Parameters(commandDescription = "Start an Alda worker in the foreground.",
              hidden = true)
  private static class CommandWorker extends AldaCommand {}

  @Parameters(commandDescription = "Start an interactive Alda REPL session.")
  private static class CommandRepl extends AldaCommand {}

  @Parameters(commandDescription = "Display this help text")
  private static class CommandHelp extends AldaCommand {}

  @Parameters(commandDescription = "Download and install the latest release of Alda")
  private static class CommandUpdate extends AldaCommand {
      @Parameter(names = {"--copy"},
                 description = "Copies the current alda binary to the given target. Indended for internal use.")
      public String updateCopyLocation = "";
  }

  @Parameters(commandDescription = "Start the Alda server")
  private static class CommandStartServer extends AldaCommand {}

  @Parameters(commandDescription = "Stop the Alda server")
  private static class CommandStopServer extends AldaCommand {}

  @Parameters(commandDescription = "Restart the Alda server")
  private static class CommandRestartServer extends AldaCommand {}

  @Parameters(commandDescription = "List running Alda servers/workers")
  private static class CommandList extends AldaCommand {}

  @Parameters(commandDescription = "Display whether the server is up")
  private static class CommandStatus extends AldaCommand {}

  @Parameters(commandDescription = "Display the version of the Alda client and server")
  private static class CommandVersion extends AldaCommand {}

  @Parameters(commandDescription = "Evaluate and play Alda code")
  private static class CommandPlay extends AldaCommand {
    @Parameter(names = {"-f", "--file"},
               description = "Read Alda code from a file",
               converter = FileConverter.class)
    public File file;

    @Parameter(names = {"-c", "--code"},
               description = "Supply Alda code as a string")
    public String code;

    @Parameter(names = {"-i", "--history"},
               description = "Alda code that can be referenced but will not be played")
    public String history = "";

    @Parameter(names = {"-F", "--from"},
               description = "A time marking or marker from which to start " +
                             "playback")
    public String from;

    @Parameter(names = {"-T", "--to"},
               description = "A time marking or marker at which to end playback")
    public String to;
  }

  @Parameters(commandDescription = "Stop playback")
  private static class CommandStop extends AldaCommand {}

  @Parameters(commandDescription = "Display the result of parsing Alda code")
  private static class CommandParse extends AldaCommand {
    @Parameter(names = {"-f", "--file"},
               description = "Read Alda code from a file",
               converter = FileConverter.class)
    public File file;

    @Parameter(names = {"-c", "--code"},
               description = "Supply Alda code as a string")
    public String code;
  }

  public static void handleCommandSpecificHelp(JCommander jc, String name, AldaCommand c) {
      if(c.help) {
          jc.usage(name);
          System.exit(0);
      }
  }

  public static void main(String[] argv) {
    GlobalOptions globalOpts = new GlobalOptions();

    CommandHelp          help          = new CommandHelp();
    CommandUpdate        update        = new CommandUpdate();
    CommandServer        serverCmd     = new CommandServer();
    CommandWorker        workerCmd     = new CommandWorker();
    CommandRepl          repl          = new CommandRepl();
    CommandStartServer   startServer   = new CommandStartServer();
    CommandStopServer    stopServer    = new CommandStopServer();
    CommandRestartServer restartServer = new CommandRestartServer();
    CommandList          list          = new CommandList();
    CommandStatus        status        = new CommandStatus();
    CommandVersion       version       = new CommandVersion();
    CommandPlay          play          = new CommandPlay();
    CommandStop          stop          = new CommandStop();
    CommandParse         parse         = new CommandParse();

    JCommander jc = new JCommander(globalOpts);
    jc.setProgramName("alda");

    jc.addCommand("help", help);

    jc.addCommand("update", update);

    jc.addCommand("server", serverCmd);
    jc.addCommand("worker", workerCmd);
    jc.addCommand("repl", repl);

    jc.addCommand("up", startServer, "start-server", "init");
    jc.addCommand("down", stopServer, "stop-server");
    jc.addCommand("downup", restartServer, "restart-server");

    jc.addCommand("list", list);
    jc.addCommand("status", status);
    jc.addCommand("version", version);

    jc.addCommand("play", play);
    jc.addCommand("stop", stop, "stop-playback");
    jc.addCommand("parse", parse);

    try {
      jc.parse(argv);
    } catch (ParameterException e) {
      System.out.println(e.getMessage());
      System.out.println();
      System.out.println("For usage instructions, see --help.");
      System.exit(1);
    }

    AldaServer server = new AldaServer(globalOpts.host,
                                       globalOpts.port,
                                       globalOpts.timeout,
                                       globalOpts.verbose,
                                       globalOpts.quiet);

    try {
      if (globalOpts.help) {
        jc.usage();
        return;
      }

      String command = jc.getParsedCommand();
      command = command == null ? "help" : command;

      // used for play and parse commands
      String mode;
      String inputType;

      // used for up and downup commands
      boolean success;

      switch (command) {
        case "help":
          jc.usage();
          System.exit(0);

        case "update":
          handleCommandSpecificHelp(jc, "update", update);
          int exitCode = 0;
          if (!update.updateCopyLocation.isEmpty()) {
            // Copy the current binary over the target
            exitCode = AldaClient.copyBinary(update.updateCopyLocation);
          } else {
            exitCode = AldaClient.updateAlda();
          }
          System.exit(exitCode);

        case "server":
          handleCommandSpecificHelp(jc, "server", serverCmd);
          server.upFg(globalOpts.numberOfWorkers);
          break;

        case "worker":
          handleCommandSpecificHelp(jc, "worker", workerCmd);
          AldaWorker worker = new AldaWorker(globalOpts.port,
                                             globalOpts.verbose);

          worker.upFg();
          break;

        case "repl":
          handleCommandSpecificHelp(jc, "repl", repl);
          AldaRepl javaRepl = new AldaRepl(server, globalOpts.verbose);
          javaRepl.run();
          break;

        case "up":
        case "start-server":
        case "init":
          handleCommandSpecificHelp(jc, "up", startServer);
          success = server.upBg(globalOpts.numberOfWorkers);
          System.exit(success ? 0 : 1);

        case "down":
        case "stop-server":
          handleCommandSpecificHelp(jc, "down", stopServer);
          server.down();
          System.exit(0);

        case "downup":
        case "restart-server":
          handleCommandSpecificHelp(jc, "restart-server", restartServer);
          success = server.downUp(globalOpts.numberOfWorkers);
          System.exit(success ? 0 : 1);

        case "list":
          handleCommandSpecificHelp(jc, "list", list);
          AldaClient.listProcesses(globalOpts.timeout);
          System.exit(0);

        case "status":
          handleCommandSpecificHelp(jc, "status", status);
          server.status();
          System.exit(0);

        case "version":
          handleCommandSpecificHelp(jc, "version", version);
          System.out.println("Client version: " + AldaClient.version());
          System.out.print("Server version: ");
          server.version();
          System.exit(0);

        case "play":
          handleCommandSpecificHelp(jc, "play", play);
          inputType = Util.inputType(play.file, play.code);

          switch (inputType) {
            case "file":
              server.play(play.file, play.from, play.to);
              break;
            case "code":
              server.play(play.code, play.history, play.from, play.to);
              break;
            case "stdin":
              server.play(Util.getStdIn(), play.from, play.to);
              break;
            default:
              throw new Exception("Please provide some Alda code in the form " +
                                  "of a string, file, or STDIN.");
          }
          System.exit(0);

        case "stop":
        case "stop-playback":
            handleCommandSpecificHelp(jc, "stop", stop);
            server.stop();
            break;

        case "parse":
          handleCommandSpecificHelp(jc, "parse", parse);
          inputType = Util.inputType(parse.file, parse.code);

          switch (inputType) {
            case "file":
              server.parse(parse.file);
              break;
            case "code":
              server.parse(parse.code);
              break;
            case "stdin":
              server.parse(Util.getStdIn());
              break;
            default:
              throw new Exception("Please provide some Alda code in the form " +
                                  "of a string, file, or STDIN.");
          }
          System.exit(0);
      }
    } catch (Throwable e) {
      server.error(e.getMessage());
      if (globalOpts.verbose) {
        System.out.println();
        e.printStackTrace();
      }
      System.exit(1);
    }
  }

}
