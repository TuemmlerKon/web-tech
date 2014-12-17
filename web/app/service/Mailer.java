package service;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.plugin.*;
import play.i18n.Messages;
import play.api.Play;
import java.lang.String;

public class Mailer extends Throwable {

    private String receiver;

    private String sender;

    private String subject;

    private String data;

    private String prefix;

    public Mailer(String receiver, String sender, String subject, String data) {
        Config conf = ConfigFactory.load();
        this.prefix = conf.getString("smtp.subjectprefix");
        //prüfen ob der prefix leer ist! Wenn nicht machen wir ans ende noch ein Leerzeichen
        if (!this.prefix.isEmpty()) this.prefix += " ";

        //ein paar überprüfungen erledigen
        this.sender = !sender.isEmpty() ? sender: null;
        this.receiver = !receiver.isEmpty() ? receiver: null;
        this.subject =  !subject.isEmpty() ? subject: null;
        this.data = !data.isEmpty() ? data: null;
    }

    public void send() {
        //Code nur ausführen, wenn in alle Variablen auch etwas gegeben ist.
        if (this.sender != null && this.subject != null && this.receiver != null && this.data != null) {
            MailerAPI mail = play.Play.application().plugin(MailerPlugin.class).email();
            mail.setSubject(this.prefix+this.subject);
            mail.setRecipient(this.receiver);
            mail.setFrom(this.sender);
            mail.send(this.data);
        }
    }

}
