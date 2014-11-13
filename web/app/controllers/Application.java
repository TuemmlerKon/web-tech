package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.i18n.Messages;

public class Application extends Controller {
    
    public static Result index() {
        return ok(views.html.index.render(Messages.get("application.general.index")));
    }

    public static Result gtc() {
        return ok(views.html.gtc.render(Messages.get("application.general.gtc")));
    }

    public static Result imprint() {
        return ok(views.html.imprint.render(Messages.get("application.general.imprint")));
    }

    public static Result settings() {
        return ok(views.html.settings.render(Messages.get("application.general.settings")));
    }
    public static Result myfiles() {
        return ok(views.html.myfiles.render(Messages.get("application.general.myfiles")));
    }

}
