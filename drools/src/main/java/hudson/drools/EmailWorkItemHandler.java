package hudson.drools;

import hudson.model.User;
import hudson.tasks.MailAddressResolver;
import hudson.tasks.Mailer;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import org.drools.process.instance.WorkItemHandler;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemManager;

public class EmailWorkItemHandler implements WorkItemHandler {

	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		try {
			String recipients = (String) workItem.getParameter("Recipients");
			String cc = (String) workItem.getParameter("CC");
			String bcc = (String) workItem.getParameter("BCC");
			String subject = (String) workItem.getParameter("Subject");
			String body = (String) workItem.getParameter("Body");
			String replyTo = (String) workItem.getParameter("Reply-To");

			String from = (String) workItem.getParameter("From");
			if (from == null) from = Mailer.descriptor().getAdminAddress();

			Message message = new MimeMessage(Mailer.descriptor()
					.createSession());
			message.setFrom(createAddress(from));
			if (replyTo != null) {
				message
						.setReplyTo(new Address[] { createAddress(replyTo) });
			}
			for (String r : recipients.split(",")) {
				message.addRecipient(RecipientType.TO, createAddress(r));
			}
			for (String r : cc.split(",")) {
				message.addRecipient(RecipientType.CC, createAddress(r));
			}
			for (String r : bcc.split(",")) {
				message.addRecipient(RecipientType.CC, createAddress(r));
			}
			message.setSubject(subject);
			message.setText(body);

			Transport.send(message);
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}

		manager.completeWorkItem(workItem.getId(), null);
	}
	
	private Address createAddress(String id) throws AddressException {
		User user =User.get(id, false);
		if (user != null) {
			String address = MailAddressResolver.resolve(user);
			if (user != null) {
				return new InternetAddress(address);
			}
		}
		return new InternetAddress(id);
	}

	public void abortWorkItem(WorkItem arg0, WorkItemManager arg1) {
		// Do nothing, email cannot be aborted
	}

}
