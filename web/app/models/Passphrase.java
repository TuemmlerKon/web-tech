package models;

import play.data.format.Formats;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Passphrase extends Model {

    @Id
    @Constraints.Min(10)
    public Long id;

    @Constraints.Required
    public String passphrase;

    public static Finder<Long, Passphrase> find = new Finder<Long, Passphrase>(
            Long.class, Passphrase.class
    );

    public Long getId() {
        return id;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
}