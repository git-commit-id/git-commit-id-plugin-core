/*
 * This file is part of git-commit-id-plugin-core by Konrad 'ktoso' Malawski <konrad.malawski@java.pl>
 *
 * git-commit-id-plugin-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * git-commit-id-plugin-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with git-commit-id-plugin-core.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.project13.core.util;

import nu.studer.java.util.OrderedProperties;
import pl.project13.core.CannotReadFileException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

public class PropertyManager {
  public static void putWithoutPrefix(@Nonnull Properties properties, String key, String value) {
    if (!isNotEmpty(value)) {
      value = "Unknown";
    }
    properties.setProperty(key, value);
  }

  private static boolean isNotEmpty(@Nullable String value) {
    return null != value && !" ".equals(value.trim().replaceAll(" ", ""));
  }

  public static Properties readProperties(@Nonnull File propertiesFile) throws CannotReadFileException {
    return PropertyManager.readProperties(propertiesFile, StandardCharsets.ISO_8859_1);
  }

  public static Properties readProperties(@Nonnull File propertiesFile, @Nonnull Charset charset) throws CannotReadFileException {
    try (FileInputStream fis = new FileInputStream(propertiesFile);
         InputStreamReader reader = new InputStreamReader(fis, charset)) {
      final OrderedProperties retVal = new OrderedProperties();
      retVal.load(reader);
      return retVal.toJdkProperties();
    } catch (final Exception ex) {
      throw new CannotReadFileException(ex);
    }
  }

  public static void dumpProperties(OutputStream outputStream, OrderedProperties sortedLocalProperties, boolean escapeUnicode) throws IOException {
    try (Writer outputWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
      // use the OrderedProperties.store(Writer, ...)-method to avoid illegal reflective access warning
      // see: https://github.com/git-commit-id/git-commit-id-maven-plugin/issues/523
      outputWriter.write("#Generated by Git-Commit-Id-Plugin");
      outputWriter.write(System.getProperty("line.separator"));
      for (Map.Entry<String, String> e : sortedLocalProperties.entrySet()) {
        String key = saveConvert(e.getKey(), true, escapeUnicode);
        String val = saveConvert(e.getValue(), false, escapeUnicode);
        outputWriter.write(key + "=" + val);
        outputWriter.write(System.getProperty("line.separator"));
      }
    }
  }

  /*
   * Converts unicodes to encoded &#92;uxxxx and escapes
   * special characters with a preceding slash.
   * @see java.util.Properties#saveConvert
   */
  private static String saveConvert(String theString,
                             boolean escapeSpace,
                             boolean escapeUnicode) {
    int len = theString.length();
    int bufLen = len * 2;
    if (bufLen < 0) {
      bufLen = Integer.MAX_VALUE;
    }
    StringBuffer outBuffer = new StringBuffer(bufLen);

    for (int x = 0; x < len; x++) {
      char aChar = theString.charAt(x);
      // Handle common case first, selecting largest block that
      // avoids the specials below
      if ((aChar > 61) && (aChar < 127)) {
        if (aChar == '\\') {
          outBuffer.append('\\');
          outBuffer.append('\\');
          continue;
        }
        outBuffer.append(aChar);
        continue;
      }
      switch (aChar) {
        case ' ':
          if (x == 0 || escapeSpace) {
            outBuffer.append('\\');
          }
          outBuffer.append(' ');
          break;
        case '\t':
          outBuffer.append('\\');
          outBuffer.append('t');
          break;
        case '\n':
          outBuffer.append('\\');
          outBuffer.append('n');
          break;
        case '\r':
          outBuffer.append('\\');
          outBuffer.append('r');
          break;
        case '\f':
          outBuffer.append('\\');
          outBuffer.append('f');
          break;
        case '=': // Fall through
        case ':': // Fall through
        case '#': // Fall through
        case '!':
          outBuffer.append('\\');
          outBuffer.append(aChar);
          break;
        default:
          if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
            outBuffer.append('\\');
            outBuffer.append('u');
            outBuffer.append(toHex((aChar >> 12) & 0xF));
            outBuffer.append(toHex((aChar >>  8) & 0xF));
            outBuffer.append(toHex((aChar >>  4) & 0xF));
            outBuffer.append(toHex(aChar        & 0xF));
          } else {
            outBuffer.append(aChar);
          }
      }
    }
    return outBuffer.toString();
  }

  /**
   * Convert a nibble to a hex character
   * @param   nibble  the nibble to convert.
   */
  private static char toHex(int nibble) {
    return hexDigit[(nibble & 0xF)];
  }

  /** A table of hex digits */
  private static final char[] hexDigit = {
      '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
  };

}
