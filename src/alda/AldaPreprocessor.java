
package alda;

import java.io.File;
import java.io.IOException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


// TODO change IllegalArgumentExeptions to something else.

/**
 * A class that contains utilties for the alda client side preprocessor
 */
public class AldaPreprocessor {

  public static CharSequence preprocess(CharSequence code) {
    return preprocess(code, new File("."));
  }

  /**
   * Preprocesses alda code
   */
  public static CharSequence preprocess(CharSequence code, File path) {
    // TODO match newline, not whitespace after an #include match
    Pattern preBlockPattern = Pattern.compile("^(#\\w+\\s\"[a-zA-Z.-_/\\\\]+\"\\s*)+");
    Matcher preBlockMatcher = preBlockPattern.matcher(code);

    if (preBlockMatcher.find()) {
      // System.out.println(preBlockMatcher.group());
      String preBlock = preBlockMatcher.group();
      StringBuilder toAppend = new StringBuilder();
      CharSequence rawScore = code.subSequence(preBlockMatcher.end(), code.length());

      Pattern prePattern = Pattern.compile("^#(?<directive>\\w+)\\s\"(?<filename>[a-zA-Z.-_/\\\\]+)\"", Pattern.MULTILINE);
      Matcher preMatcher = prePattern.matcher(code);

      while (preMatcher.find()) {
        String directive = preMatcher.group("directive");
        switch (directive) {
        case "include":
          File file = new File(preMatcher.group("filename"));
          try {
            String fileBody = Util.readFile(file);
            toAppend.append("(alda-code \"");
            toAppend.append(fileBody);
            toAppend.append("\")\n");
          } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read file: " + file.getAbsolutePath());
          }
          break;
        default:
          throw new IllegalArgumentException("'" + directive + "' is not a valid preprocessor directive.");
        }
      }
      toAppend.append(rawScore);
      return toAppend;
    } else {
      return code;
    }
  }
}
