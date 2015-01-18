package controllers;

import models.User;
import play.mvc.Controller;
import play.mvc.Result;
import play.i18n.Messages;
import play.Logger;


public class Application extends Controller {
    public static Logger.ALogger logger = Logger.of("application.controllers.Application");


    public static Result index() {

        User user = Account.getCurrentUser();

        if(user == null) {
            logger.debug("Index: User unauthenticated");
            return redirect(controllers.routes.Account.login());
        }

        return ok(views.html.index.render(Messages.get("application.general.index"), user));
    }

    public static Result gtc() {
        return ok(views.html.gtc.render(Messages.get("application.general.gtc")));
    }

    public static Result imprint() {
        return ok(views.html.imprint.render(Messages.get("application.general.imprint")));
    }

    public static Result settings() {

        if(Account.getCurrentUser() == null) {
            logger.debug("Index: User unauthenticated");
            return redirect(controllers.routes.Account.login());
        }

        return ok(views.html.settings.render(Messages.get("application.general.settings")));
    }
}
