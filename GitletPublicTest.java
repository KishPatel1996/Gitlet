import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Class that provides JUnit tests for Gitlet, as well as a couple of utility
 * methods.
 *
 * @author Joseph Moghadam
 *
 *         Some code adapted from StackOverflow:
 *
 *         http://stackoverflow.com/questions
 *         /779519/delete-files-recursively-in-java
 *
 *         http://stackoverflow.com/questions/326390/how-to-create-a-java-string
 *         -from-the-contents-of-a-file
 *
 *         http://stackoverflow.com/questions/1119385/junit-test-for-system-out-
 *         println
 *
 */
public class GitletPublicTest {
    private static final String GITLET_DIR = ".gitlet/";
    private static final String TESTING_DIR = "test_files/";

    /* matches either unix/mac or windows line separators */
    private static final String LINE_SEPARATOR = "\r\n|[\r\n]";

    /**
     * Deletes existing gitlet system, resets the folder that stores files used
     * in testing.
     *
     * This method runs before every @Test method. This is important to enforce
     * that all tests are independent and do not interact with one another.
     */
    @Before
    public void setUp() {
        File f = new File(GITLET_DIR);
        if (f.exists()) {
            recursiveDelete(f);
        }
        f = new File(TESTING_DIR);
        if (f.exists()) {
            recursiveDelete(f);
        }
        f.mkdirs();
    }

    /**
     * Tests that init creates a .gitlet directory. Does NOT test that init
     * creates an initial commit, which is the other functionality of init.
     */
    @Test
    public void testBasicInitialize() {
        gitlet("init");
        File f = new File(GITLET_DIR);
        assertTrue(f.exists());
    }

    /**
     * Tests that checking out a file name will restore the version of the file
     * from the previous commit. Involves init, add, commit, and checkout.
     */
    @Test
    public void testBasicCheckout() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("checkout", wugFileName);
        assertEquals(wugText, getText(wugFileName));
    }

    /**
     * Tests that log prints out commit messages in the right order. Involves
     * init, add, commit, and log.
     */
    @Test
    public void testBasicLog() {
        gitlet("init");
        String commitMessage1 = "initial commit";

        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("add", wugFileName);
        String commitMessage2 = "added wug";
        gitlet("commit", commitMessage2);

        String logContent = gitlet("log");
        assertArrayEquals(new String[]{commitMessage2, commitMessage1},
                extractCommitMessages(logContent));
    }


//    @Test
//    public void testReset() {
//        gitlet("init");
//        String dogFile = TESTING_DIR + "dog.txt";
//        String dogText = "woof!";
//        createFile(dogFile, dogText);
//        String cM2 = "Added dog";
//        gitlet("add", dogFile);
//        gitlet("commit", cM2);
//        gitlet("branch", "b1");
//        gitlet("checkout", "b1");
//        String dogTextB1 = "Woof woof!";
//        writeFile(dogFile, dogTextB1);
//        String cM3 = "Added a woof";
//        gitlet("add", dogFile);
//        gitlet("commit", cM3);
//        String dogTextB2 = "woof woof woof!";
//        writeFile(dogFile, dogTextB2);
//        gitlet("add", dogFile);
//        String cM4 = "3 woofs";
//        gitlet("commit", cM4);
//        gitlet("checkout", "master");
//        assertEquals(dogText, getText(dogFile));
//        String masterDog1 = "I don't like woof";
//        writeFile(dogFile, masterDog1);
//        gitlet("add", dogFile);
//        String cM5 = "I don't like woofs";
//        String log = gitlet("log");
//        gitlet("commit",cM5);
//        gitlet("reset", "3");
//        assertEquals("woof woof woof!", getText(dogFile));
//        gitlet("reset", "1");
//        assertEquals("woof!", getText(dogFile));
//
//
//    }

    @Test
    public void mergeTest() {
        gitlet("init");
        String dogFile = TESTING_DIR + "dog.txt";
        String dogText = "woof";
        createFile(dogFile, dogText);
        String catFile = TESTING_DIR + "cat.txt";
        String catText = "meow";
        createFile(catFile, catText);
        String cowFile = TESTING_DIR + "cow.txt";
        String cowText = "moo";
        createFile(cowFile, cowText);
        String mouseFile = TESTING_DIR + "mouse.txt";
        String mouseText = "squeak";
        createFile(mouseFile, mouseText);
        gitlet("add", catFile);
        gitlet("add", dogFile);
        gitlet("add", mouseFile);
        String commit1 = "Added cat,dog,mouse";
        gitlet("commit", commit1);
        gitlet("branch", "branch1");
        gitlet("checkout", "branch1");
        gitlet("add", cowFile);
        String dogB1Text = "b1 dog";
        writeFile(dogFile, dogB1Text);
        String catB1Text = "b1 cat";
        writeFile(catFile, catB1Text);
        gitlet("add", dogFile);
        gitlet("add", catFile);
        String mouseB1Text = "b1 mouse";
        writeFile(mouseFile, mouseB1Text);
        gitlet("add", mouseFile);
        String b1Message = "Changed cat, dog, and added cow";
        gitlet("commit", b1Message);
        System.out.println(gitlet("checkout", "master"));
        gitlet("rm", mouseFile);
        System.out.println(getText(catFile));
        String masterCatText = "master cat";
        writeFile(catFile, masterCatText);
        gitlet("add", catFile);
        System.out.println(getText(catFile));
        String masterCommit = "Changed cat and removed mouse";
        gitlet("commit", masterCommit);
        System.out.println(gitlet("merge", "branch1"));

        assertEquals(masterCatText, getText(catFile));
        assertEquals(dogB1Text, getText(dogFile));
        assertEquals(mouseB1Text, getText(mouseFile));
        assertEquals(cowText, getText(cowFile));

    }

    @Test
    public void testGlobalAndBranches() {

        gitlet("init");
        String cM1 = "initial commit";
        String dogFile = TESTING_DIR + "dog.txt";
        String dogText = "woof!";
        createFile(dogFile, dogText);
        String cM2 = "Added dog";
        gitlet("add", dogFile);
        gitlet("commit", cM2);
        gitlet("branch", "b1");
        gitlet("checkout", "b1");
        String dogTextB1 = "Woof woof!";
        writeFile(dogFile, dogTextB1);
        String cM3 = "Added a woof";
        gitlet("add", dogFile);
        gitlet("commit", cM3);
        String dogTextB2 = "woof woof woof!";
        writeFile(dogFile, dogTextB2);
        gitlet("add", dogFile);
        String cM4 = "3 woofs";
        gitlet("commit", cM4);
        String log = gitlet("log");
        assertArrayEquals(new String[] {cM4,cM3,cM2,cM1}, extractCommitMessages(log));
        gitlet("checkout", "master");
        assertEquals(dogText, getText(dogFile));
        String masterDog1 = "I don't like woof";
        writeFile(dogFile, masterDog1);
        gitlet("add", dogFile);
        String cM5 = "I don't like woofs";
        gitlet("commit",cM5);
        log = gitlet("log");
        assertArrayEquals(new String[] {cM5, cM2, cM1}, extractCommitMessages(log));
        log = gitlet("global-log");

        assertArrayEquals(new String[] {cM5,cM4,cM3,cM2,cM1} , extractCommitMessages(log));

    }

    @Test
    public void testRebase() {
        gitlet("init");
        String dogFile = TESTING_DIR + "dog.txt";
        String dogText = "woof";
        createFile(dogFile, dogText);
        String catFile = TESTING_DIR + "cat.txt";
        String catText = "meow";
        createFile(catFile, catText);
        String cowFile = TESTING_DIR + "cow.txt";
        String cowText = "moo";
        createFile(cowFile, cowText);
        String mouseFile = TESTING_DIR + "mouse.txt";
        String mouseText = "squeak";
        createFile(mouseFile, mouseText);
        gitlet("add", catFile);
        gitlet("add", dogFile);
        gitlet("add", mouseFile);
        String commit1 = "Added cat,dog,mouse";
        gitlet("commit", commit1);
        gitlet("branch", "branch1");
        gitlet("checkout", "branch1");
        String catMessageB1 = "meow?";
        writeFile(catFile, catMessageB1);
        gitlet("add", catFile);
        String commit2M ="commit one of B1";
        gitlet("commit", commit2M);
        String catMessageB2 = "meow??????";
        writeFile(catFile, catMessageB2);
        gitlet("add", catFile);
        String commit3M = "commit two of B2";
        gitlet("commit", commit3M);
        gitlet("checkout","master");
        String dogChange = "Should see this dog.";
        writeFile(dogFile, dogChange);
        gitlet("add", dogFile);
        String catMessageMaster1 = "Masta cat";
        writeFile(catFile, catMessageMaster1);
        gitlet("add", catFile);
        String commit4M = "changed cat and dog";
        gitlet("commit", commit4M);
        gitlet("add", cowFile);
        String commit5M = "added cow";
        gitlet("commit", commit5M);
        gitlet("rebase", "branch1");
        String logContent = gitlet("log");
        assertArrayEquals(new String[]{commit5M, commit4M, commit3M, commit2M, commit1, "initial commit"}, extractCommitMessages(logContent));
        assertEquals(dogChange, getText(dogFile));
        assertEquals(catMessageMaster1, getText(catFile));

    }

    @Test
    public void hardMergeTest() {
        gitlet("init");
        String dogFile = TESTING_DIR + "dog.txt";
        String dogText = "woof";
        createFile(dogFile, dogText);
        String catFile = TESTING_DIR + "cat.txt";
        String catText = "meow";
        createFile(catFile, catText);
        String cowFile = TESTING_DIR + "cow.txt";
        String cowText = "moo";
        createFile(cowFile, cowText);
        String mouseFile = TESTING_DIR + "mouse.txt";
        String mouseText = "squeak";
        createFile(mouseFile, mouseText);
        String elephantFile = TESTING_DIR + "elephant.txt";
        String elephantText = "peanut";
        createFile(elephantFile, elephantText);
        String batFile = TESTING_DIR + "bat.txt";
        String batText = "echo echo";
        createFile(batFile, batText);
        String sharkFile = TESTING_DIR + "shark.txt";
        String sharkText = "swimmy swimmy";
        createFile(sharkFile, sharkText);
        String whaleFile = TESTING_DIR + "whale.txt";
        String whaleText = "splash";
        createFile(whaleFile, whaleText);
        gitlet("add", dogFile);
        gitlet("add", catFile);
        gitlet("add", cowFile);
        gitlet("add", mouseFile);
        gitlet("add", elephantFile);
        gitlet("commit", "added split node animals");
        gitlet("branch", "b1");

        String catMaster = "masta cat!";
        writeFile(catFile, catMaster);
        gitlet("add",catFile);
        String dogMaster = "masta dog!";
        writeFile(dogFile, dogMaster);
        gitlet("add", dogFile);
        gitlet("rm", elephantFile);
        gitlet("add", batFile);  //change bat in b1 to get conflicted
        gitlet("add", sharkFile);
        gitlet("commit", "added some files and changed some");

        gitlet("checkout", "b1");
        String catBranch = "brancha cat!";
        writeFile(catFile,catBranch);
        gitlet("add", catFile);
        String cowBranch = "brancha cow!";
        writeFile(cowFile, cowBranch);
        gitlet("add", cowFile);
        String batBranch = "brancha bat!";
        writeFile(batFile, batBranch);
        gitlet("add", batFile);
        String whaleBranch = "brancha whale";
        writeFile(whaleFile, whaleBranch);
        gitlet("add", whaleFile);
        String elephantBranch = "brancha elephant!";
        writeFile(elephantFile, elephantBranch);
        gitlet("add", elephantFile);
        gitlet("commit", "branch to merge is ready!");


        gitlet("checkout", "master");
        gitlet("merge", "b1");

        assertEquals(catMaster, getText(catFile));
        assertEquals(catBranch, getText(catFile + ".conflicted"));
        assertEquals(dogMaster, getText(dogFile));
        assertEquals(cowBranch, getText(cowFile));
        assertEquals(mouseText, getText(mouseFile));
        assertEquals(batText, getText(batFile));
        assertEquals(batBranch,getText(batFile + ".conflicted"));
        assertEquals(sharkText, getText(sharkFile));
        assertEquals(whaleBranch, getText(whaleFile));
        assertEquals(elephantBranch, getText(elephantFile));


    }

    /**
     * Convenience method for calling Gitlet's main. Anything that is printed
     * out during this call to main will NOT actually be printed out, but will
     * instead be returned as a string from this method.
     *
     * Prepares a 'yes' answer on System.in so as to automatically pass through
     * dangerous commands.
     *
     * The '...' syntax allows you to pass in an arbitrary number of String
     * arguments, which are packaged into a String[].
     */
    private static String gitlet(String... args) {
        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;
        ByteArrayOutputStream printingResults = new ByteArrayOutputStream();
        try {
            /*
             * Below we change System.out, so that when you call
             * System.out.println(), it won't print to the screen, but will
             * instead be added to the printingResults object.
             */
            System.setOut(new PrintStream(printingResults));

            /*
             * Prepares the answer "yes" on System.In, to pretend as if a user
             * will type "yes". You won't be able to take user input during this
             * time.
             */
            String answer = "yes";
            InputStream is = new ByteArrayInputStream(answer.getBytes());
            System.setIn(is);

            /* Calls the main method using the input arguments. */
            Gitlet.main(args);

        } finally {
            /*
             * Restores System.out and System.in (So you can print normally and
             * take user input normally again).
             */
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
        return printingResults.toString();
    }





    /**
     * Returns the text from a standard text file (won't work with special
     * characters).
     */
    private static String getText(String fileName) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(fileName));
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Creates a new file with the given fileName and gives it the text
     * fileText.
     */
    private static void createFile(String fileName, String fileText) {
        File f = new File(fileName);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writeFile(fileName, fileText);
    }

    /**
     * Replaces all text in the existing file with the given text.
     */
    private static void writeFile(String fileName, String fileText) {
        FileWriter fw = null;
        try {
            File f = new File(fileName);
            fw = new FileWriter(f, false);
            fw.write(fileText);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes the file and all files inside it, if it is a directory.
     */
    private static void recursiveDelete(File d) {
        if (d.isDirectory()) {
            for (File f : d.listFiles()) {
                recursiveDelete(f);
            }
        }
        d.delete();
    }

    /**
     * Returns an array of commit messages associated with what log has printed
     * out.
     */
    private static String[] extractCommitMessages(String logOutput) {
        String[] logChunks = logOutput.split("====");
        int numMessages = logChunks.length - 1;
        String[] messages = new String[numMessages];
        for (int i = 0; i < numMessages; i++) {
            System.out.println(logChunks[i + 1]);
            String[] logLines = logChunks[i + 1].split(LINE_SEPARATOR);
            messages[i] = logLines[3];
        }
        return messages;
    }
}
