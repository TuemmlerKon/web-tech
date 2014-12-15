package models;

import play.data.validation.Constraints;

public class Login {
    @Constraints.Required
    public String email;
    @Constraints.Required
    public String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
