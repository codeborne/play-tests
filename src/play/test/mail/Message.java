package play.test.mail;

import org.apache.commons.io.IOUtils;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Message {
  public final String from;
  public final String recipient;
  public final MimeMessage mimeMessage;
  private final StringBuilder text = new StringBuilder();
  public final String raw;
  public List<String> attachments = new ArrayList<>(1);

  public Message(String from, String recipient, InputStream data) {
    this.from = from;
    this.recipient = recipient;

    try {
      raw = IOUtils.toString(data, "UTF-8");

      Session session = Session.getDefaultInstance(new Properties());
      this.mimeMessage = new MimeMessage(session, new ByteArrayInputStream(raw.getBytes("UTF-8")));
      parseContent();
    } catch (IOException | MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public String getText() {
    return text.toString();
  }

  public String getRecipient() {
    try {
      return mimeMessage.getAllRecipients()[0].toString();
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  public String getSubject() {
    try {
      return mimeMessage.getSubject();
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  private void parseContent() throws IOException, MessagingException {
    Object content = mimeMessage.getContent();
    if (content instanceof MimeMultipart) {
      MimeMultipart mimeMultipart = (MimeMultipart) content;
      parseMultipart(mimeMultipart);
    }
    else if (content instanceof String) {
      text.append((String) content);
    }
  }

  private void parseMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
    for (int i = 0; i < mimeMultipart.getCount(); i++) {
      BodyPart part = mimeMultipart.getBodyPart(i);
      if (part.getContent() instanceof MimeMultipart) {
        parseMultipart((MimeMultipart) part.getContent());

      }
      if (part.getContentType().contains("text/plain") || part.getContentType().contains("text/html")) {
        text.append(part.getContent());
      }
      if (part.getHeader("Content-Disposition") != null) {
        attachments.add(part.getHeader("Content-Disposition")[0]);
      }
    }
  }
}
