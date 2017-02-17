package play.test.mail;

import java.io.Serializable;

public class Attachment implements Serializable {
  public final String name;
  public final byte[] content;

  public Attachment(String name, byte[] content) {
    this.name = name;
    this.content = content;
  }
}
