package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity
public class File extends Model {

    @Id
    @Constraints.Min(10)
    public Long id;

    @Constraints.Required
    public String filename;

    @Constraints.Required
    public long size;

    @Constraints.Required
    public String service;

    @Formats.DateTime(pattern="dd/MM/yyyy")
    public Date createDate = new Date();

    public static Finder<Long,File> find = new Finder<Long,File>(
            Long.class, File.class
    );

}