package controllers;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.File;
import models.FileSocket;
import models.User;
import org.h2.store.fs.FileUtils;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.i18n.Messages;
import play.libs.Akka;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.WebSocket;
import scala.concurrent.duration.Duration;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Filesystem extends Controller {

    public static final String FILETYPE_FILE = "file";
    public static final String FILETYPE_FOLDER = "folder";

    public static final String ROOT_FOLDER = "./storage";

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

    public static Result jsonFilesList() {
        User user = Account.getCurrentUser();

        if(user == null) {
            logger.debug("Filesystem AJAX Request: User unauthenticated");
            return notFound();
        }

        File dir = getCWD();
        String cwd = null;
        if (dir != null) {
            cwd = dir.id.toString();
        }

        List<File> list = File.find.where().eq("owner", user.getId().toString()).eq("parent_index", cwd).findList();

        return ok(Json.toJson(list));
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

    public static Result download(Long id) {

        User user = Account.getCurrentUser();

        if(user == null) {
            logger.debug("Filesystem: User unauthenticated");
            return redirect(controllers.routes.Account.login());
        }
        //mal schauen ob es die Datei überhaupt gibt
        File file = getFile(id);
        if(file == null) {
            flash("error", Messages.get("filesystem.file.notfound"));
            logger.debug("Filesystem: Downloadfile not found");
            return redirect(controllers.routes.Filesystem.index());
        }
        //wenn die Datei existiert, müssen wir noch schauen ob das auch unsere ist
        if (!file.owner.equals(user.getId())) {
            flash("error", Messages.get("filesystem.file.notownedbyyou"));
            logger.debug("Filesystem: Downloadfile not owned by user"+user.getEmail());
            return redirect(controllers.routes.Filesystem.index());
        }
        //wenn hier angelangt, können wir die Datei herunterladen
        String sub = "";
        File f = file.getParent();
        if (f != null) {
            sub = f.id.toString()+"/";
        }

        logger.debug("sub: "+file.getParent());

        response().setContentType("application/x-download");
        response().setHeader("Content-disposition","attachment; filename=\""+file.getFilename()+"\"");
        return ok(new java.io.File(ROOT_FOLDER, user.getId() + "/" + sub + file.getFilename()));
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

    public static Result newFile() {
        //Pürfen ob der User überhaupt das Recht hat hier was zu machen
        User user = Account.getCurrentUser();

        if(user == null) {
            logger.debug("Filesystem: User unauthenticated");
            return redirect(controllers.routes.Account.login());
        }
        //Wir prüfen mal ob der betreffende Benutzer überhaupt schon einen eigenen Ordner hat
        checkUserFolder(user);

        //jetzt holen wir uns noch die ordnerid
        String folderid = "";
        File cwd = getCWD();
        if (cwd != null) {
            //wenn die Datei nicht im Root-Verzeichnis abgelegt werden soll,
            //dann hängen wir einfach die ID des Ordners und ein / an den Pfad
            folderid = cwd.id.toString()+"/";
        }

        //laden der Datei vom hochladen
        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart data = body.getFile("filename");
        if (data != null) {
            String fileName = data.getFilename();
            //ToDo: Den Contenttype hier auch mit in die Datenbank schreiben. Dann ist der Download einfacher
            //String contentType = data.getContentType();
            java.io.File file = data.getFile();
            java.io.File test = new java.io.File(ROOT_FOLDER, user.getId().toString()+"/"+folderid+fileName);

            if(test.exists()) {
                logger.debug("Filesystem: File already exists "+test.getPath());
                flash("error", "File already exists");
                return redirect(controllers.routes.Filesystem.index());
            }

            if(file.renameTo(test)) {
                //Daten in die Datenbank schreiben
                File f_obj = new File();
                f_obj.setFilename(fileName);
                f_obj.setFiletype(FILETYPE_FILE);
                f_obj.setCreateDate(new Date());
                f_obj.setSize(test.length());
                f_obj.setService("lokal");
                f_obj.setOwner(user.getId());
                f_obj.setParent(getCWD());
                Ebean.save(f_obj);

                logger.debug("Filesystem: Successful fileupload of "+fileName+" for user "+user.getEmail());
                flash("success", "File uploaded");
            }
        } else {
            logger.debug("Filesystem: Error while uploading file for user "+user.getEmail());
            flash("error", "Missing file");
        }

        return redirect(controllers.routes.Filesystem.index());
    }

    public static Result newFolder() {

        User user = Account.getCurrentUser();

        if(user == null) {
            logger.debug("Filesystem: User unauthenticated");
            return redirect(controllers.routes.Account.login());
        }
        //falls zuerst ein Ordner angelegt wird, müssen wir auch hier schauen, dass das Heimverzeichnis des Benutzers existiert
        checkUserFolder(user);

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
            Ebean.refresh(folder);
            //wenn wir einen neuen Ordner anlegen, dann müssen wir auch im selben Zug den Ordner auf dem Dateisystem anlegen
            java.io.File newfolder = new java.io.File(ROOT_FOLDER, user.getId()+"/"+folder.id);
            if(newfolder.mkdir()) {
                logger.debug("Filesystem: Successfull created folder "+newfolder.getPath()+" for user "+user.getEmail());
            } else {
                logger.debug("Filesystem: Error while creating folder "+newfolder.getPath()+" for user "+user.getEmail());
            }

            flash("success", Messages.get("filesystem.folder.creationsuccess", foldername));
            logger.debug("Filesystem: Successfull created folder "+foldername+" for user "+user.getEmail());
        } else {
            flash("error", Messages.get("filesystem.folder.creationerror", foldername));
            logger.debug("Filesystem: Error while creating " + foldername + " for user " + user.getEmail() + ". Folder exists");
        }

        return redirect(controllers.routes.Filesystem.index());
    }

    private static void checkUserFolder(User user) {
        java.io.File root = new java.io.File(ROOT_FOLDER);
        //zuerst initialisieren wir mit dem Root direktory falls das noch nicht existiert
        if (!root.exists()) {
            if(root.mkdir()) {
                logger.debug("Filesystem: Created root directory");
            } else {
                logger.debug("Filesystem: Error while creating root directory");
            }
        }
        logger.debug(root.getAbsolutePath());
        //jetzt schauen wir ob der betreffende Benutzer überhaupt schon ein eigenes Verzeichnis hat
        java.io.File userfolder = new java.io.File(root, user.getId().toString());
        if (!userfolder.exists()) {
            if(userfolder.mkdir()) {
                logger.debug("Filesystem: Created user directory for "+user.getEmail());
            } else {
                logger.debug("Filesystem: Error while creating user directory for "+user.getEmail());
            }
        }
    }

    public static boolean rmUserFolder(User user) {
        java.io.File file = new java.io.File(ROOT_FOLDER, user.getId().toString());
        //Dateien aus der Datenbank entfernen
        Ebean.delete(File.find.where().eq("owner", user.getId()).findList());
        //Die Dateien löschen wir hier aus dem Dateisystem
        if(file.exists() && file.isDirectory()) {
            FileUtils.deleteRecursive(ROOT_FOLDER+"/"+user.getId().toString(), true);
            return true;
        }
        //hier nochmal schauen ob der Ordner weg ist. Wenn ja wars erfolgreich sonst false
        return !file.exists();
    }

    public static WebSocket<String> filesWS() {

        User user = Account.getCurrentUser();

        return new WebSocket<String>() {
            public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {
                final ActorRef pingActor = Akka.system().actorOf(Props.create(FileSocket.class, in, out, user));
                final Cancellable cancellable = Akka.system().scheduler().schedule(Duration.create(1, TimeUnit.SECONDS),
                        Duration.create(20, TimeUnit.SECONDS),
                        pingActor,
                        "Files",
                        Akka.system().dispatcher(),
                        null
                );

                in.onClose(() -> {
                    cancellable.cancel();
                });
            }

        };
    }

}
