package gitlet;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Samhith Kakarla
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        if (args.length < 1) {
            System.out.println("Please enter a command.");
            return;
        }
        Commands c = new Commands();
        switch (args[0]) {
        case "init":
            c.init();
            break;
        case "add":
            c.add(args[1]);
            break;
        case "commit":
            c.commit(args[1]);
            break;
        case "checkout":
            if (args[1].equals("--")) {
                c.checkout(args[2]);
                break;
            } else if (args.length > 2) {
                if (args[2].equals("++")) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                c.checkout(args[1], args[3]);
                break;
            } else {
                c.checkoutBranch(args[1]);
                break;
            }
        case "log":
            c.log();
            break;
        case "global-log":
            c.globalLog();
            break;
        case "branch":
            c.branch(args[1]);
            break;
        case "find":
            c.find(args[1]);
            break;
        case "rm":
            c.rm(args[1]);
            break;
        case "status":
            c.status();
            break;
        case "reset": c.reset(args[1]);
            break;
        case "rm-branch": c.rmBranch(args[1]);
            break;
        case "merge": c.merge(args[1]);
            break;
        default:
            System.out.println("No command with that name exists.");
            break;
        }
    }

}
