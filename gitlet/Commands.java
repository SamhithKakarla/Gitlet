package gitlet;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;

public class Commands {

    /**
     * current branch.
     */
    private Branch currBranch;

    /**
     * list of branches.
     */
    private HashMap<String, String> branchList = new HashMap<>();

    /**
     * current head.
     */
    private Commit head;

    /**
     * staging area.
     */
    private HashMap<String, String> stagingArea;
    /**
     * remove-staging area.
     */
    private HashSet<String> removeStaging;

    /**
     * list of branch objects.
     */
    private ArrayList<Branch> branchObjs = new ArrayList<>();

    /**
     * Current working directory.
     */
    static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * folder to hold serialized data.
     */
    static final File GIT = new File(".gitlet");

    /**
     * commits folder.
     */
    static final File CMTS = Utils.join(GIT, ".commits");

    /**
     * staging area folder.
     */
    static final File ADD_STAGING = Utils.join(GIT, "add_staging_area");

    /**
     * remove staging folder.
     */
    static final File REMOVE_STAGING = Utils.join(GIT, "remove_staging_area");
    /**
     * branches folder.
     */
    static final File BRANCHES = Utils.join(GIT, "branches");
    /**
     * current branch folder.
     */
    static final File CURRENT = Utils.join(BRANCHES, "current");
    /**
     * BLOBS folder.
     */
    static final File BLOBS = Utils.join(GIT, "blobs");

    /**
     * sha len.
     */
    static final int SHALEN = 40;

    @SuppressWarnings("unchecked")
    public Commands() {
        try {
            stagingArea = (HashMap<String, String>)
                    Utils.readObject(ADD_STAGING, HashMap.class);
        } catch (IllegalArgumentException e) {
            stagingArea = new HashMap<String, String>();
        }

        try {
            for (File f : BRANCHES.listFiles()) {
                if (!f.getName().equals("current")) {
                    branchObjs.add(Utils.readObject(f, Branch.class));
                }
            }
        } catch (NullPointerException e) {
            branchObjs = new ArrayList<>();
        }

        try {
            for (Branch b : branchObjs) {
                branchList.put(b.getName(), Utils.sha1(b.toString()));
            }
        } catch (NullPointerException e) {
            branchList = new HashMap<String, String>();
        }

        try {
            removeStaging = (HashSet<String>)
                    Utils.readObject(REMOVE_STAGING, HashSet.class);
        } catch (IllegalArgumentException e) {
            removeStaging = new HashSet<>();
        }

        try {
            currBranch = (Branch) Utils.readObject(CURRENT, Branch.class);
        } catch (IllegalArgumentException e) {
            currBranch = new Branch("master");
        }

        try {
            try {
                try {
                    head = Utils.readObject
                    (Utils.join(CMTS, currBranch.getHead()), Commit.class);
                } catch (NullPointerException e) {
                    head = new Commit
                    ("empty", "empty", new HashMap<String, String>());
                }
            } catch (NoSuchElementException e) {
                head = new Commit
                ("empty", "empty", new HashMap<String, String>());
            }
        } catch (IllegalArgumentException e) {
            head = new Commit
            ("empty", "empty", new HashMap<String, String>());
        }


    }


    public void init() {
        if (!GIT.exists()) {
            GIT.mkdir();
            CMTS.mkdir();
            BRANCHES.mkdir();
            BLOBS.mkdir();
            try {
                ADD_STAGING.createNewFile();
                REMOVE_STAGING.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Branch master = new Branch("master");
            currBranch = master;
            Commit c1 = new Commit("initial commit",
                "Wed Dec 31 16:00:00 1969 -0800",
                    new HashMap<String, String>());
            currBranch.setHead(Utils.sha1(c1.toString()));
            head = c1;
            File cmt1 = Utils.join(CMTS, Utils.sha1(c1.toString()));
            File brnch1 = Utils.join(BRANCHES, Utils.sha1(master.toString()));

            try {
                CURRENT.createNewFile();
                cmt1.createNewFile();
                brnch1.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeObject(CURRENT, master);
            Utils.writeObject(cmt1, c1);
            Utils.writeObject(brnch1, master);

        } else {
            System.out.println("A Gitlet version-control "
                + "system already exists in the current directory.");
        }

    }

    public void add(String filename) {
        File temp = Utils.join(CWD, filename);
        if (temp.exists()) {
            Blob b = new Blob(filename);
            b.setContets(Utils.readContentsAsString(temp));
            if (removeStaging.contains(filename)) {
                removeStaging.remove(filename);
            }
            if (head.getFilenameToSha().containsKey(filename)) {
                if (head.getFilenameToSha().get(filename)
                        .equals(Utils.sha1(Utils.serialize(b)))) {
                    stagingArea.remove(filename);
                    Utils.writeObject(ADD_STAGING, stagingArea);
                    Utils.writeObject(REMOVE_STAGING, removeStaging);
                    return;
                }
            }
            stagingArea.put(filename, Utils.sha1(Utils.serialize(b)));
            File blob = Utils.join(BLOBS, Utils.sha1(Utils.serialize(b)));
            try {
                blob.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Utils.writeObject(blob, b);
            Utils.writeObject(ADD_STAGING, stagingArea);
            Utils.writeObject(REMOVE_STAGING, removeStaging);
        } else {
            System.out.println("File does not exist.");
        }

    }

    @SuppressWarnings("unchecked")
    public void commit(String message) {
        if (stagingArea.isEmpty() && removeStaging.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        HashMap<String, String> prevContents =
                (HashMap) head.getFilenameToSha().clone();
        if (stagingArea != null && !stagingArea.isEmpty()) {
            for (String name : stagingArea.keySet()) {
                prevContents.put(name, stagingArea.get(name));
            }
        }
        if (removeStaging != null && !removeStaging.isEmpty()) {
            for (String name : removeStaging) {
                prevContents.remove(name);
            }
        }
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf =
                new SimpleDateFormat("EE MMM dd kk:mm:ss YYYY Z");
        Commit toCom = new Commit(message,
                sdf.format(cal.getTime()), prevContents);
        if (message.length() > 5) {
            if (message.substring(0, 6).equals("Merged")) {
                String[] arr = message.split(" ");
                toCom.setMergeParent(Utils.readObject
                        (Utils.join(BRANCHES, branchList.get(arr[1])),
                                Branch.class).getHead());
            }
        }
        toCom.setParent(currBranch.getHead());
        String c = Utils.sha1(toCom.toString());
        File actual = Utils.join(BRANCHES, Utils.sha1(currBranch.toString()));
        Branch b = Utils.readObject(actual, Branch.class);
        b.setHead(c);
        currBranch.setHead(c);
        File temp = Utils.join(CMTS, c);
        try {
            temp.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeObject(temp, toCom);

        stagingArea.clear();
        removeStaging.clear();
        Utils.writeObject(ADD_STAGING, stagingArea);
        Utils.writeObject(REMOVE_STAGING, removeStaging);
        Utils.writeObject(CURRENT, currBranch);
        Utils.writeObject(actual, b);
    }

    public void checkout(String filename) {
        if (head.getFilenameToSha().containsKey(filename)) {
            File newf = Utils.join(CWD, filename);
            Blob temp = Utils.readObject(Utils.join(BLOBS,
                    head.getFilenameToSha().get(filename)), Blob.class);
            Utils.writeContents(newf, temp.getContents());
            try {
                newf.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    public void checkout(String commitId, String filename) {
        if (commitId.length() < SHALEN) {
            for (String s : currBranch.getCommits().keySet()) {
                if (commitId.equals(s.substring(0, commitId.length()))) {
                    commitId = s;
                }
            }
        }
        if (currBranch.getCommits().containsKey(commitId)) {
            Commit tempCom = Utils.readObject(
                    Utils.join(CMTS, commitId), Commit.class);
            if (tempCom.getFilenameToSha().containsKey(filename)) {
                Blob temp = Utils.readObject(Utils.join(
                        BLOBS, tempCom.getFilenameToSha().get(filename)),
                        Blob.class);
                File newf = Utils.join(CWD, filename);
                Utils.writeContents(newf, temp.getContents());
                try {
                    newf.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("File does not exist in that commit.");
            }
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    public void checkoutBranch(String branchname) {
        if (branchList.containsKey(branchname)) {
            if (!branchname.equals(currBranch.getName())) {
                Branch inQuestion = Utils.readObject(
                        Utils.join(BRANCHES, branchList.get(branchname)),
                        Branch.class);
                Commit lastCom = Utils.readObject(Utils.join(CMTS,
                        inQuestion.getHead()), Commit.class);
                File[] cwdFileList = CWD.listFiles();
                for (File f : cwdFileList) {
                    if (!f.getName().substring(0, 1).equals(".")
                            && !f.isDirectory()) {
                        if (!head.getFilenameToSha().containsKey(f.getName())
                                && !stagingArea.containsKey(f.getName())) {
                            System.out.println("There is an untracked"
                                    + " file in the way; delete it, or add "
                                    + "and commit it first.");
                            return;
                        }
                    }
                }
                for (String name : lastCom.getFilenameToSha().keySet()) {
                    File newf = Utils.join(CWD, name);
                    Blob temp = Utils.readObject(Utils.join
                            (BLOBS, lastCom.getFilenameToSha().get(name)),
                            Blob.class);
                    Utils.writeContents(newf, temp.getContents());
                    try {
                        newf.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                for (String name : head.getFilenameToSha().keySet()) {
                    if (!lastCom.getFilenameToSha().containsKey(name)) {
                        Utils.join(CWD, name).delete();
                    }
                }

                if (!currBranch.getName().equals(branchname)) {
                    stagingArea.clear();
                    removeStaging.clear();
                }
                currBranch = inQuestion;
                head = lastCom;

                Utils.writeObject(ADD_STAGING, stagingArea);
                Utils.writeObject(REMOVE_STAGING, removeStaging);
                Utils.writeObject(CURRENT, currBranch);
            } else {
                System.out.println("No need to checkout the current branch.");
            }
        } else {
            System.out.println("No such branch exists.");
        }
    }


    public void log() {
        List<String> keys = new ArrayList<String>(
                currBranch.getCommits().keySet());
        Collections.reverse(keys);
        boolean fc = false;
        for (String com : keys) {
            if (com.equals(currBranch.getHead())) {
                fc = true;
            }
            if (fc) {
                File f = Utils.join(CMTS, com);
                Commit temp = Utils.readObject(f, Commit.class);
                System.out.println(temp.logMessage());
                System.out.println();
            }
        }
    }

    public void globalLog() {
        for (File com : CMTS.listFiles()) {
            Commit temp = Utils.readObject(com, Commit.class);
            System.out.println(temp.logMessage());
            System.out.println();
        }
    }

    public void find(String message) {
        boolean found = false;
        for (File com : CMTS.listFiles()) {
            Commit temp = Utils.readObject(com, Commit.class);
            if (temp.getMessage().equals(message)) {
                System.out.println(Utils.sha1(temp.toString()));
                found = true;
            }
        }
        if (!found) {
            System.out.println(
                    "Found no commit with that message.");
        }
    }

    public void rm(String filename) {
        if (stagingArea.containsKey(filename)) {
            stagingArea.remove(filename);
        } else if (head.getFilenameToSha().containsKey(filename)) {
            removeStaging.add(filename);
            if (Utils.join(CWD, filename).exists()) {
                Utils.join(CWD, filename).delete();
            }
        } else {
            System.out.println("No reason to remove the file.");
        }
        Utils.writeObject(ADD_STAGING, stagingArea);
        Utils.writeObject(REMOVE_STAGING, removeStaging);
    }

    public void status() {
        if (!GIT.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        System.out.println("=== Branches ===");
        ArrayList<String> brs = new ArrayList<>();
        for (String a : branchList.keySet()) {
            brs.add(a);
        }
        Collections.sort(brs);
        for (String s : brs) {
            Branch b = Utils.readObject(
                    Utils.join(BRANCHES, branchList.get(s)), Branch.class);
            if (Utils.sha1(b.toString()).equals(
                    Utils.sha1(currBranch.toString()))) {
                System.out.println("*" + b.getName());
            } else {
                System.out.println(b.getName());
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        ArrayList<String> sa = new ArrayList<>();
        for (String a : stagingArea.keySet()) {
            sa.add(a);
        }
        Collections.sort(sa);
        for (String name : sa) {
            System.out.println(name);
        }

        System.out.println();

        System.out.println("=== Removed Files ===");
        ArrayList<String> ra = new ArrayList<>();
        for (Object a : removeStaging.toArray()) {
            ra.add(a.toString());
        }
        Collections.sort(ra);
        for (String name : ra) {
            System.out.println(name);
        }

        System.out.println();

        statusHelper(sa);
    }

    private void statusHelper(ArrayList<String> sa) {
        System.out.println("=== Modifications Not Staged For Commit ===");
        ArrayList<String> resultList = new ArrayList<>();
        for (String f : CWD.list()) {
            if (!f.substring(0, 1).equals(".")
                    && !Utils.join(CWD, f).isDirectory()) {
                if (head.getFilenameToSha().containsKey(f)
                        && !Utils.readContentsAsString(
                                Utils.join(CWD, f)).equals(Utils.readObject(
                                Utils.join(BLOBS, head.getFilenameToSha()
                                        .get(f)),
                                Blob.class).getContents())
                        && !stagingArea.containsKey(f)) {
                    resultList.add(f + "(modified)");
                } else if (stagingArea.containsKey(f)
                        && !Utils.readObject(Utils.join(BLOBS,
                        stagingArea.get(f)), Blob.class).getContents().equals(
                        Utils.readContentsAsString(Utils.join(CWD, f)))) {
                    resultList.add(f + "(modified)");
                }
            }
        }
        for (String name : sa) {
            if (!name.substring(0, 1).equals(".")
                    && !Utils.join(CWD, name).isDirectory()) {
                if (!Utils.join(CWD, name).exists()) {
                    resultList.add(name + "(deleted)");
                }
            }
        }
        for (String name : head.getFilenameToSha().keySet()) {
            if (!name.substring(0, 1).equals(".")
                    && !Utils.join(CWD, name).isDirectory()) {
                if (!removeStaging.contains(name)
                        && !Utils.join(CWD, name).exists()) {
                    resultList.add(name + "(deleted)");
                }
            }
        }
        Collections.sort(resultList);
        for (String toPrint : resultList) {
            System.out.println(toPrint);
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        ArrayList<String> cwdFiles = new ArrayList<>();
        for (String f : CWD.list()) {
            if (!Utils.join(CWD, f).isDirectory()
                    && !f.substring(0, 1).equals(".")) {
                cwdFiles.add(f);
            }
        }
        Collections.sort(cwdFiles);
        for (String file : cwdFiles) {
            if (!head.getFilenameToSha().containsKey(file)
                    && !stagingArea.containsKey(file)) {
                System.out.println(file);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void branch(String bName) {
        for (Branch b : branchObjs) {
            if (b.getName().equals(bName)) {
                System.out.println("A branch with that name already exists.");
                return;
            }
        }
        Branch b = new Branch(bName);
        b.setCommits((LinkedHashMap) currBranch.getCommits().clone());
        b.setOnlyHead(currBranch.getHead());
        Utils.writeObject(Utils.join(BRANCHES, Utils.sha1(b.toString())), b);
    }

    public void rmBranch(String branchName) {
        if (!branchList.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (branchName.equals(currBranch.getName())) {
            System.out.println("Cannot remove the current branch.");
            return;
        }

        Utils.join(BRANCHES, branchList.get(branchName)).delete();
        branchList.remove(branchName);
        for (int i = 0; i < branchObjs.size(); i++) {
            if (branchObjs.get(i).getName().equals(branchName)) {
                branchObjs.remove(i);
            }
        }
    }

    public void reset(String commitId) {
        if (commitId.length() < SHALEN) {
            for (String s : CMTS.list()) {
                if (commitId.equals(s.substring(0, commitId.length()))) {
                    commitId = s;
                }
            }
        }
        for (String f : CWD.list()) {
            if (!head.getFilenameToSha().containsKey(f)
                    && !stagingArea.containsKey(f) && !Utils.join(
                    CWD, f).isDirectory() && !f.substring(0, 1).equals(".")) {
                System.out.println("There is an untracked file in "
                        + "the way; delete it, or add and commit it first.");
                return;
            }
        }
        if (!Utils.join(CMTS, commitId).exists()) {
            System.out.println("No commit with that id exists.");
            return;
        } else {
            Commit newCom = Utils.readObject(
                    Utils.join(CMTS, commitId), Commit.class);
            for (String filename : newCom.getFilenameToSha().keySet()) {
                Blob temp = Utils.readObject(Utils.join(BLOBS,
                        newCom.getFilenameToSha().get(filename)), Blob.class);
                File newf = Utils.join(CWD, filename);
                Utils.writeContents(newf, temp.getContents());
                try {
                    newf.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            for (String f : stagingArea.keySet()) {
                if (!newCom.getFilenameToSha().containsKey(f)) {
                    Utils.join(CWD, f).delete();
                }
            }
            for (String f : head.getFilenameToSha().keySet()) {
                if (!newCom.getFilenameToSha().containsKey(f)) {
                    Utils.join(CWD, f).delete();
                }
            }
            currBranch.setOnlyHead(commitId);
            String ecb = "";
            for (String br : branchList.keySet()) {
                if (br.equals(currBranch.getName())) {
                    ecb = branchList.get(br);
                }
            }
            head = newCom;
            stagingArea.clear();
            removeStaging.clear();
            Utils.writeObject(REMOVE_STAGING, removeStaging);
            Utils.writeObject(ADD_STAGING, stagingArea);
            Utils.writeObject(CURRENT, currBranch);
            Utils.writeObject(Utils.join(BRANCHES, ecb), currBranch);
        }
    }

    public void merge(String branchName) {
        for (String f : CWD.list()) {
            if (!head.getFilenameToSha().containsKey(f)
                    && !stagingArea.containsKey(f) && !Utils.join(
                    CWD, f).isDirectory() && !f.substring(0, 1).equals(".")) {
                System.out.println("There is an untracked file in the"
                        + " way; delete it, or add and commit it first.");
                return;
            }
        }
        if (!stagingArea.isEmpty() || !removeStaging.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (!branchList.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (currBranch.getName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Branch other = Utils.readObject(Utils.join(
                BRANCHES, branchList.get(branchName)), Branch.class);
        Commit splitPoint = findSplitPoint(branchName, other);
        mergeHelper(splitPoint, other, branchName);
    }

    private Commit findSplitPoint(String branchName, Branch other) {
        Commit splitPoint = new Commit("troll commit",
                "0", new HashMap<>());
        HashSet<String> parentSet = new HashSet<>();
        ArrayList<String> queue = new ArrayList<>();
        queue.add(currBranch.getHead());
        while (queue.size() > 0) {
            if (!Utils.join(CMTS, queue.get(0)).isDirectory()) {
                parentSet.add(queue.get(0));
                Commit temp = Utils.readObject(Utils.join(
                        CMTS, queue.get(0)), Commit.class);
                if (!temp.getMergeParent().equals("")) {
                    queue.add(temp.getMergeParent());
                }
                if (!temp.getParent().equals("")) {
                    queue.add(temp.getParent());
                }
            }
            queue.remove(0);
        }
        ArrayList<String> queue2 = new ArrayList<>();
        queue2.add(other.getHead());
        while (queue2.size() > 0) {
            if (!Utils.join(CMTS, queue2.get(0)).isDirectory()) {
                Commit temp = Utils.readObject(Utils.join(
                        CMTS, queue2.get(0)), Commit.class);
                if (parentSet.contains(queue2.get(0))) {
                    splitPoint = temp;
                    break;
                }
                if (temp.getMergeParent().equals("")) {
                    queue2.add(temp.getParent());
                } else {
                    queue2.add(temp.getMergeParent());
                    queue2.add(temp.getParent());
                }
            }
            queue2.remove(0);
        }

        return splitPoint;
    }

    private void mergeHelper(Commit splitPoint, Branch other,
                             String branchName) {
        if (Utils.sha1(splitPoint.toString()).equals(
                other.getHead())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        }
        if (Utils.sha1(splitPoint.toString()).equals(
                Utils.sha1(head.toString()))) {
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(branchName);
            return;
        }
        Commit otherHead = Utils.readObject(Utils.join(
                CMTS, other.getHead()), Commit.class);
        for (String name : otherHead.getFilenameToSha().keySet()) {
            if (!splitPoint.getFilenameToSha().containsKey(name)
                    && !head.getFilenameToSha().containsKey(name)) {
                Blob temp = Utils.readObject(Utils.join(BLOBS,
                        otherHead.getFilenameToSha().get(name)), Blob.class);
                File newf = Utils.join(CWD, name);
                Utils.writeContents(newf, temp.getContents());
                try {
                    newf.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                stagingArea.put(name, Utils.sha1(Utils.serialize(temp)));
            } else if (splitPoint.getFilenameToSha().containsKey(name)
                    && head.getFilenameToSha().containsKey(name)) {
                insideHelper(otherHead, name, splitPoint);
            } else if (!splitPoint.getFilenameToSha().containsKey(name)
                    && !otherHead.getFilenameToSha().get(name).
                    equals(head.getFilenameToSha().get(name))) {
                String toPrint = "<<<<<<< HEAD\n"
                        + Utils.readObject(Utils.join(BLOBS, head.
                        getFilenameToSha().
                        get(name)), Blob.class).getContents()
                        + "=======\n"
                        + Utils.readObject(Utils.join(BLOBS, otherHead.
                        getFilenameToSha().get(name)), Blob.class).getContents()
                        + ">>>>>>>\n";
                Utils.writeContents(Utils.join(CWD, name), toPrint);
                System.out.println("Encountered a merge conflict.");
                add(name);
            }
        }
        mergeHelper2(otherHead, splitPoint, branchName);
    }

    private void insideHelper(Commit otherHead, String name,
                              Commit splitPoint) {
        if (!otherHead.getFilenameToSha().get(name).equals(
                splitPoint.getFilenameToSha().get(name))
                && head.getFilenameToSha().get(name).equals(
                        splitPoint.getFilenameToSha().get(name))) {
            Blob temp = Utils.readObject(Utils.join(BLOBS,
                    otherHead.getFilenameToSha().get(name)), Blob.class);
            File newf = Utils.join(CWD, name);
            Utils.writeContents(newf, temp.getContents());
            try {
                newf.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            stagingArea.put(name, Utils.sha1(Utils.serialize(temp)));
        } else if (!otherHead.getFilenameToSha().get(name).equals(
                splitPoint.getFilenameToSha().get(name)) && !head.
                getFilenameToSha().get(
                name).equals(splitPoint.getFilenameToSha().get(name))) {
            String toPrint = "<<<<<<< HEAD\n"
                    + Utils.readObject(Utils.join(BLOBS, head.getFilenameToSha()
                    .get(name)), Blob.class).getContents()
                    + "=======\n"
                    + Utils.readObject(Utils.join(BLOBS, otherHead.
                    getFilenameToSha().get(name)), Blob.class).getContents()
                    + ">>>>>>>\n";
            Utils.writeContents(Utils.join(CWD, name), toPrint);
            System.out.println("Encountered a merge conflict.");
            add(name);

        }
    }

    private void mergeHelper2(Commit otherHead, Commit splitPoint,
                              String branchName) {
        for (String name : splitPoint.getFilenameToSha().keySet()) {
            if (splitPoint.getFilenameToSha().get(name).equals(
                    head.getFilenameToSha().get(name))
                    && !otherHead.getFilenameToSha().containsKey(name)) {
                if (Utils.join(CWD, name).exists()) {
                    Utils.join(CWD, name).delete();
                    removeStaging.add(name);
                }
            } else if (!splitPoint.getFilenameToSha().get(name).equals(
                    head.getFilenameToSha().get(name)) && !otherHead.
                    getFilenameToSha().containsKey(name) && BLOBS != null
                    && head.getFilenameToSha().get(name) != null) {
                String toPrint = "<<<<<<< HEAD\n"
                        + Utils.readObject(Utils.join(BLOBS, head.
                        getFilenameToSha()
                                .get(name)), Blob.class).getContents()
                        + "=======\n"
                        + ">>>>>>>\n";
                Utils.writeContents(Utils.join(CWD, name), toPrint);
                System.out.println("Encountered a merge conflict.");
                add(name);
            }
        }

        commit("Merged " + branchName + " into " + currBranch.getName() + ".");


    }


}
