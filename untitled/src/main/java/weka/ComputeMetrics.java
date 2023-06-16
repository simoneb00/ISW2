package weka;

import exceptions.ExecutionException;
import model.Class;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComputeMetrics {
    private String projName;

    private void setSize(Class c) {
        Matcher m = Pattern.compile("\r\n|\r|\n").matcher(c.getImplementation());
        int lines = 0;
        while (m.find())
        {
            lines ++;
        }

        c.setSize(lines);
    }

    private void setNAuth(Class c) {
        Set<String> authors = new HashSet<>();

        for (RevCommit commit : c.getAssociatedCommits()) {
            authors.add(commit.getAuthorIdent().getName());
        }

        c.setnAuth(authors.size());
    }

    private void setFanOut(Class c) {
        String[] words = c.getImplementation().replace("\\p{Punct}", "").replace("\n", " ").split(" ");

        int count = 0;
        for (String word : words) {
            if (word.equals("import"))
                count++;
        }

        c.setFanOut(count);
    }

    private void setNR(Class c) {
        c.setnR(c.getAssociatedCommits().size());
    }

    private void setLOCAndChurn(Class c) throws IOException, ExecutionException {

        List<List<Integer>> locAddedAndDeleted = getLOCAddedAndDeleted(c);
        List<Integer> locAdded = locAddedAndDeleted.get(0);
        List<Integer> locDeleted = locAddedAndDeleted.get(1);

        // max LOC added
        c.setMaxLOCAdded(getMax(locAdded));

        // average LOC added
        c.setAverageLOCAdded(computeAverage(locAdded));

        // LOC added
        int sumLOCAdded = 0;
        for (int l : locAdded) {
            sumLOCAdded += l;
        }

        c.setLocAdded(sumLOCAdded);


        // Churn
        int churn = 0;

        for (int i = 0; i < locAdded.size(); i++) {
            churn += Math.abs(locAdded.get(i) - locDeleted.get(i));
        }

        c.setChurn(churn);

        List<Integer> churnValues = new ArrayList<>();

        for (int i = 0; i < locAdded.size(); i++) {
            churnValues.add(i, Math.abs(locAdded.get(i) - locDeleted.get(i)));
        }

        // max Churn
        c.setMaxChurn(getMax(churnValues));

        // average Churn
        c.setAverageChurn(computeAverage(churnValues));
    }


    /*   we're setting the time span in which the class has been modified,
     *   i.e. the length of the time interval [A, B], where A is the date of the first commit in which c is modified, and B is the date of the last commit in which c in modified
     */
    private void setTimeSpan(Class c) {
        List<LocalDateTime> commitsDates = new ArrayList<>();

        for (RevCommit commit : c.getAssociatedCommits()) {
            commitsDates.add(commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime());
        }

        if (commitsDates.size() <= 1) {
            c.setTimeSpan(0);
            return;
        }

        Collections.sort(commitsDates);

        c.setTimeSpan(ChronoUnit.DAYS.between(commitsDates.get(0), commitsDates.get(commitsDates.size() - 1)));
    }


    private int getMax(List<Integer> array) {
        int max = 0;

        for (int val : array) {
            if (val > max)
                max = val;
        }

        return max;
    }

    private float computeAverage(List<Integer> array) {

        if (array.isEmpty())
            return 0;

        int sum = 0;

        for (int val : array) {
            sum += val;
        }

        return (float) sum / array.size();
    }

    private List<List<Integer>> getLOCAddedAndDeleted(Class c) throws IOException, ExecutionException {

        List<RevCommit> commits = c.getAssociatedCommits();
        List<List<Integer>> locAddedAndDeleted = new ArrayList<>();
        List<Integer> locAdded = new ArrayList<>();
        List<Integer> locDeleted = new ArrayList<>();
        Repository repository = new FileRepository(projName.toLowerCase() + "/.git/");

        for (RevCommit commit : commits) {
            try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {  // we're not interested in the output
                diffFormatter.setRepository(repository);
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                diffFormatter.setDetectRenames(true);

                List<DiffEntry> diffEntries = diffFormatter.scan(commit.getParent(0).getTree(), commit.getTree());

                for (DiffEntry entry : diffEntries) {
                    if (entry.getNewPath().equals(c.getName())) {
                        int addedLOC = getAddedLines(diffFormatter, entry);
                        int deletedLOC = getDeletedLines(diffFormatter, entry);

                        locAdded.add(addedLOC);
                        locDeleted.add(deletedLOC);
                    }
                }

            } catch (ArrayIndexOutOfBoundsException e) {
                // ignore
            } catch (IOException e) {
                throw new ExecutionException(e);
            }
        }

        locAddedAndDeleted.add(0, locAdded);
        locAddedAndDeleted.add(1, locDeleted);

        return locAddedAndDeleted;
    }

    private int getAddedLines(DiffFormatter diffFormatter, DiffEntry diffEntry) throws IOException {
        int addedLOC = 0;

        for (Edit edit : diffFormatter.toFileHeader(diffEntry).toEditList()) {
            addedLOC += edit.getEndB() - edit.getBeginB();
        }

        return addedLOC;
    }

    private int getDeletedLines(DiffFormatter diffFormatter, DiffEntry diffEntry) throws IOException {
        int deletedLOC = 0;

        for (Edit edit : diffFormatter.toFileHeader(diffEntry).toEditList()) {
            deletedLOC += edit.getEndA() - edit.getBeginA();
        }

        return deletedLOC;
    }

    public void computeMetrics(List<Class> allClasses, String projName) throws IOException, ExecutionException {
        this.projName = projName;
        for (Class c : allClasses) {
            setSize(c);
            setNAuth(c);
            setNR(c);
            setLOCAndChurn(c);
            setFanOut(c);
            setTimeSpan(c);
        }
    }
}
