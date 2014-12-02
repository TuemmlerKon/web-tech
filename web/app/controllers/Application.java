package controllers;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.File;
import models.News;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.i18n.Messages;
import play.Logger;
import scala.Array;
import play.db.*;
import java.sql.*;
import java.util.List;

import securesocial.core.BasicProfile;
import securesocial.core.RuntimeEnvironment;
import securesocial.core.java.SecureSocial;
import securesocial.core.java.SecuredAction;
import securesocial.core.java.UserAwareAction;

public class Application extends Controller {
    public static Logger.ALogger logger = Logger.of("application.controllers.Application");
    private RuntimeEnvironment env;
    /**
     * A constructor needed to get a hold of the environment instance.
     * This could be injected using a DI framework instead too.
     *
     * @param env
     */
    public Application(RuntimeEnvironment env) {
        this.env = env;
    }

    public static Result index() {
        return ok(views.html.index.render(Messages.get("application.general.index"), News.find.all()));
    }

    public static Result test() {

        JsonNode json = request().body().asJson();
        if(json == null) {
            return badRequest("Expecting Json data");
        } else {
            ObjectNode result = Json.newObject();
            if (result != null) {
                Array t = new Array<String>(json.asInt());
                result.put("title", Messages.get("files.detele.list"));
                //result.put("return", views.html.test.render(t);
            }
            return ok(result);
        }
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
        return ok(views.html.myfiles.render(Messages.get("application.general.myfiles"), File.find.all()));
    }

}
