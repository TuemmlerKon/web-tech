package models;

import org.joda.time.DateTime;
import play.data.validation.Constraints;


public class User {
    @Constraints.Required
    public String prename;
    @Constraints.Required
    public String surname;
    @Constraints.Required
    public String email;
    public DateTime createdate;
    public DateTime lastlogin;
    @Constraints.Required
    public String password;
    public String password2;

    public DateTime getCreatedate() {
        return createdate;
    }

    public void setCreatedate(DateTime createdate) {
        this.createdate = createdate;
    }

    public DateTime getLastlogin() {
        return lastlogin;
    }

    public void setLastlogin(DateTime lastlogin) {
        this.lastlogin = lastlogin;
    }

    public String getPrename() {
        return prename;
    }

    public void setPrename(String prename) {
        this.prename = prename;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

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

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }
}
