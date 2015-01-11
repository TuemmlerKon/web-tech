package models;

import controllers.Account;
import org.joda.time.DateTime;
import play.data.validation.Constraints;
import scala.math.BigInt;

import java.io.Serializable;


public class User implements Serializable{

    public static final String ROLE_DEFAULT  = "ROLE_USER";
    public static final String ROLE_ADMIN  = "ROLE_ADMIN";

    public Long id;
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
    public String roles;
    public Integer storage;
    public Integer used;
    private boolean invalid;

    public Integer getUsed() {
        return used;
    }

    public void setUsed(Integer used) {
        this.used = used;
    }

    public Integer getStorage() {
        return storage;
    }

    public void setStorage(Integer storage) {
        this.storage = storage;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void updateDone() {
        invalid = false;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    private boolean rmRole(User user, String role) {
        //schauen ob überhaupt was übergeben wurde
        if (role == null || role.isEmpty()) return false;
        //nun schauen wir ob es vllt. die default role ist (die darf nicht gelöscht werden)
        if (role.equals(User.ROLE_DEFAULT)) return false;
        //prüfen ob die ROLE überhaupt existiert
        //wenn ja geben wir trotzdem true zurück weil ja die ROLE nichtmehr da ist
        if (!hasRole(role)) return true;
        //ansonsten entfernen wir den INHALT aus der role
        setRoles(role.replace(";" + role.toUpperCase(), ""));
        //den Benutzer in der Datenbank updated
        Account.updateUser(this);

        return true;
    }

    private boolean addRole(String role) {
        //schauen ob überhaupt was übergeben wurde
        if (role == null || role.isEmpty()) return false;
        //prüfen ob die ROLE vielleicht schon existiert
        //wenn ja geben wir trotzdem true zurück weil ja die ROLE da steht und die Rechte existieren
        if (hasRole(role)) return true;
        //wenn die ROLE nicht drin ist, schreiben wir sie jetzt rein
        setRoles(role+";"+role.toUpperCase());
        //den Benutzer in der Datenbank updated
        Account.updateUser(this);

        return true;
    }

    public boolean hasRole(String role) {
        //schauen ob überhaupt was übergeben wurde
        if (role == null || role.isEmpty()) return false;
        //jetzt prüfen wir ob die ROLE in seinen Rollen steht
        return getRoles().contains(role.toUpperCase());
    }

    public boolean isAdmin() {
        return hasRole(User.ROLE_ADMIN);
    }

}
