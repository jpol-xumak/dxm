/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.apache.lucene.search.spell;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.Iterator;

public class JahiaExtendedSpellChecker extends SpellChecker {

    public static final String F_LANGUAGE = "language".intern();

    public static final String F_SITE = "site".intern();
    
    private static final String F_WORD_INTERNED = F_WORD.intern();

    // minimum score for hits generated by the spell checker query
    private float minScore = 0.5f;

    /**
     * Boost value for start and end grams
     */
    private float bStart = 2.0f;

    private float bEnd = 1.0f;

    private IndexSearcher searcher;

    /**
     * Use the given directory as a spell checker index. The directory is
     * created if it doesn't exist yet.
     * 
     * @param spellIndex
     * @throws IOException
     */
    public JahiaExtendedSpellChecker(Directory spellIndex, StringDistance sd) throws IOException {
        super(spellIndex, sd);
    }

    public JahiaExtendedSpellChecker(Directory spellIndex) throws IOException {
        super(spellIndex);
    }

    /**
     * Suggest similar words (optionally restricted to a field of an index).
     * 
     * <p>
     * As the Lucene similarity that is used to fetch the most relevant
     * n-grammed terms is not the same as the edit distance strategy used to
     * calculate the best matching spell-checked word from the hits that Lucene
     * found, one usually has to retrieve a couple of numSug's in order to get
     * the true best match.
     * 
     * <p>
     * I.e. if numSug == 1, don't count on that suggestion being the best one.
     * Thus, you should set this value to <b>at least</b> 5 for a good
     * suggestion.
     * 
     * @param word
     *            the word you want a spell check done on
     * @param numSug
     *            the number of suggested words
     * @param ir
     *            the indexReader of the user index (can be null see field
     *            param)
     * @param field
     *            the field of the user index: if field is not null, the
     *            suggested words are restricted to the words present in this
     *            field.
     * @param morePopular
     *            return only the suggest words that are as frequent or more
     *            frequent than the searched word (only if restricted mode =
     *            (indexReader!=null and field!=null)
     * @throws IOException
     * @return String[] the sorted list of the suggest words with these 2
     *         criteria: first criteria: the edit distance, second criteria
     *         (only if restricted mode): the popularity of the suggest words in
     *         the field of the user index
     */
    public String[] suggestSimilar(String word, int numSug, IndexReader ir, String field, boolean morePopular,
            String site, String language) throws IOException {

        float min = this.minScore;
        final int lengthWord = word.length();

        final int freq = (ir != null && field != null) ? ir.docFreq(new Term(field, word)) : 0;
        final int goalFreq = (morePopular && ir != null && field != null) ? freq : 0;
        // if the word exists in the real index and we don't care for word
        // frequency, return the word itself
        if (!morePopular && freq > 0) {
            return new String[] { word };
        }

        BooleanQuery query = new BooleanQuery();
        String[] grams;
        String key;

        // ensure language
        if (language != null) {
            add(query, F_LANGUAGE, language, BooleanClause.Occur.MUST);
        }

        // ensure site
        if (site != null) {
            add(query, F_SITE, site, BooleanClause.Occur.MUST);
        }

        for (int ng = getMin(lengthWord); ng <= getMax(lengthWord); ng++) {

            key = "gram" + ng; // form key

            grams = formGrams(word, ng); // form word into ngrams (allow dups
            // too)

            if (grams.length == 0) {
                continue; // hmm
            }

            if (bStart > 0) { // should we boost prefixes?
                add(query, "start" + ng, grams[0], bStart); // matches start of
                // word

            }
            if (bEnd > 0) { // should we boost suffixes
                add(query, "end" + ng, grams[grams.length - 1], bEnd); // matches
                // end of
                // word

            }
            for (int i = 0; i < grams.length; i++) {
                add(query, key, grams[i]);
            }
        }

        int maxHits = 10 * numSug;

        // System.out.println("Q: " + query);
        IndexSearcher usedSearcher = searcher;
        ScoreDoc[] hits = null;
        boolean retry = true;
        while (retry) {
            boolean useOtherSearcher = false;            
            try {
                hits = usedSearcher.search(query, maxHits).scoreDocs;
            } catch (IOException e) {
                if (retry && usedSearcher != searcher) {
                    usedSearcher = searcher;
                    useOtherSearcher = true;    
                } else {
                    throw e;
                }
            } 
            if (!useOtherSearcher) {
                retry = false;
            }
        }
        // System.out.println("HITS: " + hits.length());
        SuggestWordQueue sugQueue = new SuggestWordQueue(numSug);

        // go thru more than 'maxr' matches in case the distance filter triggers

        int stop = hits == null ? 0 : Math.min(hits.length, 10 * numSug);

        SuggestWord sugWord = new SuggestWord();
        for (int i = 0; i < stop; i++) {

            sugWord.string = usedSearcher.doc(hits[i].doc).get(language != null ? (F_WORD + "-" + language) : F_WORD); // get
            // orig
            // word

            // don't suggest a word for itself, that would be silly
            if (sugWord.string == null || word.equals(sugWord.string)) {
                continue;
            }

            // edit distance
            sugWord.score = getStringDistance().getDistance(word, sugWord.string);
            if (sugWord.score < min) {
                continue;
            }

            if (ir != null && field != null) { // use the user index
                sugWord.freq = ir.docFreq(new Term(field, sugWord.string)); // freq
                // in
                // the
                // index
                // don't suggest a word that is not present in the field
                if ((morePopular && goalFreq > sugWord.freq) || sugWord.freq < 1) {
                    continue;
                }
            }
            sugQueue.insertWithOverflow(sugWord);
            if (sugQueue.size() == numSug) {
                // if queue full, maintain the minScore score
                min = ((SuggestWord) sugQueue.top()).score;
            }
            sugWord = new SuggestWord();
        }

        // convert to array string
        String[] list = new String[sugQueue.size()];
        for (int i = sugQueue.size() - 1; i >= 0; i--) {
            list[i] = ((SuggestWord) sugQueue.pop()).string;
        }

        return list;
    }

    private int getMin(int l) {
        if (l > 5) {
            return 3;
        }
        if (l == 5) {
            return 2;
        }
        return 1;
    }

    private int getMax(int l) {
        if (l > 5) {
            return 4;
        }
        if (l == 5) {
            return 3;
        }
        return 2;
    }

    /**
     * Sets the accuracy 0 &lt; minScore &lt; 1; default 0.5
     */
    public void setAccuracy(float minScore) {
        this.minScore = minScore;
        super.setAccuracy(minScore);
    }

    /**
     * Form all ngrams for a given word.
     * 
     * @param text
     *            the word to parse
     * @param ng
     *            the ngram length e.g. 3
     * @return an array of all ngrams in the word and note that duplicates are
     *         not removed
     */
    private static String[] formGrams(String text, int ng) {
        int len = text.length();
        String[] res = new String[len - ng + 1];
        for (int i = 0; i < len - ng + 1; i++) {
            res[i] = text.substring(i, i + ng);
        }
        return res;
    }

    /**
     * Add a clause to a boolean query.
     */
    private static void add(BooleanQuery q, String name, String value, float boost) {
        Query tq = new TermQuery(new Term(name, value));
        tq.setBoost(boost);
        q.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));
    }

    private static void add(BooleanQuery q, String name, String value, Occur occur) {
        q.add(new BooleanClause(new TermQuery(new Term(name, value)), occur));
    }

    /**
     * Add a clause to a boolean query.
     */
    private static void add(BooleanQuery q, String name, String value) {
        q.add(new BooleanClause(new TermQuery(new Term(name, value)), BooleanClause.Occur.SHOULD));
    }

    /**
     * Use a different index as the spell checker index or re-open the existing
     * index if <code>spellIndex</code> is the same value as given in the
     * constructor.
     * 
     * @param spellIndex
     * @throws IOException
     */
    public void setSpellIndex(Directory spellIndex) throws IOException {
        this.spellIndex = spellIndex;
        if (!IndexReader.indexExists(spellIndex)) {
            IndexWriter writer = new IndexWriter(spellIndex, null, true, IndexWriter.MaxFieldLength.UNLIMITED);
            writer.close();
        }
        // close the old searcher, if there was one
        reopenSearcher();
    }
    
    private void reopenSearcher() throws IOException {
        // create a new and close the old searcher
        IndexSearcher oldSearcher = searcher; 
        IndexSearcher newSearcher = new IndexSearcher(this.spellIndex);
        searcher = newSearcher;
        if (oldSearcher != null) {
            oldSearcher.close();      
        }
    }

    /**
     * Removes all terms from the spell check index.
     * 
     * @throws IOException
     */
    public void clearIndex() throws IOException {
        IndexWriter writer = new IndexWriter(spellIndex, null, true, IndexWriter.MaxFieldLength.UNLIMITED);
        writer.close();

        reopenSearcher();
    }

    /**
     * Check whether the word exists in the index.
     * 
     * @param word
     * @throws IOException
     * @return true iff the word exists in the index
     */
    public boolean exist(String word, String langCode, String site) throws IOException {
        BooleanQuery query = new BooleanQuery();
        add(query, langCode != null ? (F_WORD + "-" + langCode) : F_WORD, word, BooleanClause.Occur.MUST);
        add(query, F_SITE, site, BooleanClause.Occur.MUST);
        return searcher.search(query,1).scoreDocs.length > 0;
    }

    /**
     * Indexes the data from the given {@link Dictionary}.
     * 
     * @param dict
     *            Dictionary to index
     * @param mergeFactor
     *            mergeFactor to use when indexing
     * @param ramMB
     *            the max amount or memory in MB to use
     * @throws IOException
     */
    public void indexDictionary(Dictionary dict, int mergeFactor, int ramMB, String site, String langCode)
            throws IOException {
        IndexWriter writer = new IndexWriter(spellIndex, new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
        writer.setMergeFactor(mergeFactor);
        writer.setRAMBufferSizeMB(ramMB);

        Iterator<?> iter = dict.getWordsIterator();
        while (iter.hasNext()) {
            String word = (String) iter.next();

            int len = word.length();
            if (len < 3) {
                continue; // too short we bail but "too long" is fine...
            }

            if (this.exist(word, langCode, site)) { // if the word already exist in
                // the gramindex
                continue;
            }

            // ok index the word
            Document doc = createDocument(word, getMin(len), getMax(len), site, langCode);
            writer.addDocument(doc);
        }
        // close writer
        writer.optimize();
        writer.close();
        // also re-open the spell index to see our own changes when the next
        // suggestion is fetched:
        // create a new and close the old searcher
        IndexSearcher oldSearcher = searcher; 
        IndexSearcher newSearcher = new IndexSearcher(this.spellIndex);
        searcher = newSearcher;
        oldSearcher.close();
    }

    private static Document createDocument(String text, int ng1, int ng2, String site, String langCode) {
        Document doc = new Document();
        doc.add(new Field(langCode != null ? (F_WORD + "-" + langCode) : F_WORD_INTERNED, langCode != null, text, Field.Store.YES,
                Field.Index.NOT_ANALYZED, Field.TermVector.NO)); // orig term
        doc.add(new Field(F_LANGUAGE, false, langCode, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO)); // language
        doc.add(new Field(F_SITE, false, site, Field.Store.YES, Field.Index.NOT_ANALYZED, Field.TermVector.NO)); // site
        addGram(text, doc, ng1, ng2);
        return doc;
    }

    private static void addGram(String text, Document doc, int ng1, int ng2) {
        int len = text.length();
        for (int ng = ng1; ng <= ng2; ng++) {
            String key = "gram" + ng;
            String end = null;
            for (int i = 0; i < len - ng + 1; i++) {
                String gram = text.substring(i, i + ng);
                doc.add(new Field(key, true, gram, Field.Store.NO, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
                if (i == 0) {
                    doc.add(new Field("start" + ng, true, gram, Field.Store.NO, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
                }
                end = gram;
            }
            if (end != null) { // may not be present if len==ng1
                doc.add(new Field("end" + ng, true, end, Field.Store.NO, Field.Index.NOT_ANALYZED, Field.TermVector.NO));
            }
        }
    }
    
    public void close() throws IOException {
        if (searcher != null) {
            searcher.close();
        }
    }
}
