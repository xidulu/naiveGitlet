package gitlet;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author XiWang
 */
public class Main {

    /** Store repo. */
    private static Repo re;
    /** Store object dir. */
    private static File objectDir;
    /** Store working dir. */
    private static File workingDir;

    /** Main function input ARGS. */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String command = args[0];
        workingDir = new File(".");
        objectDir = Utils.join(workingDir, ".gitlet");
        if (command.equals("init")) {
            doInit(args);
        }
        if (!Utils.join(objectDir, "REPO").exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        re = Utils.readObject(Utils.join(objectDir, "REPO"), Repo.class);
        try {
            if (command.equals("add-remote")) {
                doAddRemote(args); System.exit(0);
            } else if (command.equals("push")) {
                doPush(args); System.exit(0);
            } else if (command.equals("pull")) {
                doPull(args); System.exit(0);
            } else if (command.equals("fetch")) {
                doFetch(args); System.exit(0);
            } else if (command.equals("rm-remote")) {
                doReRemote(args); System.exit(0);
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
        if (command.equals("add")) {
            doAdd(args);
        } else if (command.equals("reset")) {
            doReset(args);
        } else if (command.equals("commit")) {
            doCommit(args);
        } else if (command.equals("rm")) {
            doRm(args);
        } else if (command.equals("log")) {
            doLog(args);
        } else if (command.equals("status")) {
            doStatus(args);
        } else if (command.equals("branch")) {
            doBranch(args);
        } else if (command.equals("rm-branch")) {
            doRmBranch(args);
        } else if (command.equals("global-log")) {
            doGlobalLog(args);
        } else if (command.equals("find")) {
            doFind(args);
        } else if (command.equals("merge")) {
            doMerge(args);
        } else if (command.equals("checkout")) {
            doCheckout(args);
        }
        System.out.println("No command with that name exists.");
        System.exit(0);
    }

    /** INPUT ARGS. */
    private static void doInit(String... args) {
        if (Utils.join(objectDir, "REPO").exists()) {
            System.out.print("A Gitlet version-control system");
            System.out.println(" already exists in the current directory.");
            System.exit(0);
        } else {
            objectDir.mkdir();
            re = new Repo();
            Utils.writeObject(Utils.join(objectDir, "REPO"), re);
            System.exit(0);
        }
    }

    /** INPUT ARGS. */
    private static void doCheckout(String... args) {
        if (args[1].equals("--")) {
            if (args.length != 3) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            } else {
                try {
                    re.revertFile(args[2]);
                    Utils.writeObject(Utils.join(objectDir, "REPO"), re);
                    System.exit(0);
                } catch (GitletException e) {
                    System.out.println(e.getMessage());
                    System.exit(0);
                }
            }
        }
        if (!args[1].equals("--")) {
            Pattern p = Pattern.compile("[a-f0-9]+");
            if (args.length == 2) {
                try {
                    re.checkout2branch(args[1]);
                    Utils.writeObject(Utils.join(objectDir, "REPO"), re);
                    System.exit(0);
                } catch (GitletException e) {
                    System.out.println(e.getMessage());
                    System.exit(0);
                }
            } else {
                if (args.length != 4) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                if (!args[2].equals("--")) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                if (!Pattern.matches("[a-f0-9]+", args[1])) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                try {
                    String id = re.changeBackShortID(args[1]);
                    re.revertFile(id, args[3]);
                    Utils.writeObject(Utils.join(objectDir, "REPO"), re);
                    System.exit(0);
                } catch (GitletException e) {
                    System.out.println(e.getMessage());
                    System.exit(0);
                }
            }
        }
    }


    /** INPUT ARGS. */
    private static void doAdd(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        File file = Utils.join(workingDir, args[1]);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else {
            try {
                re.addFile(args[1]);
                Utils.writeObject(Utils.join(objectDir, "REPO"), re);
                System.exit(0);
            } catch (GitletException e) {
                System.out.println(e.getMessage());
                System.exit(0);
            }
        }
    }


    /** INPUT ARGS. */
    private static void doReset(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        try {
            String id = re.changeBackShortID(args[1]);
            re.revertWorkingFolder(id);
            Utils.writeObject(Utils.join(objectDir, "REPO"), re);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** INPUT ARGS. */
    private static void doCommit(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        String message = args[1];
        try {
            if (message.equals("")) {
                System.out.println("Please enter a commit message.");
                System.exit(0);
            }
            re.newCommit(message);
            Utils.writeObject(Utils.join(objectDir, "REPO"), re);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** INPUT ARGS. */
    private static void doRm(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        File file = Utils.join(workingDir, args[1]);
        try {
            re.removeFile(args[1]);
            Utils.writeObject(Utils.join(objectDir, "REPO"), re);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** INPUT ARGS. */
    private static void doLog(String... args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        re.printLog();
        System.exit(0);
    }

    /** INPUT ARGS. */
    private static void doStatus(String... args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        re.printStatus();
        System.exit(0);
    }

    /** INPUT ARGS. */
    private static void doBranch(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        try {
            re.createBranch(args[1]);
            Utils.writeObject(Utils.join(objectDir, "REPO"), re);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** INPUT ARGS. */
    private static void doRmBranch(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        try {
            re.removeBranch(args[1]);
            Utils.writeObject(Utils.join(objectDir, "REPO"), re);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** INPUT ARGS. */
    private static void doFind(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands:");
            System.exit(0);
        }
        try {
            re.doFind(args[1]);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** INPUT ARGS. */
    private static void doGlobalLog(String... args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands:");
            System.exit(0);
        }
        re.printGlobalLog();
        System.exit(0);
    }

    /** INPUT ARGS. */
    private static void doMerge(String... args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        try {
            re.merge(args[1]);
            Utils.writeObject(Utils.join(objectDir, "REPO"), re);
            System.exit(0);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /** INPUT ARGS. */
    private static void doAddRemote(String... args) {
        re.addRemote(args[1], new File(args[2]));
        Utils.writeObject(Utils.join(objectDir, "REPO"), re);
    }

    /** INPUT ARGS. */
    private static void doFetch(String... args) {
        re.fetchRemoteBranch(args[1], args[2]);
        Utils.writeObject(Utils.join(objectDir, "REPO"), re);
    }

    /** INPUT ARGS. */
    private static void doPull(String... args) {
        re.pull(args[1], args[2]);
        Utils.writeObject(Utils.join(objectDir, "REPO"), re);
    }

    /** INPUT ARGS. */
    private static void doPush(String... args) {
        re.push(args[1], args[2]);
        Utils.writeObject(Utils.join(objectDir, "REPO"), re);
    }

    /** INPUT ARGS. */
    private static void doReRemote(String... args) {
        re.removeRemote(args[1]);
        Utils.writeObject(Utils.join(objectDir, "REPO"), re);
    }

}
