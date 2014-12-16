package controllers;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.File;
import models.User;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.BodyParser;

import java.util.Date;
import java.util.List;

public class Filesystem extends Controller {

    public static final String FILETYPE_FILE = "file";
    public static final String FILETYPE_FOLDER = "folder";

    public static Logger.ALogger logger = Logger.of("application.controllers.Filesystem");

    public static Result index() {
        User user = Account.getCurrentUser();

        if(user == null) {
            logger.debug("Filesystem: User unauthenticated");
            return redirect(controllers.routes.Account.login());
        }

        File dir = getCWD();
        String cwd = null;
        if (dir != null) {
            cwd = dir.id.toString();
        }

        List<File> list = File.find.where().eq("owner", user.getId().toString()).eq("parent_index", cwd).findList();

        return ok(views.html.filesystem.index.render(Messages.get("application.general.myfiles"), list));
    }

    public static Result cwd(Long folder) {

        User user = Account.getCurrentUser();

        if(user == null) {
            logger.debug("Filesystem: User unauthenticated");
            return redirect(controllers.routes.Account.login());
        }

        String cwd = "0";

        //Spezialfall: Wenn der Ordner 0 angefordert wird, wechseln wir direkt wieder ins ROOT-Verzeichnis
        if(folder != 0) {
            File file = getFolder(folder);
            if (file == null) {
                flash("error", Messages.get("filesystem.folder.notfound"));
                logger.debug("Filesystem: Folder not found");
                return redirect(controllers.routes.Filesystem.index());
            }
            //setzen des CWD auf die entsprechende ID
            cwd = file.id.toString();
        }

        logger.debug("Filesystem: Changed directory to "+folder);
        //wenn der Ordner doch existiert setzen wir in der Session die entsprechende ID als Ordner
        session("cwd", cwd);

        return redirect(controllers.routes.Filesystem.index());
    }

    public static Result download(Long file) {
        return redirect(controllers.routes.Filesystem.index());
    }

    public static File getCWD() {
        String cwd = session("cwd");
        return getFolder(Long.parseLong(cwd == null ? "0" : cwd));
    }

    private static File getItem(Long id) {
        //Wenn die ID 0 mitgegeben wird, dann handelt es sich immer um das ROOT-Verzeichnis des Benutzers
        if (id == 0) {
            logger.debug("Filesystem: Requesting root directory! Returning null");
            return null;
        }

        File file = File.find.byId(id);
        User user = Account.getCurrentUser();
        //prüfen ob es überhaupt ein Ergebnis gab
        if(file == null) {
            logger.debug("Filesystem: Item ("+id.toString()+") not found in database");
            return null;
        }

        //Prüfen ob dem Benutzer der Ordner gehört
        if(!file.owner.equals(user.getId())) {
            //wenn die Besitzer nicht gleich sind, dann machen wir einfach null draus und geben das zurück
            //Fehlerbehandlung muss in der entsprechenden Aktion erfolgen
            logger.debug("Filesystem: Owner does not match! Is: "+file.owner+" Should: "+user.getId());
            return null;
        }

        return file;
    }

    private static File getFolder(Long id) {
        File folder =  getItem(id);
        //prüfen ob es sich um eine Datei handelt
        if (folder == null || !folder.filetype.equals(FILETYPE_FOLDER)) {
            //wenn null zurückgegeben wurde (Besitzer falsch oder nicht gefunden)
            //oder wenn der Dateityp kein Ordner ist
            logger.debug("Filesystem: Item is no folder");
            return null;
        }
        //ansonsten geben wir die Datei zurück
        return folder;
    }

    private static File getFile(Long id) {
        File file =  getItem(id);
        //prüfen ob es sich um eine Datei handelt
        if (file == null || !file.filetype.equals(FILETYPE_FILE)) {
            //wenn null zurückgegeben wurde (Besitzer falsch oder nicht gefunden)
            //oder wenn der Dateityp keine Datei ist
            logger.debug("Filesystem: Item is no file");
            return null;
        }
        //ansonsten geben wir die Datei zurück
        return file;
    }

    public static Result newFolder() {

        User user = Account.getCurrentUser();

        if(user == null) {
            logger.debug("Filesystem: User unauthenticated");
            return redirect(controllers.routes.Account.login());
        }

        DynamicForm requestData = Form.form().bindFromRequest();
        String foldername = requestData.get("foldername");
        //zuerst müssen wir schauen ob überhaupt etwas übertragen wurde
        if (foldername == null) {
            flash("error", Messages.get("filesystem.folder.error"));
            logger.debug("Filesystem: User submitted empty folder name or an error occured");
            return redirect(controllers.routes.Filesystem.index());
        }

        File dir = getCWD();
        String cwd = null;
        if (dir != null) {
            cwd = dir.id.toString();
        }

        //jetzt müssen wir schauen, ob es evtl. schon einen Ordner mit diesem Namen gibt
        List<File> folders = File.find.where().eq("owner", user.getId()).eq("filetype", FILETYPE_FOLDER).eq("filename", foldername).eq("parent_index", cwd).findList();
        //wenn in der Liste keine Objekte liegen, dann können wir den Ordner anlegen
        if (folders.isEmpty()) {
            File folder = new File();
            folder.setFilename(foldername);
            folder.setFiletype(FILETYPE_FOLDER);
            folder.setCreateDate(new Date());
            folder.setSize(0);
            folder.setService("lokal");
            folder.setOwner(user.getId());
            folder.setParent(getCWD());
            Ebean.save(folder);
            flash("success", Messages.get("filesystem.folder.creationsuccess", foldername));
            logger.debug("Filesystem: Successfull created folder "+foldername+" for user "+user.getEmail());
        } else {
            flash("error", Messages.get("filesystem.folder.creationerror", foldername));
            logger.debug("Filesystem: Error while creating "+foldername+" for user "+user.getEmail()+". Folder exists");
        }

        return redirect(controllers.routes.Filesystem.index());
    }
}
