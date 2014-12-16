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

    @ManyToOne
    @JoinColumn(name = "parent_index", referencedColumnName = "id")
    public File parent;

    public long size;

    @Constraints.Required
    public String service;

    @Constraints.Required
    public String filetype;

    @Constraints.Required
    public Long owner;

    @Formats.DateTime(pattern="dd/MM/yyyy")
    public Date createDate = new Date();

    public static Finder<Long,File> find = new Finder<Long,File>(
            Long.class, File.class
    );

    public String toString() {
        return filename;
    }

    public Long toLong() {
        return id;
    }

}