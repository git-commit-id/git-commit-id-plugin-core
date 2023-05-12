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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import pl.project13.core.CannotReadFileException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class YmlManager {
  protected static void dumpYml(OutputStream outputStream, OrderedProperties sortedLocalProperties, Charset sourceCharset) throws IOException {
    try (Writer outputWriter = new OutputStreamWriter(outputStream, sourceCharset)) {
      DumperOptions dumperOptions = new DumperOptions();
      dumperOptions.setAllowUnicode(true);
      dumperOptions.setAllowReadOnlyProperties(true);
      dumperOptions.setPrettyFlow(true);
      // dumperOptions.setCanonical(true);
      dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.AUTO);

      Yaml yaml = new Yaml(dumperOptions);

      Map<String, Object> dataMap = new HashMap<>();
      for (Map.Entry<String, String> e: sortedLocalProperties.entrySet()) {
        dataMap.put(e.getKey(), e.getValue());
      }
      yaml.dump(dataMap, outputWriter);
    }
  }

  protected static Properties readYmlProperties(@Nonnull File xmlFile, Charset sourceCharset) throws CannotReadFileException {
    Properties retVal = new Properties();

    try (FileInputStream fis = new FileInputStream(xmlFile)) {
      try (InputStreamReader reader = new InputStreamReader(fis, sourceCharset)) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        loaderOptions.setAllowRecursiveKeys(false);
        loaderOptions.setProcessComments(false);
        Yaml yaml = new Yaml(loaderOptions);
        Map<String, Object> data = yaml.load(reader);
        for (Map.Entry<String, Object> e: data.entrySet()) {
          retVal.put(e.getKey(), e.getValue());
        }
      }
    } catch (IOException e) {
      throw new CannotReadFileException(e);
    }
    return retVal;
  }
}
