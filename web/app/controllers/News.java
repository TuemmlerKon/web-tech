package controllers;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import models.NewsSocket;
import play.Logger;
import play.libs.F.Callback0;
import play.mvc.WebSocket;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;


public class News extends Controller {

    public static Logger.ALogger logger = Logger.of("application.controller.news");

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