import model.Class;
import model.Ticket;
import model.TicketCommit;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.InternalHttpServerGlue;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.NullOutputStream;
import utils.CommitUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComputeMetrics {
    private String projName;

    /*
     *  This class has the responsibility to compute all metrics for all classes
     *  The metrics here considered are:
     *  - size
     *  - NAuth
     *  - NFix
     *  - NR
     *  - LOC added
     *  - max LOC added
     *  - average LOC added
     *  - churn
     *  - maxChurn
     *  - averageChurn
     *
     */

    private void setSize(Class c) {
        Matcher m = Pattern.compile("\r\n|\r|\n").matcher(c.getImplementation());
        int lines = 1;
        while (m.find())
        {
            lines ++;
        }

        c.setSize(lines);
    }

    private void setNAuth(Class c) {
        Set<PersonIdent> authors = new HashSet<>();

        for (RevCommit commit : c.getAssociatedCommits()) {
            authors.add(commit.getAuthorIdent());
        }

        c.setNAuth(authors.size());
    }

    private void setNFix(Class c) {
    }

    private void setNR(Class c) {
        c.setNR(c.getAssociatedCommits().size());
    }

    private void setLOCAndChurn(Class c) throws IOException {

        List<List<Integer>> locAddedAndDeleted = getLOCAddedAndDeleted(c.getAssociatedCommits());
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

        c.setLOCAdded(sumLOCAdded);


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


    private void setAge(Class c) {
        c.setAge(c.getRelease().getId());
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

    private List<List<Integer>> getLOCAddedAndDeleted(List<RevCommit> commits) throws IOException {

        List<List<Integer>> locAddedAndDeleted = new ArrayList<>();
        List<Integer> locAdded = new ArrayList<>();
        List<Integer> locDeleted = new ArrayList<>();
        Repository repository = new FileRepository(projName.toLowerCase() + "/.git/");

        for (RevCommit commit : commits) {
            try (DiffFormatter diffFormatter = new DiffFormatter(NullOutputStream.INSTANCE)) {  // we're not interested in the output
                diffFormatter.setRepository(repository);
                diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
                diffFormatter.setDetectRenames(true);

                List<DiffEntry> diffEntries = diffFormatter.scan(commit.getParent(0).getTree(), commit.getTree());

                for (DiffEntry entry : diffEntries) {
                    for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
                        locAdded.add(edit.getEndB() - edit.getBeginB());
                        locDeleted.add(edit.getEndA() - edit.getBeginA());
                    }

                }

            } catch (ArrayIndexOutOfBoundsException e) {

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        locAddedAndDeleted.add(0, locAdded);
        locAddedAndDeleted.add(1, locDeleted);

        return locAddedAndDeleted;
    }

    public void computeMetrics(List<Class> allClasses, String projName) throws IOException {
        this.projName = projName;
        for (Class c : allClasses) {
            setSize(c);
            setNAuth(c);
            setNR(c);
            setNFix(c);
            setLOCAndChurn(c);
            setAge(c);
        }
    }
}
