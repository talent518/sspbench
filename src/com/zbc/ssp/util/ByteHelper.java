package com.zbc.ssp.util;

public class ByteHelper {
	public static int byteToInt(byte b) {
		return b >= 0 ? b : 256 + b;
	}
	
	public static byte intToByte(int b) {
		return new Integer(b).byteValue();
	}
	
	public static void main(String[] args) {
		System.out.println(byteToInt((byte)254));
		System.out.println((char)(intToByte(98)));
	}
}
