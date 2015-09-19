
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by kishanpatel on 3/29/15.
 */

public class Commit implements Serializable {
    //"How to print color in console using System.out.println?" from Stack Overflow
    private static final long serialVersionUID = 42L;
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_YELLOW = "\u001B[33m";


    private String id;
    private String message;
    private String time;
    private HashMap<String, String> allFilesInCommit;
    private long accurateTime;
    private String parentMessageAndTime;
    private final  int a = 0xff;
    private final int b = 0x10;
    private final int c = 0xFF;



    public Commit(String m, String t) {
        message = m;
        time = t;
        allFilesInCommit = new HashMap<>();
        accurateTime = System.currentTimeMillis();
        parentMessageAndTime = "";
        id = hashCodeGenerator();
    }
    public String getId() {
        return id;
    }

    public String getParentMessageAndTime() {
        return parentMessageAndTime;
    }

    public void setParentMessageAndTime(String parentMessageAndTime) {
        this.parentMessageAndTime = parentMessageAndTime;
        id = hashCodeGenerator();
    }



    public String getMessage() {
        return message;
    }



    public HashMap<String, String> getAllFilesInCommit() {
        return allFilesInCommit;
    }

    public void setAllFilesInCommit(HashMap<String, String> allFilesInCommit) {
        this.allFilesInCommit = allFilesInCommit;
    }

    public String getTime() {
        return time;
    }
    

    @Override
    public String toString() {
        String s = ANSI_YELLOW + "Commit " + id + "." + ANSI_RESET;
        s += "\n" + time;
        s += "\n" + message;
        return s;
    }

    public void put(String s, String i) {
        allFilesInCommit.put(s, i);
    }

    public String remove(String s) {
        return allFilesInCommit.remove(s);
    }

    public String get(String s) {
        return allFilesInCommit.get(s);
    }

    public long getAccurateTime() {
        return accurateTime;
    }

    public boolean containsKey(String name) {
        return allFilesInCommit.containsKey(name);
    }

    public Set<String> keySet() {
        return allFilesInCommit.keySet();
    }
    //http://stackoverflow.com/questions/5470219/java-get-md5-string-from-message-digest
    public String hashCodeGenerator() {
        String stringToHash = message + accurateTime + parentMessageAndTime;

        StringBuffer hexString = new StringBuffer();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = md.digest(stringToHash.getBytes());
        for (int i = 0; i < hash.length; i++) {
            if ((a & hash[i]) < b) {
                hexString.append("0"
                        + Integer.toHexString((c & hash[i])));
            } else {
                hexString.append(Integer.toHexString(c & hash[i]));
            }
        }
        return hexString.toString();
    }

}
