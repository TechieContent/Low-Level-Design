import java.util.*;
import java.time.*;

// Enum for Meeting Status
enum MeetingStatus { SCHEDULED, CANCELLED, COMPLETED }

// TimeSlot Class
class TimeSlot {
    private LocalDateTime start;
    private LocalDateTime end;

    public TimeSlot(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }

    // Simple overlap check
    public boolean conflictsWith(TimeSlot other) {
        // [start1,end1) & [start2,end2): overlap if start1 < end2 and start2 < end1
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    @Override
    public String toString() {
        return "[" + start + " - " + end + "]";
    }
}

// Notification class (optional for system extension)
class Notification {
    private String notificationId;
    private String message;
    private User recipient;
    private LocalDateTime notifiedAt;

    public Notification(String notificationId, String message, User recipient) {
        this.notificationId = notificationId;
        this.message = message;
        this.recipient = recipient;
        this.notifiedAt = LocalDateTime.now();
    }
}

// Meeting class
class Meeting {
    private String meetingId;
    private String title;
    private User organizer;
    private List<User> participants;
    private TimeSlot slot;
    private MeetingStatus status;

    public Meeting(String meetingId, String title, User organizer, List<User> participants, TimeSlot slot) {
        this.meetingId = meetingId;
        this.title = title;
        this.organizer = organizer;
        this.participants = new ArrayList<>(participants);
        this.slot = slot;
        this.status = MeetingStatus.SCHEDULED;
    }

    public String getTitle() { return title; }
    public List<User> getParticipants() { return participants; }
    public TimeSlot getSlot() { return slot; }
    public MeetingStatus getStatus() { return status; }
    public void setStatus(MeetingStatus status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Meeting[%s]: %s Organizer: %s, Slot: %s, Status: %s", 
            meetingId, title, organizer.getName(), slot, status);
    }
}

// User class
class User {
    private String userId;
    private String name;
    private String email;
    private List<User> contacts;
    private List<Meeting> meetings;

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.contacts = new ArrayList<>();
        this.meetings = new ArrayList<>();
    }

    public String getName() { return name; }
    public List<Meeting> getMeetings() { return meetings; }
    public List<User> getContacts() { return contacts; }

    public void addContact(User user) {
        if (!contacts.contains(user) && user != this) contacts.add(user);
    }

    public boolean isAvailable(TimeSlot slot) {
        for (Meeting meeting : meetings) {
            if (meeting.getStatus() == MeetingStatus.SCHEDULED && meeting.getSlot().conflictsWith(slot))
                return false;
        }
        return true;
    }

    // Schedule meeting: checks that all users are available
    public Meeting scheduleMeeting(String title, TimeSlot slot, List<User> participants) {
        // Add self as organizer/participant automatically
        Set<User> allParticipants = new HashSet<>(participants);
        allParticipants.add(this);

        // Conflict checking
        for (User u : allParticipants) {
            if (!u.isAvailable(slot)) {
                System.out.println("Cannot schedule: " + u.getName() + " is not available at " + slot);
                return null;
            }
        }

        Meeting meeting = new Meeting(UUID.randomUUID().toString(), title, this, new ArrayList<>(allParticipants), slot);
        for (User u : allParticipants)
            u.meetings.add(meeting);

        System.out.println("Scheduled meeting: " + title + " with " + allParticipants.size() + " participants at " + slot);
        return meeting;
    }
}

public class MeetingSchedulerDemo {
    public static void main(String[] args) {
        // Setup users
        User alice = new User("u1", "Alice", "alice@email.com");
        User bob = new User("u2", "Bob", "bob@email.com");
        User carol = new User("u3", "Carol", "carol@email.com");

        alice.addContact(bob);
        alice.addContact(carol);
        bob.addContact(alice);

        // Alice schedules a meeting with Bob and Carol
        TimeSlot slot1 = new TimeSlot(
            LocalDateTime.of(2024, 7, 1, 10, 0), 
            LocalDateTime.of(2024, 7, 1, 11, 0)
        );
        Meeting m1 = alice.scheduleMeeting("Team Sync", slot1, Arrays.asList(bob, carol));

        // Bob tries to schedule an overlapping meeting -- should fail
        TimeSlot slot2 = new TimeSlot(
            LocalDateTime.of(2024, 7, 1, 10, 30),
            LocalDateTime.of(2024, 7, 1, 11, 30)
        );
        Meeting m2 = bob.scheduleMeeting("Project Meeting", slot2, Arrays.asList(alice));

        // Carol schedules a non-conflicting meeting
        TimeSlot slot3 = new TimeSlot(
            LocalDateTime.of(2024, 7, 1, 11, 30),
            LocalDateTime.of(2024, 7, 1, 12, 30)
        );
        Meeting m3 = carol.scheduleMeeting("Lunch Catchup", slot3, Arrays.asList(alice));

        // View all scheduled meetings for Alice
        System.out.println("\nAlice's Meetings:");
        for (Meeting m : alice.getMeetings()) {
            System.out.println("  " + m);
        }
    }
}
