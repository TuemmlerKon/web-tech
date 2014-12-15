package controllers;

import java.sql.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import models.User;
import play.Logger;
import play.db.*;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.Statement;

import play.i18n.Messages;
import service.Mailer;

public class Account extends Controller {

    private static String TABLE = "user";
    private static String SALT  = "q3gdt4wx$%ZGFEWSC$%XHZ!Q§X$ZA$§ger";

    public static Logger.ALogger logger = play.Logger.of("application.controller.account");

    private static Connection connection = DB.getConnection();

    public static Result login() {

        createTableIfNotExist();

        return ok(views.html.account.login.render(Messages.get("application.general.login")));
    }

    public static Result logout() {

        return ok(views.html.account.login.render(Messages.get("application.general.logout")));
    }

    public static Result register() {

        createTableIfNotExist();

        return ok(views.html.account.register.render(Messages.get("user.register.head.title"), Form.form(User.class)));
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
                flash("error", Messages.get("user.activate.successful"));
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

    public static boolean isLoggedIn() {
        String result = session("email");
        if(result == null || result.equals("")) return false;
        return true;
    }

    private final class generateCode {

        private SecureRandom random = new SecureRandom();

        public String generate() {
            return new BigInteger(130, random).toString(32);
        }
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
                        "  `createdate` date NOT NULL," +
                        "  `lastlogin` date" +
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