/**
 * Encryption utility library
 * Supports SM4 (Chinese National Standard) and AES encryption
 *
 * @author SmartAdmin Team
 * @date 2026-03-04
 */
import CryptoJS from 'crypto-js';
import CryptoSM from 'sm-crypto';

/**
 * Convert object to string for encryption
 */
function object2string(data: unknown): string {
  if (typeof data === 'object') {
    return JSON.stringify(data);
  }

  let str = JSON.stringify(data);
  if (str.startsWith("'") || str.startsWith('"')) {
    str = str.substring(1);
  }
  if (str.endsWith("'") || str.endsWith('"')) {
    str = str.substring(0, str.length - 1);
  }
  return str;
}

/**
 * Convert string to hexadecimal
 */
function stringToHex(str: string): string {
  let hex = '';
  for (let i = 0; i < str.length; i++) {
    hex += str.charCodeAt(i).toString(16).padStart(2, '0');
  }
  return hex;
}

/*
 * -------------------- ※ SM4 Encryption/Decryption begin ※ --------------------
 *
 * 1. SM4 (Chinese National Standard) requires 128-bit key = 16 bytes
 * 2. Frontend uses UCS-2 or UTF-16 encoding, letters/numbers/symbols = 1 byte each
 * 3. Java backend also uses 1 byte per letter/number
 * 4. Key composition: 16 letters/numbers/symbols
 *
 * -------------------- ※ SM4 Encryption/Decryption end ※ --------------------
 */
const SM4_KEY = '1024lab__1024lab';

const SM4 = {
  encryptData: function (data: unknown): string {
    // Step 1: SM4 encrypt
    const encryptData = CryptoSM.sm4.encrypt(object2string(data), stringToHex(SM4_KEY));
    // Step 2: Base64 encode
    return CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(encryptData));
  },

  decryptData: function (data: string): string {
    // Step 1: Base64 decode
    const words = CryptoJS.enc.Base64.parse(data);
    const decode64Str = CryptoJS.enc.Utf8.stringify(words);

    // Step 2: SM4 decrypt
    return CryptoSM.sm4.decrypt(decode64Str, stringToHex(SM4_KEY));
  },
};

// -----------------------  Public API: Encryption/Decryption -----------------------

// Default to SM4 algorithm (Chinese National Standard)
const EncryptObject = SM4;

/**
 * Encrypt data using SM4 algorithm
 *
 * @param data - Data to encrypt (string, number, object, etc.)
 * @returns Encrypted Base64 string
 *
 * @example
 * const encrypted = encryptData('myPassword123');
 * // Returns: "Q3J5cHRvZ3JhcGh5..."
 */
export const encryptData = function (data: unknown): string | null {
  return !data ? null : EncryptObject.encryptData(data);
};

/**
 * Decrypt data using SM4 algorithm
 *
 * @param data - Base64 encrypted string
 * @returns Decrypted string
 *
 * @example
 * const decrypted = decryptData('Q3J5cHRvZ3JhcGh5...');
 * // Returns: "myPassword123"
 */
export const decryptData = function (data: string): string | null {
  return !data ? null : EncryptObject.decryptData(data);
};
