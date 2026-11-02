package pl.booklist.exception;

/**
 * Thrown when someone tries to register a username that already exists.
 */
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Username already taken: " + username);
    }
}
