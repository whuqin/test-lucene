package test.lucene;

import junit.framework.Assert;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Locale;

/**
 * 特殊字符15个：\ + - ! ( ) : ^ [ ] { } ~ * ?
 * QueryParser将文本表达式转换成复杂查询
 * 关键词：
 * + - 包含和排除
 * AND OR NOT 布尔操作符
 * "" phrase
 * ~ int 设置slop,指定松散短语查询，~编辑距离：也可用于创建模糊查询
 * *? wildcard
 * TO [] {} 范围查询
 * ()查询组合
 * ^用于因子加权
 * 提供要匹配的域名,不提供域名则匹配默认域：
 * field:text
 *
 * Created by qinbin on 2015/10/29.
 */
public class TestQueryParser {
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

    private void search(Query query) throws IOException {
        System.out.println("----------" + query.toString() + "----------------");
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(dir));
        TopDocs docs = searcher.search(query, 10);
        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            Document document = searcher.doc(scoreDoc.doc);
            System.out.println(document.get("title") + "," + document.get("sn") + ","
                    + document.get("price") + "," + document.get("left"));
        }
        System.out.println("\n");
    }

    @Test
    public void testBoolean() throws ParseException, IOException {
        QueryParser parser = new QueryParser("title", new StandardAnalyzer());
        Query query = parser.parse("+programming -python");
        search(query);

        query = parser.parse("c++ OR python^10");
        search(query);

        //MatchAllDocsQuery
        query = parser.parse("*:*");
        search(query);

        query = parser.parse("title:programming NOT title:python");
        search(query);
    }

    @Test
    public void testWildcard() throws ParseException, IOException {
        QueryParser parser = new QueryParser("", new StandardAnalyzer());
        Query query = parser.parse("title:Luce*");
        Assert.assertEquals("title:luce*", query.toString());
        search(query);

        query = parser.parse("title:co?e");
        search(query);

        parser.setAllowLeadingWildcard(true);
        query = parser.parse("title:*gramming");
        search(query);
    }

    @Test
    public void testPhrase() throws ParseException, IOException {
        QueryParser parser = new QueryParser("title", new StandardAnalyzer());
        Query query = parser.parse("\"core python\"");
        search(query);

        query = parser.parse("\"core programming\"~1");
        search(query);

        //松散的PhraseQuery不需要按照同样的Term先后顺序进行匹配，SpanNearQuery可以保证Term的先后顺序
        query = parser.parse("\"programming core\"~3");
        search(query);

        query = parser.parse("\"python core\"~2");
        search(query);
    }

    @Test
    public void testSpanNear() throws ParseException, IOException {
        QueryParser parser = new SpanNearQueryParser("title", new StandardAnalyzer());
        Query query = parser.parse("\"python core\"~2");
        search(query);

        query = parser.parse("\"core python\"");
        search(query);
    }

    @Test
    public void testFuzzyQuery() throws ParseException, IOException {
        //默认/最大编辑距离为2
        QueryParser parser = new QueryParser("title", new StandardAnalyzer());
        Query query = parser.parse("program~");
        search(query);

        query = parser.parse("program~4");
        search(query);

        query = parser.parse("programmin~1");
        search(query);
    }


    @Test
    public void  testNumericRangeQuery() throws ParseException, IOException {
        QueryParser parser = new NumericRangeParser("", null);
        Query query = parser.parse("left:[56 TO 210}");
        search(query);
    }

    @Test
    public void testDateRangeQuery() throws ParseException {
        String expression = "publish:[01/01/2010 TO 06/01/2010]";
        QueryParser parser = new NumericDateRangeQueryParser("", new StandardAnalyzer());
        parser.setDateResolution("publish", DateTools.Resolution.DAY);
        parser.setLocale(Locale.US);
        Query query = parser.parse(expression);

        System.out.println(query.toString());
    }

    public class NumericRangeParser extends QueryParser {
        public NumericRangeParser(String f, Analyzer a) {
            super(f, a);
        }
        @Override
        protected Query getRangeQuery(String field, String part1, String part2,
                                      boolean startInclusive, boolean endInclusive) throws ParseException {
            NumericRangeQuery<Integer> rangeQuery = NumericRangeQuery.newIntRange(
                    field, Integer.valueOf(part1), Integer.valueOf(part2), startInclusive, endInclusive);
            return rangeQuery;
        }
    }

    public class NumericDateRangeQueryParser extends QueryParser {

        public NumericDateRangeQueryParser(String f, Analyzer a) {
            super(f, a);
        }

        @Override
        protected Query getRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) throws ParseException {
            TermRangeQuery query = (TermRangeQuery)super.getRangeQuery(field, part1, part2, startInclusive, endInclusive);
            if ("publish".equals(field)) {
                return NumericRangeQuery.newIntRange("publish",
                        Integer.parseInt(query.getLowerTerm().utf8ToString()),
                        Integer.parseInt(query.getUpperTerm().utf8ToString()),
                        startInclusive, endInclusive);
            } else {
                return query;
            }
        }
    }

    public class SpanNearQueryParser extends QueryParser {

        public SpanNearQueryParser(String f, Analyzer a) {
            super(f, a);
        }

        @Override
        protected Query getFieldQuery(String field, String queryText, int slop) throws ParseException {
            Query query = super.getFieldQuery(field, queryText, slop);
            if (!(query instanceof PhraseQuery)) {
                return query;
            }
            PhraseQuery pq = (PhraseQuery) query;
            Term[] terms = pq.getTerms();
            SpanTermQuery[] clauses = new SpanTermQuery[terms.length];
            for (int i = 0; i < terms.length; ++i) {
                clauses[i] = new SpanTermQuery(terms[i]);
            }
            SpanNearQuery spanNearQuery = new SpanNearQuery(clauses, slop, true);
            return spanNearQuery;
        }
    }

}
