/**
 * This file Copyright (c) 2005-2008 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain Eclipse Public Licensed code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.rdt.profiling;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.zip.CRC32;

/**
 * DO NOT EDIT THIS FILE
 * 
 * @author Contact Kevin Sawicki (ksawicki@aptana.com) or Ingo Muschenetz (ingo@aptana.com) for details
 */
/**
 * Client Key
 */
public final class ClientKey
{

	/**
	 * BEGIN_LICENSE_MARKER
	 */
	public static final String BEGIN_LICENSE_MARKER = "--begin-aptana-license--";

	/**
	 * END_LICENSE_MARKER
	 */
	public static final String END_LICENSE_MARKER = "--end-aptana-license--";

	// private static final Pattern EMAIL_PATTERN = Pattern
	// .compile("^[a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]+(\\.[a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~])*@[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,6}$");

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	private static final String EMAILS_NON_MATCHING = "EMAILS_NON_MATCHING";
	private static final int PRO = 0;
	private static final int TRIAL = 1;

	private String email;
	private long expiration;
	private int type;

	private ClientKey(int type, String email, long expiration)
	{
		this.type = type;
		this.email = email;
		this.expiration = expiration;
	}

	/**
	 * Decrypts an encrypted key string
	 * 
	 * @param encrypted
	 * @param email
	 * @return client key
	 */
	public static ClientKey decrypt(String encrypted, String email)
	{
		// Public Keys
		// Key Pair 1 - 1024 bits
		String modulus = "115801190261221214754334668902722425936509505416457970789287297728816388753627896293249501578830570324705253515546383166989625001335561947096747210280001245977114030627247212292377290543869343996595819188362915644707269064020812435233012510929338706599216007185654748959001143012936618501934698642942289379979";
		String exponent = "65537";

		// Key Pair 2 - 512 bits
		// String modulus =
		// "7161565172885831980647861120279411120958785711339417901210739422288321094397693927782833643808877128760462895064358542798826171057389792795593008266372799";
		// String exponent = "65537";
		if (encrypted != null)
		{
			encrypted = encrypted.trim();
		}
		Decrypt decrypter = new Decrypt(exponent, modulus);
		return decrypt(decrypter, encrypted, email);
	}

	private static ClientKey decrypt(Decrypt decrypter, String encrypted, String email)
	{
		String value = decrypter.decrypt(encrypted);
		if (value == null)
		{
			return new ClientKey(TRIAL, null, 0L);
		}
		String[] values = value.split(";");
		int type = TRIAL;
		String genedEmail = null;
		long expiration = 0L;
		if (values.length == 3)
		{
			if ("p".equals(values[0].toLowerCase()))
			{
				type = PRO;
			}
			genedEmail = values[1];
			// Verify decrypted email against entered email
			if (genedEmail != null)
			{
				if (!genedEmail.equalsIgnoreCase(email))
				{
					genedEmail = EMAILS_NON_MATCHING;
				}
			}
			else
			{
				genedEmail = null;
			}
			try
			{
				expiration = Long.parseLong(values[2]);
			}
			catch (Exception e)
			{
				expiration = 0L;
			}
		}
		return new ClientKey(type, genedEmail, expiration);
	}

	/**
	 * True if this key is close to expiring
	 * 
	 * @return true if close, false otherwise
	 */
	public boolean isCloseToExpiring()
	{
		Calendar currentCalendar = Calendar.getInstance(ClientKey.GMT);
		currentCalendar.add(Calendar.MONTH, 1);
		return getExpiration().before(currentCalendar);
	}

	/**
	 * True if valid key
	 * 
	 * @return true if valid, false otherwise
	 */
	public boolean isValid()
	{
		return email != null && email != EMAILS_NON_MATCHING;
	}

	/**
	 * True if key has valid fields but email did not match
	 * 
	 * @return - true if close, false otherwise
	 */
	public boolean isCloseToMatching()
	{
		return email == EMAILS_NON_MATCHING;
	}

	/**
	 * True if this key is expired
	 * 
	 * @return true if expired, false otherwise
	 */
	public boolean isExpired()
	{
		Calendar currentCalendar = Calendar.getInstance(GMT);
		return currentCalendar.after(getExpiration());
	}

	/**
	 * Gets the email for this key
	 * 
	 * @return - email
	 */
	public String getEmail()
	{
		return email;
	}

	/**
	 * Gets the expiration of this key
	 * 
	 * @return - calendar
	 */
	public Calendar getExpiration()
	{
		Calendar expirationCal = Calendar.getInstance(GMT);
		expirationCal.setTimeInMillis(expiration);
		return expirationCal;
	}

	/**
	 * True if trial key
	 * 
	 * @return true if trial, false otherwise
	 */
	public boolean isTrial()
	{
		return type == TRIAL;
	}

	/**
	 * True if pro key
	 * 
	 * @return true if pro, false otherwise
	 */
	public boolean isPro()
	{
		return !isTrial();
	}

	/**
	 * True if professional plugins should run
	 * 
	 * @return true to run pros, false otherwise
	 */
	public boolean shouldProPluginsRun()
	{
		if (isPro())
		{
			return true;
		}
		return !isExpired();
	}

	/**
	 * Gets the trimmed version of a license key
	 * 
	 * @param encrypted
	 * @return - trimmed key
	 */
	public static String trimEncryptedLicense(String encrypted)
	{
		String newEncrypted = encrypted;
		newEncrypted = newEncrypted.trim();
		newEncrypted = newEncrypted.replaceAll(BEGIN_LICENSE_MARKER, "");
		newEncrypted = newEncrypted.replaceAll(END_LICENSE_MARKER, "");
		newEncrypted = newEncrypted.replaceAll("\\s+", "");
		return newEncrypted;
	}

	/**
	 * Internal decrypting class
	 */
	private static class Decrypt
	{
		private BigInteger modulus;
		private BigInteger exponent;

		Decrypt(String exponent, String modulus)
		{
			this.modulus = new BigInteger(modulus);
			this.exponent = new BigInteger(exponent);
		}

		/**
		 * Decrypts an encrypted string
		 * 
		 * @param encrypted
		 * @return - decrypted string
		 */
		public String decrypt(String encrypted)
		{
			try
			{
				if (encrypted == null)
				{
					encrypted = "";
				}
				else
				{
					encrypted = trimEncryptedLicense(encrypted);
				}
				BigInteger big = new BigInteger(encrypted);
				BigInteger decrypted = big.modPow(exponent, modulus);

				long crc32Value = ((long) decrypted.intValue()) & 0xffffffffL;
				decrypted = decrypted.shiftRight(32);
				byte[] bytes = decrypted.toByteArray();
				CRC32 crc32 = new CRC32();
				crc32.update(bytes);
				if (crc32Value == crc32.getValue())
				{
					return new String(bytes);
				}
			}
			catch (NumberFormatException e)
			{
			}
			return null;
		}
	}

	/**
	 * TODO add later to call from command line
	 * 
	 * @param args
	 */
	// public static void main(String[] args)
	// {
	// if (args.length == 2)
	// {
	// String email = args[0];
	// String key = args[1];
	// }
	// }
}
