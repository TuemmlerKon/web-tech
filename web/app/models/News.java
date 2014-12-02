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
    public Date date = new Date();

    public static Finder<Long,News> find = new Finder<Long,News>(
            Long.class, News.class
    );

}