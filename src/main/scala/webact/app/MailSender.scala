package webact.app

import javax.mail._
import javax.mail.internet._
import org.xbill.DNS._
import cats.syntax.all._, cats.instances.all._, cats.data._
import org.slf4j._

import webact.config._

object MailSender {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  case class Mail(address: String) {
    val (user, domain) = {
      address.lastIndexOf('@') match {
        case idx if idx > 0 =>
          (address.substring(0, idx), address.substring(idx+1))
        case _ => sys.error("Invalid email: "+ address)
      }
    }
  }

  case class Message(recipients: Seq[Mail], sender: Mail, subject: String, text: String, listId: Option[String])


  final class SmtpClient(settings: Config.Smtp) {
    def send(m: Message): ValidatedNel[Exception, Unit] = {
      if (settings.host.nonEmpty) {
        send(m, m.recipients, None).toValidatedNel
      } else {
        val bydomain = m.recipients.groupBy(_.domain)
        bydomain.map({
          case (domain, recipients) =>
            val mx = findMx(domain).headOption
            send(m, recipients, mx)
        })
        .map(_.toValidatedNel)
        .reduce(_ combine _)
      }
    }

    def send(m: Message, recipients: Seq[Mail], mx: Option[String]): Either[Exception, Unit] = {
      makeSession(mx).map(new MimeMessage(_)).flatMap { msg =>
        Either.catchOnly[Exception] {
          msg.setFrom(new InternetAddress(m.sender.address))
          for (to <- recipients) {
            msg.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to.address))
          }
          m.listId.foreach(id => msg.setHeader("List-Id", "<"+id+">"))
          msg.setSubject(m.subject)
          msg.setText(m.text)
          Transport.send(msg)
          logger.debug("sent mail to {}", recipients)
        }
      }
    }

    def makeSession(mx: Option[String]): Either[Exception, Session] = {
      val props = System.getProperties()
      if (settings.host.nonEmpty) {
        props.setProperty("mail.smtp.host", settings.host)
        if (settings.port > 0) {
          props.setProperty("mail.smtp.port", settings.port.toString)
        }
        if (settings.user.nonEmpty) {
          props.setProperty("mail.user", settings.user)
        }
        if (settings.password.nonEmpty) {
          props.setProperty("mail.password", settings.password)
        }
      } else {
        mx.foreach { host =>
          logger.debug("using mx: "+ host)
          props.setProperty("mail.smtp.host", host)
        }
      }
      if (Option(props.getProperty("mail.smtp.host")).exists(_.nonEmpty))
        Right(Session.getInstance(props))
      else Left(new Exception("no smtp host provided"))
    }

    def findMx(domain: String): List[String] = {
      Either.catchOnly[Exception] {
        val records = new Lookup(domain, Type.MX).run()
          .map(_.asInstanceOf[MXRecord]).toList.sortBy(_.getPriority)

        records.map(_.getTarget.toString.stripSuffix("."))
      }.getOrElse(Nil)
    }
  }

  object SmtpClient {
//    def apply(): SmtpClient = new SmtpClient(Config.Smtp("", 0, "", "", Mail("pill@test.com")))
    def apply(s: Config.Smtp): SmtpClient = new SmtpClient(s)

  //  def fromConfig = apply(Config.master.smtp)
  }

}
