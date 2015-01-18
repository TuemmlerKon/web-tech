package controllers;

import java.io.IOException;
import java.sql.*;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import com.avaje.ebean.Ebean;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import models.*;
import org.joda.time.DateTime;
import play.Logger;
import play.data.DynamicForm;
import play.db.*;
import play.data.Form;
import play.libs.Akka;
import play.mvc.Controller;
import play.mvc.Result;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import play.i18n.Messages;
import play.mvc.WebSocket;
import scala.concurrent.duration.Duration;
import service.Mailer;
import service.Serializer;

public class Account extends Controller {

    private static final String SALT  = "q3gdt4wx$%ZGFEWSC$%XHZ!Q§X$ZA$§ger";

    public static Logger.ALogger logger = play.Logger.of("application.controller.account");

    private static Connection connection = DB.getConnection();

    public static Result login() {

        User user = getCurrentUser();

        if(user != null) {
            logger.debug("User is already logged in. Redirect to main page");
            return redirect(controllers.routes.Application.index());
        }

        return ok(views.html.account.login.render(Messages.get("application.general.login"), Form.form(Login.class)));
    }

    public static Result loginPost() {
        Form<Login> userForm = Form.form(Login.class);
        Login login = userForm.bindFromRequest().get();

        if (login == null || userForm.hasErrors()) {
            //Wenn es schon beim Formular ein Problem gab, gehen wir ebenfalls gleich wieder zum Login
            flash("error", Messages.get("user.login.invalidcredentials"));
            logger.debug("Login: Form submission error");
            return redirect(controllers.routes.Account.login());
        }

        User user = getUser(login.getEmail(), login.getPassword());

        if (user == null) {
            //Wenn es aus der Datenbank keine Daten gab, dann gehen wir wieder zur Loginseite
            flash("error", Messages.get("user.login.invalidcredentials"));
            logger.debug("Login: Could not find user in database");
            return redirect(controllers.routes.Account.login());
        }

        writeUserToSession(user);

        //Nachricht dass alles gepasst hat
        flash("success", Messages.get("user.login.successful", user.getPrename()));
        logger.debug("Login: User "+user.getEmail()+" successful logged in");
        return redirect(controllers.routes.Application.index());
    }

    private static void writeUserToSession(User user) {
        try {
            session("user", Serializer.toString(user));
            logger.debug("Writing userdata to session");
        } catch (IOException e) {
            logger.debug("Error while trying to write object to session "+e);
        }
    }

    public static Result changePassword() {
        DynamicForm requestData = Form.form().bindFromRequest();

        if(requestData == null || requestData.hasErrors()) {
            flash("error", Messages.get("user.settings.unknownerror"));
            logger.debug("Change password: Unknown error while changeing the password");
            return redirect(controllers.routes.Application.settings());
        }
        //Daten lesen
        String oldpassword = requestData.get("oldpassword");
        String password = requestData.get("password");
        String password2 = requestData.get("password2");

        User user = getUser(getCurrentUser().getEmail(), oldpassword);

        if (user == null) {
            //wenn kein User gefunden wurde war das alte Passwort falsch
            flash("error", Messages.get("user.settings.invalidpassword"));
            logger.debug("Change password: Invalid password");
            return redirect(controllers.routes.Application.settings());
        }
        //überprüfen ob die beiden Passwörter gleich sind
        if (!password.equals(password2)) {
            //wenn kein User gefunden wurde war das alte Passwort falsch
            flash("error", Messages.get("user.settings.newpasswordsdontmatch"));
            logger.debug("Change password: Passwords dont match");
            return redirect(controllers.routes.Application.settings());
        }
        //wenn hier angekommen dann passt alles also passwort update
        PreparedStatement stmt;
        try {
            stmt = connection.prepareStatement("UPDATE `user` SET `password` = SHA2(?, 512) WHERE `email` = ?;");
            stmt.setString(2, user.getEmail());
            stmt.setString(1, password+SALT);
            stmt.execute();
            stmt.close();
            //Nachricht ausgeben, dass die Registrierung erfolgreich war und dann ab gehts zur Bestätigunsseite
            flash("success", Messages.get("user.changepassword.succesful"));
            logger.debug("Change password: Successful changed password for user: "+user.getEmail());
            return redirect(controllers.routes.Application.settings());
        }
        catch (SQLException e) {
            logger.error(e.getMessage());
        }

        return redirect(controllers.routes.Application.settings());
    }

    public static Result logout() {

        User user = getCurrentUser();

        session("user", "");

        flash("success", Messages.get("user.logout.successful", user.getPrename()));
        logger.debug("Logout: User successful logged out");
        return redirect(controllers.routes.Account.login());
    }

    public static Result register() {
        return ok(views.html.account.register.render(Messages.get("user.register.head.title"), Form.form(User.class)));
    }

    public static User getById(Long id) {
        PreparedStatement prep;
        try {
            prep = connection.prepareStatement("SELECT * FROM `user` WHERE `id` = ?;");
            prep.setLong(1, id);
            prep.execute();
            ResultSet rs = prep.getResultSet();
            if (rs.next()) {
                User user = new User();
                user.setId(Long.parseLong(rs.getString("ID")));
                user.setEmail(rs.getString("email"));
                user.setPrename(rs.getString("prename"));
                user.setSurname(rs.getString("surname"));
                user.setRoles(rs.getString("roles"));
                user.setStorage(rs.getInt("storage"));
                user.setUsed(rs.getInt("storage_used"));
                user.setDefault_encrypt(rs.getBoolean("default_encrypt"));
                user.setCreatedate(DateTime.parse(rs.getTimestamp("createdate").toLocalDateTime().toString()));

                return user;

            }

            return null;

        } catch(SQLException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public static Result rm() {

        User user = getCurrentUser();

        if(user == null) {
            logger.debug("Filesystem: User unauthenticated");
            return redirect(controllers.routes.Account.login());
        }

        DynamicForm requestData = Form.form().bindFromRequest();
        String rm = requestData.get("rmaccount");

        if(rm != null && !rm.isEmpty() && rm.equals("yes")) {
            //hier führen wir den Code nur aus, wenn es wirklich gewollt ist
            if(Filesystem.rmUserFolder(user)) {
                //wenn die Dateien erfolgreich gelöscht wurden, können wir jetzt den Benutzer aus dem System löschen und ihn abmelden
                try {
                    Statement stmt = connection.createStatement();
                    stmt.execute("DELETE FROM `user` WHERE `ID` = "+user.getId()+";");
                    stmt.close();
                    //Wenn das löschen des Users aus der Datenbank erfolgreich war
                    flash("success", Messages.get("account.rmaccount.success"));
                    logger.error("Account: Useraccount successful deleted for user "+user.getEmail());
                    //nach dem löschen des Users aus der Datenbank melden wir ihn gleich ab
                    return redirect(controllers.routes.Account.logout());
                }
                catch (SQLException e) {
                    //Wenn das löschen des Users aus der Datenbank einen Fehler geworfen hat
                    flash("error", Messages.get("account.rmaccount.error"));
                    logger.error(e.getMessage());
                }
            } else {
                //Wenn das löschen der Dateien einen Fehler verursacht hat
                flash("error", Messages.get("account.rmaccount.error"));
                logger.debug("Account: Could not delete user files");
            }
        } else {
            //Wenn die Daten, welche vom User abgeschickt wurden fehlerhaft waren
            flash("error", Messages.get("account.rmaccount.error"));
            logger.debug("Account: Invalid data from user");
        }

        return redirect(controllers.routes.Application.settings());
    }

    public static Result setDefaultEncryption() {
        DynamicForm requestData = Form.form().bindFromRequest();
        User user = getCurrentUser();
        boolean encrypt = false;

        if(requestData == null || requestData.hasErrors()) {
            flash("error", Messages.get("user.settings.unknownerror"));
            logger.debug("Change password: Unknown error while setting new value");
            return redirect(controllers.routes.Application.settings());
        }
        //Daten lesen
        String data = requestData.get("default_encryption");

        if (data == null || !data.equals("selected")) {
            encrypt = false;
            flash("success", Messages.get("user.settings.encryption.tofalse"));
            Logger.debug("Account: setDefaultEncryption(): Updated to false");
        } else {
            encrypt = true;
            flash("success", Messages.get("user.settings.encryption.totrue"));
            Logger.debug("Account: setDefaultEncryption(): Updated to true");
        }

        user.setDefault_encrypt(encrypt);
        Account.updateUser(user);

        return redirect(controllers.routes.Application.settings());
    }

    public static Result registerPost() {
        Form<User> userForm = Form.form(User.class);
        User user = userForm.bindFromRequest().get();

        Config conf = ConfigFactory.load();

        Integer standardStorage = conf.getInt("cloudplex.standardstorage");

        if (user == null) {
            return redirect(controllers.routes.Account.register());
        }
        PreparedStatement stmt;
        try {
            stmt = connection.prepareStatement("SELECT * FROM `user` WHERE `user`.email = ?;");
            stmt.setString(1, user.getEmail());
            stmt.execute();
            ResultSet rs = stmt.getResultSet();
            if(!rs.next()) {

                //den Aktivierungscode generieren
                String code = new Account().new generateCode().generate();

                //es gab keine Ergebnisse also können wir den Benutzer speichern.
                //hierfür benutzen wir ein preparedStatement
                PreparedStatement prep = connection.prepareStatement("INSERT INTO `user` SET `prename` = ?, `surname` = ?, `email` = ?, `password` = SHA2(?, 512), `activation` = ?, `createdate` = NOW(), `lastlogin` = NULL, `storage` = ?, `roles` = '"+User.ROLE_DEFAULT+"';");
                prep.setString(1, user.getPrename());
                prep.setString(2, user.getSurname());
                prep.setString(3, user.getEmail());
                prep.setString(4, user.getPassword()+SALT);
                prep.setString(5, code);
                prep.setInt(6, standardStorage);
                prep.execute();
                prep.close();
                //Die Aktivierungs-email versenden
                String url = controllers.routes.Account.activation(code).absoluteURL(request());
                String data = Messages.get("user.activation.email.text", url);

                if(conf.getBoolean("smtp.enabled")) {
                    new Mailer(user.getEmail(), conf.getString("smtp.from"), Messages.get("user.activation.email.subject.sent"), data).send();
                } else {
                    logger.debug("Mailer currently disabled: Could not send activation mail");
                }
                //Nachricht ausgeben, dass die Registrierung erfolgreich war und dann ab gehts zur Bestätigunsseite
                flash("success", Messages.get("user.register.succesful"));
                logger.debug("Successful created new user: "+user.getEmail());
                return redirect(controllers.routes.Account.login());
            } else {
                //Es gibt einen Benutzer mit dieser E-Mailadresse also leiten wir zurück zur registrierung und melden das ganze per Flash
                flash("error", Messages.get("user.register.emailalreadytaken"));
                logger.debug("E-Mailaddress is already taken. Redirecting to register view");
                return redirect(controllers.routes.Account.register());
            }
        }
        catch (SQLException e) {
            logger.error(e.getMessage());
        }
        //wenn alles schief ging und wir auch keine entsprechende Meldung machen konnten, geben wir einfach nen Fehler aus und gehen zurück zur Registrierung
        //Dieser Fall sollte aber eigentlich nie eintreten
        flash("error", Messages.get("user.register.unknownerror"));
        logger.debug("Unknown error! This should not happen");
        return redirect(controllers.routes.Account.register());
    }

    public static Result activation(String key) {

        if (key.isEmpty()) {
            //wenn der Aktivierungssschlüssel leer war dann geben wir einen Fehler aus und ab zurück zur Registrierung
            flash("error", Messages.get("user.activate.invalidkey"));
            logger.debug("User submitted an invalid activationkey (empty key)");
            return redirect(controllers.routes.Account.register());
        }

        PreparedStatement prep;
        try {
            prep = connection.prepareStatement("SELECT * FROM `user` WHERE `activation` = ?;");
            prep.setString(1, key);
            prep.execute();
            ResultSet rs = prep.getResultSet();
            if (rs.next()) {
                //der Aktivierungskey war vorhanden, wir können den Benutzer also aktivieren
                prep.close();
                prep = connection.prepareStatement("UPDATE `user` SET `activation` = '' WHERE `activation` = ?;");
                prep.setString(1, key);
                prep.execute();
                prep.close();
                //per E-Mail über die Aktivierung benachrichtigen
                Config conf = ConfigFactory.load();
                String data = Messages.get("user.activation.success");
                if(conf.getBoolean("smtp.enabled")) {
                    new Mailer("konstantin@tuemmler.org", conf.getString("smtp.from"), Messages.get("user.activation.email.subject.success"), data).send();
                } else {
                    logger.debug("Mailer currently disabled: Could not send activation verification");
                }
                //Nachrichtausgeben, dass die Aktivierung erfolgreich war und leiten zum Login weiter
                flash("success", Messages.get("user.activate.successful"));
                logger.debug("User activated successful with key "+key);
                return redirect(controllers.routes.Account.login());
            } else {
                //kein passender Aktivierungsschlüssel gefunden
                flash("error", Messages.get("user.activate.invalidkey"));
                logger.debug("User submitted an invalid activationkey (not found)");
                return redirect(controllers.routes.Account.register());
            }

        }
        catch (SQLException e) {
            logger.error(e.getMessage());
        }
        //wenn alles schief ging und wir auch keine entsprechende Meldung machen konnten, geben wir einfach nen Fehler aus und gehen zurück zur Registrierung
        //Dieser Fall sollte aber eigentlich nie eintreten
        flash("error", Messages.get("user.activate.unknownerror"));
        logger.debug("Unknown error! This should not happen");
        return redirect(controllers.routes.Account.register());
    }

    public static User getCurrentUser() {
        //schauen ob wir an die JSON Daten aus der Session kommen
        String jsonData = session("user");

        //wenn NULL oder leer können wir keine Daten lesen -> return null
        if(jsonData == null || jsonData.equals("")) return null;

        Object user;
        try {
            //Mit dem ObjectMapper versuchen wir den JSON-String in ein Userobjekt umzuwandeln
            user = Serializer.fromString(jsonData);

        } catch (Exception e) {
            //wenn es zu einer Exception kam dann setzen wir user = null und melden das per logger
            user = null;
            logger.debug("Error while trying to read object from session"+e);
        }

        return (User)user;
    }

    public static boolean dekreaseUsed(User user, Integer ammount) {
        if (user == null) {
            Logger.debug("User not found");
            return false;
        }

        ammount = user.getUsed()-ammount;

        if (ammount <= 0) {
            ammount = 0;
        }

        user.setUsed(ammount);

        return updateUser(user);
    }

    public static boolean inkreaseUsed(User user, Integer ammount, boolean inkrement) {

        if (user == null) {
            Logger.debug("User not found");
            return false;
        }

        if (inkrement) {
            ammount = user.getUsed()+ammount;
        }

        if(ammount > user.getStorage()) {
            return false;
        }

        user.setUsed(ammount);

        return updateUser(user);
    }

    public final class generateCode {

        private SecureRandom random = new SecureRandom();

        public String generate() {
            return new BigInteger(130, random).toString(32);
        }
    }

    public static boolean updateUser(User user) {

        if (user == null) {
            return false;
        }

        PreparedStatement prep;
        try {
            prep = connection.prepareStatement("UPDATE `user` SET `prename` = ?, `surname` = ?, `email` = ?, `roles` = ?, `storage` = ?, `storage_used` = ?, `default_encrypt` = ? WHERE `user`.`ID` = ?;");
            prep.setString(1, user.getPrename());
            prep.setString(2, user.getSurname());
            prep.setString(3, user.getEmail());
            prep.setString(4, user.getRoles());
            prep.setInt(5, user.getStorage());
            prep.setInt(6, user.getUsed());
            prep.setBoolean(7, user.isDefault_encrypt());
            prep.setLong(8, user.getId());
            prep.execute();
            prep.close();
        }
        catch (SQLException e) {
            logger.error(e.getMessage());
        }

        writeUserToSession(user);

        return true;
    }

    private static User getUser(String email, String password) {
        PreparedStatement prep;

        try {
            prep = connection.prepareStatement("SELECT * FROM `user` WHERE `email` = ? AND `password` = SHA2(?, 512);");
            prep.setString(1, email);
            prep.setString(2, password+SALT);
            prep.execute();
            ResultSet rs = prep.getResultSet();
            if (rs.next()) {
                //Wenn ein Benutzer mit dem entsprechenden Passwort und E-Mail gefunden wurde

                //zuerst Prüfen ob er aktiviert ist. Sonst brechen wir ebenfalls ab
                if(!rs.getString("activation").isEmpty()) {
                    logger.debug("GetUser: User not activated");
                    return null;
                }

                User user = new User();
                user.setId(Long.parseLong(rs.getString("ID")));
                user.setEmail(rs.getString("email"));
                user.setPrename(rs.getString("prename"));
                user.setSurname(rs.getString("surname"));
                user.setRoles(rs.getString("roles"));
                user.setStorage(rs.getInt("storage"));
                user.setUsed(rs.getInt("storage_used"));
                user.setCreatedate(DateTime.parse(rs.getTimestamp("createdate").toLocalDateTime().toString()));

                Timestamp timestamp = rs.getTimestamp("lastlogin");
                if(timestamp != null) {
                    user.setLastlogin(DateTime.parse(timestamp.toLocalDateTime().toString()));
                }

                //da sich der Benutzer ja erfolgreich angemeldet hat updaten wir jetzt noch den letzten Login in der Datenbank
                prep.execute("UPDATE `user` SET `lastlogin` = NOW() WHERE `email` = '"+user.getEmail()+"';");
                prep.close();
                return user;
            } else {
                //wenn es einen Fehler bei der Autorisierung gab
                return null;
            }
        }
        catch (SQLException e) {
            logger.error(e.getMessage());
        }

        return null;
    }

    public static WebSocket<String> userWS() {

        User user = getCurrentUser();

        return new WebSocket<String>() {
            public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {
                final ActorRef pingActor = Akka.system().actorOf(Props.create(UserSocket.class, in, out, user == null ? 0 : user.getId()));
                final Cancellable cancellable = Akka.system().scheduler().schedule(Duration.create(1, TimeUnit.SECONDS),
                        Duration.create(5, TimeUnit.SECONDS),
                        pingActor,
                        "User",
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