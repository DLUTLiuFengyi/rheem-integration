package io.rheem.tests;

import io.rheem.java.Java;
import io.rheem.giraph.Giraph;
import org.junit.Assert;
import org.junit.Test;
import io.rheem.basic.data.Tuple2;
import io.rheem.core.api.RheemContext;
import io.rheem.core.plan.rheemplan.RheemPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Integration tests for the integration of GraphChi with Rheem.
 */
public class GiraphIntegrationIT {

    //@Test
    //TODO validate if this test is helpfull
    public void testPageRankWithJava() {
        List<Tuple2<Character, Float>> pageRanks = new ArrayList<>();
        RheemPlan rheemPlan = RheemPlans.pageRankWithDictionaryCompression(pageRanks);
        RheemContext rc = new RheemContext().with(Java.basicPlugin()).with(Giraph.plugin());
        rc.execute(rheemPlan);

        pageRanks.stream().forEach(System.out::println);
        this.check(pageRanks);
    }


    @Test
    public void testPageRankWithoutGiraph() {
        List<Tuple2<Character, Float>> pageRanks = new ArrayList<>();
        RheemPlan rheemPlan = RheemPlans.pageRankWithDictionaryCompression(pageRanks);
        RheemContext rc = new RheemContext()
                .with(Java.basicPlugin())
                .with(Java.graphPlugin());
        rc.execute(rheemPlan);

        this.check(pageRanks);
    }

    private void check(List<Tuple2<Character, Float>> pageRanks) {
        final Map<Character, Float> solutions = RheemPlans.pageRankWithDictionaryCompressionSolution();
        Set<Character> vertices = pageRanks.stream().map(Tuple2::getField0).collect(Collectors.toSet());
        solutions.forEach((k, v) -> Assert.assertTrue(String.format("Missing page rank for %s.", k), vertices.contains(k)));
        Assert.assertEquals(String.format("Illegal number of page ranks in %s.", pageRanks), solutions.size(), pageRanks.size());
    }

}
