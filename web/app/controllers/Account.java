package controllers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import play.api.db.DB;
import play.mvc.Controller;
import play.mvc.Result;
import java.math.BigInteger;
import java.security.SecureRandom;
import play.i18n.Messages;
import service.Mailer;

public class Account extends Controller {

    public DB db = null;

    public Account() {
        db = DB.getConnection(true);
    }

    public static Result login() {
        return ok(views.html.account.login.render(Messages.get("application.general.login")));
    }

    public static Result logout() {
        return ok(views.html.account.login.render(Messages.get("application.general.logout")));
    }

    public static Result register() {

        generateCode Code = new Account().new generateCode();
        String url = controllers.routes.Account.activation(Code.generate()).absoluteURL(request());
        String data = Messages.get("user.activation.email.text", url);
        Config conf = ConfigFactory.load();
        //TODO: Den Mailer aktivieren
        //new Mailer("konstantin@tuemmler.org", conf.getString("smtp.from"), Messages.get("user.activation.email.subject.sent"), data).send();

        return ok(views.html.account.register.render("Danke"));
    }

    public static Result activation(String key) {
        //TODO: Den Key gegen eine Datenbank prüfen
        if (key.isEmpty()) {
            return ok(views.html.account.activation.render(Messages.get("user.activation.invalidkey")));
        }

        Config conf = ConfigFactory.load();
        String data = Messages.get("user.activation.success");

        //TODO: Den Mailer aktivieren
        //new Mailer("konstantin@tuemmler.org", conf.getString("smtp.from"), Messages.get("user.activation.email.subject.success"), data).send();

        return ok(views.html.account.activation.render(Messages.get("user.activation.successfull")));
    }

    public boolean isLoggedIn() {
        //ToDo: Hier muss noch die Logik hin
        return true;
    }

    private final class generateCode {

        private SecureRandom random = new SecureRandom();

        public String generate() {
            return new BigInteger(130, random).toString(32);
        }
    }
}