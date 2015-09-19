import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import static java.nio.file.StandardCopyOption.*;
import java.io.Serializable;
/**
 * Created by kishanpatel on 3/29/15.
 */

public class GitletHead implements Serializable {

    //"How to print color in console using System.out.println?" from Stack Overflow
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final long serialVersionUID = 42L;





    private HashSet<String> stagedFiles;
    private HashSet<String> filesToRemove;
    private HashMap<String, KPLinkedList<String>> branches;
    private String currentBranch;
    private HashMap<String, KPLinkedList<String>> idToCommitChain;
    private HashMap<String, String[]> remotes;

    public GitletHead(String commitID) {
        stagedFiles = new HashSet<>();
        branches = new HashMap<>();
        currentBranch = "master";
        branches.put("master", new KPLinkedList<String>(commitID, null));
        filesToRemove = new HashSet<>();
        idToCommitChain = new HashMap<>();
        idToCommitChain.put(commitID, branches.get("master"));
        remotes = new HashMap<>();

    }


    /**
     * Checks whether name exists or hasn't been modified since the latest commit.
     *
     * @param name
     */
    public void addFile(String name) {
        if (filesToRemove.contains(name)) {
            filesToRemove.remove(name);
            return;
        }
        if (stagedFiles.contains(name)) {
            return;
        } else {
            File fileToAdd = new File(name);
            if (!fileToAdd.exists()) {
                System.out.println("File does not exist");
                return;
            } else  {
                Commit c;
                try {
                    FileInputStream filein = new FileInputStream(".gitlet/"
                            + branches.get(currentBranch).getHead() + "/commit.ser");
                    ObjectInputStream in = new ObjectInputStream(filein);
                    c = (Commit) in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
                File f = new File(".gitlet/" + c.get(name) + "/" + name);
                if (checkFiles(f, fileToAdd)) {
                    System.out.println("File has not been modified since the last commit.");
                    return;
                }
                stagedFiles.add(name);

            }

        }
    }

    /**
     * Creates new Commit object, and copies over files into a folder determined by a hashcode.
     * Only staged files are copied into the directory, and ones marked for removal are removed from
     * the HashMap in Commit.
     * Clears out staged Files, filesToRemove, and increments commitId
     *
     * Allows for the creation of Commits that skip failure casees with the override boolean.
     * Comes in handy with Rebase
     *
     * @param time
     * @param message
     * @param override
     */
    public String createCommit(String time, String message, boolean override) {
        if (!override && stagedFiles.size() == 0 && filesToRemove.size() == 0) {
            System.out.println("No reason to commit");
            return null;
        }
        Commit oldCommit;

        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/"
                    + branches.get(currentBranch).getHead() + "/commit.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            oldCommit = (Commit) in.readObject();
        } catch (IOException  | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        Commit newCommit = new Commit(message, time);

        newCommit.setAllFilesInCommit(new HashMap<String, String>(oldCommit.getAllFilesInCommit()));
        newCommit.setParentMessageAndTime(oldCommit.getMessage() + oldCommit.getAccurateTime());
        String newCommitId = newCommit.hashCodeGenerator();

        File newFile = new File(".gitlet/" + newCommitId);
        newFile.mkdir();




        for (String fileName : stagedFiles) {
            try {
                File source = new File(fileName);
                if (source.exists()) {
                    File destination = new File(".gitlet/" + newCommitId + "/" + fileName);
                    destination.mkdirs();
                    Files.copy(source.toPath(), destination.toPath(), REPLACE_EXISTING);
                    destination.setLastModified(source.lastModified());
                    newCommit.put(fileName, newCommitId);
                }



            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        for (String fileName: filesToRemove) {
            newCommit.remove(fileName);
        }

        addFileToGlobal(newCommit);
        addMessageAndIdToOther(message, newCommitId);
        stagedFiles.clear();
        filesToRemove.clear();
        branches.put(currentBranch, new KPLinkedList<>(newCommitId,  branches.get(currentBranch)));
        idToCommitChain.put(newCommitId, branches.get(currentBranch));

        try {
            FileOutputStream fileOut = new FileOutputStream(".gitlet/"
                    + newCommitId + "/commit.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(newCommit);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return newCommitId;

    }

    /**
     * Adds files to the Global History
     * @param newCommit
     */
    private void addFileToGlobal(Commit newCommit) {
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/allHist.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            LinkedList<Commit> llc = GitletHead.readObject(in);
            llc.addFirst(newCommit);
            FileOutputStream fileOut = new FileOutputStream(".gitlet/allHist.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(llc);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Marks files for removal if in history or staged
     * @param name
     */
    public void removeFile(String name) {
        if (stagedFiles.contains(name)) {
            stagedFiles.remove(name);
            return;
        }
        Commit currentCommit;
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/"
                    + branches.get(currentBranch).getHead() + "/commit.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            currentCommit = (Commit) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        if (!currentCommit.containsKey(name)) {
            System.out.println("No reason to remove the file");
            return;
        }

        filesToRemove.add(name);



    }

    /**
     * Prints Commit's toString for branch history
     */
    public void printLog() {
        KPLinkedList<String> temp = branches.get(currentBranch);
        while (temp.getBottom() != null) {
            System.out.println("====");
            printCommitString(temp.getHead());
            System.out.println("");
            temp = temp.getBottom();
        }
        System.out.println("====");
        printCommitString(temp.getHead());
    }

    /**
     * Helper function for printLog()
     * @param head
     */
    private void printCommitString(String head) {
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/" + head + "/commit.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Commit c = (Commit) in.readObject();
            System.out.println(c);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * Adds files to MessageToId HashMap
     * @param message
     * @param id
     */
    private void addMessageAndIdToOther(String message, String id) {
        HashMap<String, HashSet<String>> messageToId;
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/messageToId.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            messageToId = readObject(in);
            if (!messageToId.containsKey(message)){
                messageToId.put(message,new HashSet<String>());
            }
            messageToId.get(message).add(id);
//            messageToId.put(message, id);

            FileOutputStream fileOut = new FileOutputStream(".gitlet/messageToId.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(messageToId);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
    }

    // stackoverflow.com/questions/1609933/unsafe-generic-cast-when-deserializing-a-collection
    @SuppressWarnings("unchecked")
    public static <T> T readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        return (T) in.readObject();
    }

    /**
     * Prints different branches, staged files, and files marked for removal
     */
    public void printStatus() {
        System.out.println("=== Branches ===");
        for (String branchName : branches.keySet()) {
            if (branchName.equals(currentBranch)) {
                String pr = "*" + ANSI_GREEN;
                pr += branchName + ANSI_RESET;
                System.out.println(pr);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        for (String stagedStuff : stagedFiles) {
            System.out.println(ANSI_GREEN + stagedStuff + ANSI_RESET);
        }
        System.out.println("");
        System.out.println("=== Files Marked for Removal ===");
        for (String rem : filesToRemove) {
            System.out.println(ANSI_RED + rem + ANSI_RESET);
        }


    }

    /**
     * Checkouts a specific File from a given ID
     * @param id
     * @param file
     */
    public void checkoutFileFromId(String id, String file) {
        Integer commit;
        try {
            commit = Integer.valueOf(id);
        } catch (NumberFormatException e) {
            System.out.println("argument was not an integer");
            return;
        }
        File commitDirectory = new File(".gitlet/" + commit);
        if (!commitDirectory.isDirectory()) {
            System.out.println("No commit with that id exists. ");
            return;
        }
        Commit c;
        try {
            FileInputStream filein = new FileInputStream(".gitlet/" + id);
            ObjectInputStream in = new ObjectInputStream(filein);
            c = (Commit) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        File source = new File(".gitlet/" + c.get(file) + "/" + file);
        if (!source.exists()) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File target = new File(file);
        try {
            Files.copy(source.toPath(), target.toPath(), REPLACE_EXISTING);
            target.setLastModified(source.lastModified());

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

    }

    /**
     * Checks whether a specific file is being checked out or a branch
     * @param name
     */
    public void checkoutBranchOrFile(String name) {
        if (branches.containsKey(name)) {
            revertBackToBranch(name, false);
        } else {
            revertFile(name);
        }
    }

    /**
     * reverts fileName back to last Commited State
     * @param fileName
     */
    private void revertFile(String fileName) {
        HashMap<String, String> inheritedFiles = getLatestInherited(currentBranch);
        if (!inheritedFiles.containsKey(fileName)) {
            System.out.println("File does not exist in the"
                    + " most recent commit, or no such branch exists.");
        } else {
            String iD = inheritedFiles.get(fileName);
            File source = new File(".gitlet/" + iD + "/" + fileName);
            File target = new File(fileName);
            try {
                Files.copy(source.toPath(), target.toPath(), REPLACE_EXISTING);
                target.setLastModified(source.lastModified());
                if (stagedFiles.contains(fileName)) {
                    stagedFiles.remove(fileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    /**
     * Reverts changes to a different branch.
     * @param branchName
     * @param isReset
     */
    private void revertBackToBranch(String branchName, boolean isReset) {
        if (branchName.equals(currentBranch) && !isReset) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        if (branches.get(branchName) == null) {
            System.out.println("File does not exist in the most recent commit"
                    + ", or no such branch exists.");
            return;
        }
        HashMap<String, String> inheritedFiles = getLatestInherited(branchName);
        for (String f : inheritedFiles.keySet()) {
            File source = new File(".gitlet/" + inheritedFiles.get(f) + "/" + f);
            File target = new File(f);
            try {
                Files.copy(source.toPath(), target.toPath(), REPLACE_EXISTING);
                target.setLastModified(source.lastModified());

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

        }
        currentBranch = branchName;
    }

    /**
     * gets the latest commit given the name of the branch
     * @param branchName
     * @return
     */
    private HashMap<String, String> getLatestInherited(String branchName) {
        Commit latestCommit;
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/"
                    + branches.get(branchName).getHead() + "/" + "commit.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            latestCommit = (Commit) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        return latestCommit.getAllFilesInCommit();
    }

    /**
     * creates a branch given a name. Checks whether branch already exists
     * @param name
     */
    public void createBranch(String name) {
        if (branches.containsKey(name)) {
            System.out.println("A branch with that name already exists.");
            return;
        } else {
            branches.put(name, branches.get(currentBranch));
        }
    }

    /**
     * removes a branch
     * @param branchName
     */
    public void removeBranch(String branchName) {
        if (currentBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else if (branches.containsKey(branchName)) {
            branches.remove(branchName);

        } else {
            System.out.println("A branch with that name does not exist.");
        }
    }

    /**
     * Moves the current branch pointer to the given id if it exists
     * @param id
     */
    public void reset(String id) {
        File f = new File(".gitlet/" + id);
        if (!f.isDirectory()) {
            System.out.println("No commit with that id exists.");
            return;
        }

        branches.put(currentBranch, idToCommitChain.get(id));
        revertBackToBranch(currentBranch, true);

    }

    /**
     * merge two branches.
     * Checks whether files exist in both branches, modifications, and whether the
     * current branch has removed a file shared in the split.
     * @param branchToMerge
     */
    public void merge(String branchToMerge) {
        if (!branches.containsKey(branchToMerge)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (branchToMerge.equals(currentBranch)) {
            System.out.printf("Cannot merge a branch with itself.");
            return;
        }
        String mostRecentSplit = findMostRecentSplit(currentBranch, branchToMerge);
        Commit splitPoint, currentBranchCommmit, mergeBranchCommit;
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/"
                    + branches.get(currentBranch).getHead() + "/commit.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            currentBranchCommmit = (Commit) in.readObject();
            in.close();
            fileIn.close();
            fileIn = new FileInputStream(".gitlet/"
                    + branches.get(branchToMerge).getHead() + "/commit.ser");
            in = new ObjectInputStream(fileIn);
            mergeBranchCommit = (Commit) in.readObject();
            in.close();
            fileIn.close();
            fileIn = new FileInputStream(".gitlet/" + mostRecentSplit + "/commit.ser");
            in = new ObjectInputStream(fileIn);
            splitPoint = (Commit) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
        for (String file : mergeBranchCommit.keySet()) {
            File split = new File(".gitlet/" + splitPoint.get(file) + "/" + file);
            File current = new File(".gitlet/" + currentBranchCommmit.get(file) + "/" + file);
            File merger = new File(".gitlet/" + mergeBranchCommit.get(file) + "/" + file);
            if (split.exists()) {
                if (checkFiles(merger, split)) {
                    split = null;

                } else if (!checkFiles(split, current) && current.exists()) {
                    File target = new File(file + ".conflicted");
                    try {
                        Files.copy(merger.toPath(), target.toPath(), REPLACE_EXISTING);
                        target.setLastModified(merger.lastModified());
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                } else {
                    try {
                        File target = new File(file);
                        Files.copy(merger.toPath(), target.toPath(), REPLACE_EXISTING);
                        target.setLastModified(merger.lastModified());
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            } else if (merger.exists() && current.exists()) {
                File target = new File(file + ".conflicted");
                try {
                    Files.copy(merger.toPath(), target.toPath(), REPLACE_EXISTING);
                    target.setLastModified(merger.lastModified());
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                try {
                    File source = new File(file);
                    Files.copy(merger.toPath(), source.toPath(), REPLACE_EXISTING);
                    current.setLastModified(merger.lastModified());
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    /**
     * returns the id of the split node two branches share
     *
     */
    private String findMostRecentSplit(String current, String branchToMerge) {
        KPLinkedList<String> l1 = branches.get(current);
        KPLinkedList<String> l2 = branches.get(branchToMerge);
        if (l1 == null || l2 == null) {
            System.out.println("branch not found");
            return null;
        }
        boolean matchFound = false;
        Commit c1;
        Commit c2;

        while (!matchFound) {
            try {
                FileInputStream fileIn = new FileInputStream(".gitlet/"
                        + l1.getHead() + "/commit.ser");
                ObjectInputStream in = new ObjectInputStream(fileIn);
                c1 = (Commit) in.readObject();
                fileIn = new FileInputStream(".gitlet/" + l2.getHead() + "/commit.ser");
                in = new ObjectInputStream(fileIn);
                c2 = (Commit) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
            if (l1 == null || l2 == null) {
                return null;
            } else if  (c1.getAccurateTime() == c2.getAccurateTime()) {
                matchFound = true;
            } else if (c1.getAccurateTime() > c2.getAccurateTime()) {
                l1 = l1.getBottom();
            } else {
                l2 = l2.getBottom();
            }
        }
        return l1.getHead();
    }

    /**
     * General rebase function that runs different functions based on boolean interactive
     *
     * @param branchName
     * @param time
     * @param interactive
     */
    public void rebase(String branchName, String time, boolean interactive) {
        if (!branches.containsKey(branchName)) {
            System.out.println("branch name not found");
            return;
        } else if (branchName.equals(currentBranch)) {
            System.out.println("Cannot rebase a branch onto itself.");
            return;
        } else if (findMostRecentSplit(currentBranch, branchName).equals(
                branches.get(currentBranch).getHead())) {
            branches.put(currentBranch, branches.get(currentBranch));
            System.out.println("Already up-to-date.");
        } else if (findMostRecentSplit(currentBranch, branchName).equals(
                branches.get(branchName).getHead())) {
            System.out.println("Already up-to-date.");
        }
        HashMap<String, String> newMessages = null;
        LinkedList<String> commit;
        if (interactive) {
            newMessages = new HashMap<>();
            commit = getCommitsToIRebase(currentBranch, branchName, newMessages);
            System.out.println(newMessages.size());
        } else {
            commit = getCommitsToRebase(currentBranch, branchName);

        }
        if (commit == null || commit.size() == 0) {
            System.out.println("Nothing to rebase");
            return;
        }

        KPLinkedList<String> originOfCurrent = branches.get(currentBranch);
        String originalName = currentBranch;
        currentBranch = branchName;
        rebaseHelper(commit, time, getSplitCommit(currentBranch, branchName) , newMessages);
        branches.put(originalName, branches.get(currentBranch));
        branches.put(currentBranch, originOfCurrent);
        currentBranch = originalName;
        reset(branches.get(currentBranch).getHead());
    }

    /**
     * Get the Commit Object of the most recent split node.
     *
     */
    private Commit getSplitCommit(String current, String branchName) {
        String splitId = findMostRecentSplit(current, branchName);
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/" + splitId + "/commit.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            return (Commit) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *Creates the additional commits based on the commitsToRebase LinkedList.
     * If file is not in the split node or has been modified since the split node, the
     * change will propagate into the rebased branch.
     *
     * @param commitsToRebase
     * @param time
     * @param split
     * @param newMessages
     */
    private void rebaseHelper(LinkedList<String> commitsToRebase, String time,
                              Commit split, HashMap<String, String> newMessages) {
        for (String i : commitsToRebase) {
            Commit c;
            try {
                FileInputStream fileIn = new FileInputStream(".gitlet/" + i + "/commit.ser");
                ObjectInputStream in = new ObjectInputStream(fileIn);
                c = (Commit) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }
            String commitID;
            if (newMessages != null && newMessages.containsKey(i)) {
                commitID =  createCommit(time, newMessages.get(i), true);
            } else {
                commitID = createCommit(time, c.getMessage(), true);
            }

            Commit newC;
            try {
                FileInputStream fileIn = new FileInputStream(".gitlet/" + commitID + "/commit.ser");
                ObjectInputStream in = new ObjectInputStream(fileIn);
                newC = (Commit) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }
            for (String file : c.keySet()) {
                if (!split.containsKey(file) || !split.get(file).equals(c.get(file))) {
                    newC.put(file, c.get(file));
                }

            }
            closeCommit(".gitlet/" + commitID + "/commit.ser", newC);


        }
    }

    /**
     * Serialized Commit c
     * @param path
     * @param c
     */
    private void closeCommit(String path, Commit c) {
        try {
            FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(c);
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    /**
     * Gets commits from the given branch up to the split node
     *

     */
    private LinkedList<String> getCommitsToRebase(String current, String branchName) {
        String split = findMostRecentSplit(current, branchName);
        LinkedList<String> retState = new LinkedList<>();
        KPLinkedList<String> traverse = branches.get(current);
        while (!traverse.getHead().equals(split)) {
            retState.addFirst(traverse.getHead());
            traverse = traverse.getBottom();
        }
        return retState;
    }

    /**
     * Asks user input on whether commit message will be changed, commit will be skiped
     * or commit will be rebased normally.
     *
     */
    private LinkedList<String> getCommitsToIRebase(String current, String branchName,
                                                   HashMap<String, String> messages) {
        String split = findMostRecentSplit(current, branchName);
        LinkedList<String> retState = new LinkedList<>();
        KPLinkedList<String> traverse = branches.get(current);
        String head = traverse.getHead();
        Scanner read = new Scanner(System.in);
        while (traverse.getHead() != split) {
            boolean redo = false;
            System.out.println("Currently replaying:");
            Commit c = getCommit(traverse.getHead());
            System.out.println(c);
            System.out.println("Would you like to (c)ontinue, "
                    + "(s)kip this commit, or change this commit's (m)essage?");
            String input = read.nextLine();
            String[] rawTokens = input.split(" ");
            if ("c".equals(rawTokens[0])) {
                retState.addFirst(traverse.getHead());

            } else if ("s".equals(rawTokens[0])) {
                if (traverse.getHead().equals(head)
                        || traverse.getBottom().getHead().equals(split)) {
                    redo = true;
                }

            } else if ("m".equals(rawTokens[0])) {
                System.out.println("Please enter a new message for this commit.");
                System.out.print(">");
                String newM = read.nextLine();
                messages.put(traverse.getHead(), newM);
                retState.addFirst(traverse.getHead());

            } else {
                System.out.println("Command not recognized.");
                redo = true;
            }
            if (!redo) {
                traverse = traverse.getBottom();
            }

        }
        return retState;

    }

    /**
     * Gets the Commit Object given commitId
     * @param head
     * @return
     */
    private Commit getCommit(String head) {
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/" + head + "/commit.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            return (Commit) in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks whether files are similar or different.
     * First checks existence, then size and last modified.
     * Lastly checks byte by byte if others fail to determine similarity
     * or difference.
     */
    private boolean checkFiles(File source, File target) {
        if (!source.exists() || !target.exists()) {
            return false;
        } else if (source.length() != target.length()) {
            return false;
        } else if (source.lastModified() == target.lastModified()) {
            return true;
        } else {
            try {
                byte[] sourceBytes = Files.readAllBytes(source.toPath());
                byte[] targetBytes = Files.readAllBytes(target.toPath());
                for (int i = 0; i < sourceBytes.length; i++) {
                    if (sourceBytes[i] != targetBytes[i]) {
                        return false;
                    }
                }
                return true;

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Adds remote to a the HashMap
     * @param extraCommands
     */
    public void addRemote(String[] extraCommands) {
        if (remotes.containsKey(extraCommands[0])) {
            System.out.println("A remote with that name already exists.");
            return;
        }
        String[] info = new String[3];
        System.arraycopy(extraCommands, 1, info, 0,  info.length);
        remotes.put(extraCommands[0], info);
    }

    /**
     * Removes a remote if it exists
     * @param remoteName
     */
    public void removeRemote(String remoteName) {
        if (!remotes.containsKey(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remotes.remove(remoteName);
    }

    /**
     * Attempts to push to remote.  Create directory if it
     * doesn't already exist.
     * @param remoteName
     * @param remoteBranchName
     * @param currentPath
     */
    public void push(String remoteName, String remoteBranchName, String currentPath) {
        if (!remotes.containsKey(remoteName)) {
            System.out.println("remote name does not exist.");
            return;
        }
        if (checkRemoteGitExists(remoteName)) {
            getRemoteHeadFile(remoteName, currentPath);
            GitletHead remoteHead = deSerialize(".gitlet/REMOTE_HEAD.ser");
            if (!remoteHead.branches.containsKey(remoteBranchName)) {
                copyOverCurrentBranch(remoteHead, remoteName, remoteBranchName, currentPath);
            } else {
                System.out.println(checkSameBranch(remoteHead, remoteBranchName));
                if (checkSameBranch(remoteHead, remoteBranchName)) {
                    copyOverCommitsFromCurrentBranch(remoteHead, remoteName,
                            remoteBranchName, currentPath);
                }
            }

            serializeRemote(remoteHead);
            sendRemoteandRename(remoteName, currentPath);
            File f = new File(".gitlet/REMOTE_HEAD.ser");
            f.delete();

        } else {
            copyOverGitletFolder(remoteName, currentPath);
        }
    }

    /**
     * Only has part of pull's implementation
     * @param remoteName
     * @param remoteBranchName
     * @param currentPath
     */
    public void pull(String remoteName, String remoteBranchName, String currentPath) {
        if (!remotes.containsKey(remoteName)) {
            return;
        }
        getRemoteHeadFile(remoteName, currentPath);
        GitletHead remoteHead = deSerialize(".gitlet/REMOTE_HEAD.ser");
        if (!remoteHead.branches.containsKey(remoteBranchName)) {
            System.out.println("That remote does not have that branch.");
            return;
        }
        copyFilesFromRemote(remoteHead, remoteName, remoteBranchName, currentPath);

        serializeRemote(remoteHead);
        sendRemoteandRename(remoteName, currentPath);
        File f = new File(".gitlet/REMOTE_HEAD.ser");
        f.delete();


    }

    /**
     * checks whether files need to be copied
     * @param remoteHead
     * @param remoteName
     * @param remoteBranchName
     * @param currentPath
     */
    private void copyFilesFromRemote(GitletHead remoteHead, String remoteName,
                                     String remoteBranchName, String currentPath) {
        if (inHistory(this, currentBranch, remoteHead, remoteBranchName)) {
            System.out.println("Already up-to-date.");
        } else if (inHistory(remoteHead, remoteBranchName, this, currentBranch)) {
            copyCommitsToLocal(remoteHead, remoteName, remoteBranchName, currentPath);
        }
    }

    /**
     * Copies directories from remote to local
     * @param remoteHead
     * @param remoteName
     * @param remoteBranchName
     * @param currentPath
     */
    private void copyCommitsToLocal(GitletHead remoteHead, String remoteName,
                                    String remoteBranchName, String currentPath) {
        KPLinkedList<String> traverse = remoteHead.branches.get(remoteBranchName);
        if (!idToCommitChain.containsKey(traverse.getHead())) {
            copyToLocal(remoteName, currentPath, traverse.getHead());
            idToCommitChain.put(traverse.getHead(), traverse);
            traverse = traverse.getBottom();
        }
        boolean notLastDifference = true;
        while (traverse != null && notLastDifference) {
            if (!remoteHead.idToCommitChain.containsKey(traverse.getHead())) {
                copyToLocal(remoteName, currentPath, traverse.getHead());
                idToCommitChain.put(traverse.getHead(),
                        remoteHead.idToCommitChain.get(traverse.getHead()));

            } else {
                notLastDifference = false;
            }
            traverse = traverse.getBottom();



        }
        branches.put(currentBranch, remoteHead.idToCommitChain.get(
                remoteHead.branches.get(remoteBranchName).getHead()));

    }

    /**
     * Runs the unix command for scp
     * @param remoteName
     * @param currentPath
     * @param head
     */
    private void copyToLocal(String remoteName, String currentPath, String head) {

        String[] info = remotes.get(remoteName);
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "scp -r " + info[0] + "@"
                    + info[1] + ":" + info[2] + ".gitlet/" + head + " " + currentPath);
            Process process = pb.start();
            InputStream is = process.getErrorStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            process.waitFor();
            String line = br.readLine();
            if (line != null) {
                return;
            }


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Checks whether a head is in the history of the "in"
     * @param in
     * @param inBranch
     * @param other
     * @param otherBranch
     * @return
     */
    private boolean inHistory(GitletHead in, String inBranch,
                              GitletHead other, String otherBranch) {
        KPLinkedList<String> inChain = in.branches.get(inBranch);
        String otherLatestId = other.branches.get(otherBranch).getHead();
        while (inChain != null) {
            if (inChain.getHead().equals(otherLatestId)) {
                return true;
            }
            inChain = inChain.getBottom();
        }
        return false;
    }

    /**
     * Checks for same branch
     * @param remoteHead
     * @param remoteBranchName
     * @return
     */
    private boolean checkSameBranch(GitletHead remoteHead, String remoteBranchName) {
        KPLinkedList<String> current = branches.get(currentBranch);
        String remote = remoteHead.branches.get(remoteBranchName).getHead();
        while (current != null) {
            if (current.getHead().equals(remote)) {
                return true;
            }
            current = current.getBottom();
        }
        System.out.println("Please pull down remote changes before pushing.");
        return false;
    }

    /**
     * Copies commits to the remote directory
     * @param remoteHead
     * @param remoteName
     * @param remoteBranchName
     * @param currentPath
     */
    private void copyOverCommitsFromCurrentBranch(GitletHead remoteHead, String remoteName,
                                                  String remoteBranchName, String currentPath) {
        KPLinkedList<String> traverse = branches.get(currentBranch);
        if (!remoteHead.idToCommitChain.containsKey(traverse.getHead())) {
            copyOverDirectory(remoteName, currentPath, traverse.getHead());
            remoteHead.idToCommitChain.put(traverse.getHead(), traverse);
            traverse = traverse.getBottom();
        }
        boolean notLastDifference = true;
        while (traverse != null && notLastDifference) {
            if (!remoteHead.idToCommitChain.containsKey(traverse.getHead())) {
                copyOverDirectory(remoteName, currentPath, traverse.getHead());
                remoteHead.idToCommitChain.put(traverse.getHead(),
                        idToCommitChain.get(traverse.getHead()));

            } else {
                notLastDifference = false;
            }
            traverse = traverse.getBottom();



        }
        remoteHead.branches.put(remoteBranchName,
                idToCommitChain.get(branches.get(currentBranch).getHead()));

    }

    /**
     * Copies whole branch to the remote repo
     * @param remote
     * @param remoteName
     * @param remoteBranchName
     * @param currentPath
     */
    private void copyOverCurrentBranch(GitletHead remote, String remoteName,
                                       String remoteBranchName, String currentPath) {
        KPLinkedList<String> traverse = branches.get(currentBranch);
        if (!remote.idToCommitChain.containsKey(traverse.getHead())) {
            copyOverDirectory(remoteName, currentPath, traverse.getHead());
            remote.idToCommitChain.put(traverse.getHead(),
                    idToCommitChain.get(traverse.getHead()));
            traverse = traverse.getBottom();
        }
        boolean notLastDifference = true;
        while (traverse != null && notLastDifference) {
            if (!remote.idToCommitChain.containsKey(traverse.getHead())) {
                copyOverDirectory(remoteName, currentPath, traverse.getHead());
                remote.idToCommitChain.put(traverse.getHead(),
                        idToCommitChain.get(traverse.getHead()));
            } else {
                notLastDifference = false;
            }
            traverse = traverse.getBottom();



        }
        remote.branches.put(remoteBranchName,
                idToCommitChain.get(branches.get(currentBranch).getHead()));

    }

    /**
     * Copies over specific commit
     * @param remoteName
     * @param currentPath
     * @param id
     */
    private void copyOverDirectory(String remoteName, String currentPath, String id) {
        String[] info = remotes.get(remoteName);
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "scp -r " + currentPath
                    + "/" + id + " " + info[0] + "@" + info[1] + ":" + info[2] + ".gitlet/");
            Process process = pb.start();
            InputStream is = process.getErrorStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            process.waitFor();
            String line = br.readLine();
            if (line != null) {
                return;
            }


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends remote HEAD.ser by copying it over and renaming it back to HEAD.ser
     * @param remoteName
     * @param currentPath
     */
    private void sendRemoteandRename(String remoteName, String currentPath) {
        String[] info = remotes.get(remoteName);
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", "scp " + currentPath
                    + "/REMOTE_HEAD.ser " + info[0] + "@" + info[1] + ":" + info[2] + ".gitlet/");
            Process process = pb.start();
            InputStream is = process.getErrorStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            process.waitFor();
            String line = br.readLine();
            if (line != null) {
                return;
            }
            pb = new ProcessBuilder("ssh", info[0] + "@" + info[1], "mv "
                    + info[2] + ".gitlet/REMOTE_HEAD.ser " + info[2] + ".gitlet/HEAD.ser");
            process = pb.start();
            is = process.getErrorStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            process.waitFor();
            line = br.readLine();
            if (line != null) {
                return;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Serializes REMOTE_HEAD.ser
     * @param remoteHead
     */
    private void serializeRemote(GitletHead remoteHead) {
        try {
            FileOutputStream fileOut = new FileOutputStream(".gitlet/REMOTE_HEAD.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(remoteHead);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deserializes REMOTE_HEAD.ser
     * @param file
     * @return
     */
    private GitletHead deSerialize(String file) {
        try {
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            return (GitletHead) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retieves remote's head by renaming it and scp'ing it
     * @param remoteName
     * @param currentPath
     */
    private void getRemoteHeadFile(String remoteName, String currentPath) {
        String[] info = remotes.get(remoteName);

        try {
            ProcessBuilder pb = new ProcessBuilder("ssh", info[0] + "@" + info[1], "mv "
                    + info[2] + ".gitlet/HEAD.ser " + info[2] + ".gitlet/REMOTE_HEAD.ser");
            Process process = pb.start();
            InputStream is = process.getErrorStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            process.waitFor();
            String line = br.readLine();
            if (line != null) {
                return;
            }
            pb = new ProcessBuilder("bash", "-c", "scp " + info[0] + "@" + info[1]
                    + ":" + info[2] + ".gitlet/REMOTE_HEAD.ser " + currentPath);
            process = pb.start();
            is = process.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            line = br.readLine();
            process.waitFor();
            if (line != null) {
                return;
            }


        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copy over whole gitlet repo
     * @param remoteName
     * @param currentPath
     */
    private void copyOverGitletFolder(String remoteName, String currentPath) {
        String[] info = remotes.get(remoteName);
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", "scp -r "
                + currentPath + " " + info[0] + "@" + info[1] + ":" + info[2]);
        Process process = null;
        try {
            process = pb.start();
            InputStream is = process.getErrorStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            process.waitFor();
            String line = br.readLine();
            if (line != null) {
                return;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks whether remote has a ".gitlet/" directory
     * @param remoteName
     * @return
     */
    private boolean checkRemoteGitExists(String remoteName) {
        String[] info = remotes.get(remoteName);
        ProcessBuilder pb = new ProcessBuilder("ssh", info[0]
                + "@" + info[1] , "cd " + info[2] + ".gitlet/");
        Process process = null;
        try {
            process = pb.start();
            InputStream is = process.getErrorStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            process.waitFor();
            String line = br.readLine();
            if (line != null) {
                return false;
            }
            return true;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;

    }

}


