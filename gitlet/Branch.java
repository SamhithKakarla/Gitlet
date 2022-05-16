package gitlet;

import java.io.Serializable;
import java.util.Calendar;
import java.util.LinkedHashMap;

/**
 * Class to store branches.
 *
 * @author Samhith Kakarla
 */
public class Branch implements Serializable {
    /**
     * stores name.
     */
    private String _name;
    /**
     * stores head.
     */
    private String head;
    /**
     * stores the time.
     */
    private String time;
    /**
     * stores the commits.
     */
    private LinkedHashMap<String, String> commits;

    /**
     * initalizes the branch.
     *
     * @param name
     */
    public Branch(String name) {
        _name = name;
        commits = new LinkedHashMap<String, String>();
        Calendar cal = Calendar.getInstance();
        time = cal.getTime().toString();
    }

    /**
     * sets the head.
     *
     * @param shaID
     */
    public void setHead(String shaID) {
        commits.put(shaID, "a");
        this.head = shaID;
    }

    /**
     * gets the head.
     *
     * @return head
     */
    public String getHead() {
        return head;
    }

    /**
     * sets the head.
     *
     * @param h
     */
    public void setOnlyHead(String h) {
        head = h;
    }

    /**
     * returns the name.
     *
     * @return name
     */
    public String getName() {
        return _name;
    }

    /**
     * returns the commits.
     *
     * @return commits
     */
    public LinkedHashMap<String, String> getCommits() {
        return commits;
    }
    /**
     * sets the head.
     *
     * @param c
     */
    public void setCommits(LinkedHashMap<String, String> c) {
        commits = c;
    }

    @Override
    public String toString() {
        return _name + time;
    }

}
