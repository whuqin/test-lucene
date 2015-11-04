package test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * IndexSearcher的
 * public void search(Query query, Collector results)
 * public void search(Query query, Filter filter, Collector results)
 * Created by qinbin on 2015/10/26.
 */
public class TestCollector {
    Directory dir;

    private void addBook(IndexWriter writer, String title, String sn, float price, int left) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new StringField("sn", sn, Field.Store.YES));
        doc.add(new FloatField("price", price, Field.Store.YES));
        doc.add(new IntField("left", left, Field.Store.YES));
        writer.addDocument(doc);
    }
    @Before
    public void setup() throws IOException {
        dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir,
                new IndexWriterConfig(Version.LUCENE_4_10_0, new StandardAnalyzer()));
        addBook(writer, "lucene in action", "book1", 30.5f, 56);
        addBook(writer, "core python programming", "book2", 40.0f, 300);
        addBook(writer, "c++ programming", "book3", 87f, 210);
        writer.close();
    }

    @Test
    public void testTimeLimitingCollector() throws IOException {
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(dir));
        Query query = new MatchAllDocsQuery();
        TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(10, false);
        Collector collector = new TimeLimitingCollector(topScoreDocCollector, Counter.newCounter(), 1);
        searcher.search(query, collector);
        System.out.println(topScoreDocCollector.getTotalHits());
    }

    @Test
    public void testBookSNCollector() throws IOException {
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(dir));
        TermQuery query = new TermQuery(new Term("title", "programming"));
        BookSNCollector collector = new BookSNCollector();
        searcher.search(query, collector);
        Set<Map.Entry<String, Float>> set = collector.documents.entrySet();
        for (Map.Entry<String, Float> entry : set) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }

    //自定义Collector，对匹配的文档进行定制化操作
    public class BookSNCollector extends Collector {
        private int docBase;
        public Map<String, Float> documents  = new HashMap<String, Float>();
        private Scorer scorer;
        private BinaryDocValues bookSNs;
        private FieldCache.Floats prices;

        @Override
        //初始化时降IndexSearcher的打分策略传给collector，collector可以对当前匹配的文档获得打分
        public void setScorer(Scorer scorer) throws IOException {
            this.scorer = scorer;
        }

        @Override
        //lucene每匹配到一个文档时，通知collector
        public void collect(int doc) throws IOException {
            String sn = bookSNs.get(doc).utf8ToString();
            float price = prices.get(doc);
            documents.put(sn, price);
            System.out.println(sn + ":" + scorer.score());
        }

        @Override
        //通知collector开始遍历新段，每个段对应一个新IndexReader
        public void setNextReader(AtomicReaderContext context) throws IOException {
            docBase = context.docBase;
            bookSNs = FieldCache.DEFAULT.getTerms(context.reader(), "sn", false);
            prices = FieldCache.DEFAULT.getFloats(context.reader(), "price", false);
        }

        @Override
        public boolean acceptsDocsOutOfOrder() {
            return true;
        }
    }

}
