package test.lucene;

/**
 * Created by qinbin on 2015/1/5.
 */

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.*;
import org.apache.lucene.facet.DrillSideways.DrillSidewaysResult;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.List;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Shows simple usage of faceted indexing and search. */
public class SimpleFacetsExample {
    private final Directory indexDir = new RAMDirectory();
    private final Directory taxoDir = new RAMDirectory();
    private final FacetsConfig config = new FacetsConfig();

    /** Empty constructor */
    public SimpleFacetsExample() {
        config.setHierarchical("Publish Date", true);
    }

    /** Build the example index. */
    private void index() throws IOException {
        IndexWriter indexWriter = new IndexWriter(indexDir, new IndexWriterConfig(Version.LUCENE_4_10_0,
                new WhitespaceAnalyzer()));
        // Writes facet ords to a separate directory from the main index
        DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir);

        Document doc = new Document();
        doc.add(new TextField("device", "手机", Field.Store.YES));
        doc.add(new TextField("name", "米1", Field.Store.YES));
        doc.add(new FacetField("brand", "小米"));
        doc.add(new FacetField("network", "移动4G"));
        indexWriter.addDocument(config.build(taxoWriter, doc));

        doc = new Document();
        doc.add(new TextField("device", "手机", Field.Store.YES));
        doc.add(new TextField("name", "米4", Field.Store.YES));
        doc.add(new FacetField("brand", "小米"));
        doc.add(new FacetField("network", "联通4G"));
        indexWriter.addDocument(config.build(taxoWriter, doc));

        doc = new Document();
        doc.add(new TextField("device", "手机", Field.Store.YES));
        doc.add(new TextField("name", "荣耀6", Field.Store.YES));
        doc.add(new FacetField("brand", "华为"));
        doc.add(new FacetField("network", "移动4G"));
        indexWriter.addDocument(config.build(taxoWriter, doc));

        doc = new Document();
        doc.add(new TextField("device", "电视", Field.Store.YES));
        doc.add(new TextField("name", "小米电视2", Field.Store.YES));
        doc.add(new FacetField("brand", "小米"));
        indexWriter.addDocument(config.build(taxoWriter, doc));

        taxoWriter.close();
        indexWriter.close();
    }

    private void facetsWithSearch() throws IOException {
        DirectoryReader indexReader = DirectoryReader.open(indexDir);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);

        FacetsCollector fc = new FacetsCollector();
        //1.查询手机
        System.out.println("-----手机-----");
        TermQuery query = new TermQuery(new Term("device", "手机"));
        FacetsCollector.search(searcher, query, 10, fc);
        Facets facets = new FastTaxonomyFacetCounts(taxoReader, config, fc);
        List<FacetResult> results = facets.getAllDims(10);
        //手机总共有3个,品牌维度：小米2个，华为1个;网络维度：移动4G 2个，联通4G 1个
        for (FacetResult tmp : results) {
            System.out.println(tmp);
        }
        //2.drill down，品牌选小米
        System.out.println("-----小米手机-----");
        DrillDownQuery drillDownQuery = new DrillDownQuery(config, query);
        drillDownQuery.add("brand", "小米");
        FacetsCollector fc1 = new FacetsCollector();//要new新collector，否则会累加
        FacetsCollector.search(searcher, drillDownQuery, 10, fc1);
        facets = new FastTaxonomyFacetCounts(taxoReader, config, fc1);
        results = facets.getAllDims(10);
        //获得小米手机的分布，总数2个，网络：移动4G 1个，联通4G 1个
        for (FacetResult tmp : results) {
            System.out.println(tmp);
        }

        //3.drill down，小米移动4G手机
        System.out.println("-----移动4G小米手机-----");
        drillDownQuery.add("network", "移动4G");
        FacetsCollector fc2 = new FacetsCollector();
        FacetsCollector.search(searcher, drillDownQuery, 10, fc2);
        facets = new FastTaxonomyFacetCounts(taxoReader, config, fc2);
        results = facets.getAllDims(10);
        for (FacetResult tmp : results) {
            System.out.println(tmp);
        }

        //4.drill sideways，横向浏览
        //如果已经进入了小米手机，但是还想看到其他牌子(华为)的手机数目，就用到了sideways
        System.out.println("-----小米手机drill sideways-----");
        DrillSideways ds = new DrillSideways(searcher, config, taxoReader);
        DrillDownQuery drillDownQuery1 = new DrillDownQuery(config, query);
        drillDownQuery1.add("brand", "小米");
        DrillSidewaysResult result = ds.search(drillDownQuery1, 10);
        results = result.facets.getAllDims(10);
        for (FacetResult tmp : results) {
            System.out.println(tmp);
        }

        indexReader.close();
        taxoReader.close();
    }

    /** Runs the search and drill-down examples and prints the results. */
    public static void main(String[] args) throws Exception {
        SimpleFacetsExample example = new SimpleFacetsExample();
        example.index();
        example.facetsWithSearch();
    }
}
