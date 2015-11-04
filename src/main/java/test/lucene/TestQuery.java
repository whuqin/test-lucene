package test.lucene;

import com.chenlb.mmseg4j.analysis.MaxWordAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

/**
 * Created by qinbin on 2014/10/21.
 */
public class TestQuery {
    @Test
    public void testRange() {
        NumericRangeQuery<Double> rangeQuery = NumericRangeQuery.newDoubleRange("test", null, 1234.45, true, true);
        System.out.println(rangeQuery.toString());
    }
//
//    @Test
//    public void test() throws ParseException {
//        PhraseQuery query = new PhraseQuery();
//        query.add(new Term("bookname", "god"));
//        query.add(new Term("bookname", "helps"));
//        query.setSlop(1);
//        query.setBoost(2);
//        System.out.println(query.toString());
//        //queryParser
//        QueryParser parser = new QueryParser(Version.LUCENE_30, "bookname", new StandardAnalyzer(Version.LUCENE_30));
//        String str = "\"god helps\"~1";
//        Query q = parser.parse(str);
//        System.out.println(q);
//
//    }
//
//    @Test
//    public void testMaxWordAnalyzer() throws IOException, ParseException {
//        //System.setProperty("mmseg.dic.path", "E:\\workspace\\TestLucene\\data");
//        MaxWordAnalyzer analyzer = new MaxWordAnalyzer();
//        /*QueryParser qp = new QueryParser(Version.LUCENE_30, "txt", analyzer);
//        Query q = qp.parse("这是一个测试的实例中华人民共和国"); //2008年底
//        System.out.println(q);*/
//
//        TokenStream stream = analyzer.tokenStream("", new StringReader("Hello, this is a test case. " +
//               "你好，这是一个测试的实例。" +  "created on 20140707"));
//
//        String out = "";
//        while (stream.incrementToken()) {
//            out += "[" + stream.getAttribute(TermAttribute.class).term() + "]";
//        }
//        System.out.println(out);
//    }
}
