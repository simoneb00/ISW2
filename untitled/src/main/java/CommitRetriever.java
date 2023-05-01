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

    private static List<RevCommit> commits = new ArrayList<>();

    private static Repository repository;

    public static void retrieveCommits() throws IOException {

        System.out.println("------------- retrieving the commits -----------------");

        repository = new FileRepository("bookkeeper/.git");

        try (Git git = new Git(repository)) {

            /*
            *   retrieving all the commits
            *   ASSUMPTION: we're discarding the last half of releases, in order to have a smaller number of commits to handle
            */

            LocalDateTime lastRelease = TicketRetriever.releases.get(Math.round(TicketRetriever.releases.size()/2)).getDate();

            List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(repository.resolve(branch.getName())).call();
                for (RevCommit commit : branchCommits) {
                    LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
                    if (commitDate.isBefore(lastRelease) || commitDate.isEqual(lastRelease))
                        commits.add(commit);
                }
            }

            System.out.println(commits.size());

            System.out.println("\n\n\n\n\n\n\n\n\n\n");

            for (RevCommit commit : commits) {
                System.out.println(commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime());
            }

            System.out.println("\n\n\n\n\n\n\n\n\n\n");

            for (int i = 0; i < 14; i++) {
                System.out.println(retrieveCommitsForRelease(TicketRetriever.releases.get(i)).size());
            }

            for (Ticket ticket : TicketRetriever.tickets) {
                System.out.println("Commits associated to ticket " + ticket.key + " = " + getCommitsAssociatedToTicket(ticket).size());
                if (Objects.equals(ticket.key, "BOOKKEEPER-1")) {
                    List<RevCommit> assComm = getCommitsAssociatedToTicket(ticket);
                    for (int i = 0; i < assComm.size(); i++) {
                        System.out.println("\n");
                        System.out.println(assComm.get(i).getShortMessage());
                        System.out.println("\n");
                    }

                    List<Class> classes = createAllClasses(assComm);

                    for (int i = 0; i < classes.size(); i++) {
                        System.out.println("\n\n");
                        System.out.println(classes.get(i).getName() + "\n");
                        System.out.println(classes.get(i).getRelease().getName());
                        System.out.println(classes.get(i).getImplementation() + "\n");
                        System.out.println("\n\n");
                    }
                }
            }

            List<RevCommit> commitsAssToTickets = new ArrayList<>();

            for (Ticket ticket : TicketRetriever.tickets) {
                List<RevCommit> commitsAssToThisTicket = getCommitsAssociatedToTicket(ticket);
                commitsAssToTickets.addAll(commitsAssToThisTicket);
            }

            System.out.println(commitsAssToTickets.size());

            List<Class> allCommitsClasses = createAllClasses(commitsAssToTickets);
            System.out.println(allCommitsClasses.size());


        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<RevCommit> retrieveCommitsForRelease(Release release) {

        List<RevCommit> releaseCommits = new ArrayList<>();

        LocalDateTime endDate = release.getDate();
        LocalDateTime startDate;
        if (release.getId() == 0)
            startDate = LocalDateTime.of(1970, Month.JANUARY, 1, 0, 0);
        else
            startDate = TicketRetriever.releases.get(release.getId() - 1).getDate();

        for (RevCommit commit : commits) {
            LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
            if (endDate.isAfter(commitDate) && startDate.isBefore(commitDate)) {
                releaseCommits.add(commit);
            }
        }

        return releaseCommits;
    }

    private static List<RevCommit> getCommitsAssociatedToTicket(Ticket ticket) {
        List<RevCommit> associatedCommits = new ArrayList<>();

        for (RevCommit commit : commits) {
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
            HashMap<String, String> classesDescription = getClassesFromCommit(commit);
            for (int i = 0; i < classesDescription.size(); i++) {
                Class newClass = new Class(classesDescription.keySet().toArray()[i].toString(), classesDescription.values().toArray()[i].toString(), getReleaseFromCommit(commit));
                classes.add(newClass);
            }
        }

        return classes;
    }

    private static Release getReleaseFromCommit(RevCommit commit) {
        LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
        return TicketRetriever.getRelease(commitDate);
    }
}

