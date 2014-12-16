package controllers;

import models.File;
import play.Logger;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;

public class Filesystem extends Controller {

    public static Logger.ALogger logger = Logger.of("application.controllers.Filesystem");

    public static Result index() {
        return ok(views.html.filesystem.index.render(Messages.get("application.general.myfiles"), File.find.all()));
    }
}
