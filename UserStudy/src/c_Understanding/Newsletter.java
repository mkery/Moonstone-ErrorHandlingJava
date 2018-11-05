package c_Understanding;

import java.io.IOException;

import Environment.*;

/**
 * A startup is using a self-written software to send out important product
 * information. Only half of all customers are receiving the emails! The team
 * suspects connection drop outs, but only some recipients seem affected.
 *
 * Given the following code, what's another explanation for the undelivered
 * emails?
 *
 * Answer = XX on line XX
 **/

public class Newsletter {

	Logger logger = new Logger();

	public void sendProductInformation(Customer customer, Greeting greeting, String productInformation) {
		Email email = new Email();
		try {
			email.setRecipient(customer);

			try {
				email.append(greeting.stringForCustomer(customer));
			} catch (DatabaseException e) {
				email.append("Dear Sir or Madam,");
			}

			email.append(productInformation);

			try {
				email.send();
			} catch (IOException e) {
				logger.log("Sending email failed", e);
			}

		} catch (EmailException e) {
			logger.log("Unexpected framework error", e);
		}
	}

}
