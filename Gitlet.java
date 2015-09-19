
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;


/**
 * Created by kishanpatel on 3/29/15.
 */
public class Gitlet {

    private static String ALL_LETTERS = "abcdef0123456789";
    /**
     * Contains the switch statements which calls different functions based on
     * different user inputs
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }
        String command = args[0];
        String[] extraCommands = new String[args.length - 1];
        System.arraycopy(args, 1, extraCommands, 0, extraCommands.length);
        switch (command) {
            case "init":
                gitletInit(extraCommands);
                break;
            case "add":
                gitletAdd(extraCommands);
                break;
            case "commit" :
                gitletCommit(extraCommands, false);
                break;
            case "rm" :
                gitletRemove(extraCommands);
                break;
            case "log" :
                gitletLog(extraCommands);
                break;
            case "global-log" :
                gitletGlobalLog(extraCommands);
                break;
            case "status" :
                gitletStatus(extraCommands);
                break;
            case "find" :
                gitletFind(extraCommands);
                break;
            case "checkout" :
                if (gitletDangerous()) {
                    gitletCheckout(extraCommands);
                }
                break;
            case "branch" :
                gitletBranch(extraCommands);
                break;
            case "rm-branch" :
                gitletRemoveBranch(extraCommands);
                break;
            case "reset" :
                if (gitletDangerous()) {
                    gitletReset(extraCommands);
                }
                break;
            case "merge" : 
                if (gitletDangerous()) {
                    gitletMerge(extraCommands);
                }
                break;
            case "rebase" :
                if (gitletDangerous()) {
                    gitletRebase(extraCommands);
                }
                break;
            case "i-rebase" :
                if (gitletDangerous()) {
                    gitletInteractiveRebase(extraCommands);
                }
                break;
            case "add-remote" :
                gitletAddRemote(extraCommands);
                break;
            case "rm-remote" :
                gitletRemoveRemote(extraCommands);
                break;
            case "push" :
                gitletPush(extraCommands);
                break;
            case "pull" :
                gitletPull(extraCommands);
                break;
            default :
                return;
        }
    }

    private static void gitletPull(String[] extraCommands) {
        if (extraCommands.length != 2) {
            return;
        }
        GitletHead head = getGitHead();
        head.pull(extraCommands[0], extraCommands[1], new File(".gitlet/").getAbsolutePath());
        storeGitHead(head);

    }


    private static void gitletPush(String[] extras) {
        if (extras.length != 2) {
            return;
        }
        GitletHead head = getGitHead();
        head.push(extras[0], extras[1], new File(".gitlet/").getAbsolutePath());
        storeGitHead(head);

    }

    private static void gitletRemoveRemote(String[] extraCommands) {
        if (extraCommands.length != 1) {
            return;
        }
        GitletHead head = getGitHead();
        head.removeRemote(extraCommands[0]);
        storeGitHead(head);
    }

    private static void gitletAddRemote(String[] extraCommands) {
        if (extraCommands.length != 4) {
            return;
        }
        GitletHead head = getGitHead();
        head.addRemote(extraCommands);
        storeGitHead(head);
    }

    /**
     * Calls GitletHead's rebase function with interactive as true
     * @param extraCommands
     */
    private static void gitletInteractiveRebase(String[] extraCommands) {
        if (extraCommands.length == 1) {
            GitletHead head = getGitHead();
            head.rebase(extraCommands[0], getCurrentTime(), true);
            storeGitHead(head);

        }
    }

    /**
     * Calls GitletHead's rebase function with interactive as false
     * @param extraCommands
     */
    private static void gitletRebase(String[] extraCommands) {
        if (extraCommands.length != 1) {
            return;
        }
        GitletHead head = getGitHead();
        head.rebase(extraCommands[0], getCurrentTime(), false);
        storeGitHead(head);
    }

    /**
     * Calls head's merge function and stores the head object
     * @param extraCommands
     */
    private static void gitletMerge(String[] extraCommands) {
        if (extraCommands.length != 1) {
            return;
        } else {
            GitletHead head = getGitHead();
            head.merge(extraCommands[0]);
            storeGitHead(head);
        }
    }

    /**
     * calls head's reset function and stores the head object
     * @param extraCommands
     */
    private static void gitletReset(String[] extraCommands) {
        if (extraCommands.length != 1) {
            return;
        }
        GitletHead head = getGitHead();
        String commit = extraCommands[0];

        head.reset(commit);
        storeGitHead(head);
    }

    /**
     * remove branch from the head object and stores it.
     * @param extraCommands
     */
    private static void gitletRemoveBranch(String[] extraCommands) {
        if (extraCommands.length != 1) {
            return;
        }

        GitletHead head = getGitHead();
        head.removeBranch(extraCommands[0]);
        storeGitHead(head);

    }

    /**
     * Creates a new branch
     * @param name
     */
    private static void gitletBranch(String[] name) {
        if (name.length != 1) {
            return;
        }
        GitletHead head = getGitHead();
        head.createBranch(name[0]);
        storeGitHead(head);
    }

    /**
     * Waits for user input on functions that modify file data
     *
     */
    private static boolean gitletDangerous() {
        String ret = "Warning: The command you entered may alter the files in your working ";
        ret += "directory. Uncommitted changes may be lost. Are you sure you want to continue? ";
        ret += "(yes/no)";
        System.out.println(ret);
        while (true) {
            System.out.print("> ");
            Scanner read = new Scanner(System.in);
            String line = read.nextLine();
            String[] rawTokens = line.split(" ");
            if (rawTokens.length >= 1) {
                if ("yes".equals(rawTokens[0])) {
                    return true;
                }
                return false;
            }
        }
    }

    /**
     * Calls head's checkout functions based on size of arguments
     * @param extraCommands
     */
    private static void gitletCheckout(String[] extraCommands) {
        GitletHead head = getGitHead();
        if (extraCommands.length == 2) {
            head.checkoutFileFromId(extraCommands[0], extraCommands[1]);
        } else {
            head.checkoutBranchOrFile(extraCommands[0]);
        }
        storeGitHead(head);
    }

    /**
     * Returns value from messageToID hashMap
     * @param extraCommands
     */
    private static void gitletFind(String[] extraCommands) {
        if (extraCommands.length != 1) {
            return;
        }
        HashMap<String, HashSet<String>> messageToId;
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/messageToId.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            messageToId = GitletHead.readObject(in);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        HashSet<String> retState = messageToId.get(extraCommands[0]);

        if (retState == null) {
            System.out.println("Found no commit with that message.");
        } else {
            for (String id : retState) {
                System.out.println(id);
            }
        }

    }

    /**
     * prints out status from Gitlethead's status function
     * @param extraCommands
     */
    private static void gitletStatus(String[] extraCommands) {
        if (extraCommands.length != 0) {
            return;
        }

        GitletHead head = getGitHead();
        head.printStatus();
        storeGitHead(head);
    }

    /**
     * Prints out global log from deserializing globalHist.ser
     * @param extraCommands
     */
    private static void gitletGlobalLog(String[] extraCommands) {
        if (extraCommands.length != 0) {
            return;
        }
        LinkedList<Commit> llc;
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/allHist.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            llc = GitletHead.readObject(in);
            //llc = (LinkedList<Commit>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
        int i = 0;
        for (; i < llc.size() - 1; i++) {
            System.out.println("====");
            System.out.println(llc.get(i));
            System.out.println("");
        }
        System.out.println("====");
        System.out.println(llc.getLast());


    }

    /**
     * Prints out log of the currentBranch
     * @param commands
     */
    private static void gitletLog(String[] commands) {
        if (commands.length != 0) {
            return;
        }
        GitletHead head = getGitHead();
        head.printLog();
        storeGitHead(head);
    }

    /**
     * marks file for removal
     * @param commands
     */
    private static void gitletRemove(String[] commands) {
        if (commands.length != 1) {
            return;
        }
        String file = commands[0];
        GitletHead head = getGitHead();
        head.removeFile(file);
        storeGitHead(head);
    }

    /**
     * Commits staged and files marked for removal
     * @param extraCommands
     *
     */
    private static void gitletCommit(String[] extraCommands, boolean override) {
        if (extraCommands.length != 1 || extraCommands[0].isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }
        String message = extraCommands[0];
        GitletHead head = getGitHead();
        head.createCommit(getCurrentTime(), message, override);
        storeGitHead(head);

    }

    /**
     * Deserializes the GitletHead object
     * @return
     */
    private static GitletHead getGitHead() {
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/HEAD.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            return (GitletHead) in.readObject();
        } catch (IOException e) {
            System.out.println("An error occurred");
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found");
            return null;
        }

    }

    /**
     * Serializes the GitletHead object
     * @param h
     */
    private static void storeGitHead(GitletHead h) {

        try {
            FileOutputStream fileOut = new FileOutputStream(".gitlet/HEAD.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(h);
        } catch (IOException e) {
            System.out.println("An error occurred");
        }

    }

    /**
     * Create initial commit and .gitlet/ folder.  Also creates all supplementary files
     * such as HEAD.ser, messageToId.ser, the commit.ser for the initial commit, and allHist.ser
     * @param extras
     */
    private static void gitletInit(String[] extras) {
        if (extras.length == 0) {
            File git = new File(".gitlet");
            if (git.exists()) {
                String message = "A gitlet version control system ";
                message += "already exists in the current directory.";
                System.out.println(message);
            } else {
                git.mkdir();
                String dateNow = getCurrentTime();
                Commit c = new Commit("initial commit", dateNow);
                String id = c.hashCodeGenerator();
                File initialCommit = new File(".gitlet/" + id);
                initialCommit.mkdir();
                GitletHead gh = new GitletHead(id);
                HashMap<String, HashSet<String>> messageToId = new HashMap<>();
                messageToId.put(c.getMessage(), new HashSet<String>());
                messageToId.get(c.getMessage()).add(c.getId());
                LinkedList<Commit> llc = new LinkedList<>();
                llc.addFirst(c);

                try {
//                    File f = new File(".gitlet/HEAD.ser");
                    FileOutputStream fileOut = new FileOutputStream(".gitlet/HEAD.ser");
                    ObjectOutputStream out = new ObjectOutputStream(fileOut);
                    out.writeObject(gh);
                    out.close();
                    fileOut.close();
//                    f = new File(".gitlet/" + initialCommit + "/commit.ser");
                    fileOut = new FileOutputStream(".gitlet/" + id + "/commit.ser");
                    out = new ObjectOutputStream(fileOut);
                    out.writeObject(c);
                    out.close();
                    fileOut.close();
//                    File g = new File(".gitlet/messageToId.ser");
                    fileOut = new FileOutputStream(".gitlet/messageToId.ser");
                    out = new ObjectOutputStream(fileOut);
                    out.writeObject(messageToId);
                    out.close();
                    fileOut.close();
//                    f = new File(".gitlet/allHist.ser");
                    fileOut = new FileOutputStream(".gitlet/allHist.ser");
                    out = new ObjectOutputStream(fileOut);
                    out.writeObject(llc);
                    out.close();
                    fileOut.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Gets the current Time and returns the string in the Gitlet Log format.
     * Code taken from
     * http://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
     */
    private static String getCurrentTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return dateFormat.format(now);
    }

    /**
     * Stages a file if it passes failure conditions
     */
    private static void gitletAdd(String[] extras) {
        if (extras.length != 1) {
            return;
        } else {
            GitletHead head = getGitHead();

            head.addFile(extras[0]);

            storeGitHead(head);

        }
    }


}
