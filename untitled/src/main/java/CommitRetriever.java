import model.Class;
import model.Release;
import model.Ticket;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

public class CommitRetriever {
    public static String projName = "BOOKKEEPER";
    public static LocalDateTime since = TicketRetriever.releases.get(0).getDate();
    public static LocalDateTime until = TicketRetriever.releases.get(TicketRetriever.releases.size() - 1).getDate();

    private static Repository repository;

    public static void retrieveCommits() throws IOException {

        System.out.println("------------- retrieving the commits -----------------");

        repository = new FileRepository("bookkeeper/.git");

        try (Git git = new Git(repository)) {

            List<RevCommit> commits = new ArrayList<>();
            List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(repository.resolve(branch.getName())).call();
                for (RevCommit commit : branchCommits) {
                    commits.add(commit);
                }
            }

            System.out.println(commits.size());

            for (RevCommit commit : commits) {
                System.out.println("---------------------------------------");
                System.out.println(commit.getAuthorIdent().getWhen());
                System.out.println(commit.getAuthorIdent());
                System.out.println(commit.getShortMessage());
                System.out.println(commit.getCommitTime());
                System.out.println(commit.getName());
                System.out.println(commit.getFooterLines());
            }

            for (int i = 0; i < 14; i++) {
                System.out.println(retrieveCommitsForRelease(commits, TicketRetriever.releases.get(i)).size());
            }

            for (Ticket ticket : TicketRetriever.tickets) {
                System.out.println("Commits associated to ticket " + ticket.key + " = " + getCommitsAssociatedToTicket(commits, ticket).size());
                if (Objects.equals(ticket.key, "BOOKKEEPER-1")) {
                    List<RevCommit> assComm = getCommitsAssociatedToTicket(commits, ticket);
                    for (int i = 0; i < assComm.size(); i++) {
                        System.out.println("\n");
                        System.out.println(assComm.get(i).getShortMessage());
                        System.out.println("\n");
                    }
                    System.out.println(getClassesFromCommit(assComm.get(0)).size());
                }
            }


        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<RevCommit> retrieveCommitsForRelease(List<RevCommit> allCommits, Release release) {

        List<RevCommit> releaseCommits = new ArrayList<>();

        LocalDateTime endDate = release.getDate();
        LocalDateTime startDate;
        if (release.getId() == 0)
            startDate = LocalDateTime.of(1970, Month.JANUARY, 1, 0, 0);
        else
            startDate = TicketRetriever.releases.get(release.getId() - 1).getDate();

        for (RevCommit commit : allCommits) {
            LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
            if (endDate.isAfter(commitDate) && startDate.isBefore(commitDate)) {
                releaseCommits.add(commit);
            }
        }

        return releaseCommits;
    }

    private static List<RevCommit> getCommitsAssociatedToTicket(List<RevCommit> allCommits, Ticket ticket) {
        List<RevCommit> associatedCommits = new ArrayList<>();

        for (RevCommit commit : allCommits) {
            if (commit.getFullMessage().contains(ticket.key + ":") && !associatedCommits.contains(commit)) {
                associatedCommits.add(commit);
            }

        }

        return associatedCommits;
    }

    private static HashMap<String, String> getClassesFromCommit(RevCommit commit) throws IOException {

        HashMap<String, String> classDescription = new HashMap<>();
        RevTree tree = commit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);

        while (treeWalk.next()) {
            if (treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test/")) {
                String path = treeWalk.getPathString();
                String content = new String(repository.open(treeWalk.getObjectId(0)).getBytes());
                classDescription.put(path, content);
            }
        }

        return classDescription;
    }

    private static List<Class> createAllClasses(List<RevCommit> allCommits) throws IOException {
        List<Class> classes = new ArrayList<>();

        for (RevCommit commit : allCommits) {
            HashMap<String, String> classDescription = getClassesFromCommit(commit);
            Class newClass = new Class(classDescription.keySet().toArray()[0].toString(), classDescription.values().toArray()[0].toString(), getReleaseFromCommit(commit));
        }

        return classes;
    }

    private static Release getReleaseFromCommit(RevCommit commit) {
        Release release;

        LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();


        return TicketRetriever.getRelease(commitDate);
    }
}

