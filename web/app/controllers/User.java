package controllers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import play.mvc.Controller;
import play.mvc.Result;
import java.math.BigInteger;
import java.security.SecureRandom;
import play.i18n.Messages;
import models.Mailer;

public class User extends Controller {

    /*
    Aufruf wenn der Benutzer sich am System registrieren möchte
     */
    public static Result register() {

        generateCode Code = new User().new generateCode();
        String url = controllers.routes.User.activation(Code.generate()).absoluteURL(request());
        String data = Messages.get("user.activation.email.text", url);
        Config conf = ConfigFactory.load();

        new Mailer("konstantin@tuemmler.org", conf.getString("smtp.from"), Messages.get("user.activation.email.subject.sent"), data).send();

        return ok(views.html.user.register.render("Danke"));
    }

    /*
    Der Benutzer aktiviert sein Konto
     */

    public static Result activation(String key) {
        //TODO: Den Key gegen eine Datenbank prüfen
        if (key.isEmpty()) {
            return ok(views.html.user.activation.render(Messages.get("user.activation.invalidkey")));
        }

        Config conf = ConfigFactory.load();
        String data = Messages.get("user.activation.success");

        new Mailer("konstantin@tuemmler.org", conf.getString("smtp.from"), Messages.get("user.activation.email.subject.success"), data).send();


        return ok(views.html.user.activation.render(Messages.get("user.activation.successfull")));
    }

    private final class generateCode {
        private SecureRandom random = new SecureRandom();

        public String generate() {
            return new BigInteger(130, random).toString(32);
        }
    }

}
