package com.zbc.ssp.crypt;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Date;

import com.zbc.ssp.util.ByteHelper;

public class Crypt {

	public final static String MD5(String res) {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		try {
			byte[] strTemp = res.getBytes("UTF-8");
			MessageDigest mdTemp = MessageDigest.getInstance("MD5");
			mdTemp.update(strTemp);
			byte[] md = mdTemp.digest();
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			String dd = new String(str);
			return dd;
		} catch (Exception e) {
			return "";
		}
	}

	public final static String encode(String str, String key) {
		return code(str, key, false, 0);
	}

	public final static String encode(String str, String key, int expiry) {
		return code(str, key, false, expiry);
	}

	public final static String decode(String str, String key) {
		return code(str, key, true, 0);
	}

	public final static String decode(String str, String key, int expiry) {
		return code(str, key, true, expiry);
	}

	// 字符串解密加密
	private final static String code(String string, String key, Boolean mode, int expiry) {
		key = MD5(key);

		// 随机密钥长度 取值 0-32;
		// 加入随机密钥，可以令密文无任何规律，即便是原文和密钥完全相同，加密结果也会每次不同，增大破解难度。
		// 取值越大，密文变动规律越大，密文变化 = 16 的 ckey_length 次方
		// 当此值为 0 时，则不产生随机密钥
		int ckey_length = 4;

		String date = MD5(String.valueOf(new Date().getTime()));
		String keya = MD5(key.substring(0, 16));
		String keyb = MD5(key.substring(16, 32));
		String keyc = ckey_length > 0 ? (mode ? string.substring(0, ckey_length) : date.substring(date.length() - ckey_length)) : "";

		byte cryptkey[] = (keya + MD5(keya + keyc)).getBytes();
		int key_length = cryptkey.length;

		byte str[];
		if (mode) {
			str = Base64.base64ToByteArray(string.substring(ckey_length, string.length()));
		} else {
			try {
				str = String.format("%010d%s%s", expiry > 0 ? expiry + new Date().getTime() / 1000 : 0, MD5(string + keyb).substring(0, 16), string).getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				return "";
			}
		}

		Integer string_length = str.length;

		byte result[] = new byte[string_length];
		byte box[] = new byte[256];
		byte rndkey[] = new byte[256];
		int i, j, a;
		byte tmp;

		for (i = 0; i <= 255; i++) {
			rndkey[i] = cryptkey[i % key_length];
			box[i] = ByteHelper.intToByte(i);
		}

		for (j = 0, i = 0; i < 256; i++) {
			j = (j + ByteHelper.byteToInt(box[i]) + ByteHelper.byteToInt(rndkey[i])) % 256;

			tmp = box[i];
			box[i] = box[j];
			box[j] = tmp;
		}

		for (a = 0, j = 0, i = 0; i < string_length; i++) {
			a = (a + 1) & 0xff;
			j = (j + ByteHelper.byteToInt(box[a])) & 0xff;

			tmp = box[a];
			box[a] = box[j];
			box[j] = tmp;

			result[i] = (byte) ((ByteHelper.byteToInt(str[i]) ^ ByteHelper.byteToInt(box[(ByteHelper.byteToInt(box[a]) + ByteHelper.byteToInt(box[j])) % 256])) & 0xff);
		}

		if (mode) {
			String ret;
			try {
				ret = new String(result, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				ret = new String(result);
			}
			Integer timestamp = (int) (new Date().getTime() / 1000);
			Integer time = new Integer(ret.substring(0, 10));

			if ((time == 0 || time - timestamp > 0) && ret.substring(10, 26).equals(MD5(ret.substring(26, ret.length()) + keyb).substring(0, 16))) {
				return ret.substring(26, ret.length());
			} else {
				return "";
			}
		} else {
			return keyc + Base64.byteArrayToBase64(result);
		}
	}

	public static void main(String[] args) {
		String src = "加解密123456465";

		String enc = encode(src, "123456"), dec = decode(enc, "123456");

		System.out.println("### source: " + src);
		System.out.println("###### md5: " + MD5(src));
		System.out.println("### encode: " + enc);
		System.out.println("### decode: " + dec);
		System.out.println("## decode2: " + decode("6993PUFp90Z+LNGRMngtJ26osn3kWSzzqrLlK/KZ82oi43a2KSkgnNRtB3wFnRo=", "123456"));
	}

}
