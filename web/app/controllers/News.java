package controllers;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import com.avaje.ebean.Ebean;
import models.File;
import models.NewsSocket;
import models.User;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.i18n.Messages;
import play.libs.F.Callback0;
import play.libs.Json;
import play.mvc.WebSocket;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.duration.Duration;
import scala.util.parsing.json.JSONObject;
import scala.util.parsing.json.JSONObject$;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class News extends Controller {

    public static Logger.ALogger logger = Logger.of("application.controller.news");

    public static Result index() {
        User user = Account.getCurrentUser();

        if (user == null || !user.isAdmin()) {
            logger.debug("Filesystem: index(): User unauthenticated or no admin rights");
            return redirect(controllers.routes.Account.login());
        }

        return ok(views.html.news.index.render(Messages.get("application.news.management")));
    }

    public static Result addPost() {
        //Standardprüfung ob der Benutzer die Rechte besitzt
        User user = Account.getCurrentUser();
        if (user == null || !user.isAdmin()) {
            logger.debug("News: addPost(): User unauthenticated or no admin rights");
            return redirect(controllers.routes.Account.login());
        }

        DynamicForm requestData = Form.form().bindFromRequest();
        String name = requestData.get("name");
        String text = requestData.get("text");

        if (name == null || name.isEmpty()) {
            //Wenn es schon beim Formular ein Problem gab brechen wir hier ab
            flash("error", Messages.get("news.add.invaliddata"));
            logger.debug("News: addPost(): Invalid form data");
            return redirect(controllers.routes.News.index());
        }

        models.News news = new models.News();
        news.setName(name);
        news.setText(text);
        news.setDate(new Date());
        news.setOwner(user.getPrename());

        //wenn mit der Form alles OK war, können wir mit Ebean das Objekt hinzufügen
        Ebean.save(news);

        //Nachricht dass alles gepasst hat
        flash("success", Messages.get("news.add.successful", news.getName()));
        logger.debug("News: add(): \""+news.getName()+"\" successful added");
        return redirect(controllers.routes.News.index());
    }

    public static Result multiRmNews() {

        DynamicForm f = play.data.Form.form().bindFromRequest();
        Map<String, String> data = f.data();

        List<models.News> list = new ArrayList<>();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String val = entry.getValue();
            if (!val.isEmpty() && Long.parseLong(val) != 0) {
                models.News news = models.News.find.byId(Long.parseLong(val));
                if(news != null) {
                    list.add(news);
                }
            }
        }

        if (list.isEmpty()) {
            //Wenn keine Daten abgesendet wurden oder es einen Fehler gab und nichts ankam
            flash("error", Messages.get("news.multirm.nonews"));
            logger.debug("News: multiRmNews(): Invalid form data");
            return redirect(controllers.routes.News.index());
        }

        //hier kommen wir an, wenn es Daten gab
        for (models.News entry : list) {
            Ebean.delete(entry);
        }

        flash("success", Messages.get("news.multirm.success", list.size()));
        logger.debug("News: multiRmNews(): Successful removed "+list.size()+" entries");
        return redirect(controllers.routes.News.index());
    }

    public static Result rm(Long id) {

        //Standardmäßig die Rechte des Benutzers für diese Aktion prüfen
        User user = Account.getCurrentUser();
        if (user == null || !user.isAdmin()) {
            logger.debug("News: rm(): User unauthenticated or no admin rights");
            return redirect(controllers.routes.Account.login());
        }
        //prüfen ob die News, welche wir löschen wollen überhaupt existiert
        models.News news = models.News.find.byId(id);
        if(news == null) {
            logger.debug("News: rm(): News not found");
            flash("error", Messages.get("news.rm.notfound"));
            return redirect(controllers.routes.News.index());
        }
        //wenn die News vorhanden ist, löschen wir sie und aktualisieren die Seite
        logger.debug("News: rm(): News successful removed");
        flash("success", Messages.get("news.rm.successful", news.getName()));
        Ebean.delete(news);
        return redirect(controllers.routes.News.index());
    }

    public static Result jsonNewsList() {
        return ok(Json.toJson(models.News.find.where().findList()));
    }

    public static WebSocket<String> newsWS() {
        return new WebSocket<String>() {
            public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {
                final ActorRef pingActor = Akka.system().actorOf(Props.create(NewsSocket.class, in, out));
                final Cancellable cancellable = Akka.system().scheduler().schedule(Duration.create(1, TimeUnit.SECONDS),
                        Duration.create(15, TimeUnit.SECONDS),
                        pingActor,
                        "News",
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