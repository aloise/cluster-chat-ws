package global.crypto

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import com.google.inject.Inject
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import play.api._
import play.api.libs.Codecs

/**
  * User: aloise
  * Date: 15.05.16
  * Time: 16:42
  */

trait CryptoProvider {
  def encryptAES(value: String): String
  def decryptAES(value: String): String
  def sign(message: String):String
}


class Crypto ( config:CryptoConfig ) extends CryptoProvider {

  def encryptAES(value: String): String = {
    encryptAES(value, config.secret)
  }


  def encryptAES(value: String, privateKey: String): String = {
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    val cipher = getCipherWithConfiguredProvider(config.aesTransformation)
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    val encryptedValue = cipher.doFinal(value.getBytes("utf-8"))
    // return a formatted, versioned encrypted string
    // '2-*' represents an encrypted payload with an IV
    // '1-*' represents an encrypted payload without an IV
    Option(cipher.getIV) match {
      case Some(iv) => s"2-${Base64.encodeBase64String(iv ++ encryptedValue)}"
      case None => s"1-${Base64.encodeBase64String(encryptedValue)}"
    }
  }

  /**
    * Generates the SecretKeySpec, given the private key and the algorithm.
    */
  private def secretKeyWithSha256(privateKey: String, algorithm: String) = {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(privateKey.getBytes("utf-8"))
    // max allowed length in bits / (8 bits to a byte)
    val maxAllowedKeyLength = Cipher.getMaxAllowedKeyLength(algorithm) / 8
    val raw = messageDigest.digest().slice(0, maxAllowedKeyLength)
    new SecretKeySpec(raw, algorithm)
  }

  /**
    * Gets a Cipher with a configured provider, and a configurable AES transformation method.
    */
  private def getCipherWithConfiguredProvider(transformation: String): Cipher = {
    config.provider.fold(Cipher.getInstance(transformation)) { p =>
      Cipher.getInstance(transformation, p)
    }
  }


  def decryptAES(value: String): String = {
    decryptAES(value, config.secret)
  }


  def decryptAES(value: String, privateKey: String): String = {
    val seperator = "-"
    val sepIndex = value.indexOf(seperator)
    if (sepIndex < 0) {
      decryptAESVersion0(value, privateKey)
    } else {
      val version = value.substring(0, sepIndex)
      val data = value.substring(sepIndex + 1, value.length())
      version match {
        case "1" =>
          decryptAESVersion1(data, privateKey)
        case "2" =>
          decryptAESVersion2(data, privateKey)
        case _ =>
          throw new Exception("Unknown version")
      }
    }
  }

  /** Backward compatible AES ECB mode decryption support. */
  private def decryptAESVersion0(value: String, privateKey: String): String = {
    val raw = privateKey.substring(0, 16).getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = getCipherWithConfiguredProvider("AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(Codecs.hexStringToByte(value)))
  }

  /** V1 decryption algorithm (No IV). */
  private def decryptAESVersion1(value: String, privateKey: String): String = {
    val data = Base64.decodeBase64(value)
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    val cipher = getCipherWithConfiguredProvider(config.aesTransformation)
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(data), "utf-8")
  }

  /** V2 decryption algorithm (IV present). */
  private def decryptAESVersion2(value: String, privateKey: String): String = {
    val data = Base64.decodeBase64(value)
    val skeySpec = secretKeyWithSha256(privateKey, "AES")
    val cipher = getCipherWithConfiguredProvider(config.aesTransformation)
    val blockSize = cipher.getBlockSize
    val iv = data.slice(0, blockSize)
    val payload = data.slice(blockSize, data.size)
    cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv))
    new String(cipher.doFinal(payload), "utf-8")
  }

  /**
    * Signs the given String with HMAC-SHA1 using the given key.
    *
    * By default this uses the platform default JSSE provider.  This can be overridden by defining
    * `play.crypto.provider` in `application.conf`.
    *
    * @param message The message to sign.
    * @param key The private key to sign with.
    * @return A hexadecimal encoded signature.
    */
  def sign(message: String, key: Array[Byte]): String = {
    val mac = config.provider.fold(Mac.getInstance("HmacSHA1"))(p => Mac.getInstance("HmacSHA1", p))
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    Codecs.toHexString(mac.doFinal(message.getBytes("utf-8")))
  }

  /**
    * Signs the given String with HMAC-SHA1 using the application’s secret key.
    *
    * By default this uses the platform default JSSE provider.  This can be overridden by defining
    * `play.crypto.provider` in `application.conf`.
    *
    * @param message The message to sign.
    * @return A hexadecimal encoded signature.
    */
  def sign(message: String): String = {
    sign(message, config.secret.getBytes("utf-8"))
  }

}


case class CryptoConfig( secret: String, provider: Option[String] = None, aesTransformation: String = "AES/CTR/NoPadding" )

object CryptoConfigParser {

  private val Blank = """\s*""".r

  def get( configuration: Configuration) = {

    /*
     * The Play secret.
     *
     * We want to:
     *
     * 1) Encourage the practice of *not* using the same secret in dev and prod.
     * 2) Make it obvious that the secret should be changed.
     * 3) Ensure that in dev mode, the secret stays stable across restarts.
     * 4) Ensure that in dev mode, sessions do not interfere with other applications that may be or have been running
     *   on localhost.  Eg, if I start Play app 1, and it stores a PLAY_SESSION cookie for localhost:9000, then I stop
     *   it, and start Play app 2, when it reads the PLAY_SESSION cookie for localhost:9000, it should not see the
     *   session set by Play app 1.  This can be achieved by using different secrets for the two, since if they are
     *   different, they will simply ignore the session cookie set by the other.
     *
     * To achieve 1 and 2, we will, in Activator templates, set the default secret to be "changeme".  This should make
     * it obvious that the secret needs to be changed and discourage using the same secret in dev and prod.
     *
     * For safety, if the secret is not set, or if it's changeme, and we are in prod mode, then we will fail fatally.
     * This will further enforce both 1 and 2.
     *
     * To achieve 3, if in dev or test mode, if the secret is either changeme or not set, we will generate a secret
     * based on the location of application.conf.  This should be stable across restarts for a given application.
     *
     * To achieve 4, using the location of application.conf to generate the secret should ensure this.
     */
    val secret = ( configuration.getString("play.crypto.secret") orElse configuration.getString("application.secret") ) match {
      case (Some("changeme") | Some(Blank()) | None)  =>
        Logger.error("The application secret has not been set, and we are in prod mode. Your application is not secure.")
        Logger.error("To set the application secret, please read http://playframework.com/documentation/latest/ApplicationSecret")
        throw new PlayException("Configuration error", "Application secret not set")
      case Some("changeme") | Some(Blank()) | None =>
          // No application.conf?  Oh well, just use something hard coded.
          val secret = "she sells sea shells on the sea shore"
          val md5Secret = DigestUtils.md5Hex(secret)
          Logger.debug(s"Generated dev mode secret $md5Secret for app")

          md5Secret
      case Some(s) => s
    }

    val provider = configuration.getString("play.crypto.provider")
    val transformation = configuration.getString("play.crypto.aes.transformation").getOrElse("AES/CTR/NoPadding")

    CryptoConfig(secret, provider, transformation)
  }


}


