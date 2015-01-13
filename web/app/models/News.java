package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity
public class News extends Model {

    @Id
    @Constraints.Min(10)
    public Long id;

    @Constraints.Required
    public String name;

    public String text;

    @Formats.DateTime(pattern="dd/MM/yyyy")
    @Constraints.Required
    public Date date = new Date();

    public String owner;

    public static Finder<Long,News> find = new Finder<Long,News>(
            Long.class, News.class
    );

    public String getText() {
        return text;
    }

    public String getName() {
        return name;
    }

    public Date getDate() {
        return date;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}