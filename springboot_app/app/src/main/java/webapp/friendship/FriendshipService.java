package webapp.friendship;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies F13 transitions only to the caller's pair, equally for USER and ADMIN. */
@Service
public class FriendshipService {
    private final FriendshipRepository repository;

    FriendshipService(FriendshipRepository repository) { this.repository = repository; }

    @Transactional
    public FriendshipView send(int actor, int other) {
        var person = users(actor, other, true);
        if (repository.insert(actor, other) == 0) {
            var pair = repository.findPair(actor, other).orElseThrow(FriendshipService::internal);
            if (pair.status().equals("accepted")) throw alreadyFriends();
            throw new FriendshipFailure(409, pair.requester() == actor
                    ? "FRIEND_REQUEST_ALREADY_EXISTS" : "INCOMING_REQUEST_EXISTS",
                    "A friend request already exists for this pair.");
        }
        return view(actor, person, repository.findPair(actor, other).orElseThrow(FriendshipService::internal));
    }

    @Transactional
    public FriendshipView accept(int actor, int other) {
        var person = users(actor, other, true);
        var pair = repository.findPair(actor, other).orElseThrow(() -> absent(false));
        if (pair.requester() == actor) throw new FriendshipFailure(403, "FORBIDDEN", "Only the recipient can accept a request.");
        if (pair.status().equals("accepted")) throw alreadyFriends();
        if (repository.accept(actor, other) != 1) throw internal();
        return view(actor, person, repository.findPair(actor, other).orElseThrow(FriendshipService::internal));
    }

    @Transactional
    public void delete(int actor, int other, boolean friends) {
        users(actor, other, true);
        var pair = repository.findPair(actor, other).orElseThrow(() -> absent(friends));
        String expected = friends ? "accepted" : "pending";
        if (!pair.status().equals(expected)) {
            throw new FriendshipFailure(409, friends ? "FRIENDSHIP_NOT_ACCEPTED" : "FRIEND_REQUEST_NOT_PENDING",
                    "The relationship is not in the required state.");
        }
        if (repository.delete(actor, other, expected) != 1) throw internal();
    }

    @Transactional(readOnly = true)
    public FriendshipView get(int actor, int other) {
        var person = users(actor, other, false);
        return view(actor, person, repository.findPair(actor, other).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<FriendshipView> list(int actor, String direction) {
        activeActor(repository.findUser(actor).orElse(null));
        return repository.list(actor, direction);
    }

    private FriendshipRepository.Person users(int actor, int other, boolean lock) {
        if (other <= 0 || actor == other) throw new FriendshipFailure(422, "VALIDATION_ERROR", "Choose another user with a positive ID.");
        FriendshipRepository.Person target;
        if (lock) {
            var people = repository.lockUsers(actor, other);
            activeActor(people.stream().filter(p -> p.id() == actor).findFirst().orElse(null));
            target = people.stream().filter(p -> p.id() == other).findFirst().orElse(null);
        } else {
            activeActor(repository.findUser(actor).orElse(null));
            target = repository.findUser(other).orElse(null);
        }
        if (target == null || target.deleted()) throw new FriendshipFailure(404, "USER_NOT_FOUND", "User not found.");
        return target;
    }

    private static void activeActor(FriendshipRepository.Person person) {
        if (person == null || person.deleted()) throw new FriendshipFailure(401, "UNAUTHORIZED", "Authentication is required.");
    }

    private static FriendshipView view(int actor, FriendshipRepository.Person other, FriendshipRepository.Pair pair) {
        String state = pair == null ? "none" : pair.status().equals("accepted") ? "friends"
                : pair.requester() == actor ? "outgoing" : "incoming";
        return new FriendshipView(other.id(), other.nickname(), other.iconUrl(), state,
                pair == null ? null : pair.createdAt(), pair == null ? null : pair.acceptedAt());
    }

    private static FriendshipFailure alreadyFriends() {
        return new FriendshipFailure(409, "ALREADY_FRIENDS", "Friendship is already accepted.");
    }

    private static FriendshipFailure absent(boolean friends) {
        return new FriendshipFailure(404, friends ? "FRIENDSHIP_NOT_FOUND" : "FRIEND_REQUEST_NOT_FOUND", "Relationship not found.");
    }

    private static FriendshipFailure internal() {
        return new FriendshipFailure(500, "INTERNAL_ERROR", "The request could not be completed.");
    }
}
