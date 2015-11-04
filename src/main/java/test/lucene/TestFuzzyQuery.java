package test.lucene;

import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.MaxWordSeg;
import com.chenlb.mmseg4j.analysis.MMSegAnalyzer;
import com.chenlb.mmseg4j.analysis.MaxWordAnalyzer;
import com.chenlb.mmseg4j.example.MaxWord;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by qinbin on 2015/8/5.
 */
public class TestFuzzyQuery {
    public static void main(String[] args) throws IOException, ParseException {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_4_10_0, new MaxWordAnalyzer());
        IndexWriter writer = new IndexWriter(new RAMDirectory(), indexWriterConfig);
        Document doc = new Document();
        doc.add(new TextField("name", "荒岛求生", Field.Store.YES));
        writer.addDocument(doc);
        writer.commit();
        //writer.close();

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(writer, true));
        //QueryParser queryParser = new QueryParser("name", new MaxWordAnalyzer());
        //Query query = queryParser.parse("name:荒野~");
        Term term = new Term("name", "荒野");
        //TermQuery query = new TermQuery(term);
        FuzzyQuery query = new FuzzyQuery(term, 1);
        TopDocs docs = searcher.search(query, 10);
        for (int i = 0; i < docs.totalHits; ++i) {
            System.out.println(searcher.doc(docs.scoreDocs[i].doc));
        }
    }
}
