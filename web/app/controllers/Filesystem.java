package controllers;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
        List<File> folders = getFolderTree(getCWD());

        return ok(views.html.filesystem.index.render(Messages.get("application.general.myfiles"), list, folders));
    }

    private static List<File> getFolderTree(File folder) {

        List<File> list = new ArrayList<>();
        if (folder != null) {
            if (folder.getParent() != null) {
                list.addAll(getFolderTree(folder.getParent()));
            }

            list.add(folder);
        }

        return list;
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
            logger.debug("Filesystem: getCWD: Item is no folder");
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

                if(Account.inkreaseUsed(user, (int) test.length(), true)) {
                    Ebean.save(f_obj);
                    logger.debug("Filesystem: Successful fileupload of "+fileName+" for user "+user.getEmail());
                    flash("success", "File uploaded");
                } else {
                    //wenn das Dateilimit erreicht ist, wird die Datei nicht in die Datenbank geschrieben und die Datei selbst gelöscht
                    file.delete();
                    //Der Benutzer hat sein Speicherlimit überschritten
                    flash("error", Messages.get("filesystem.storage.exceeded"));
                    Logger.debug("User "+user.getEmail()+" exceeded his storage limit of "+user.getStorage()+" Bytes");
                }
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

    public static Result deleteFile(Long id) {
        File file = getFile(id);
        User user = Account.getCurrentUser();

        //prüfen ob es den Benutzer überhaupt gibt
        if(user == null) {
            Logger.debug("Filesystem: Delete: User not found");
            return redirect(controllers.routes.Account.login());
        }

        //Prüfen ob die Datei überhaupt existiert
        if (file == null) {
            Logger.debug("Filesystem: Delete: File not found");
            flash("error", Messages.get("filesystem.delete.filenotfound"));
            return redirect(controllers.routes.Filesystem.index());
        }

        //Überprüfen ob die gewünschte Datei ein Ordner ist. Das sollte aber normalerweise nie der Fall sein
        if (file.getFiletype().equals(FILETYPE_FOLDER)) {
            Logger.debug("Filesystem: Delete: Cannot delete file becaus it's a folder");
            flash("error", Messages.get("filesystem.delete.fileisafolder"));
            return redirect(controllers.routes.Filesystem.index());
        }

        //jetzt können wir die Datei sowohl aus der Datenbank als auch auf der Festplatte löschen
        if(doFileDelete(user, file)) {
            flash("success", Messages.get("filesystem.delete.success", file.getFilename()));
            return redirect(controllers.routes.Filesystem.index());
        };

        Logger.debug("Filesystem: Delete: Unknown error occured");
        flash("error", Messages.get("filesystem.delete.unknownerror"));
        return redirect(controllers.routes.Filesystem.index());
    }

    public static Result deleteFolder(Long id) {
        File file = getFolder(id);
        User user = Account.getCurrentUser();

        //prüfen ob es den Benutzer überhaupt gibt
        if(user == null) {
            Logger.debug("Filesystem: deleteFolder(): User not found");
            return redirect(controllers.routes.Account.login());
        }

        //Prüfen ob die Datei überhaupt existiert
        if (file == null) {
            Logger.debug("Filesystem: deleteFolder(): File not found id: "+id);
            flash("error", Messages.get("filesystem.delete.foldernotfound"));
            return redirect(controllers.routes.Filesystem.index());
        }

        //Überprüfen ob die gewünschte Datei ein Ordner ist. Das sollte aber normalerweise nie der Fall sein
        if (file.getFiletype().equals(FILETYPE_FILE)) {
            Logger.debug("Filesystem: deleteFolder(): Cannot delete folder because it's a file");
            flash("error", Messages.get("filesystem.delete.folderisafile"));
            return redirect(controllers.routes.Filesystem.index());
        }

        //schauen ob es Elemente gibt die diesem Ordner untergeordnet sind
        List<models.File> found = File.find.where().eq("parent_index", file.getId()).findList();

        if(!found.isEmpty()) {
            Logger.debug("Filesystem: doFolderDelete(): Folder not empty");
            flash("error", Messages.get("filesystem.delete.foldernotempty", file.getFilename()));
            return redirect(controllers.routes.Filesystem.index());
        }

        //jetzt können wir den Ordner sowohl aus der Datenbank als auch auf der Festplatte löschen
        if(doFolderDelete(user, file)) {
            flash("success", Messages.get("filesystem.delete.folder.success", file.getFilename()));
            return redirect(controllers.routes.Filesystem.index());
        };

        Logger.debug("Filesystem: deleteFolder(): Unknown error occured");
        flash("error", Messages.get("filesystem.delete.unknownerror"));
        return redirect(controllers.routes.Filesystem.index());
    }

    private static boolean doFolderDelete(User user, File folder) {
        //wenn das alles passt laden wir den Ordner im Dateisystem
        java.io.File real_folder = new java.io.File(ROOT_FOLDER, user.getId()+"/"+folder.getId());
        Logger.debug("Folder: "+ folder.getFilename());
        //Prüfen ob der Ordner überhaupt existiert
        if (!real_folder.exists()) {
            Logger.debug("Filesystem: doFolderDelete(): Real folder not found");
            return false;
        }
        //jetzt müssen wir rekursiv alle Dateien und Ordner suchen
        //ToDo: Rekursives löschen von Ordnern

        //den aktuellen Ordner hauen wir auch noch weg
        Ebean.delete(folder);
        //jetzt löschen wir den Ordner mit all seinen Unterordnern
        FileUtils.deleteRecursive(ROOT_FOLDER+"/"+user.getId().toString()+"/"+folder.getId().toString(), true);

        Logger.debug("Filesystem: doFolderDelete(): Folder "+folder.getFilename()+" successful removed");
        return true;
    }

    private static boolean doFileDelete(User user, File file) {
        //wenn hier angelangt bauen wir uns den Unterordner für den Pfad
        String sub = "";
        File f = file.getParent();
        if (f != null) {
            sub = f.id.toString()+"/";
        }

        //wenn das alles passt laden wir die Datei im Dateisystem
        java.io.File real_file = new java.io.File(ROOT_FOLDER, user.getId()+"/"+sub+file.getFilename());

        //Prüfen ob die Datei überhaupt existiert
        if (!real_file.exists()) {
            Logger.debug("Filesystem: Delete: Real file not found");
            return false;
        }

        //jetzt können wir die Datei sowohl aus der Datenbank als auch auf der Festplatte löschen
        if(real_file.delete()) {
            Account.dekreaseUsed(user, (int)file.getSize());
            Ebean.delete(file);
            Logger.debug("Filesystem: Delete: File "+file.getFilename()+" successful removed");
            return true;
        }

        return false;
    }

    public static Result multiRm() {

        User user = Account.getCurrentUser();

        //prüfen ob es den Benutzer überhaupt gibt
        if(user == null) {
            Logger.debug("Filesystem: multiRm(): User not found");
            return redirect(controllers.routes.Account.login());
        }

        DynamicForm f = play.data.Form.form().bindFromRequest();
        Map<String, String> data = f.data();

        List<models.File> list = new ArrayList<>();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String val = entry.getValue();
            if (!val.isEmpty() && Long.parseLong(val) != 0) {
                models.File file = models.File.find.byId(Long.parseLong(val));
                if(file != null) {
                    list.add(file);
                }
            }
        }

        if (list.isEmpty()) {
            //Wenn keine Daten abgesendet wurden oder es einen Fehler gab und nichts ankam
            flash("error", Messages.get("filesystem.multirm.nofiles"));
            logger.debug("News: multiRm(): Invalid form data");
            return redirect(controllers.routes.Filesystem.index());
        }

        boolean hasError = false;

        //hier kommen wir an, wenn es Daten gab
        for (models.File entry : list) {
            if(entry.getFiletype().equals(FILETYPE_FOLDER)) {
                if(!doFolderDelete(user, entry)) {
                    hasError = true;
                }
            } else if(entry.getFiletype().equals(FILETYPE_FILE)) {
                if (!doFileDelete(user, entry)) {
                    hasError = true;
                }
            }
        }
        //Prüfen ob ein Fehler aufgetreten ist
        if(!hasError) {
            //wenn kein Fehler auftrat
            flash("success", Messages.get("filesystem.multirm.success", list.size()));
            logger.debug("Filesysten: multiRmFiles(): Successful removed "+list.size()+" entries");
        } else {
            //Wenn ein Fehler auftrat das entsprechend wiedergeben
            flash("error", Messages.get("filesystem.multirm.erroroccured"));
            logger.debug("Filesysten: multiRmFiles(): Error while deleting multiple files");
        }
        //in jedem Fall geht es wieder zur Dateiliste zurück
        return redirect(controllers.routes.Filesystem.index());
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
