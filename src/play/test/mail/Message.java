package play.test.mail;

import org.apache.commons.io.IOUtils;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Message implements Serializable {
  public final String from;
  public final String recipient;
  public String text = "";
  public final String subject;
  public List<Attachment> attachments = new ArrayList<>(1);

  public Message(InputStream data) {
    this(parseMimeMessage(data));
  }

  public Message(MimeMessage mimeMessage) {
    try {
      this.from = mimeMessage.getFrom()[0].toString();
      this.recipient = mimeMessage.getAllRecipients()[0].toString();
      this.subject = mimeMessage.getSubject();
      parseContent(mimeMessage);
    }
    catch (IOException | MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  private static MimeMessage parseMimeMessage(InputStream data) {
    try {
      Session session = Session.getDefaultInstance(new Properties());
      return new MimeMessage(session, data);
    }
    catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  private void parseContent(MimeMessage mimeMessage) throws MessagingException, IOException {
    Object content = mimeMessage.getContent();
    if (content instanceof MimeMultipart) {
      MimeMultipart mimeMultipart = (MimeMultipart) content;
      parseMultipart(mimeMultipart);
    }
    else if (content instanceof String) {
      text += content;
    }
  }

  private void parseMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
    for (int i = 0; i < mimeMultipart.getCount(); i++) {
      BodyPart part = mimeMultipart.getBodyPart(i);
      if (part.getContent() instanceof MimeMultipart) {
        parseMultipart((MimeMultipart) part.getContent());

      }
      if (part.getContentType().contains("text/plain") || part.getContentType().contains("text/html")) {
        text += part.getContent();
      }
      if (part.getHeader("Content-Disposition") != null) {
        Attachment attachment = new Attachment(part.getHeader("Content-Disposition")[0], IOUtils.toByteArray((InputStream) part.getContent()));
        attachments.add(attachment);
      }
    }
  }
}
