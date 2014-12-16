package controllers;

import java.sql.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import models.*;
import org.joda.time.DateTime;
import play.Logger;
import play.api.libs.Codecs;
import play.data.DynamicForm;
import play.db.*;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Date;

import play.i18n.Messages;
import service.Mailer;

public class Account extends Controller {

    private static String TABLE = "user";
    private static String SALT  = "q3gdt4wx$%ZGFEWSC$%XHZ!Q§X$ZA$§ger";

    public static Logger.ALogger logger = play.Logger.of("application.controller.account");

    private static Connection connection = DB.getConnection();

    public static Result login() {

        String email = session("email");

        if(email != null && !email.isEmpty()) {
            logger.debug("User is already logged in. Redirect to main page");
            return redirect(controllers.routes.Application.index());
        }

        createTableIfNotExist();

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

        session("userid", user.getId().toString());
        session("email", user.getEmail());
        session("prename", user.getPrename());
        session("surname", user.getSurname());
        session("lastlogin", user.getLastlogin() == null ? "" : user.getLastlogin().toLocalDateTime().toString());
        //Nachricht dass alles gepasst hat
        flash("success", Messages.get("user.login.successful", user.getPrename()));
        logger.debug("Login: User "+user.getEmail()+" successful logged in");
        return redirect(controllers.routes.Application.index());
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

        User user = getUser(session("email"), oldpassword);

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
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("UPDATE " + TABLE + " SET `password` = SHA1(?) WHERE `email` = ?;");
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

        String user = session("prename");

        session("email", "");
        session("prename", "");
        session("surname", "");
        session("lastlogin", "");
        session("userid", "");

        flash("success", Messages.get("user.logout.successful", user));
        logger.debug("Logout: User successful logged out");
        return redirect(controllers.routes.Account.login());
    }

    public static Result register() {

        createTableIfNotExist();

        return ok(views.html.account.register.render(Messages.get("user.register.head.title"), Form.form(User.class)));
    }

    public static Result rm() {

        User user = Account.getCurrentUser();

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
                    stmt.execute("DELETE FROM "+TABLE+" WHERE `ID` = "+user.getId()+";");
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

    public static Result registerPost() {
        Form<User> userForm = Form.form(User.class);
        User user = userForm.bindFromRequest().get();

        if (user == null) {
            return redirect(controllers.routes.Account.register());
        }
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement("SELECT * FROM " + TABLE + " WHERE " + TABLE + ".email = ?;");
            stmt.setString(1, user.getEmail());
            stmt.execute();
            ResultSet rs = stmt.getResultSet();
            if(!rs.next()) {

                //den Aktivierungscode generieren
                String code = new Account().new generateCode().generate();

                //es gab keine Ergebnisse also können wir den Benutzer speichern.
                //hierfür benutzen wir ein preparedStatement
                PreparedStatement prep = connection.prepareStatement("INSERT INTO "+TABLE+" SET `prename` = ?, `surname` = ?, `email` = ?, `password` = SHA1(?), `activation` = ?, `createdate` = NOW(), `lastlogin` = NULL;");
                prep.setString(1, user.getPrename());
                prep.setString(2, user.getSurname());
                prep.setString(3, user.getEmail());
                prep.setString(4, user.getPassword()+SALT);
                prep.setString(5, code);
                prep.execute();
                prep.close();
                //Die Aktivierungs-email versenden
                String url = controllers.routes.Account.activation(code).absoluteURL(request());
                String data = Messages.get("user.activation.email.text", url);
                Config conf = ConfigFactory.load();
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

        PreparedStatement prep = null;
        try {
            prep = connection.prepareStatement("SELECT * FROM "+TABLE+" WHERE `activation` = ?;");
            prep.setString(1, key);
            prep.execute();
            ResultSet rs = prep.getResultSet();
            if (rs.next()) {
                //der Aktivierungskey war vorhanden, wir können den Benutzer also aktivieren
                prep.close();
                prep = connection.prepareStatement("UPDATE " + TABLE + " SET `activation` = '' WHERE `activation` = ?;");
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
        String result = session("email");
        if(result == null || result.equals("")) return null;

        User user = new User();
        user.setId(Long.parseLong(session("userid")));
        user.setEmail(session("email"));
        user.setPrename(session("prename"));
        user.setSurname(session("surname"));

        if(!session("lastlogin").isEmpty()) {
            user.setLastlogin(DateTime.parse(session("lastlogin")));
        }

        return user;
    }

    private final class generateCode {

        private SecureRandom random = new SecureRandom();

        public String generate() {
            return new BigInteger(130, random).toString(32);
        }
    }

    private static User getUser(String email, String password) {
        PreparedStatement prep = null;

        try {
            prep = connection.prepareStatement("SELECT * FROM "+TABLE+" WHERE `email` = ? AND `password` = SHA1(?);");
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
                user.setCreatedate(DateTime.parse(rs.getTimestamp("createdate").toLocalDateTime().toString()));

                Timestamp timestamp = rs.getTimestamp("lastlogin");
                if(timestamp != null) {
                    user.setLastlogin(DateTime.parse(timestamp.toLocalDateTime().toString()));
                }

                //da sich der Benutzer ja erfolgreich angemeldet hat updaten wir jetzt noch den letzten Login in der Datenbank
                prep.execute("UPDATE "+TABLE+" SET `lastlogin` = NOW() WHERE `email` = '"+user.getEmail()+"';");
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

    /**
     * Erstellt eine neue Tabelle für die User in der Datenbank, falls diese noch nicht existiert
     */
    private static void createTableIfNotExist() {
        Statement stmt = null;
        try {
            DatabaseMetaData dbm = connection.getMetaData();
            ResultSet tables = dbm.getTables(null, null, TABLE, null);
            if (!tables.next()) {
                stmt = connection.createStatement();
                stmt.execute("CREATE TABLE IF NOT EXISTS `user` (" +
                        "`ID` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        "  `prename` varchar(255) NOT NULL," +
                        "  `surname` varchar(255) NOT NULL," +
                        "  `email` varchar(255) NOT NULL," +
                        "  `password` varchar(255) NOT NULL," +
                        "  `activation` varchar(255) NOT NULL," +
                        "  `createdate` datetime NOT NULL," +
                        "  `lastlogin` datetime" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=latin1;");
                stmt.close();
                logger.info("Created new table " + TABLE + " in database!");
            }
        }
        catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }
}