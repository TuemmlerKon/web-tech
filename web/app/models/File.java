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

    public Long getId() {
        return id;
    }

    public String toString() {
        return filename;
    }

    public Long toLong() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public File getParent() {
        return parent;
    }

    public void setParent(File parent) {
        this.parent = parent;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getFiletype() {
        return filetype;
    }

    public void setFiletype(String filetype) {
        this.filetype = filetype;
    }

    public Long getOwner() {
        return owner;
    }

    public void setOwner(Long owner) {
        this.owner = owner;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
}