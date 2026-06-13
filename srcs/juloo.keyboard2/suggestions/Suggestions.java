package juloo.keyboard2.suggestions;

import java.util.Arrays;
import java.util.List;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.Config;
import juloo.keyboard2.ComposeKey;
import juloo.keyboard2.ComposeKeyData;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;
  Config _config;
  boolean _enabled;

  /** Current suggestions. The best suggestion is at index [0]. */
  public String[] suggestions = new String[MAX_COUNT];
  /** Number of suggestions at the beginning of the [suggestions] array that
      are not [null]. */
  public int count = 0;
  public String emoji_suggestion = null;
  /** Number of suggestions in [suggestions]. */
  public static final int MAX_COUNT = 3;

  public Suggestions(Callback c, Config conf)
  {
    _callback = c;
    _config = conf;
  }

  public void started()
  {
    _enabled = _config.editor_config.should_show_candidates_view;
    clear();
  }

  public void currently_typed_word(String word)
  {
    if (!_enabled)
      return;
    if (word.length() < 2 || _config.current_dictionary == null)
      clear();
    else
        if(word.startsWith(":") && word.length()>2){
            String[] ss = query_emojis(word.substring(1));
            suggestions[0]=ss[0];
            suggestions[1]=ss[1];
            suggestions[2]=ss[2];
            count = 3;
            emoji_suggestion = ss[3];
        }
        else{
            query_suggestions(word);
        }
    set_suggestions();
  }

  void clear()
  {
    count = 0;
    suggestions[0] = null;
    emoji_suggestion = null;
  }

  int query_suggestions(String word)
  {
    Cdict dict = _config.current_dictionary;
    boolean first_char_upper = Character.isUpperCase(word.charAt(0));
    word = apply_substitutions(word);
    Cdict.Result r = dict.find(word);
    int i = 0;
//    if (r.found)
//      suggestions[i++] = dict.word(r.index);
    int start_distance = 1;
    int[] suffixes = dict.suffixes(r, MAX_COUNT+start_distance);
    // Disable distance search for small words
    int[] dist = (word.length() < 3 || i + 1 >= MAX_COUNT) ? NO_RESULTS :
      dict.distance(word, 1, MAX_COUNT+start_distance);
    for (int j = 1; j < MAX_COUNT+start_distance && i < MAX_COUNT; j++)
    {
      if (suffixes.length > j)
        suggestions[i++] = dict.word(suffixes[j]);
      if (dist.length > j && i < MAX_COUNT)
        suggestions[i++] = dict.word(dist[j]);
    }
    if (first_char_upper)
      capitalize_results();
    emoji_suggestion = query_emoji(word); // word with substitutions applied
    count = i;
    return i;
  }

  void capitalize_results()
  {
    for (int i = 0; i < count; i++)
      suggestions[i] = suggestions[i].substring(0, 1).toUpperCase()
        + suggestions[i].substring(1);
  }

  String query_emoji(String word)
  {
    Cdict dict = _config.emoji_dictionary;
    // Disable emoji suggestion for short words
    if (dict == null || word.length() < 3)
      return null;
    Cdict.Result r = dict.find(word);
    int[] s = dict.suffixes(r, 1);
    if (s.length > 0)
      return dict.word(s[0]);
    return null;
  }
  String[] query_emojis(String word)
  {
    Cdict dict = _config.emoji_dictionary;
    String[] res = {null,null,null,null};
    Cdict.Result r = dict.find(word);
    int[] a = dict.suffixes(r, 4);
    int[] b = dict.distance(word,2, 3);
    int[] c = dict.distance(word,3, 3);
    int i = 0;
    int j = 0;
    int k = 0;
    for(int idx=0; idx < res.length; idx++){
      if((res[idx] == null || res[idx].isEmpty()) && i<a.length){res[idx]=dict.word(a[i++]);}
      if((res[idx] == null || res[idx].isEmpty()) && j<b.length){res[idx]=dict.word(b[j++]);}
      if((res[idx] == null || res[idx].isEmpty()) && k<c.length){res[idx]=dict.word(c[k++]);}
    }
    return res;
  }

  /** Apply the same substitutions that were used when building the
      dictionaries to find word aliases. This catches missing diacritics for
      example. */
  String apply_substitutions(String w)
  {
    StringBuilder b = new StringBuilder(w);
    int len = w.length();
    for (int i = 0; i < len; i++)
    {
      char r =
        ComposeKey.transform_char(ComposeKeyData.substitutions, b.charAt(i));
      if (r != 0) b.setCharAt(i, r);
    }
    return b.toString();
  }

  void set_suggestions()
  {
    _callback.set_suggestions(this);
  }

  static final int[] NO_RESULTS = new int[0];

  public static interface Callback
  {
    public void set_suggestions(Suggestions suggestions);
  }
}
