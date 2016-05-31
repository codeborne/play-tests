package play.test.mail;

import org.junit.rules.ExternalResource;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;
import play.Logger;
import play.Play;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.openqa.selenium.net.PortProber.findFreePort;

public class MailMock extends ExternalResource {
  private static SMTPServer smtpServer;

  private static final List<Message> inbox = new ArrayList<>(1);

  @Override
  protected void before() throws Throwable {
    String smtp = Play.configuration.getProperty("mail.smtp", "");
    if (!"mock".equals(smtp)) {
      Logger.info("mail.smtp=" + smtp + ", smtp mock is not needed.");
      return;
    }

    runSmtpServer();

    inbox.clear();
  }

  @Override
  protected void after() {
    inbox.clear();
  }

  private void runSmtpServer() {
    if (smtpServer == null) {
      smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(new SimpleMessageListener() {
        @Override
        public boolean accept(String from, String recipient) {
          return true;
        }

        @Override
        public void deliver(String from, String recipient, InputStream data) {
          inbox.add(new Message(from, recipient, data));
        }
      }));

      int port = findFreePort();
      smtpServer.setHostName(Play.configuration.getProperty("mail.smtp.host", "localhost"));
      smtpServer.setPort(port);
      Logger.info("Starting SMTP server mock on port " + smtpServer.getPort());

      Play.configuration.setProperty("mail.smtp.port", String.valueOf(port));
      Play.configuration.setProperty("mail.smtp", "smtp.mock");

      smtpServer.start();
    }
  }

  public List<Message> inbox() {
    return Collections.unmodifiableList(inbox);
  }

  public Message getLastMessageReceivedBy(String recipient) {
    Message lastMessage = null;
    for (Message message : inbox) {
      if (recipient.equals(message.recipient)) lastMessage = message;
    }
    return lastMessage;
  }
}
