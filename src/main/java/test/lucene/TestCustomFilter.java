package test.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by qinbin on 2015/11/9.
 */
public class TestCustomFilter {
    private Directory dir;

    private void addBook(IndexWriter writer, String title, String sn, float price, int left) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new StringField("isbn", sn, Field.Store.YES));
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
    //FilteredQuery在每次调用时都会调用Filter.getDocIdSet,所以在过滤器不变的情况下使用过滤器缓存CachingWrapperFilter
    //FilteredQuery是将一个过滤条件特用于某个Query，而不是最终的结果
    public void testFilteredQuery() throws IOException {
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(dir));
        Filter filter = new SpecialsFilter(new SpecialsAccessor());
        CachingWrapperFilter cachingWrapperFilter = new CachingWrapperFilter(filter);
        WildcardQuery wildcardQuery = new WildcardQuery(new Term("title", "luce*"));
        FilteredQuery filteredQuery = new FilteredQuery(wildcardQuery, cachingWrapperFilter);

        TermQuery termQuery = new TermQuery(new Term("title", "core"));

        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(filteredQuery, BooleanClause.Occur.SHOULD);
        booleanQuery.add(termQuery, BooleanClause.Occur.SHOULD);

        TopDocs hits = searcher.search(booleanQuery, 10);
        for (ScoreDoc doc : hits.scoreDocs){
            Document document = searcher.doc(doc.doc);
            System.out.println(document.get("title") + "," + document.get("isbn") + ":" + doc.score);
        }
    }

    @Test
    public void testCustomFilter() throws IOException {
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(dir));
        SpecialsAccessor accessor = new SpecialsAccessor();
        Filter filter = new SpecialsFilter(accessor);
        TopDocs docs = searcher.search(new MatchAllDocsQuery(), filter, 10);
        for (ScoreDoc doc : docs.scoreDocs){
            Document document = searcher.doc(doc.doc);
            System.out.println(document.get("title") + "," + document.get("isbn"));
        }
    }

    //自定义Filter
    public class SpecialsFilter extends Filter {
        private SpecialsAccessor accessor;
        public SpecialsFilter(SpecialsAccessor accessor) {
            this.accessor = accessor;
        }

        @Override
        public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
            AtomicReader reader = context.reader();
            OpenBitSet bits = new OpenBitSet(reader.maxDoc());
            String[] isbns = accessor.isbns();
            for (String isbn : isbns) {
                DocsEnum termDocs = reader.termDocsEnum(new Term("isbn", isbn));
                if (null == termDocs) {
                    continue;
                }
                int docId = termDocs.nextDoc();
                while (docId != DocsEnum.NO_MORE_DOCS) {
                    bits.set(docId);
                    docId = termDocs.nextDoc();
                }
            }
            return bits;
        }
    }
    //返回特价书籍的isbn号
    public class SpecialsAccessor {
        String[] isbns() {
            return new String[] {"book1", "book3"};
        };
    }
}
