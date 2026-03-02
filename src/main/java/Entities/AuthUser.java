package Entities;

public class AuthUser {
    private final String email;
    private final String fullName;
    private final String googleSub; // ID unique Google (sub)

    public AuthUser(String email, String fullName, String googleSub) {
        this.email = email;
        this.fullName = fullName;
        this.googleSub = googleSub;
    }

    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getGoogleSub() { return googleSub; }
}
