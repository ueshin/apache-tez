/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tez.runtime.library.common.security;


import java.io.IOException;
import java.net.URL;

import javax.crypto.SecretKey;

import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.io.WritableComparator;
import org.apache.tez.common.security.JobTokenSecretManager;

/**
 * 
 * utilities for generating kyes, hashes and verifying them for shuffle
 *
 */
@InterfaceAudience.Private
@InterfaceStability.Unstable
public class SecureShuffleUtils {
  public static final String HTTP_HEADER_URL_HASH = "UrlHash";
  public static final String HTTP_HEADER_REPLY_URL_HASH = "ReplyHash";
  
  /**
   * Base64 encoded hash of msg
   * @param msg
   */
  public static String generateHash(byte[] msg, SecretKey key) {
    return new String(Base64.encodeBase64(generateByteHash(msg, key)), Charsets.UTF_8);
  }

  /**
   * calculate hash of msg
   * @param msg
   * @return byte array containing computed hash of message
   */
  private static byte[] generateByteHash(byte[] msg, SecretKey key) {
    return JobTokenSecretManager.computeHash(msg, key);
  }

  /**
   * verify that hash equals to HMacHash(msg)
   * @param hash
   * @param msg
   * @param mgr JobTokenSecretManager
   * @return true when hashes match; false otherwise
   */
  private static boolean verifyHash(byte[] hash, byte[] msg, JobTokenSecretManager mgr) {
    byte[] msg_hash = mgr.computeHash(msg);
    return WritableComparator.compareBytes(msg_hash, 0, msg_hash.length, hash, 0, hash.length) == 0;
  }

  /**
   * Aux util to calculate hash of a String
   * @param enc_str
   * @param mgr JobTokenSecretManager
   * @return Base64 encodedHash
   * @throws IOException
   */
  public static String hashFromString(String enc_str, JobTokenSecretManager mgr)
      throws IOException {
    return new String(Base64.encodeBase64(mgr.computeHash(enc_str.getBytes(Charsets.UTF_8))), Charsets.UTF_8);
  }
  
  /**
   * verify that base64Hash is same as HMacHash(msg)
   * @param base64Hash (Base64 encoded hash)
   * @param msg
   * @throws IOException if not the same
   */
  public static void verifyReply(String base64Hash, String msg, JobTokenSecretManager mgr)
      throws IOException {
    byte[] hash = Base64.decodeBase64(base64Hash.getBytes(Charsets.UTF_8));

    boolean res = verifyHash(hash, msg.getBytes(Charsets.UTF_8), mgr);

    if(res != true) {
      throw new IOException("Verification of the hashReply failed");
    }
  }
  
  /**
   * Shuffle specific utils - build string for encoding from URL
   * @param url
   * @return string for encoding
   */
  public static String buildMsgFrom(URL url) {
    return buildMsgFrom(url.getPath(), url.getQuery(), url.getPort());
  }

  /**
   * Shuffle specific utils - build string for encoding from URL
   * @param uri_path
   * @param uri_query
   * @return string for encoding
   */
  private static String buildMsgFrom(String uri_path, String uri_query, int port) {
    return String.valueOf(port) + uri_path + "?" + uri_query;
  }
}
